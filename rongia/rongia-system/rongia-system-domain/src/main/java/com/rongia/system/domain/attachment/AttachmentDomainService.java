package com.rongia.system.domain.attachment;

import com.rongia.shared.domain.exception.DomainException;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 附件领域服务 */
@NullMarked
public class AttachmentDomainService {

  /** 附件工厂 */
  private final AttachmentFactory attachmentFactory;

  /** 附件仓储 */
  private final AttachmentRepository attachmentRepository;

  /**
   * 构造器注入
   *
   * @param attachmentFactory 附件工厂
   * @param attachmentRepository 附件仓储
   */
  public AttachmentDomainService(
      AttachmentFactory attachmentFactory, AttachmentRepository attachmentRepository) {
    this.attachmentFactory = attachmentFactory;
    this.attachmentRepository = attachmentRepository;
  }

  /**
   * 登记一条附件
   *
   * <p>只登记元数据：文件本身由基础设施先落到对象存储或磁盘，拿到存储键后再来这里登记。
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
    return attachmentFactory.create(
        entityType, entityId, entityField, fileName, storageKey, fileSize, mimeType, hash);
  }

  /**
   * 修改附件的展示信息
   *
   * @param id 附件Id
   * @param fileName 文件名
   * @param mimeType MIME 类型（可空）
   * @return 附件聚合
   * @throws DomainException 附件不存在时抛出
   */
  public Attachment modify(long id, String fileName, @Nullable String mimeType) {
    Attachment model = getExist(id);
    model.modify(fileName, mimeType);
    return model;
  }

  /**
   * 删除附件（逻辑删除）
   *
   * <p>只删登记记录；文件本身还在存储上，物理删除由基础设施按存储键另行处理。
   *
   * @param id 附件Id
   * @return 附件聚合（已标记删除）
   * @throws DomainException 附件不存在时抛出
   */
  public Attachment delete(long id) {
    Attachment model = getExist(id);
    model.delete();
    return model;
  }

  /**
   * 按持有者查附件
   *
   * @param entityType 持有者表名
   * @param entityId 持有者Id
   * @return 附件列表
   */
  public List<Attachment> getListByEntity(String entityType, long entityId) {
    return attachmentRepository.getListByEntityTypeAndEntityId(entityType, entityId);
  }

  /**
   * 按Id取附件
   *
   * @param id 附件Id
   * @return 附件聚合
   * @throws DomainException 附件不存在时抛出
   */
  private Attachment getExist(long id) {
    Attachment model = attachmentRepository.get(id);
    if (model == null) {
      throw new DomainException("附件" + id + "不存在!");
    }
    return model;
  }
}
