package com.rongia.framework.dev;

import java.awt.*;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 本地开发时，应用启动完成后自动打开 API 文档
 *
 * <p>属于应用装配层的开发便利功能，放在 start 模块而非 shared-infrastructure：它依赖
 * server.port / context-path / 文档地址等部署配置，且带有桌面环境副作用，不应污染被各业务
 * 上下文共用的公共库
 */
@Slf4j
@Component
@Profile("local")
@ConditionalOnProperty(
    prefix = "rongia.dev",
    name = "open-browser",
    havingValue = "true",
    matchIfMissing = false)
public class DevBrowserLauncher {

  /** 环境 */
  private final Environment environment;

  /**
   * 构造器注入
   *
   * @param environment 环境
   */
  public DevBrowserLauncher(Environment environment) {
    this.environment = environment;
  }

  /** 应用启动完成后打开文档 */
  @EventListener(ApplicationReadyEvent.class)
  public void openBrowser() {
    String port = environment.getProperty("server.port", "8080");
    String contextPath = environment.getProperty("server.servlet.context-path", "");
    String docPath = environment.getProperty("rongia.dev.doc-path", "/doc.html");
    String url = "http://localhost:" + port + contextPath + docPath;

    try {
      if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(new URI(url));
      } else {
        // 非桌面环境，用系统命令兜底
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;
        if (os.contains("win")) {
          pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
        } else if (os.contains("mac")) {
          pb = new ProcessBuilder("open", url);
        } else {
          pb = new ProcessBuilder("xdg-open", url);
        }
        pb.start();
      }
      log.info("已打开 API 文档: {}", url);
    } catch (Exception e) {
      log.warn("打开浏览器失败，请手动访问: {}", url, e);
    }
  }
}
