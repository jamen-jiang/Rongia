package com.rongia.basedata.domain.customergroup;

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
 * 客户组
 *
 * <p>把客户按集团、渠道这类口径归堆，客户料号在具体客户查不到时按组兜底。不建树：一层平铺就够，集团下面
 * 再分层是客户自己的事，靠命名区分。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "CustomerGroup",
    indexes = {
      @Index(name = "IX_CustomerGroup_Code", columnList = "Code"),
      @Index(name = "IX_CustomerGroup_Name", columnList = "Name"),
      @Index(name = "IX_CustomerGroup_Sequence", columnList = "Sequence")
      // 注意：Code 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class CustomerGroup extends UpdateAggregateRoot {

  /** 代码 */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 100)
  private String code;

  /** 名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 排序（可空） */
  @Column(name = "Sequence")
  @Nullable
  private Integer sequence;

  /** 是否禁用 */
  @Column(name = "Disabled", nullable = false)
  private Boolean disabled;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /**
   * 构造器
   *
   * @param code 代码
   * @param name 名称
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  CustomerGroup(
      String code,
      String name,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    apply(code, name, disabled, sequence, remark);
  }

  /**
   * 修改客户组
   *
   * @param code 代码
   * @param name 名称
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  void modify(
      String code,
      String name,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    apply(code, name, disabled, sequence, remark);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param code 代码
   * @param name 名称
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @throws DomainException 代码或名称为空时抛出
   */
  private void apply(
      String code,
      String name,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    String trimmedCode = StringUtils.trimToNull(code);
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedCode == null) {
      throw new DomainException("客户组代码不能为空");
    }
    if (trimmedName == null) {
      throw new DomainException("客户组名称不能为空");
    }
    this.code = trimmedCode;
    this.name = trimmedName;
    this.disabled = disabled;
    this.sequence = sequence;
    this.remark = StringUtils.trimToNull(remark);
  }
}
