package com.rongia.basedata.domain.material;

import com.rongia.shared.domain.event.DomainEvent;
import org.jspecify.annotations.NullMarked;

/** 物料删除事件 */
@NullMarked
public class MaterialDeletedEvent extends DomainEvent {

  /**
   * 构造器
   *
   * @param material 物料
   */
  public MaterialDeletedEvent(Material material) {
    super(material);
  }
}
