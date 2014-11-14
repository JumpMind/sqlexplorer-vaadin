package org.jumpmind.symmetric.ui.sqlexplorer;

import java.io.IOException;
import java.util.Iterator;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.DmlStatement;
import org.jumpmind.db.sql.DmlStatement.DmlType;
import org.jumpmind.db.sql.JdbcSqlTemplate;
import org.jumpmind.symmetric.io.data.DbExport;
import org.jumpmind.symmetric.io.data.DbExport.Format;
import org.jumpmind.symmetric.ui.common.UiUtils;
import org.jumpmind.symmetric.ui.sqlexplorer.SqlRunner.ISqlRunnerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.server.FontAwesome;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.SelectedTabChangeEvent;
import com.vaadin.ui.TabSheet.SelectedTabChangeListener;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class TableInfoPanel extends VerticalLayout implements IContentTab {

    protected static final Logger log = LoggerFactory.getLogger(TableInfoPanel.class);

    private static final long serialVersionUID = 1L;

    TabSheet tabSheet;
    
    String selectedCaption;

    public TableInfoPanel(org.jumpmind.db.model.Table table, String user, IDb db, Settings settings, String selectedTabCaption) {

        setSizeFull();

        tabSheet = UiUtils.createTabSheet();
        tabSheet.setImmediate(true);
        tabSheet.addSelectedTabChangeListener(new SelectedTabChangeListener() {
            
            private static final long serialVersionUID = 1L;

            @Override
            public void selectedTabChange(SelectedTabChangeEvent event) {
                selectedCaption = tabSheet.getTab(tabSheet.getSelectedTab()).getCaption();
            }
        });
        addComponent(tabSheet);

        JdbcSqlTemplate sqlTemplate = (JdbcSqlTemplate) db.getPlatform().getSqlTemplate();

        refreshData(table, user, db, settings);

        tabSheet.addTab(create(new ColumnMetaDataTableCreator(sqlTemplate, table, settings)),
                "Columns");
        tabSheet.addTab(create(new PrimaryKeyMetaDataTableCreator(sqlTemplate, table, settings)),
                "Primary Keys");
        tabSheet.addTab(create(new IndexMetaDataTableCreator(sqlTemplate, table, settings)),
                "Indexes");
        if (db.getPlatform().getDatabaseInfo().isForeignKeysSupported()) {
            tabSheet.addTab(create(new ImportedKeysMetaDataTableCreator(sqlTemplate, table,
                    settings)), "Imported Keys");
            tabSheet.addTab(create(new ExportedKeysMetaDataTableCreator(sqlTemplate, table,
                    settings)), "Exported Keys");
        }
        
        try {
            DbExport export = new DbExport(db.getPlatform());
            export.setNoCreateInfo(false);
            export.setNoData(true);
            export.setCatalog(table.getCatalog());
            export.setSchema(table.getSchema());
            export.setFormat(Format.SQL);
            AceEditor editor = UiUtils.createAceEditor();
            editor.setMode(AceMode.sql);
            editor.setValue(export.exportTables(new org.jumpmind.db.model.Table[] { table }));
            tabSheet.addTab(editor, "Source");
        } catch (IOException e) {
            log.warn("Failed to export the create information", e);
        }
        
        Iterator<Component> i = tabSheet.iterator();
        while (i.hasNext()) {
            Component component = i.next();
            Tab tab = tabSheet.getTab(component);
            if (tab.getCaption().equals(selectedTabCaption)) {
                tabSheet.setSelectedTab(component);
                break;
            }            
        }
    }
    
    public String getSelectedTabCaption() {
        return selectedCaption;
    }

    protected void refreshData(final org.jumpmind.db.model.Table table, final String user, final IDb db,
            final Settings settings) {
        IDatabasePlatform platform = db.getPlatform();
        DmlStatement dml = platform.createDmlStatement(DmlType.SELECT_ALL, table, null);

        final HorizontalLayout executingLayout = new HorizontalLayout();
        executingLayout.setSizeFull();
        final ProgressBar p = new ProgressBar();
        p.setIndeterminate(true);
        final int oldPollInterval = UI.getCurrent().getPollInterval();
        UI.getCurrent().setPollInterval(100);
        executingLayout.addComponent(p);
        tabSheet.addTab(executingLayout, "Data", FontAwesome.SPINNER, 0);

        SqlRunner runner = new SqlRunner(dml.getSql(), false, user, db, settings,
                new ISqlRunnerListener() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void writeSql(String sql) {
                    }

                    @Override
                    public void reExecute(String sql) {
                        refreshData(table, user, db, settings);
                    }

                    @Override
                    public void finished(final FontAwesome icon, final Component results,
                            long executionTimeInMs, boolean transactionStarted,
                            boolean transactionEnded) {
                        VaadinSession.getCurrent().access(new Runnable() {

                            @Override
                            public void run() {
                                boolean select = tabSheet.getSelectedTab().equals(executingLayout);
                                tabSheet.removeComponent(executingLayout);
                                results.setSizeFull();
                                tabSheet.addTab(results, "Data", null, 0);
                                UI.getCurrent().setPollInterval(oldPollInterval);
                                if (select) {
                                    tabSheet.setSelectedTab(results);
                                }
                            }
                        });
                    }
                });
        runner.setShowSqlOnResults(false);
        runner.setLogAtDebug(true);
        runner.start();
    }

    protected AbstractLayout create(AbstractMetaDataTableCreator creator) {
        Table table = creator.create();
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSizeFull();
        layout.addComponent(table);
        layout.setExpandRatio(table, 1);
        return layout;
    }

    @Override
    public void selected(MenuBar menuBar) {
    }

    @Override
    public void unselected() {
    }
}
