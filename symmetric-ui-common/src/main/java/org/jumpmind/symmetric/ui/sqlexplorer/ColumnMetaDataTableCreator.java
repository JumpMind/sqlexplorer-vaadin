package org.jumpmind.symmetric.ui.sqlexplorer;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jumpmind.db.model.Table;
import org.jumpmind.db.sql.JdbcSqlTemplate;

public class ColumnMetaDataTableCreator extends AbstractMetaDataTableCreator {

    public ColumnMetaDataTableCreator(JdbcSqlTemplate sqlTemplate, Table table, Settings settings) {
        super(sqlTemplate, table, settings);
    }

    @Override
    protected ResultSet getMetaDataResultSet(DatabaseMetaData metadata) throws SQLException {
        return metadata.getColumns(table.getCatalog(), table.getSchema(),
                table.getName(), null);
    }

    @Override
    protected String[] getColumnsToExclude() {
        return TABLE_NAME_METADATA_COLUMNS;
    }

}
