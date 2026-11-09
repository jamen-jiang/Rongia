package com.rongia.shared.request;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;

/** 分页请求 */
@NullMarked
@Getter
@Setter
public class PageRequest {

  /** 当前页码 */
  private Integer pageIndex = 1;

  /** 每页大小 */
  private Integer pageSize = 10;
}
