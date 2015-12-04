package org.jumpmind.vaadin.ui.sqlexplorer;

import static org.jumpmind.vaadin.ui.sqlexplorer.Settings.SQL_EXPLORER_AUTO_COMMIT;
import static org.jumpmind.vaadin.ui.sqlexplorer.Settings.SQL_EXPLORER_DELIMITER;
import static org.jumpmind.vaadin.ui.sqlexplorer.Settings.SQL_EXPLORER_MAX_RESULTS;
import static org.jumpmind.vaadin.ui.sqlexplorer.Settings.SQL_EXPLORER_RESULT_AS_TEXT;

import java.io.Serializable;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jumpmind.db.platform.DatabaseNamesConstants;
import org.jumpmind.db.sql.JdbcSqlTemplate;
import org.jumpmind.db.sql.SqlScriptReader;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

public class SqlRunner extends Thread {

    protected static final Logger log = LoggerFactory.getLogger(SqlRunner.class);

    private static List<SqlRunner> sqlRunners = new ArrayList<SqlRunner>();

    private ISqlRunnerListener listener;

    private boolean runAsScript;

    private String sqlText;

    private Connection connection;

    private Date startTime = new Date();

    private Date endTime = null;

    private boolean rowsUpdated = false;

    private boolean createdConnection = true;

    private boolean showSqlOnResults = true;

    private IDb db;

    private String user;

    private boolean autoCommit;

    private boolean logAtDebug;

    private static final String COMMIT_COMMAND = "commit";

    private Settings settings;

    public static List<SqlRunner> getSqlRunners() {
        return sqlRunners;
    }

    public SqlRunner(String sqlText, boolean runAsScript, String user, IDb db, Settings settings) {
        this(sqlText, runAsScript, user, db, settings, null);
    }

    public SqlRunner(String sqlText, boolean runAsScript, String user, IDb db, Settings settings,
            ISqlRunnerListener listener) {
        this.setName("sql-runner-" + getId());
        this.sqlText = sqlText;
        this.runAsScript = runAsScript;
        this.db = db;
        this.listener = listener;
        this.settings = settings;
        this.autoCommit = settings.getProperties().is(SQL_EXPLORER_AUTO_COMMIT);
        this.user = user;
        sqlRunners.add(0, this);
    }

    public void setLogAtDebug(boolean logAtDebug) {
        this.logAtDebug = logAtDebug;
    }

    public void setShowSqlOnResults(boolean showSqlOnResults) {
        this.showSqlOnResults = showSqlOnResults;
    }

    public static void commit(Connection connection) throws SQLException {
        if (connection != null) {
            try {
                connection.commit();
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                }
                throw e;
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                }
                JdbcSqlTemplate.close(connection);
            }
        }
    }

    public static void rollback(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                // do nothing
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                }
                JdbcSqlTemplate.close(connection);
            }
        }
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    @Override
    public void run() {
        TypedProperties properties = settings.getProperties();
        boolean resultsAsText = properties.is(SQL_EXPLORER_RESULT_AS_TEXT);
        int maxResultsSize = properties.getInt(SQL_EXPLORER_MAX_RESULTS);
        String delimiter = properties.get(SQL_EXPLORER_DELIMITER);

        List<Component> resultComponents = new ArrayList<Component>();
        FontAwesome icon = FontAwesome.CHECK_CIRCLE;
        rowsUpdated = false;
        boolean committed = false;
        boolean autoCommitBefore = true;
        try {
            DataSource dataSource = db.getPlatform().getDataSource();
            JdbcSqlTemplate sqlTemplate = (JdbcSqlTemplate)db.getPlatform().getSqlTemplate();
            PreparedStatement stmt = null;
            StringBuilder results = new StringBuilder();
            try {
                if (connection == null) {
                    connection = dataSource.getConnection();
                    connection.setAutoCommit(autoCommit);
                }
                               
                autoCommitBefore = connection.getAutoCommit();
                if (connection.getTransactionIsolation() != sqlTemplate.getIsolationLevel()) {
                    connection.setTransactionIsolation(sqlTemplate.getIsolationLevel());
                }
                if (sqlTemplate.isRequiresAutoCommitFalseToSetFetchSize()) {
                    connection.setAutoCommit(false);
                }

                SqlScriptReader sqlReader = null;
                try {
                    sqlReader = new SqlScriptReader(new StringReader(sqlText));
                    sqlReader.setDelimiter(delimiter);
                    String sql = sqlReader.readSqlStatement();
                    while (sql != null) {
                        JdbcSqlTemplate.close(stmt);
                        stmt = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY);
                        
                        String lowercaseSql = sql.trim().toLowerCase();
                        if (!lowercaseSql.startsWith("delete") && !lowercaseSql.startsWith("update") &&
                                !lowercaseSql.startsWith("insert")) {
                            if (db.getPlatform().getName().equals(DatabaseNamesConstants.MYSQL)) {
                                stmt.setFetchSize(Integer.MIN_VALUE);
                            } else {
                                stmt.setFetchSize(maxResultsSize < 100  ? maxResultsSize : 100);
                            }
                        }

                        if (logAtDebug) {
                            log.debug("Executing: {}", sql.trim());
                        } else {
                            log.info("Executing: {}", sql.trim());
                        }
                        if (sql.replaceAll("\\s", "").equalsIgnoreCase(COMMIT_COMMAND)) {
                            committed = true;
                        } else {
                            committed = false;
                        }

                        boolean hasResults = stmt.execute();
                        int updateCount = stmt.getUpdateCount();
                        while (hasResults || updateCount != -1) {
                            ResultSet rs = null;
                            try {
                                if (hasResults) {
                                    rs = stmt.getResultSet();
                                    if (!runAsScript) {
                                        if (!resultsAsText) {
                                            resultComponents.add(new TabularResultLayout(db, sql,
                                                    rs, listener, settings, showSqlOnResults));
                                        } else {
                                            resultComponents.add(putResultsInArea(stmt,
                                                    maxResultsSize));
                                        }
                                    } else {
                                        int rowsRetrieved = 0;
                                        while (rs.next()) {
                                            rowsRetrieved++;
                                        }
                                        results.append(sql);
                                        results.append("\n");
                                        results.append("Rows Retrieved: ");
                                        results.append(rowsRetrieved);
                                        results.append("\n");
                                        results.append("\n");
                                    }
                                } else {
                                    rowsUpdated = updateCount > 0 ? true : false;
                                    if (!runAsScript) {
                                        resultComponents.add(wrapTextInComponent(String.format(
                                                "%d rows affected", updateCount)));
                                    } else {
                                        results.append(sql);
                                        results.append("\n");
                                        results.append("Rows Affected: ");
                                        results.append(updateCount);
                                        results.append("\n");
                                        results.append("\n");
                                    }
                                }
                                hasResults = stmt.getMoreResults();
                                updateCount = stmt.getUpdateCount();
                            } finally {
                                JdbcSqlTemplate.close(rs);
                            }
                        }

                        sql = sqlReader.readSqlStatement();

                    }

                } finally {
                    IOUtils.closeQuietly(sqlReader);
                }

            } catch (Throwable ex) {
                icon = FontAwesome.BAN;
                resultComponents.add(wrapTextInComponent(buildErrorMessage(ex), "marked"));
            } finally {
                if (autoCommitBefore) {
                    try {
                        connection.commit();
                        connection.setAutoCommit(autoCommitBefore);
                    } catch (SQLException e) {
                    }
                }
                JdbcSqlTemplate.close(stmt);
                if (autoCommit || (!autoCommit && !rowsUpdated && createdConnection)) {
                    JdbcSqlTemplate.close(connection);
                    connection = null;
                }

            }

            if (resultComponents.size() == 0 && StringUtils.isNotBlank(results.toString())) {
                resultComponents.add(wrapTextInComponent(results.toString(),
                        icon == FontAwesome.BAN ? "marked" : null));
            }

        } finally {
            endTime = new Date();
            if (listener != null) {
                listener.finished(icon, resultComponents, endTime.getTime() - startTime.getTime(),
                        !autoCommit && rowsUpdated, committed);
            } else if (!autoCommit) {
                rollback(connection);
            }

        }
    }

    protected String buildErrorMessage(Throwable ex) {
        StringBuilder errorMessage = new StringBuilder();
        if (ex instanceof SQLException) {
            SQLException sqlException = (SQLException) ex;
            errorMessage.append("SQL Message: ");
            errorMessage.append("\n");
            String[] lines = FormatUtils.wordWrap(ex.getMessage(), 120);
            for (String line : lines) {
                errorMessage.append(line);
                errorMessage.append("\n");
            }
            errorMessage.append("\n");
            errorMessage.append("SQL State: ");
            errorMessage.append(sqlException.getSQLState());
            errorMessage.append("\n");
            errorMessage.append("Error Code: ");
            errorMessage.append(sqlException.getErrorCode());
        } else {
            errorMessage.append(ex.getMessage());
            errorMessage.append(ExceptionUtils.getStackTrace(ex));
        }
        return errorMessage.toString();
    }

    protected Class<?> getClass(ResultSetMetaData meta, int i) throws SQLException {
        try {
            return Class.forName(meta.getColumnClassName(i));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected Component wrapTextInComponent(String text) {
        return wrapTextInComponent(text, null);
    }

    protected Component wrapTextInComponent(String text, String style) {
        Panel panel = new Panel();
        VerticalLayout content = new VerticalLayout();
        content.setMargin(true);
        panel.setContent(content);
        Label label = new Label(text.toString(), ContentMode.PREFORMATTED);
        if (StringUtils.isNotBlank(style)) {
            label.setStyleName(style);
        }
        content.addComponent(label);
        return panel;
    }

    protected Component putResultsInArea(Statement stmt, int maxResultSize) throws SQLException {
        return wrapTextInComponent(resultsAsText(stmt, maxResultSize));
    }

    protected String resultsAsText(Statement stmt, int maxResultSize) throws SQLException {
        ResultSet rs = null;
        try {
            rs = stmt.getResultSet();
            ResultSetMetaData meta = rs.getMetaData();
            int columns = meta.getColumnCount();
            int[] maxColumnSizes = new int[columns];
            for (int i = 1; i <= columns; i++) {
                String columnName = meta.getColumnName(i);
                maxColumnSizes[i - 1] = columnName.length();
            }
            int rowNumber = 1;
            List<Object[]> rows = new ArrayList<Object[]>();
            while (rs.next() && rowNumber <= maxResultSize) {
                Object[] row = new Object[columns];
                for (int i = 1; i <= columns; i++) {
                    Object obj = CommonUiUtils.getObject(rs, i);
                    row[i - 1] = obj;
                    if (obj != null) {
                        int size = obj.toString().length();
                        if (maxColumnSizes[i - 1] < size) {
                            maxColumnSizes[i - 1] = size;
                        }
                    }
                }
                rows.add(row);
                rowNumber++;
            }

            StringBuilder text = new StringBuilder();
            for (int i = 1; i <= columns; i++) {
                String columnName = meta.getColumnName(i);
                text.append(StringUtils.rightPad(columnName, maxColumnSizes[i - 1]));
                text.append(" ");
            }
            text.append("\n");

            for (int i = 1; i <= columns; i++) {
                text.append(StringUtils.rightPad("", maxColumnSizes[i - 1], "-"));
                text.append(" ");
            }
            text.append("\n");

            for (Object[] objects : rows) {
                for (int i = 0; i < objects.length; i++) {
                    text.append(StringUtils.rightPad(objects[i] != null ? objects[i].toString()
                            : "<null>", maxColumnSizes[i]));
                    text.append(" ");
                }
                text.append("\n");
            }

            return text.toString();
        } finally {
            JdbcSqlTemplate.close(rs);
        }
    }

    public void setListener(ISqlRunnerListener listener) {
        this.listener = listener;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        if (connection != null) {
            this.createdConnection = false;
        }
        this.connection = connection;
    }

    public String getUser() {
        return user;
    }

    public String getSqlText() {
        return sqlText;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public boolean isRowsUpdated() {
        return rowsUpdated;
    }

    public boolean isRunAsScript() {
        return runAsScript;
    }

    interface ISqlRunnerListener extends Serializable {

        public void writeSql(String sql);

        public void reExecute(String sql);

        public void finished(FontAwesome icon, List<Component> results, long executionTimeInMs,
                boolean transactionStarted, boolean transactionEnded);
    }

}
