package com.rongia.system.domain.privilege;

import com.rongia.shared.domain.aggregate.DeleteAggregateRoot;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;

/** 权限 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Privilege",
    indexes = {
      @Index(name = "IX_Privilege_Master", columnList = "[Master]"),
      @Index(name = "IX_Privilege_MasterValue", columnList = "MasterValue"),
      @Index(name = "IX_Privilege_Access", columnList = "[Access]"),
      @Index(name = "IX_Privilege_AccessValue", columnList = "AccessValue")
    })
public class Privilege extends DeleteAggregateRoot {

  /** 权限对象编码，取值见 {@link MasterEnum} */
  @Nationalized
  @Column(name = "Master", nullable = false, length = 50)
  private String master;

  /** 权限对象值 */
  @Column(name = "MasterValue", nullable = false)
  private Long masterValue;

  /** 权限通道编码，取值见 {@link AccessEnum} */
  @Nationalized
  @Column(name = "Access", nullable = false, length = 50)
  private String access;

  /** 权限通道值 */
  @Column(name = "AccessValue", nullable = false)
  private Long accessValue;

  /**
   * 构造器（枚举）
   *
   * @param master 权限对象
   * @param masterValue 权限对象值
   * @param access 权限通道
   * @param accessValue 权限通道值
   */
  public Privilege(MasterEnum master, long masterValue, AccessEnum access, long accessValue) {
    this.master = master.getCode();
    this.masterValue = masterValue;
    this.access = access.getCode();
    this.accessValue = accessValue;
  }

  /**
   * 构造器（字符串）
   *
   * @param master 权限对象
   * @param masterValue 权限对象值
   * @param access 权限通道
   * @param accessValue 权限通道值
   * @throws DomainException 权限对象或权限通道不在枚举中时抛出
   */
  public Privilege(String master, long masterValue, String access, long accessValue) {
    this.master = EnumUtils.getCode(MasterEnum.class, master, "权限对象");
    this.masterValue = masterValue;
    this.access = EnumUtils.getCode(AccessEnum.class, access, "权限通道");
    this.accessValue = accessValue;
  }
}
