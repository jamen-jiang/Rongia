package com.rongia.system.domain.attribute;

import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.ValueTextUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 属性值的还原与解析
 *
 * <p>输入用的是「代码 → 值」的 Map（值可以是标量，也可以是数组），落库却是一行一个值，中间还要按定义
 * 校验；这三步必须有唯一的换算处，否则物料、规格各写一遍，同一段代码解释两遍，签名和界面对不上。
 *
 * <p>「摊平」和「规范成比较文本」这两个动作不放在这里，见 {@link ValueTextUtils}——规格属性值算规格签名时要用同一套规则，两处各写一遍会让同一个组合算出两个签名。
 */
@NullMarked
public final class AttributeUtils {

  /** 工具类，不实例化 */
  private AttributeUtils() {}

  /**
   * 解析入参：按代码找定义 → 校验取值 → 摊平成属性值行数据，顺带做必填校验
   *
   * <p>入参为 {@code null} 表示「这一笔不改动属性值」，返回 {@code null} 让调用方跳过同步； 传空 Map
   * 才是「清空」。这两种写法差别要分清，否则导入只带规格组合的那一步会把已维护的属性全删掉。
   *
   * @param definitions 该持有者类型下的属性定义
   * @param attributes 目标属性值（可空）
   * @return 属性值行数据，入参为 null 时返回 null
   * @throws DomainException 代码没有定义 / 取值不合法 / 必填没给值时抛出
   */
  @Nullable
  public static List<AssignedAttributeData> resolve(
      Collection<Attribute> definitions, @Nullable Map<String, Object> attributes) {
    if (attributes == null) {
      return null;
    }
    Map<String, List<String>> flattened = ValueTextUtils.flatten(attributes);
    Map<String, Attribute> dict = new TreeMap<>();
    for (Attribute definition : definitions) {
      dict.put(definition.getCode(), definition);
    }

    List<AssignedAttributeData> rows = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : flattened.entrySet()) {
      Attribute definition = dict.get(entry.getKey());
      if (definition == null) {
        throw new DomainException("属性" + entry.getKey() + "不存在!");
      }
      for (String value : entry.getValue()) {
        definition.validateValue(value);
        rows.add(new AssignedAttributeData(definition.getId(), definition.getCode(), value));
      }
    }

    // 必填：定义了必填、这一笔却没给值的，当场拦下
    for (Attribute definition : definitions) {
      definition.validateFilled(flattened.get(definition.getCode()));
    }
    return rows;
  }

  /**
   * 把属性值行还原成「代码 → 值」的 Map
   *
   * <p>一个代码只有一行时给出标量，多行时给出列表——读的人不需要先数行数。
   *
   * @param attributes 属性值行
   * @return 属性值 Map
   */
  public static TreeMap<String, Object> toMap(List<AttributeAssignment> attributes) {
    Map<String, List<String>> grouped = new LinkedHashMap<>();
    for (AttributeAssignment attribute : attributes) {
      if (attribute.getIsDeleted()) {
        continue;
      }
      grouped
          .computeIfAbsent(attribute.getCode(), code -> new ArrayList<>())
          .add(attribute.getValue());
    }
    TreeMap<String, Object> result = new TreeMap<>();
    for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
      List<String> values = entry.getValue();
      result.put(entry.getKey(), values.size() == 1 ? values.getFirst() : List.copyOf(values));
    }
    return result;
  }
}
