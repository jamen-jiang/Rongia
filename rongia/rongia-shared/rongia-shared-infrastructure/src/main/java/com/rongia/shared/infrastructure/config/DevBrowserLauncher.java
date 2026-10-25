package com.rongia.shared.infrastructure.config;

import java.awt.*;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** 启动后自动打开 API 文档 */
@Slf4j
@Component
@Profile("local")
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
    String url = "http://localhost:" + port + contextPath + "/doc.html";

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
