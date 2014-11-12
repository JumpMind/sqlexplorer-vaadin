package org.jumpmind.symmetric.ui.sqlexplorer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jumpmind.properties.TypedProperties;

public class Settings implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SQL_EXPLORER_EXCLUDE_TABLES_REGEX = "sql.explorer.exclude.tables.regex";

    public static final String SQL_EXPLORER_SHOW_ROW_NUMBERS = "sql.explorer.show.row.numbers";

    public static final String SQL_EXPLORER_AUTO_COMMIT = "sql.explorer.auto.commit";

    public static final String SQL_EXPLORER_RESULT_AS_TEXT = "sql.explorer.result.as.text";

    public static final String SQL_EXPLORER_DELIMITER = "sql.explorer.delimiter";

    public static final String SQL_EXPLORER_MAX_RESULTS = "sql.explorer.max.results";

    List<SqlHistory> sqlHistory = new ArrayList<SqlHistory>();

    TypedProperties properties = new TypedProperties();

    public Settings() {
        properties.put(SQL_EXPLORER_DELIMITER, ";");
        properties.put(SQL_EXPLORER_SHOW_ROW_NUMBERS, "true");
        properties.put(SQL_EXPLORER_AUTO_COMMIT, "true");
        properties.put(SQL_EXPLORER_RESULT_AS_TEXT, "false");
        properties.put(SQL_EXPLORER_EXCLUDE_TABLES_REGEX, "");
        properties.put(SQL_EXPLORER_MAX_RESULTS, "1000");
    }

    public TypedProperties getProperties() {
        return properties;
    }

    public void setProperties(TypedProperties properties) {
        this.properties = properties;
    }

    public List<SqlHistory> getSqlHistory() {
        return sqlHistory;
    }

    public void setSqlHistory(List<SqlHistory> sqlHistory) {
        this.sqlHistory = sqlHistory;
    }
    
    public SqlHistory getSqlHistory(String sql) {
        sql = sql.trim();
        for (SqlHistory sqlHistory2 : sqlHistory) {
            if (sqlHistory2.getSqlStatement().equals(sql)) {
                return sqlHistory2;
            }
        }
        return null;
    }

}
