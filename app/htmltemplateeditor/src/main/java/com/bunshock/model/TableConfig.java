package com.bunshock.model;

import java.util.List;

public class TableConfig {
    private String tableName;  // e.g., "ITEMS"
    private List<FieldConfig> columns;

    public TableConfig(String tableName, List<FieldConfig> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }

    // Getters

    public String getTableName() {
        return tableName;
    }

    public List<FieldConfig> getColumns() {
        return columns;
    }
}
