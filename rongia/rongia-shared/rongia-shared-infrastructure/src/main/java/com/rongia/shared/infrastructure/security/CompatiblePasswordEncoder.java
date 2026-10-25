package com.rongia.shared.infrastructure.security;

import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 兼容明文的密码编码器
 *
 * <p>数据库中旧数据为明文时能校验通过；新密码统一用 BCrypt 加密
 */
public class CompatiblePasswordEncoder implements PasswordEncoder {

  /** BCrypt 编码器 */
  private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

  /**
   * 加密密码，统一用 BCrypt
   *
   * @param rawPassword 明文密码
   * @return BCrypt 加密后的密码
   */
  @Override
  public String encode(CharSequence rawPassword) {
    return bcrypt.encode(rawPassword);
  }

  /**
   * 校验密码；支持 BCrypt 和明文两种格式
   *
   * @param rawPassword 明文密码
   * @param encodedPassword 数据库中存储的密码（可空）
   * @return true 表示匹配
   */
  @Override
  public boolean matches(CharSequence rawPassword, @Nullable String encodedPassword) {
    if (encodedPassword == null || encodedPassword.isEmpty()) {
      return false;
    }
    // BCrypt 格式：$2a$ / $2b$ / $2y$
    if (isBCrypt(encodedPassword)) {
      return bcrypt.matches(rawPassword, encodedPassword);
    }
    // 非 BCrypt 格式一律当明文比对
    return encodedPassword.contentEquals(rawPassword);
  }

  /**
   * 判断是否为明文（非 BCrypt 格式）
   *
   * @param encodedPassword 数据库中存储的密码（可空）
   * @return true 表示明文
   */
  public boolean isPlaintext(@Nullable String encodedPassword) {
    return encodedPassword != null && !encodedPassword.isEmpty() && !isBCrypt(encodedPassword);
  }

  /**
   * 判断是否为 BCrypt 格式
   *
   * @param encodedPassword 数据库中存储的密码
   * @return true 表示 BCrypt 格式
   */
  private boolean isBCrypt(String encodedPassword) {
    return encodedPassword.startsWith("$2a$")
        || encodedPassword.startsWith("$2b$")
        || encodedPassword.startsWith("$2y$");
  }
}
