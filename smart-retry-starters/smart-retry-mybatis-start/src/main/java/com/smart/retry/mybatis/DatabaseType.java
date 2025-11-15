package com.smart.retry.mybatis;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * @Author xiaoqiang
 * @Version DatabaseType.java, v 0.1 2025年11月15日 08:51 xiaoqiang
 * @Description: TODO
 */
public enum DatabaseType {
    MYSQL("mysql", "MySQL", "classpath:smart-mybatis-config-mysql.xml"),
    POSTGRESQL("postgresql", "PostgreSQL", "classpath:smart-mybatis-config-postgresql.xml"),
    ORACLE("oracle", "Oracle",null),
    SQLSERVER("sqlserver", "Microsoft SQL Server",null),
    H2("h2", "H2",null),
    SQLITE("sqlite", "SQLite",null),
    UNKNOWN("unknown", "Unknown",null);

    private final String code;
    private final String name;

    private final String resource;

    DatabaseType(String code, String name, String resource) {
        this.code = code;
        this.name = name;
        this.resource = resource;
    }

    public String getCode() { return code; }
    public String getName() { return name; }

    public String getResource() {
        return resource;
    }

    public static DatabaseType fromDataSource(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName().toLowerCase();

            for (DatabaseType type : values()) {
                if (productName.contains(type.name.toLowerCase())) {
                    return type;
                }
            }
        } catch (SQLException e) {
            // 处理异常
            //e.printStackTrace();
            throw new RuntimeException("Failed to determine database type", e);
        }
        return UNKNOWN;
    }
}

