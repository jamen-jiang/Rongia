package com.rongia.inventory.domain.stock;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 库存状态
 *
 * <p>它是库存定位键的第四维（D365 的口径：「库存状态是存储维度组里的一个维度」，还能给仓库交易指定默认状态），
 * 不是数量列的分档（SAP 走的是 {@code MARD} 上的非限制/质检/冻结三列，多一种状态就要加一列、改一遍程序），
 * 也不是靠建仓或建库位表达（Odoo 用 QC 区、Blocked 区这类库位来表达，代价是库位既要管物理位置又要管会计语义）。
 * 状态做维度，新状态是主数据里多一个码，表结构和代码都不用动。
 *
 * <p>「在途」现在不放：没有调拨单之前它没有消费者。等做跨仓调拨时再补一个码，跨仓在途按金蝶那样用状态表达，
 * 不建虚拟中转仓。
 *
 * <p>「能不能用」和「是谁的」是两条轴（SAP 把特殊库存与库存类型分得清清楚楚），所以这里只管前者；
 * 货主、保管者这类所有权维度不在这套模型里。
 */
@NullMarked
@Getter
public enum StockStatusEnum implements BaseEnum {

  /** 可用：能领、能发、能计划 */
  AVAILABLE("Available", "可用"),

  /** 待检：已入仓但还没放行，不进可用量 */
  PENDING_INSPECT("PendingInspect", "待检"),

  /** 冻结：数量在仓里但禁止动用，等处理结论 */
  FROZEN("Frozen", "冻结"),

  /** 废品：等报废处理，不再回到可用 */
  SCRAPPED("Scrapped", "废品");

  /** 编码 */
  private final String code;

  /** 标签 */
  private final String label;

  /**
   * 构造器
   *
   * @param code 编码
   * @param label 标签
   */
  StockStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
