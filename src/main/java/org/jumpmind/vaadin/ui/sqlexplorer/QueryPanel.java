/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.vaadin.ui.sqlexplorer;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.jumpmind.vaadin.ui.sqlexplorer.Settings.SQL_EXPLORER_AUTO_COMMIT;
import static org.jumpmind.vaadin.ui.sqlexplorer.Settings.SQL_EXPLORER_DELIMITER;

import java.io.Serializable;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceEditor.SelectionChangeEvent;
import org.vaadin.aceeditor.AceEditor.SelectionChangeListener;
import org.vaadin.aceeditor.AceMode;
import org.vaadin.aceeditor.TextRange;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutAction.ModifierKey;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

public class QueryPanel extends VerticalSplitPanel implements IContentTab {

    private static final long serialVersionUID = 1L;

    AceEditor sqlArea;

    IDb db;

    SelectionChangeListener selectionChangeListener;

    List<ShortcutListener> shortCutListeners = new ArrayList<ShortcutListener>();

    boolean executeAtCursorButtonValue = false;

    boolean executeScriptButtonValue = false;

    boolean commitButtonValue = false;

    boolean rollbackButtonValue = false;
    
    IButtonBar buttonBar;

    TabSheet resultsTabs;

    Tab errorTab;

    int maxNumberOfResultTabs = 10;

    ISettingsProvider settingsProvider;

    String user;

    Connection connection;

    Label status;

    transient Set<SqlRunner> runnersInProgress = new HashSet<SqlRunner>();

    public QueryPanel(IDb db, ISettingsProvider settingsProvider, IButtonBar buttonBar, String user) {
        this.settingsProvider = settingsProvider;
        this.db = db;
        this.user = user;
        this.buttonBar = buttonBar;
        this.sqlArea = buildSqlEditor();
        this.shortCutListeners.add(createExecuteSqlShortcutListener());

        VerticalLayout resultsLayout = new VerticalLayout();
        resultsLayout.setSizeFull();

        resultsTabs = CommonUiUtils.createTabSheet();

        HorizontalLayout statusBar = new HorizontalLayout();
        statusBar.addStyleName(ValoTheme.PANEL_WELL);
        statusBar.setMargin(new MarginInfo(true, true, true, true));
        statusBar.setWidth(100, Unit.PERCENTAGE);

        status = new Label("No Results");
        statusBar.addComponent(status);

        resultsLayout.addComponents(resultsTabs, statusBar);
        resultsLayout.setExpandRatio(resultsTabs, 1);

        addComponents(sqlArea, resultsLayout);

        setSplitPosition(400, Unit.PIXELS);

    }
    
    public IDb getDb() {
        return db;
    }

    protected AceEditor buildSqlEditor() {
        final AceEditor editor = CommonUiUtils.createAceEditor();
        editor.setMode(AceMode.sql);
        editor.addValueChangeListener(new ValueChangeListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent event) {
                if (!editor.getValue().equals("")) {
                    executeAtCursorButtonValue = true;
                    executeScriptButtonValue = true;
                } else {
                    executeAtCursorButtonValue = false;
                    executeScriptButtonValue = false;
                }
                setButtonsEnabled();
            }
        });

        selectionChangeListener = new DummyChangeListener();
        return editor;
    }
    
    public IButtonBar getButtonBar() {
        return buttonBar;
    }

    @Override
    public void selected() {
        unselected();

        sqlArea.addSelectionChangeListener(selectionChangeListener);
        for (ShortcutListener l : shortCutListeners) {
            sqlArea.addShortcutListener(l);
        }

        setButtonsEnabled();

        sqlArea.focus();
    }

    @Override
    public void unselected() {
        sqlArea.removeSelectionChangeListener(selectionChangeListener);
        for (ShortcutListener l : shortCutListeners) {
            sqlArea.removeShortcutListener(l);
        }
    }

    protected void setButtonsEnabled() {
        buttonBar.setExecuteScriptButtonEnabled(executeScriptButtonValue);
        buttonBar.setExecuteAtCursorButtonEnabled(executeAtCursorButtonValue);
        buttonBar.setCommitButtonEnabled(commitButtonValue);
        buttonBar.setRollbackButtonEnabled(rollbackButtonValue);
    }
    
    protected ShortcutListener createExecuteSqlShortcutListener() {
        return new ShortcutListener("", KeyCode.ENTER, new int[] { ModifierKey.CTRL }) {

            private static final long serialVersionUID = 1L;

            @Override
            public void handleAction(Object sender, Object target) {
                if (target instanceof Table) {
                    Table table = (Table) target;
                    TabularResultLayout layout = (TabularResultLayout) table.getParent();
                    reExecute(layout.getSql());
                } else if (target instanceof AceEditor) {
                    if (executeAtCursorButtonValue) {
                        if (execute(false)
                                && !settingsProvider.get().getProperties()
                                        .is(SQL_EXPLORER_AUTO_COMMIT)) {
                            setButtonsEnabled();
                        }
                    }
                }
            }
        };
    }

    protected void addToSqlHistory(String sqlStatement, Date executeTime, long executeDuration,
            String userId) {
        sqlStatement = sqlStatement.trim();
        Settings settings = settingsProvider.load();
        SqlHistory history = settings.getSqlHistory(sqlStatement);
        if (history == null) {
            history = new SqlHistory();
            history.setSqlStatement(sqlStatement);
            settings.addSqlHistory(history);
        }
        history.setLastExecuteDuration(executeDuration);
        history.setExecuteCount(history.getExecuteCount() + 1);
        history.setLastExecuteUserId(userId);
        history.setLastExecuteTime(executeTime);
        settingsProvider.save(settings);
    }

    protected boolean reExecute(String sql) {
        Component comp = resultsTabs.getSelectedTab();
        Tab tab = resultsTabs.getTab(comp);
        int tabPosition = resultsTabs.getTabPosition(tab);
        resultsTabs.removeTab(tab);
        return execute(false, sql, tabPosition);
    }

    public boolean execute(final boolean runAsScript) {
        return execute(runAsScript, null, 0);
    }

    public void appendSql(String sql) {
        if (isNotBlank(sql)) {
            sqlArea.setValue((isNotBlank(sqlArea.getValue()) ? sqlArea.getValue() + "\n" : "") + sql);
        }
    }
    
    public String getSql() {
        return sqlArea.getValue();
    }

    protected void executeSql(String sql, boolean writeToQueryWindow) {
        if (writeToQueryWindow) {
            appendSql(sql);
        }
        execute(false, sql, 0);
    }

    protected boolean execute(final boolean runAsScript, String sqlText, final int tabPosition) {
        boolean scheduled = false;
        if (runnersInProgress == null) {
            runnersInProgress = new HashSet<SqlRunner>();
        }

        if (sqlText == null) {
            if (!runAsScript) {
                sqlText = selectSqlToRun();
            } else {
                sqlText = sqlArea.getValue();
            }

            sqlText = sqlText != null ? sqlText.trim() : null;
        }

        if (StringUtils.isNotBlank(sqlText)) {

            HorizontalLayout executingLayout = new HorizontalLayout();
            executingLayout.setMargin(true);
            executingLayout.setSizeFull();
            Label label = new Label("Executing:\n\n" + StringUtils.abbreviate(sqlText, 250), ContentMode.PREFORMATTED);
            label.setEnabled(false);
            executingLayout.addComponent(label);
            executingLayout.setComponentAlignment(label, Alignment.TOP_LEFT);

            final String sql = sqlText;
            final Tab executingTab = resultsTabs.addTab(executingLayout,
                    StringUtils.abbreviate(sql, 20), FontAwesome.SPINNER, tabPosition);

            executingTab.setClosable(true);
            resultsTabs.setSelectedTab(executingTab);

            final SqlRunner runner = new SqlRunner(sql, runAsScript, user, db,
                    settingsProvider.get());
            runnersInProgress.add(runner);
            runner.setConnection(connection);
            runner.setListener(new SqlRunner.ISqlRunnerListener() {

                private static final long serialVersionUID = 1L;

                @Override
                public void writeSql(String sql) {
                    QueryPanel.this.appendSql(sql);
                }

                @Override
                public void reExecute(String sql) {
                    QueryPanel.this.reExecute(sql);
                }

                public void finished(final FontAwesome icon, final List<Component> results,
                        final long executionTimeInMs, final boolean transactionStarted,
                        final boolean transactionEnded) {
                    VaadinSession.getCurrent().access(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                if (transactionEnded) {
                                    transactionEnded();
                                } else if (transactionStarted) {
                                    rollbackButtonValue = true;
                                    commitButtonValue = true;
                                    setButtonsEnabled();
                                    sqlArea.setStyleName("transaction-in-progress");
                                    connection = runner.getConnection();
                                }

                                addToSqlHistory(StringUtils.abbreviate(sql, 1024*8), runner.getStartTime(), executionTimeInMs, user);

                                for (Component resultComponent : results) {
                                    resultComponent.setSizeFull();
                                    Tab tab = resultsTabs.addTab(resultComponent,
                                            StringUtils.abbreviate(sql, 20), icon, tabPosition);

                                    tab.setClosable(true);

                                    resultsTabs.setSelectedTab(tab.getComponent());

                                    if (errorTab != null) {
                                        resultsTabs.removeTab(errorTab);
                                        errorTab = null;
                                    }

                                    if (maxNumberOfResultTabs > 0
                                            && resultsTabs.getComponentCount() > maxNumberOfResultTabs) {
                                        resultsTabs.removeTab(resultsTabs.getTab(resultsTabs
                                                .getComponentCount() - 1));
                                    }

                                    status.setValue("Last sql executed in " + executionTimeInMs
                                            + " ms for " + db.getName() + ".  Finished at "
                                            + SimpleDateFormat.getTimeInstance().format(new Date()));

                                    if (icon == FontAwesome.STOP) {
                                        errorTab = tab;
                                    }

                                }
                            } finally {
                                setButtonsEnabled();
                                resultsTabs.removeTab(executingTab);
                                runnersInProgress.remove(runner);
                                runner.setListener(null);
                            }
                        }
                    });

                }

            });

            scheduled = true;
            runner.start();

        }
        setButtonsEnabled();
        return scheduled;
    }

    public void commit() {
        try {
            SqlRunner.commit(connection);
        } catch (Exception ex) {
            Notification.show(ex.getMessage());
        } finally {
            commitButtonValue = false;
            rollbackButtonValue = false;
            executeAtCursorButtonValue = true;
            executeScriptButtonValue = true;
            setButtonsEnabled();
            connection = null;
        }
    }

    public void transactionEnded() {
        commitButtonValue = false;
        rollbackButtonValue = false;
        executeAtCursorButtonValue = true;
        executeScriptButtonValue = true;
        setButtonsEnabled();
        connection = null;
    }

    public void rollback() {
        try {
            SqlRunner.rollback(connection);
        } catch (Exception ex) {
            Notification.show(ex.getMessage());
        } finally {
            commitButtonValue = false;
            rollbackButtonValue = false;
            executeAtCursorButtonValue = true;
            executeScriptButtonValue = true;
            setButtonsEnabled();
            connection = null;
        }
    }

    protected String selectSqlToRun() {
        String delimiter = settingsProvider.get().getProperties().get(SQL_EXPLORER_DELIMITER);
        String sql = sqlArea.getValue();
        TextRange range = sqlArea.getSelection();
        if (!range.isZeroLength()) {
            if (range.isBackwards()) {
                sql = sql.substring(range.getEnd(), range.getStart());
            } else {
                sql = sql.substring(range.getStart(), range.getEnd());
            }
        } else {
            StringBuilder sqlBuffer = new StringBuilder();
            String[] lines = sql.split("\n");
            int charCount = 0;
            boolean pastCursor = false;
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                charCount += line.length() + (i > 0 ? 1 : 0);
                if (charCount >= sqlArea.getCursorPosition()) {
                    pastCursor = true;
                }
                if (!pastCursor) {
                    if (line.trim().endsWith(delimiter) || line.trim().equals("")) {
                        sqlBuffer.setLength(0);
                    } else {
                        sqlBuffer.append(line).append("\n");
                    }
                } else if (line.trim().endsWith(delimiter)) {
                    sqlBuffer.append(line);
                    break;
                } else if (line.trim().equals("")) {
                    break;
                } else {
                    sqlBuffer.append(line).append("\n");
                }
            }
            sql = sqlBuffer.toString();
        }
        sql = sql.trim();
        if (sql.endsWith(delimiter)) {
            sql = sql.substring(0, sql.length() - 1);
        }
        return sql;
    }
    
    class DummyChangeListener implements SelectionChangeListener, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public void selectionChanged(SelectionChangeEvent e) {
        }
    }

}
