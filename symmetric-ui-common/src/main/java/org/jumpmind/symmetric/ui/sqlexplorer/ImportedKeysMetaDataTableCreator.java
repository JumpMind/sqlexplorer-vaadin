package org.jumpmind.symmetric.ui.sqlexplorer;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jumpmind.db.sql.JdbcSqlTemplate;

public class ImportedKeysMetaDataTableCreator extends AbstractMetaDataTableCreator {

    public ImportedKeysMetaDataTableCreator(JdbcSqlTemplate sqlTemplate, TableName table,
            Settings settings) {
        super(sqlTemplate, table, settings);
    }

    @Override
    protected ResultSet getMetaDataResultSet(DatabaseMetaData metadata) throws SQLException {
        return metadata.getImportedKeys(table.getCatalogName(), table.getSchemaName(),
                table.getTableName());
    }

}