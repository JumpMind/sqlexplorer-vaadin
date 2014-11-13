package org.jumpmind.symmetric.ui.sqlexplorer;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jumpmind.db.sql.JdbcSqlTemplate;

public class IndexMetaDataTableCreator extends AbstractMetaDataTableCreator {

    public IndexMetaDataTableCreator(JdbcSqlTemplate sqlTemplate, TableName table, Settings settings) {
        super(sqlTemplate, table, settings);
    }

    @Override
    protected ResultSet getMetaDataResultSet(DatabaseMetaData metadata) throws SQLException {
        return metadata.getIndexInfo(table.getCatalogName(), table.getSchemaName(),
                table.getTableName(), false, false);
    }

    @Override
    protected String[] getColumnsToExclude() {
        return TABLE_NAME_METADATA_COLUMNS;
    }
}
