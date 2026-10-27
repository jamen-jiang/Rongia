package com.rongia.stock.domain.stockentry;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 库存单据仓储接口 */
@NullMarked
public interface StockEntryRepository extends Repository<StockEntry> {

  /**
   * 按单号查询
   *
   * @param number 库存单据号
   * @return 库存单据（可空），不存在返回 null
   */
  @Nullable StockEntry getByNumber(String number);

  /**
   * 取某张业务单据带来的全部动账
   *
   * <p>一次业务动作可能落两张单：原来那张，和后来冲它的红字单，两张都在这里。按号排序，
   * 看起来就是「先出、后来又冲回来」这两笔。
   *
   * @param sourceType 来源单据类型编码
   * @param sourceId 来源单据Id
   * @return 库存单据列表
   */
  List<StockEntry> getListBySource(String sourceType, long sourceId);

  /**
   * 取冲某张单的全部红字单
   *
   * <p>一张单可以分几次冲——今天冲 20、过两天再冲 10，所以是一串不是一张。「已冲多少、还剩多少能冲」就是这么
   * 加出来的：被冲的原单上不留痕迹，也不另存「已红冲」一列。
   *
   * @param reversalOfStockEntryId 被冲的原单Id
   * @return 红字单列表，还没冲过时为空列表
   */
  List<StockEntry> getListByReversalOfStockEntryId(long reversalOfStockEntryId);
}
