package com.rongia.system.domain.serialnumber;

import com.rongia.kernel.domain.exception.DomainException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 流水号领域服务，负责取号编排（计数消费 + 模板渲染）
 *
 * <p>取号流程（对齐 Odoo {@code ir_sequence} 的 {@code next_by_code}）：
 *
 * <ol>
 *   <li>按类型找规则，没配直接报错
 *   <li>悲观锁行消费计数：当前事务推进（回滚归还号，单号严格连续无缺口；规则配了重置周期时跨期归 1）
 *   <li>用前缀/后缀模板渲染：{@code %(year)s}、{@code %(month)s}、{@code %(day)s}、{@code %(h24)s}、
 *       {@code %(min)s}、{@code %(sec)s}、{@code %(doy)s}（年内第几天）、{@code %(woy)s}（年内第几周），
 *       中间补零到 {@code pad} 位
 * </ol>
 *
 * <p>取号发生在单据落库的事务里（领域事件监听器调用），因此行锁会持到业务事务提交，
 * 并发取同一类型天然串行——保证号严格连续无缺口；代价是并发高的类型会在锁上排队。
 */
@NullMarked
public class SerialNumberRuleDomainService {

  /** 日期插值模板，形如 {@code %(year)s} */
  private static final Pattern TOKEN = Pattern.compile("%\\((\\w+)\\)s");

  /** 流水号规则仓储 */
  private final SerialNumberRuleRepository ruleRepository;

  /**
   * 构造器注入
   *
   * @param ruleRepository 流水号规则仓储
   */
  public SerialNumberRuleDomainService(SerialNumberRuleRepository ruleRepository) {
    this.ruleRepository = ruleRepository;
  }

  /**
   * 按类型取下一个流水号
   *
   * @param type 规则类型（对应流水号类型编码，如 SalesOrder）
   * @return 渲染好的流水号，如 {@code SO2026-0001}
   * @throws DomainException 规则不存在或模板变量不支持时抛出
   */
  public String next(String type) {
    String trimmedType = StringUtils.trimToEmpty(type);
    SerialNumberRule rule = ruleRepository.getByType(trimmedType);
    if (rule == null) {
      throw new DomainException("流水号规则" + trimmedType + "不存在!");
    }

    LocalDateTime now = LocalDateTime.now();
    int number = consumeRuleCounter(rule, now);
    return format(rule, number, now);
  }

  // ==================== 私有：计数消费 ====================

  /**
   * 消费规则上的计数
   *
   * <p>悲观锁当前事务推进，回滚归还号，锁持到业务事务提交；规则配了重置周期且跨期时，计数归 1。
   *
   * @param rule 规则
   * @param now 本次取号时间（重置周期判定用）
   * @return 本次使用的号
   */
  private int consumeRuleCounter(SerialNumberRule rule, LocalDateTime now) {
    // 悲观锁当前事务推进，回滚归还号，锁持到业务事务提交
    SerialNumberRule locked = ruleRepository.getByTypeForUpdate(rule.getType());
    if (locked == null) {
      throw new DomainException("流水号规则" + rule.getType() + "不存在!");
    }
    int number = locked.advance(now);
    ruleRepository.save(locked);
    return number;
  }

  // ==================== 私有：模板渲染 ====================

  /**
   * 渲染流水号：前缀 + 补零的号 + 后缀
   *
   * @param rule 规则
   * @param number 本次使用的号
   * @param now 当前时间（插值用）
   * @return 流水号
   */
  private String format(SerialNumberRule rule, int number, LocalDateTime now) {
    String digits =
        rule.getPad() > 0
            ? String.format("%0" + rule.getPad() + "d", number)
            : String.valueOf(number);
    return interpolate(rule.getPrefix(), now) + digits + interpolate(rule.getSuffix(), now);
  }

  /**
   * 日期插值：把模板里的 {@code %(xxx)s} 换成当前时间对应的值
   *
   * <p>支持 year / month / day / h24 / min / sec / doy / woy 八个变量（Odoo 同名变量，
   * {@code woy} 按 ISO-8601 周计算，周一为一周开始）。
   *
   * @param template 模板（可空）
   * @param now 当前时间
   * @return 渲染结果，模板为空返回空串
   * @throws DomainException 变量不在支持列表时抛出
   */
  private String interpolate(@Nullable String template, LocalDateTime now) {
    if (StringUtils.isBlank(template)) {
      return "";
    }
    Matcher matcher = TOKEN.matcher(template);
    StringBuilder rendered = new StringBuilder();
    while (matcher.find()) {
      String token = matcher.group(1);
      matcher.appendReplacement(
          rendered, Matcher.quoteReplacement(now.format(formatterOf(token))));
    }
    matcher.appendTail(rendered);
    return rendered.toString();
  }

  /**
   * 模板变量对应的时间格式
   *
   * @param token 变量名
   * @return 时间格式器（Locale.ROOT，保证周数等与地区无关）
   * @throws DomainException 变量不在支持列表时抛出
   */
  private DateTimeFormatter formatterOf(String token) {
    return switch (token) {
      case "year" -> DateTimeFormatter.ofPattern("yyyy", Locale.ROOT);
      case "month" -> DateTimeFormatter.ofPattern("MM", Locale.ROOT);
      case "day" -> DateTimeFormatter.ofPattern("dd", Locale.ROOT);
      case "h24" -> DateTimeFormatter.ofPattern("HH", Locale.ROOT);
      case "min" -> DateTimeFormatter.ofPattern("mm", Locale.ROOT);
      case "sec" -> DateTimeFormatter.ofPattern("ss", Locale.ROOT);
      case "doy" -> DateTimeFormatter.ofPattern("DDD", Locale.ROOT);
      case "woy" -> DateTimeFormatter.ofPattern("ww", Locale.ROOT);
      default ->
          throw new DomainException(
              "流水号模板的日期变量%("
                  + token
                  + ")s不支持，可用：%(year)s、%(month)s、%(day)s、%(h24)s、%(min)s、%(sec)s、%(doy)s、%(woy)s");
    };
  }
}
