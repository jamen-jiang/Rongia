package com.rongia.shared.domain.exception;

import org.jspecify.annotations.NullMarked;

/** 领域异常，业务规则违反时抛出 */
@NullMarked
public class DomainException extends RuntimeException {

  /**
   * 构造器
   *
   * @param message 异常信息
   */
  public DomainException(String message) {
    super(message);
  }
}
