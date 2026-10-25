package com.rongia.shared.infrastructure.websocket;

import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 通知推送服务。
 *
 * <p>本项目只做服务端 → 客户端推送，所有推送都通过本类。
 *
 * <p>对应前端订阅：
 *
 * <ul>
 *   <li>Notify → 前端订阅 /user/queue/notification.notify
 *   <li>TaskDone → 前端订阅 /user/queue/notification.taskDone
 * </ul>
 *
 * <p>注意：convertAndSendToUser 的目的地不带 /user 前缀，Spring 自动补。
 */
@Service
public class NotificationPushService {

  /** 推送通知的目的地（不带 /user） */
  private static final String DEST_NOTIFY = "/queue/notification.notify";

  /** 推送任务结果的目的地（不带 /user） */
  private static final String DEST_TASK_DONE = "/queue/notification.taskDone";

  private final SimpMessagingTemplate messaging;

  public NotificationPushService(SimpMessagingTemplate messaging) {
    this.messaging = messaging;
  }

  /**
   * 推送通知给指定用户。
   *
   * @param userName 目标用户账号，必须与 UserPrincipal#getName() 一致
   * @param type 通知类型：Message（顶部 toast）或 Log（日志面板）
   * @param content 提示内容
   * @param success 是否成功
   */
  public void pushNotify(String userName, String type, String content, boolean success) {
    messaging.convertAndSendToUser(
        userName,
        DEST_NOTIFY,
        Map.of(
            "type",
            type,
            "data",
            Map.of("content", content, "status", success ? "Success" : "Error")));
  }

  /** 推送顶部消息通知（前端弹 toast）。 */
  public void pushMessage(String userName, String content, boolean success) {
    pushNotify(userName, "Message", content, success);
  }

  /** 推送日志型通知（前端追加到日志面板）。 */
  public void pushLog(String userName, String content, boolean success) {
    pushNotify(userName, "Log", content, success);
  }

  /**
   * 推送任务完成结果。
   *
   * @param userName 目标用户账号
   * @param jobId 任务Id，前端按此匹配
   */
  public void pushTaskDone(String userName, String jobId) {
    messaging.convertAndSendToUser(userName, DEST_TASK_DONE, Map.of("jobId", jobId));
  }
}
