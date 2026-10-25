package com.rongia.basedata.domain.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 单位树与换算的纯算法测试 */
@DisplayName("单位 Unit")
class UnitTest {

  /** 根单位：千克，自己是根，系数 1 */
  private static Unit root() {
    return new Unit(null, null, "KG", "千克", BigDecimal.ONE, null, new BigDecimal("0.001"), false, null, null);
  }

  /** 克：挂在千克下，1 克 = 0.001 千克 */
  private static Unit gram(Unit parent) {
    return new Unit(1L, "1", "G", "克", new BigDecimal("0.001"), parent.getFactor(), new BigDecimal("0.001"), false, null, null);
  }

  /** 毫克：挂在克下，1 毫克 = 0.001 克 */
  private static Unit milligram(Unit parent) {
    return new Unit(2L, "1-2", "MG", "毫克", new BigDecimal("0.001"), parent.getFactor(), new BigDecimal("0.001"), false, null, null);
  }

  /** 用桩仓储拼一个服务 */
  private static UnitDomainService service(Unit root, Unit gram, Unit milligram, Unit otherRoot) {
    UnitRepository unitRepository = mock(UnitRepository.class);
    when(unitRepository.get(1L)).thenReturn(root);
    when(unitRepository.get(2L)).thenReturn(gram);
    when(unitRepository.get(3L)).thenReturn(milligram);
    when(unitRepository.get(99L)).thenReturn(otherRoot);
    when(unitRepository.getListByParentId(anyLong())).thenReturn(java.util.List.of());
    return new UnitDomainService(mock(UnitFactory.class), unitRepository);
  }

  private static UnitDomainService service() {
    Unit root = root();
    return service(root, gram(root), milligram(gram(root)), null);
  }

  @Test
  @DisplayName("系数逐级相乘：克 0.001、毫克 0.000001")
  void factorMultipliesUpTheTree() {
    Unit root = root();
    Unit gram = gram(root);
    Unit milligram = milligram(gram);

    assertEquals(0, BigDecimal.ONE.compareTo(root.getFactor()));
    assertEquals(0, new BigDecimal("0.001").compareTo(gram.getFactor()));
    assertEquals(0, new BigDecimal("0.000001").compareTo(milligram.getFactor()));
  }

  @Test
  @DisplayName("同树换算：1 千克 = 1000 克")
  void kilogramToGram() {
    BigDecimal result = service().convert(1L, 2L, BigDecimal.ONE);
    assertEquals(0, new BigDecimal("1000").compareTo(result));
  }

  @Test
  @DisplayName("同树换算：500 克 = 0.5 千克")
  void gramToKilogram() {
    BigDecimal result = service().convert(2L, 1L, new BigDecimal("500"));
    assertEquals(0, new BigDecimal("0.5").compareTo(result));
  }

  @Test
  @DisplayName("隔代换算：1 克 = 1000 毫克")
  void gramToMilligram() {
    BigDecimal result = service().convert(2L, 3L, BigDecimal.ONE);
    assertEquals(0, new BigDecimal("1000").compareTo(result));
  }

  @Test
  @DisplayName("不同根的两棵树不能换算")
  void crossTreeRejected() {
    Unit root = root();
    Unit gram = gram(root);
    Unit milligram = milligram(gram);
    Unit lengthRoot = new Unit(null, null, "M", "米", BigDecimal.ONE, null, new BigDecimal("0.001"), false, null, null);

    assertThrows(
        DomainException.class,
        () -> service(root, gram, milligram, lengthRoot).convert(1L, 99L, BigDecimal.ONE));
  }

  @Test
  @DisplayName("数量为负报错")
  void negativeQtyRejected() {
    assertThrows(
        DomainException.class, () -> service().convert(1L, 2L, new BigDecimal("-1")));
  }

  @Test
  @DisplayName("同一个单位原样返回")
  void sameUnitReturnsQty() {
    assertEquals(
        0, new BigDecimal("3.5").compareTo(service().convert(1L, 1L, new BigDecimal("3.5"))));
  }

  @Test
  @DisplayName("参考单位不能挂到自己的下级上")
  void cycleRejected() {
    Unit root = root();
    Unit gram = gram(root);
    Unit milligram = milligram(gram);

    assertThrows(
        DomainException.class,
        () ->
            service(root, gram, milligram, null)
                .modify(1L, 3L, "KG", "千克", BigDecimal.ONE, BigDecimal.ONE, false, null, null));
  }

  @Test
  @DisplayName("参数校验：代码名称不能为空、换算值必须大于零")
  void fieldValidation() {
    assertThrows(
        DomainException.class,
        () -> new Unit(null, null, " ", "千克", BigDecimal.ONE, null, BigDecimal.ONE, false, null, null));
    assertThrows(
        DomainException.class,
        () -> new Unit(null, null, "KG", " ", BigDecimal.ONE, null, BigDecimal.ONE, false, null, null));
    assertThrows(
        DomainException.class,
        () -> new Unit(null, null, "KG", "千克", BigDecimal.ZERO, null, BigDecimal.ONE, false, null, null));
  }
}
