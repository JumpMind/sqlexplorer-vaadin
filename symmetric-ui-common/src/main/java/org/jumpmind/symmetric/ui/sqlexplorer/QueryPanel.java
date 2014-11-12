package org.jumpmind.symmetric.ui.sqlexplorer;

import static org.jumpmind.symmetric.ui.sqlexplorer.Settings.*;

import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.symmetric.ui.common.UiUtils;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceEditor.SelectionChangeEvent;
import org.vaadin.aceeditor.AceEditor.SelectionChangeListener;
import org.vaadin.aceeditor.AceMode;
import org.vaadin.aceeditor.TextRange;

import com.vaadin.addon.tableexport.CsvExport;
import com.vaadin.addon.tableexport.ExcelExport;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutAction.ModifierKey;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
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

    MenuItem executeAtCursorButton;

    MenuItem executeScriptButton;

    MenuItem commitButton;

    MenuItem rollbackButton;

    MenuItem databaseExplorerButton;

    MenuItem historyButton;

    MenuItem settingsButton;

    MenuItem importButton;

    MenuItem exportButton;

    MenuItem fillButton;

    TabSheet resultsTabs;

    Tab errorTab;

    int maxNumberOfResultTabs = 10;

    ISettingsProvider settingsProvider;

    String user;

    Connection connection;

    Label status;

    transient Set<SqlRunner> runnersInProgress = new HashSet<SqlRunner>();

    public QueryPanel(IDb db, ISettingsProvider settingsProvider, String user) {
        this.settingsProvider = settingsProvider;
        this.db = db;
        this.user = user;
        this.sqlArea = buildSqlEditor();
        this.shortCutListeners.add(createExecuteSqlShortcutListener());

        VerticalLayout resultsLayout = new VerticalLayout();
        resultsLayout.setSizeFull();

        resultsTabs = UiUtils.createTabSheet();

        HorizontalLayout statusBar = new HorizontalLayout();
        statusBar.setMargin(new MarginInfo(false, true, false, true));
        statusBar.setWidth(100, Unit.PERCENTAGE);

        status = new Label((String) null);
        status.setValue("No Results");
        statusBar.addComponent(status);

        resultsLayout.addComponents(resultsTabs, status);
        resultsLayout.setExpandRatio(resultsTabs, 1);

        addComponents(sqlArea, resultsLayout);

    }

    protected AceEditor buildSqlEditor() {
        final AceEditor editor = UiUtils.createAceEditor();
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

        selectionChangeListener = new SelectionChangeListener() {
            public void selectionChanged(SelectionChangeEvent e) {
                // adding this seems to make the cursor position not flake out
            }
        };
        return editor;
    }

    @Override
    public void selected(MenuBar menuBar) {
        unselected();

        executeAtCursorButton = menuBar.addItem("", FontAwesome.PLAY, new Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                execute(false);
            }
        });
        executeAtCursorButton.setEnabled(executeAtCursorButtonValue);
        executeAtCursorButton.setDescription("Run sql under cursor (CTRL+ENTER)");

        executeScriptButton = menuBar.addItem("", FontAwesome.FILE_TEXT_O, new Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                execute(true);
            }
        });
        executeScriptButton.setDescription("Run as script");

        commitButton = menuBar.addItem("", FontAwesome.ARROW_CIRCLE_O_RIGHT, new Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                commit();
            }
        });
        commitButton.setDescription("Commit");
        commitButton.setEnabled(false);

        rollbackButton = menuBar.addItem("", FontAwesome.ARROW_CIRCLE_O_LEFT, new Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                rollback();
            }
        });
        rollbackButton.setDescription("Rollback");
        rollbackButton.setEnabled(false);

        historyButton = menuBar.addItem("", FontAwesome.CLOCK_O, new Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                // SqlExplorerUiUtils.addWindow(new SqlHistoryWindow(folder,
                // Sqlthis,
                // explorerPanel));
            }
        });
        historyButton.setDescription("Sql History");
        historyButton.setEnabled(true);

        MenuItem optionsButton = menuBar.addItem("", FontAwesome.COG, null);
        optionsButton.setDescription("Options");

        importButton = optionsButton.addItem("DB Import", FontAwesome.DOWNLOAD, new Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                // SqlExplorerUiUtils.addWindow(new DbImportDialog(platform,
                // new HashSet<org.jumpmind.db.model.Table>(), template));
            }
        });

        exportButton = optionsButton.addItem("DB Export", FontAwesome.UPLOAD, new Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                // SqlExplorerUiUtils.addWindow(new DbExportDialog(platform,
                // new HashSet<org.jumpmind.db.model.Table>(), Sqlthis,
                // explorerPanel, template));
            }
        });

        fillButton = optionsButton.addItem("DB Fill", FontAwesome.BEER, new Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                // SqlExplorerUiUtils.addWindow(new DbFillDialog(platform,
                // new HashSet<org.jumpmind.db.model.Table>(), Sqlthis,
                // explorerPanel, template));
            }
        });

        sqlArea.addSelectionChangeListener(selectionChangeListener);
        for (ShortcutListener l : shortCutListeners) {
            sqlArea.addShortcutListener(l);
        }
        setButtonsEnabled();
    }

    @Override
    public void unselected() {
        sqlArea.removeSelectionChangeListener(selectionChangeListener);
        for (ShortcutListener l : shortCutListeners) {
            sqlArea.removeShortcutListener(l);
        }
    }

    protected void setButtonsEnabled() {
        executeScriptButton.setEnabled(executeScriptButtonValue);
        executeAtCursorButton.setEnabled(executeAtCursorButtonValue);
        commitButton.setEnabled(commitButtonValue);
        rollbackButton.setEnabled(rollbackButtonValue);
    }

    protected ShortcutListener createExecuteSqlShortcutListener() {
        return new ShortcutListener("", KeyCode.ENTER, new int[] { ModifierKey.CTRL }) {

            private static final long serialVersionUID = 1L;

            @Override
            public void handleAction(Object sender, Object target) {
                if (target instanceof Table) {
                    Table table = (Table) target;
                    TabularResultLayout layout = (TabularResultLayout) table.getParent();
                    layout.refreshButton.click();
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

    protected void add(String sqlStatement, Date executeTime, long executeDuration, String userId) {
        sqlStatement = sqlStatement.trim();
        Settings settings = settingsProvider.load();
        SqlHistory history = settings.getSqlHistory(sqlStatement);
        if (history == null) {
            history = new SqlHistory();
            history.setSqlStatement(sqlStatement);
            settings.getSqlHistory().add(history);
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

    protected boolean execute(final boolean runAsScript) {
        return execute(runAsScript, null, 0);
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
            final ProgressBar p = new ProgressBar();
            p.setIndeterminate(true);
            UI.getCurrent().setPollInterval(100);
            executingLayout.addComponent(p);

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
                    sqlArea.setValue(sqlArea.getValue() + "\n" + sql);
                }

                @Override
                public void reExecute(String sql) {
                    QueryPanel.this.reExecute(sql);
                }

                public void finished(final FontAwesome icon, final Component resultComponent,
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

                                add(sql, runner.getStartTime(), executionTimeInMs, user);

                                if (resultComponent != null) {
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
                                UI.getCurrent().setPollInterval(0);
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

    public void csvExport() {
        Table table = getTableFromResults();
        if (table != null) {
            CsvExport csvExport = new CsvExport(table);
            csvExport.excludeCollapsedColumns();
            csvExport.setDisplayTotals(false);
            csvExport.setExportFileName(db.getName() + "-export.csv");
            csvExport.setReportTitle(resultsTabs.getTab(resultsTabs.getSelectedTab())
                    .getDescription());
            csvExport.export();
        }
    }

    public void excelExport() {
        Table table = getTableFromResults();
        if (table != null) {
            ExcelExport excelExport = new ExcelExport(table);
            excelExport.excludeCollapsedColumns();
            excelExport.setDisplayTotals(false);
            excelExport.setExportFileName(db.getName() + "-export.xls");
            excelExport.setReportTitle(resultsTabs.getTab(resultsTabs.getSelectedTab())
                    .getDescription());
            excelExport.export();
        }
    }

    public Table getTableFromResults() {
        Component comp = resultsTabs.getSelectedTab();
        if (comp instanceof Table) {
            return (Table) comp;
        } else if (comp instanceof AbstractLayout) {
            AbstractLayout layout = (AbstractLayout) comp;
            Iterator<Component> i = layout.iterator();
            while (i.hasNext()) {
                Component component = i.next();
                if (component instanceof Table) {
                    return (Table) component;
                }
            }
        }
        return null;
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

}
