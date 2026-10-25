package com.rongia.system.domain.attachment;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 附件仓储接口 */
@NullMarked
public interface AttachmentRepository extends Repository<Attachment> {

  /**
   * 按持有者查询附件
   *
   * @param entityType 持有者表名
   * @param entityId 持有者Id
   * @return 附件列表
   */
  List<Attachment> getListByEntityTypeAndEntityId(String entityType, long entityId);

  /**
   * 按存储键查询
   *
   * @param storageKey 存储键
   * @return 附件（可空），不存在返回 null
   */
  @Nullable
  Attachment getByStorageKey(String storageKey);
}
