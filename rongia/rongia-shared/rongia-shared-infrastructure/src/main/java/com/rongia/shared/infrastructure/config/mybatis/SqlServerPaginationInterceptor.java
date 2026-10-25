package com.rongia.shared.infrastructure.config.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

public class SqlServerPaginationInterceptor extends PaginationInnerInterceptor {

  public SqlServerPaginationInterceptor() {
    super(DbType.SQL_SERVER);
  }

  @Override
  protected String lowLevelCountSql(String originalSql) {
    // 将 SQL 转大写，方便查找 ORDER BY
    String upperSql = originalSql.toUpperCase();
    int orderByIndex = upperSql.lastIndexOf("ORDER BY");

    // 如果找到了 ORDER BY，就把 ORDER BY 以及它后面的所有内容全部截掉
    if (orderByIndex > 0) {
      String sqlWithoutOrderBy = originalSql.substring(0, orderByIndex);
      return super.lowLevelCountSql(sqlWithoutOrderBy);
    }

    return super.lowLevelCountSql(originalSql);
  }
}
