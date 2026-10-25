package com.rongia.basedata.domain.material;

import com.rongia.shared.domain.event.DomainEvent;
import java.util.List;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 物料保存事件（新增或修改） */
@NullMarked
@Getter
public class MaterialSavedEvent extends DomainEvent {

  /** 附件Id集合（可空） */
  @Nullable private final List<Long> attachmentIds;

  /**
   * 构造器
   *
   * @param material 物料
   * @param attachmentIds 附件Id集合（可空）
   */
  public MaterialSavedEvent(Material material, @Nullable List<Long> attachmentIds) {
    super(material);
    this.attachmentIds = attachmentIds;
  }
}
