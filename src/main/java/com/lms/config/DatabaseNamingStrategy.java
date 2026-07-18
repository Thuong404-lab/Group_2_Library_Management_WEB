package com.lms.config;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Giữ nguyên tên bảng trong @Table và @JoinTable.
 * Tên cột Java vẫn được đổi từ camelCase sang snake_case.
 */
public class DatabaseNamingStrategy extends CamelCaseToUnderscoresNamingStrategy {

    @Override
    public Identifier toPhysicalTableName(Identifier logicalName,
                                          JdbcEnvironment jdbcEnvironment) {
        System.out.println("[DatabaseNamingStrategy] logicalName: " + logicalName.getText());
        Identifier physicalName = super.toPhysicalTableName(logicalName, jdbcEnvironment);
        System.out.println("[DatabaseNamingStrategy] super physicalName: " + physicalName.getText());
        return logicalName;
    }
}
