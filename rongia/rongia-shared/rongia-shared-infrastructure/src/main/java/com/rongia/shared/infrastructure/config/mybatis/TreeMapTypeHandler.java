package com.rongia.shared.infrastructure.config.mybatis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeMap;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.jspecify.annotations.Nullable;

/** TreeMap 与 JSON 字符串互转的 TypeHandler（只读场景） */
@MappedTypes(TreeMap.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class TreeMapTypeHandler extends BaseTypeHandler<TreeMap<String, Object>> {

  /** JSON 序列化器 */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * 写入数据库：只读场景不支持写入
   *
   * @param ps PreparedStatement
   * @param i 参数下标
   * @param parameter 属性值
   * @param jdbcType JDBC 类型
   */
  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, TreeMap<String, Object> parameter, JdbcType jdbcType)
      throws SQLException {
    throw new UnsupportedOperationException("只读场景，不支持写入 TreeMap");
  }

  /**
   * 读取数据库（按列名）：JSON 字符串 → TreeMap
   *
   * @param rs ResultSet
   * @param columnName 列名
   * @return 属性值
   */
  @Override
  public TreeMap<String, Object> getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return parse(rs.getString(columnName));
  }

  /**
   * 读取数据库（按列下标）：JSON 字符串 → TreeMap
   *
   * @param rs ResultSet
   * @param columnIndex 列下标
   * @return 属性值
   */
  @Override
  public TreeMap<String, Object> getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return parse(rs.getString(columnIndex));
  }

  /**
   * 读取存储过程结果：JSON 字符串 → TreeMap
   *
   * @param cs CallableStatement
   * @param columnIndex 列下标
   * @return 属性值
   */
  @Override
  public TreeMap<String, Object> getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return parse(cs.getString(columnIndex));
  }

  /**
   * 解析 JSON 字符串为 TreeMap
   *
   * @param json JSON 字符串
   * @return 属性值（可空），空字符串返回 null
   */
  @Nullable
  private TreeMap<String, Object> parse(@Nullable String json) throws SQLException {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
    } catch (Exception e) {
      throw new SQLException("反序列化 TreeMap 失败", e);
    }
  }
}
