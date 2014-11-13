package org.jumpmind.symmetric.ui.sqlexplorer;

import java.io.Serializable;

import org.jumpmind.db.model.Table;

public class TableName implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String catalogName;
    
    protected String schemaName;
    
    protected String tableName;
    
    public TableName(String catalogName, String schemaName, String name) {
        this.catalogName = catalogName;
        this.schemaName = schemaName;
        this.tableName = name;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String name) {
        this.tableName = name;
    }
    
    public String getCatalogName() {
        return catalogName;
    }
    
    public String getSchemaName() {
        return schemaName;
    }
    
    public String getFullyQualifiedTableName() {
        return Table.getFullyQualifiedTableName(catalogName, schemaName, tableName);
    }
}
