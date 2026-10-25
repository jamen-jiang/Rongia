package com.rongia.system.domain.attachment;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 附件工厂，负责创建附件 */
@NullMarked
public class AttachmentFactory {

  /** 附件仓储 */
  private final AttachmentRepository attachmentRepository;

  /**
   * 构造器注入
   *
   * @param attachmentRepository 附件仓储
   */
  public AttachmentFactory(AttachmentRepository attachmentRepository) {
    this.attachmentRepository = attachmentRepository;
  }

  /**
   * 创建附件
   *
   * @param entityType 持有者表名
   * @param entityId 持有者Id
   * @param entityField 持有者字段（可空）
   * @param fileName 文件名
   * @param storageKey 存储键
   * @param fileSize 文件大小（字节）
   * @param mimeType MIME 类型（可空）
   * @param hash 校验和（可空）
   * @return 附件聚合
   * @throws DomainException 存储键已存在时抛出
   */
  public Attachment create(
      String entityType,
      long entityId,
      @Nullable String entityField,
      String fileName,
      String storageKey,
      long fileSize,
      @Nullable String mimeType,
      @Nullable String hash) {
    if (attachmentRepository.getByStorageKey(storageKey) != null) {
      throw new DomainException("存储键" + storageKey + "已存在!");
    }
    return new Attachment(
        entityType, entityId, entityField, fileName, storageKey, fileSize, mimeType, hash);
  }
}
