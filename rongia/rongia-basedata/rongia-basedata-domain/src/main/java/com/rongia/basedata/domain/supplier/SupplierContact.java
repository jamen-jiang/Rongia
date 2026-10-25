package com.rongia.basedata.domain.supplier;

import com.rongia.shared.domain.entity.BaseDeleteEntity;
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
 * 供应商联系人（供应商的子表）
 *
 * <p>照金蝶供应商「联系人」页签做：一个供应商挂多个联系人，姓名必填，职务/电话/邮箱选填。
 * 采购单打印、对账时从这里取对接人。
 *
 * <p>是 {@link Supplier} 的子表：FK 由父侧 {@code @JoinColumn(name = "SupplierId")} 写，
 * 生命周期跟供应商一起（orphanRemoval），不单独建仓储/工厂/服务。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "SupplierContact",
    indexes = {
      @Index(name = "IX_SupplierContact_SupplierId", columnList = "SupplierId")
      // 注意：(SupplierId, Name) 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class SupplierContact extends BaseDeleteEntity {

  /** 供应商Id（由父侧 JoinColumn 写，Java 侧不赋值） */
  @Column(name = "SupplierId")
  private Long supplierId;

  /** 姓名 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 电话（可空） */
  @Nationalized
  @Column(name = "Phone", length = 50)
  @Nullable
  private String phone;

  /** 手机（可空） */
  @Nationalized
  @Column(name = "Mobile", length = 50)
  @Nullable
  private String mobile;

  /** 邮箱（可空） */
  @Nationalized
  @Column(name = "Email", length = 200)
  @Nullable
  private String email;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /**
   * 构造器
   *
   * @param name 姓名
   * @param phone 电话（可空）
   * @param mobile 手机（可空）
   * @param email 邮箱（可空）
   * @param remark 备注（可空）
   */
  SupplierContact(
      String name,
      @Nullable String phone,
      @Nullable String mobile,
      @Nullable String email,
      @Nullable String remark) {
    apply(name, phone, mobile, email, remark);
  }

  /**
   * 修改联系人
   *
   * @param name 姓名
   * @param phone 电话（可空）
   * @param mobile 手机（可空）
   * @param email 邮箱（可空）
   * @param remark 备注（可空）
   */
  void modify(
      String name,
      @Nullable String phone,
      @Nullable String mobile,
      @Nullable String email,
      @Nullable String remark) {
    apply(name, phone, mobile, email, remark);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param name 姓名
   * @param phone 电话（可空）
   * @param mobile 手机（可空）
   * @param email 邮箱（可空）
   * @param remark 备注（可空）
   * @throws DomainException 姓名为空时抛出
   */
  private void apply(
      String name,
      @Nullable String phone,
      @Nullable String mobile,
      @Nullable String email,
      @Nullable String remark) {
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedName == null) {
      throw new DomainException("联系人姓名不能为空");
    }
    this.name = trimmedName;
    this.mobile = StringUtils.trimToNull(mobile);
    this.phone = StringUtils.trimToNull(phone);
    this.email = StringUtils.trimToNull(email);
    this.remark = remark;
  }
}
