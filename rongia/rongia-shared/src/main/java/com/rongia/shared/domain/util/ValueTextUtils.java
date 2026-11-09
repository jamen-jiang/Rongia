package com.rongia.shared.domain.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 取值文本工具（摊平与规范化），属性值和规格属性值共用
 *
 * <p>入参是「代码 → 值」的 Map（值可以是标量，也可以是数组），要落库或拼编码都得先摊平成「代码 →
 * 值列表」并把数字规范成统一文本。这两步属性和规格属性都要用，放共享层，避免两边各写一遍导致解释不一致。
 */
@NullMarked
public final class ValueTextUtils {

  /** 工具类，不实例化 */
  private ValueTextUtils() {}

  /**
   * 把「代码 → 值」的 Map 摊平成「代码 → 值列表」
   *
   * <p>标量算一个值，集合摊开成多个值，空白值直接丢弃；同一代码下的值去重后排序，保证同样的输入
   * 每次得到同样的行。
   *
   * @param values 值 Map（可空）
   * @return 代码 → 值列表，按代码排序
   */
  public static Map<String, List<String>> flatten(@Nullable Map<String, Object> values) {
    Map<String, List<String>> result = new LinkedHashMap<>();
    if (values == null) {
      return result;
    }
    Map<String, Object> ordered = new TreeMap<>(values);
    for (Map.Entry<String, Object> entry : ordered.entrySet()) {
      String code = StringUtils.trimToNull(entry.getKey());
      if (code == null) {
        continue;
      }
      Set<String> collected = new LinkedHashSet<>();
      collectValues(entry.getValue(), collected);
      List<String> sorted = new ArrayList<>(collected);
      sorted.sort(String::compareTo);
      if (!sorted.isEmpty()) {
        result.put(code, sorted);
      }
    }
    return result;
  }

  /**
   * 把取值规范成用于比较与拼编码的文本
   *
   * <p>数字统一去掉多余精度，所以 {@code 25}、{@code 25.00}、{@code "25"} 是同一个值；文本保持原样。
   *
   * @param value 取值（可空）
   * @return 规范文本，空值返回 {@code "null"}
   */
  public static String canonicalText(@Nullable Object value) {
    if (value == null) {
      return "null";
    }
    String text = StringUtils.trimToNull(value.toString());
    if (text == null) {
      return "null";
    }
    if (value instanceof Number || isNumeric(text)) {
      return new BigDecimal(text).stripTrailingZeros().toPlainString();
    }
    return text;
  }

  /**
   * 递归收集值：集合摊开，标量收一个
   *
   * @param value 原始值（可空）
   * @param values 收集结果
   */
  private static void collectValues(@Nullable Object value, Set<String> values) {
    if (value == null) {
      return;
    }
    if (value instanceof Collection<?> collection) {
      for (Object item : collection) {
        collectValues(item, values);
      }
      return;
    }
    String text = StringUtils.trimToNull(value.toString());
    if (text != null) {
      values.add(text);
    }
  }

  /**
   * 判断文本是否可当数字解析
   *
   * @param text 文本
   * @return true：是数字
   */
  private static boolean isNumeric(String text) {
    try {
      new BigDecimal(text);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
