package com.rongia.shared.infrastructure.core.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * 自定义 Logback Appender，异步将日志写入 SQL Server。
 *
 * <p>业务线程只负责将日志事件入队，独立的后台线程负责批量写库，避免阻塞业务。 匹配表结构：Level, Message, MessageTemplate, Exception,
 * Properties, Timestamp
 *
 * <p>队列满时丢弃日志，只警告一次，保证业务线程不因日志阻塞。
 *
 * @author RONGIA
 */
@Setter
@SuppressWarnings("unused")
public class SqlServerLogAppender extends AppenderBase<ILoggingEvent> {

  // ==================== 配置属性（由 logback-spring.xml 注入） ====================

  /** JDBC URL */
  private String url;

  /** 数据库用户名 */
  private String user;

  /** 数据库密码 */
  private String password;

  /** 表名，默认 SystemLog */
  private String tableName = "SystemLog";

  /** 队列容量，默认 8192 */
  private int queueSize = 8192;

  /** 批量提交大小，默认 100 */
  private int batchSize = 100;

  /** 队列为空时，消费线程的等待时间（毫秒），默认 500 */
  private long pollTimeoutMs = 500L;

  // ==================== 运行时组件 ====================

  /** 日志事件队列，业务线程入队、消费线程出队 */
  private LinkedBlockingQueue<ILoggingEvent> queue;

  /** 日志库连接池 */
  private HikariDataSource dataSource;

  /** 消费线程 */
  private Thread worker;

  /** 运行标志，控制消费线程退出 */
  private final AtomicBoolean running = new AtomicBoolean(false);

  /** 丢日志警告标志，避免日志风暴时反复打警告 */
  private final AtomicBoolean droppingWarningLogged = new AtomicBoolean(false);

  // ==================== 生命周期 ====================

  /** Appender 启动：初始化队列、连接池，启动消费线程 */
  @Override
  public void start() {
    // 0. 参数兜底，避免 logback 配置漏项或配错导致启动失败
    if (queueSize <= 0) {
      addWarn("queueSize 配置非法(" + queueSize + ")，回退为默认值 10000");
      queueSize = 10000;
    }
    if (batchSize <= 0) {
      addWarn("batchSize 配置非法(" + batchSize + ")，回退为默认值 100");
      batchSize = 100;
    }
    if (pollTimeoutMs <= 0) {
      addWarn("pollTimeoutMs 配置非法(" + pollTimeoutMs + ")，回退为默认值 1000");
      pollTimeoutMs = 1000L;
    }
    if (tableName == null || tableName.isBlank()) {
      addWarn("tableName 未配置，回退为默认值 LogTable");
      tableName = "LogTable";
    }

    // 1. 初始化队列
    this.queue = new LinkedBlockingQueue<>(queueSize);

    // 2. 初始化连接池
    try {
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(url);
      config.setUsername(user);
      config.setPassword(password);
      config.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
      config.setMaximumPoolSize(5);
      config.setMinimumIdle(1);
      config.setPoolName("Logback-HikariPool");
      config.setConnectionTimeout(3000);
      this.dataSource = new HikariDataSource(config);
    } catch (Exception e) {
      addError("初始化日志数据库连接池失败，Appender 将不会启动", e);
      return;
    }

    // 3. 启动消费线程
    this.running.set(true);
    this.worker = new Thread(this::consumeLoop, "Logback-DB-Worker");
    this.worker.setDaemon(true);
    this.worker.start();

    super.start();
    addInfo("SqlServerLogAppender 启动成功，表名: " + tableName);
  }

  /** Appender 停止：停消费线程、关连接池 */
  @Override
  public void stop() {
    // 1. 停止接收新日志
    this.running.set(false);

    // 2. 等消费线程把队列里的日志刷完（最多等 5 秒）
    if (worker != null) {
      try {
        worker.join(5000);
        if (worker.isAlive()) {
          addWarn("消费线程 5 秒内未退出，队列中可能仍有未落库日志，将强制关闭连接池");
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    // 3. 关连接池
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }

    super.stop();
    addInfo("SqlServerLogAppender 已停止");
  }

  // ==================== 核心写入逻辑 ====================

  /**
   * 业务线程调用：把日志事件入队
   *
   * @param event 日志事件
   */
  @Override
  protected void append(ILoggingEvent event) {
    if (!running.get() || queue == null) {
      return;
    }
    // offer 是非阻塞的：队列满了直接丢弃，避免拖慢业务线程
    if (queue.offer(event)) {
      // 入队成功说明队列已不满，重置警告标志，下次再满还能告警
      droppingWarningLogged.set(false);
    } else if (droppingWarningLogged.compareAndSet(false, true)) {
      addWarn("日志队列已满(" + queueSize + ")，新日志将被丢弃！请检查数据库写入性能或调大队列。");
    }
  }

  /** 消费线程的主循环：批量取日志，批量插入 */
  private void consumeLoop() {
    List<ILoggingEvent> batch = new ArrayList<>(batchSize);

    while (running.get() || !queue.isEmpty()) {
      try {
        ILoggingEvent first = queue.poll(pollTimeoutMs, TimeUnit.MILLISECONDS);
        if (first == null) {
          continue;
        }
        batch.add(first);
        queue.drainTo(batch, batchSize - 1);
        flushBatch(batch);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } finally {
        batch.clear();
      }
    }

    // 退出前把队列剩余的日志全部刷掉
    List<ILoggingEvent> remaining = new ArrayList<>();
    queue.drainTo(remaining);
    if (!remaining.isEmpty()) {
      flushBatch(remaining);
    }
  }

  /**
   * 批量插入到 SQL Server
   *
   * @param batch 待写入的日志事件列表
   */
  private void flushBatch(List<ILoggingEvent> batch) {
    if (batch.isEmpty()) {
      return;
    }
    String sql =
        "INSERT INTO ["
            + tableName
            + "] ([Level], [Message], [MessageTemplate], [Exception], [Properties], [Timestamp]) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

    try (Connection conn = dataSource.getConnection()) {
      conn.setAutoCommit(false);
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        for (ILoggingEvent event : batch) {
          stmt.setString(1, event.getLevel().toString());
          stmt.setString(2, event.getFormattedMessage());
          stmt.setString(3, event.getMessage());
          stmt.setString(4, getExceptionString(event));
          stmt.setString(5, getPropertiesXml(event));
          stmt.setTimestamp(6, new Timestamp(event.getTimeStamp()));
          stmt.addBatch();
        }
        stmt.executeBatch();
        conn.commit();
      } catch (Exception e) {
        try {
          conn.rollback();
        } catch (Exception ignored) {
          // rollback 失败忽略，连接关闭时仍会隐式回滚
        }
        throw e;
      }
    } catch (Exception e) {
      addError("批量写入 SQL Server 失败，本批次日志数量: " + batch.size(), e);
    }
  }

  // ==================== 字段提取工具方法 ====================

  /**
   * 提取异常堆栈字符串
   *
   * @param event 日志事件
   * @return 异常堆栈字符串，无异常返回 null
   */
  @Nullable
  private String getExceptionString(ILoggingEvent event) {
    if (event.getThrowableProxy() == null) {
      return null;
    }
    return ThrowableProxyUtil.asString(event.getThrowableProxy());
  }

  /**
   * 提取 MDC 属性，拼成 XML 字符串
   *
   * @param event 日志事件
   * @return XML 字符串，无属性返回 null
   */
  @Nullable
  private String getPropertiesXml(ILoggingEvent event) {
    Map<String, String> mdc = event.getMDCPropertyMap();
    if (mdc == null || mdc.isEmpty()) {
      return null;
    }
    StringBuilder sb = new StringBuilder("<properties>");
    mdc.forEach(
        (k, v) ->
            sb.append("<property key=\"")
                .append(escapeXml(k))
                .append("\">")
                .append(escapeXml(v))
                .append("</property>"));
    sb.append("</properties>");
    return sb.toString();
  }

  /**
   * 简单 XML 转义，防止 MDC 值破坏 XML 结构
   *
   * @param s 原始字符串
   * @return 转义后的字符串
   */
  private static String escapeXml(@Nullable String s) {
    if (s == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '&' -> sb.append("&amp;");
        case '<' -> sb.append("&lt;");
        case '>' -> sb.append("&gt;");
        case '"' -> sb.append("&quot;");
        case '\'' -> sb.append("&apos;");
        default -> sb.append(c);
      }
    }
    return sb.toString();
  }
}
