package com.rongia.system.domain.signinlog;

import com.rongia.shared.domain.aggregate.LogAggregateRoot;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 登录日志 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "SignInLog",
    indexes = {
      @Index(name = "IX_SignInLog_Type", columnList = "[Type]"),
      @Index(name = "IX_SignInLog_UserCode", columnList = "UserCode")
    })
public class SignInLog extends LogAggregateRoot {

  /** 登录类型编码，取值见 {@link SignEntryTypeEnum} */
  @Nationalized
  @Column(name = "Type", nullable = false, length = 50)
  private String type;

  /** 登录账号（可空） */
  @Nationalized
  @Column(name = "UserCode", length = 50)
  @Nullable
  private String userCode;

  /** 令牌（可空） */
  @Nationalized
  @Column(name = "Token", length = 500)
  @Nullable
  private String token;

  /** 刷新令牌（可空） */
  @Nationalized
  @Column(name = "RefreshToken", length = 500)
  @Nullable
  private String refreshToken;

  /** 旧刷新令牌（可空） */
  @Nationalized
  @Column(name = "OldRefreshToken", length = 500)
  @Nullable
  private String oldRefreshToken;

  /** IP地址（可空） */
  @Nationalized
  @Column(name = "IP", length = 200)
  @Nullable
  private String ip;

  /** 客户端地点（可空） */
  @Nationalized
  @Column(name = "City", length = 50)
  @Nullable
  private String city;

  /** 客户端 UserAgent（可空） */
  @Nationalized
  @Column(name = "UserAgent", length = 500)
  @Nullable
  private String userAgent;

  /** 浏览器（可空） */
  @Nationalized
  @Column(name = "Browser", length = 200)
  @Nullable
  private String browser;

  /** 操作系统（可空） */
  @Nationalized
  @Column(name = "Os", length = 200)
  @Nullable
  private String os;

  /**
   * 构造器
   *
   * @param type 登录类型
   * @param userId 用户Id（可空）
   * @param userCode 登录账号（可空）
   * @param userName 用户名称（可空）
   * @param startedOn 开始时间
   * @param endedOn 结束时间
   * @param elapsedMilliseconds 耗时（毫秒）
   * @param token 令牌（可空）
   * @param refreshToken 刷新令牌（可空）
   * @param oldRefreshToken 旧刷新令牌（可空）
   * @param ip IP地址（可空）
   * @param city 客户端地点（可空）
   * @param userAgent 客户端 UserAgent（可空）
   * @param browser 浏览器（可空）
   * @param os 操作系统（可空）
   * @param isSuccess 是否成功
   * @param errorMessage 错误信息（可空）
   * @param exception 异常信息（可空）
   * @throws DomainException 登录类型为空或不在枚举范围内时抛出
   */
  public SignInLog(
      String type,
      @Nullable Long userId,
      @Nullable String userCode,
      @Nullable String userName,
      LocalDateTime startedOn,
      LocalDateTime endedOn,
      long elapsedMilliseconds,
      @Nullable String token,
      @Nullable String refreshToken,
      @Nullable String oldRefreshToken,
      @Nullable String ip,
      @Nullable String city,
      @Nullable String userAgent,
      @Nullable String browser,
      @Nullable String os,
      boolean isSuccess,
      @Nullable String errorMessage,
      @Nullable String exception) {
    super(
        userId,
        StringUtils.trimToNull(userName),
        startedOn,
        endedOn,
        elapsedMilliseconds,
        null,
        null,
        isSuccess,
        errorMessage,
        exception);
    this.type = EnumUtils.getCode(SignEntryTypeEnum.class, type, "登录类型");
    this.userCode = StringUtils.trimToNull(userCode);
    this.token = StringUtils.trimToNull(token);
    this.refreshToken = StringUtils.trimToNull(refreshToken);
    this.oldRefreshToken = StringUtils.trimToNull(oldRefreshToken);
    this.ip = StringUtils.trimToNull(ip);
    this.city = StringUtils.trimToNull(city);
    this.userAgent = StringUtils.trimToNull(userAgent);
    this.browser = StringUtils.trimToNull(browser);
    this.os = StringUtils.trimToNull(os);
  }
}
