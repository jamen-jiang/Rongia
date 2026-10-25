package com.rongia.system.domain.attachment;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 附件
 *
 * <p>一张全系统的通用附件表：任何一张单据、一条主数据都能挂，靠 {@code entityType + entityId}
 * 指回去（照 Odoo 的 {@code ir_attachment}：{@code res_model + res_id}）。文件本身存在对象存储或磁盘上，
 * 这里只留元数据与取回用的 {@code storageKey}。
 *
 * <p>上传、下载、删文件这些动作归基础设施，本聚合只管「这条附件的元数据是什么、挂在谁身上」。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Attachment",
    indexes = {
      @Index(name = "IX_Attachment_EntityType_EntityId", columnList = "EntityType, EntityId"),
      @Index(name = "IX_Attachment_StorageKey", columnList = "StorageKey")
    })
public class Attachment extends UpdateAggregateRoot {

  /** 持有者表名（如 {@code SalesOrder} / {@code Material}） */
  @Nationalized
  @Column(name = "EntityType", nullable = false, length = 50)
  private String entityType;

  /** 持有者Id */
  @Column(name = "EntityId", nullable = false)
  private Long entityId;

  /** 持有者字段（可空：整单附件为空，字段级附件填字段名） */
  @Nationalized
  @Column(name = "EntityField", length = 50)
  @Nullable
  private String entityField;

  /** 文件名（含扩展名，原样保留用户上传时的名字） */
  @Nationalized
  @Column(name = "FileName", nullable = false, length = 200)
  private String fileName;

  /** 存储键：文件在对象存储或磁盘上的取回凭据，上传后由基础设施生成 */
  @Nationalized
  @Column(name = "StorageKey", nullable = false, length = 500)
  private String storageKey;

  /** 文件大小（字节） */
  @Column(name = "FileSize", nullable = false)
  private Long fileSize;

  /** MIME 类型（可空） */
  @Nationalized
  @Column(name = "MimeType", length = 100)
  @Nullable
  private String mimeType;

  /** 文件哈希（SHA1 十六进制，40 位，可空）：上传时由基础设施算好带进来，用于完整性校验与秒传 */
  @Nationalized
  @Column(name = "Hash", length = 40)
  @Nullable
  private String hash;

  /**
   * 构造器
   *
   * @param entityType 持有者表名
   * @param entityId 持有者Id
   * @param entityField 持有者字段（可空）
   * @param fileName 文件名
   * @param storageKey 存储键
   * @param fileSize 文件大小（字节）
   * @param mimeType MIME 类型（可空）
   * @param hash 文件哈希（可空）
   */
  Attachment(
      String entityType,
      long entityId,
      @Nullable String entityField,
      String fileName,
      String storageKey,
      long fileSize,
      @Nullable String mimeType,
      @Nullable String hash) {
    if (entityId <= 0) {
      throw new DomainException("附件的持有者不能为空");
    }
    if (fileSize <= 0) {
      throw new DomainException("附件大小必须大于零");
    }
    this.entityType = StringUtils.trimToNull(entityType) == null ? "" : entityType.trim();
    this.entityId = entityId;
    this.entityField = StringUtils.trimToNull(entityField);
    this.fileName = requireFile(fileName, "文件名");
    this.storageKey = requireFile(storageKey, "存储键");
    this.fileSize = fileSize;
    this.mimeType = StringUtils.trimToNull(mimeType);
    this.hash = StringUtils.trimToNull(hash);
  }

  /**
   * 修改附件的展示信息
   *
   * <p>文件本身不可换：换文件等于另一条附件，重新上传即可。
   *
   * @param fileName 文件名
   * @param mimeType MIME 类型（可空）
   */
  public void modify(String fileName, @Nullable String mimeType) {
    this.fileName = requireFile(fileName, "文件名");
    this.mimeType = StringUtils.trimToNull(mimeType);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 去空格并校验必填
   *
   * @param value 原始值
   * @param label 字段中文名，用于报错
   * @return 去空格后的值
   * @throws DomainException 值为空时抛出
   */
  private static String requireFile(String value, String label) {
    String trimmed = StringUtils.trimToNull(value);
    if (trimmed == null) {
      throw new DomainException(label + "不能为空");
    }
    return trimmed;
  }
}
