package org.jumpmind.symmetric.ui.sqlexplorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Database;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.DatabaseInfo;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.DmlStatement;
import org.jumpmind.db.sql.DmlStatement.DmlType;
import org.jumpmind.db.sql.Row;
import org.jumpmind.db.util.BinaryEncoding;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.ui.common.CommonUiUtils;
import org.jumpmind.symmetric.ui.common.ConfirmDialog;
import org.jumpmind.symmetric.ui.common.ConfirmDialog.IConfirmListener;
import org.jumpmind.symmetric.ui.common.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.annotations.StyleSheet;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.SelectedTabChangeEvent;
import com.vaadin.ui.TabSheet.SelectedTabChangeListener;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@StyleSheet({ "sqlexplorer.css" })
public class SqlExplorer extends HorizontalSplitPanel {

    private static final long serialVersionUID = 1L;

    final Logger log = LoggerFactory.getLogger(getClass());

    final static FontAwesome QUERY_ICON = FontAwesome.FILE_O;

    final static float DEFAULT_SPLIT_POS = 225;

    IDbProvider databaseProvider;

    ISettingsProvider settingsProvider;

    MenuItem showButton;

    DbTree dbTree;

    TabSheet contentTabs;

    MenuBar contentMenuBar;

    IContentTab selected;

    float savedSplitPosition = DEFAULT_SPLIT_POS;

    String user;

    Set<TableInfoPanel> tableInfoTabs = new HashSet<TableInfoPanel>();    

    public SqlExplorer(String configDir, IDbProvider databaseProvider, ISettingsProvider settingsProvider, String user) {
        this(configDir, databaseProvider, settingsProvider, user, DEFAULT_SPLIT_POS);
    }
    
    public SqlExplorer(String configDir, IDbProvider databaseProvider, String user) {
        this(configDir, databaseProvider, new DefaultSettingsProvider(configDir), user, DEFAULT_SPLIT_POS);
    }
    
    public SqlExplorer(String configDir, IDbProvider databaseProvider, String user, float leftSplitPos) {
        this(configDir, databaseProvider, new DefaultSettingsProvider(configDir), user, leftSplitPos);
    }

    public SqlExplorer(String configDir, IDbProvider databaseProvider, ISettingsProvider settingsProvider, String user, float leftSplitSize) {
        this.databaseProvider = databaseProvider;
        this.settingsProvider = settingsProvider;
        this.savedSplitPosition = leftSplitSize;

        setSizeFull();
        addStyleName("sqlexplorer");

        VerticalLayout leftLayout = new VerticalLayout();
        leftLayout.setSizeFull();
        leftLayout.addStyleName(ValoTheme.MENU_ROOT);

        leftLayout.addComponent(buildLeftMenu());

        Panel scrollable = new Panel();
        scrollable.setSizeFull();
        //scrollable.addStyleName(ValoTheme.PANEL_BORDERLESS);

        dbTree = buildDbTree();
        scrollable.setContent(dbTree);

        leftLayout.addComponent(scrollable);
        leftLayout.setExpandRatio(scrollable, 1);

        VerticalLayout rightLayout = new VerticalLayout();
        rightLayout.setSizeFull();

        VerticalLayout rightMenuWrapper = new VerticalLayout();
        rightMenuWrapper.setWidth(100, Unit.PERCENTAGE);
        rightMenuWrapper.addStyleName(ValoTheme.MENU_ROOT);
        contentMenuBar = new MenuBar();
        contentMenuBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
        contentMenuBar.setWidth(100, Unit.PERCENTAGE);
        addShowButton(contentMenuBar);

        rightMenuWrapper.addComponent(contentMenuBar);
        rightLayout.addComponent(rightMenuWrapper);

        contentTabs = CommonUiUtils.createTabSheet();
        contentTabs.addSelectedTabChangeListener(new SelectedTabChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void selectedTabChange(SelectedTabChangeEvent event) {
                selectContentTab((IContentTab) contentTabs.getSelectedTab());
            }
        });
        rightLayout.addComponent(contentTabs);
        rightLayout.setExpandRatio(contentTabs, 1);

        addComponents(leftLayout, rightLayout);

        setSplitPosition(savedSplitPosition, Unit.PIXELS);
    }

    protected MenuBar buildLeftMenu() {
        MenuBar leftMenu = new MenuBar();
        leftMenu.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
        leftMenu.setWidth(100, Unit.PERCENTAGE);
        MenuItem hideButton = leftMenu.addItem("", new Command() {
            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                savedSplitPosition = getSplitPosition() > 10 ? getSplitPosition()
                        : DEFAULT_SPLIT_POS;
                setSplitPosition(0);
                setLocked(true);
                showButton.setVisible(true);
            }
        });
        hideButton.setDescription("Hide the database explorer");
        hideButton.setIcon(FontAwesome.BARS);

        MenuItem refreshButton = leftMenu.addItem("", new Command() {
            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                dbTree.refresh();
            }
        });
        refreshButton.setIcon(FontAwesome.REFRESH);
        refreshButton.setDescription("Refresh the database explorer");

        MenuItem openQueryTab = leftMenu.addItem("", new Command() {
            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                openQueryWindow(dbTree.getSelected());
            }
        });
        openQueryTab.setIcon(QUERY_ICON);
        openQueryTab.setDescription("Open a query tab");

        MenuItem settings = leftMenu.addItem("", new Command() {
            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                SettingsDialog dialog = new SettingsDialog(settingsProvider);
                dialog.showAtSize(.5);
            }
        });
        settings.setIcon(FontAwesome.GEAR);
        settings.setDescription("Modify sql explorer settings");
        return leftMenu;
    }

    protected void addShowButton(MenuBar contentMenuBar) {
        boolean visible = showButton != null ? showButton.isVisible() : false;
        showButton = contentMenuBar.addItem("", new Command() {
            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                setSplitPosition(savedSplitPosition, Unit.PIXELS);
                setLocked(false);
                showButton.setVisible(false);
            }
        });
        showButton.setIcon(FontAwesome.BARS);
        showButton.setDescription("Show the database explorer");
        showButton.setVisible(visible);
    }

    protected void selectContentTab(IContentTab tab) {
        if (selected != null) {
            selected.unselected();
        }
        contentTabs.setSelectedTab(tab);
        contentMenuBar.removeItems();
        addShowButton(contentMenuBar);
        if (tab instanceof QueryPanel) {
            ((DefaultButtonBar)((QueryPanel)tab).getButtonBar()).populate(contentMenuBar);
        }
        tab.selected();
        selected = tab;
    }

    protected QueryPanel openQueryWindow(TreeNode node) {
        return openQueryWindow(dbTree.getDbForNode(node));
    }

    protected QueryPanel openQueryWindow(IDb db) {
        String dbName = db.getName();
        DefaultButtonBar buttonBar = new DefaultButtonBar();
        QueryPanel panel = new QueryPanel(db, settingsProvider, buttonBar, user);
        buttonBar.init(db, settingsProvider, panel);
        Tab tab = contentTabs.addTab(panel, getTabName(dbName));
        tab.setClosable(true);
        tab.setIcon(QUERY_ICON);
        selectContentTab(panel);
        return panel;
    }

    protected void openQueryWindow(Set<TreeNode> nodes) {
        Set<String> dbNames = new HashSet<String>();
        for (TreeNode node : nodes) {
            IDb db = dbTree.getDbForNode(node);
            String dbName = db.getName();
            if (!dbNames.contains(dbName)) {
                dbNames.add(dbName);
                openQueryWindow(node);
            }
        }
    }

    public QueryPanel findQueryPanelForDb(IDb db) {
        QueryPanel panel = null;
        if (contentTabs.getComponentCount() > 0) {
            Component comp = contentTabs.getSelectedTab();
            if (comp instanceof QueryPanel) {
                QueryPanel prospectiveQueryPanel = (QueryPanel) comp;
                if (prospectiveQueryPanel.getDb().getName().equals(db.getName())) {
                    panel = prospectiveQueryPanel;
                }
            }

            if (panel == null) {
                Iterator<Component> i = contentTabs.iterator();
                while (i.hasNext()) {
                    comp = (Component) i.next();
                    if (comp instanceof QueryPanel) {
                        QueryPanel prospectiveQueryPanel = (QueryPanel) comp;
                        if (prospectiveQueryPanel.getDb().getName().equals(db.getName())) {
                            panel = prospectiveQueryPanel;
                            break;
                        }
                    }
                }
            }

            if (panel == null) {
                panel = openQueryWindow(db);
            }
        }

        return panel;
    }

    protected void generateSelectForSelectedTables() {
        Set<TreeNode> tableNodes = dbTree.getSelected(DbTree.NODE_TYPE_TABLE);
        for (TreeNode treeNode : tableNodes) {
            IDb db = dbTree.getDbForNode(treeNode);
            QueryPanel panel = findQueryPanelForDb(db);
            IDatabasePlatform platform = db.getPlatform();
            Table table = getTableFor(treeNode);
            DmlStatement dmlStatement = platform
                    .createDmlStatement(DmlType.SELECT_ALL, table, null);
            panel.appendSql(dmlStatement.getSql());
            contentTabs.setSelectedTab(panel);
        }
    }

    protected Table getTableFor(TreeNode treeNode) {
        IDb db = dbTree.getDbForNode(treeNode);
        IDatabasePlatform platform = db.getPlatform();
        TypedProperties nodeProperties = treeNode.getProperties();
        return platform.getTableFromCache(nodeProperties.get("catalogName"),
                nodeProperties.get("schemaName"), treeNode.getName(), false);
    }

    protected Set<Table> getTablesFor(Set<TreeNode> nodes) {
        Set<Table> tables = new HashSet<Table>();
        for (TreeNode treeNode : nodes) {
            Table table = getTableFor(treeNode);
            if (table != null) {
                tables.add(table);
            }
        }
        return tables;
    }

    protected void generateDmlForSelectedTables(DmlType dmlType) {
        Set<TreeNode> tableNodes = dbTree.getSelected(DbTree.NODE_TYPE_TABLE);
        for (TreeNode treeNode : tableNodes) {
            IDb db = dbTree.getDbForNode(treeNode);
            QueryPanel panel = findQueryPanelForDb(db);
            IDatabasePlatform platform = db.getPlatform();
            Table table = getTableFor(treeNode);
            DmlStatement dmlStatement = platform.createDmlStatement(dmlType, table, null);
            Row row = new Row(table.getColumnCount());
            Column[] columns = table.getColumns();
            for (Column column : columns) {
                String value = null;
                if (column.getParsedDefaultValue() == null) {
                    value = CommonUiUtils.getJdbcTypeValue(column.getJdbcTypeName());
                } else {
                    value = column.getParsedDefaultValue().toString();
                }
                row.put(column.getName(), value);

            }
            String sql = dmlStatement.buildDynamicSql(BinaryEncoding.HEX, row, false, true);
            panel.appendSql(sql);
            contentTabs.setSelectedTab(panel);
        }
    }

    protected void dropSelectedTables() {
        Set<TreeNode> tableNodes = dbTree.getSelected(DbTree.NODE_TYPE_TABLE);
        List<Table> tables = new ArrayList<Table>();
        Map<Table, TreeNode> tableToTreeNode = new HashMap<Table, TreeNode>();
        for (TreeNode treeNode : tableNodes) {
            Table table = getTableFor(treeNode);
            tables.add(table);
            tableToTreeNode.put(table, treeNode);
        }

        tables = Database.sortByForeignKeys(tables);
        Collections.reverse(tables);
        dropTables(tables, tableToTreeNode);
    }

    private void dropTables(final List<Table> tables, final Map<Table, TreeNode> tableToTreeNode) {
        final Table table = tables.remove(0);
        ConfirmDialog.show("Drop " + table.getFullyQualifiedTableName() + "?",
                "Do you want to drop " + table.getFullyQualifiedTableName() + "?",
                new IConfirmListener() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean onOk() {
                        TreeNode treeNode = tableToTreeNode.get(table);
                        IDb db = dbTree.getDbForNode(treeNode);
                        try {
                            db.getPlatform().dropTables(false, table);
                            if (tables.size() > 0) {
                                dropTables(tables, tableToTreeNode);
                            } else {
                                dbTree.refresh();
                            }
                        } catch (Exception e) {
                            String msg = "Failed to drop " + table.getFullyQualifiedTableName()
                                    + ".  ";
                            CommonUiUtils.notify(msg + "See log file for more details",
                                    Type.WARNING_MESSAGE);
                            log.warn(msg, e);
                            dbTree.refresh();
                        }
                        return true;
                    }
                });
    }

    protected DbTree buildDbTree() {

        final DbTree tree = new DbTree(databaseProvider, settingsProvider);
        tree.addValueChangeListener(new ValueChangeListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent event) {
                Set<TreeNode> nodes = dbTree.getSelected();
                if (nodes != null) {
                    String selectedTabCaption = null;
                    for (TableInfoPanel panel : tableInfoTabs) {
                        selectedTabCaption = panel.getSelectedTabCaption();
                        contentTabs.removeComponent(panel);                        
                    }
                    if (nodes.size() > 0) {
                        TreeNode treeNode = nodes.iterator().next();
                        if (treeNode != null && DbTree.NODE_TYPE_TABLE.equals(treeNode.getType())) {
                            Table table = getTableFor(treeNode);
                            if (table != null) {
                                IDb db = dbTree.getDbForNode(treeNode);
                                TableInfoPanel tableInfoTab = new TableInfoPanel(table, user, db, settingsProvider.get(), selectedTabCaption);
                                Tab tab = contentTabs.addTab(tableInfoTab,
                                        table.getFullyQualifiedTableName(), FontAwesome.TABLE, 0);
                                tab.setClosable(true);
                                selectContentTab(tableInfoTab);
                                tableInfoTabs.add(tableInfoTab);
                            }
                        }                        
                    }
                    
                    for (TreeNode treeNode : nodes) {
                        IDb db = dbTree.getDbForNode(treeNode);
                        QueryPanel panel = getQueryPanelForDb(db);
                        if (panel == null && db != null) {
                            openQueryWindow(db);
                        }
                    }
                }
            }
        });
        tree.registerAction(new DbTreeAction("Query", QUERY_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void handle(Set<TreeNode> nodes) {
                openQueryWindow(nodes);
            }
        }, DbTree.NODE_TYPE_DATABASE, DbTree.NODE_TYPE_CATALOG, DbTree.NODE_TYPE_SCHEMA,
                DbTree.NODE_TYPE_TABLE);

        tree.registerAction(new DbTreeAction("Select", QUERY_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void handle(Set<TreeNode> nodes) {
                generateSelectForSelectedTables();
            }
        }, DbTree.NODE_TYPE_TABLE);

        tree.registerAction(new DbTreeAction("Insert", QUERY_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void handle(Set<TreeNode> nodes) {
                generateDmlForSelectedTables(DmlType.INSERT);
            }
        }, DbTree.NODE_TYPE_TABLE);

        tree.registerAction(new DbTreeAction("Update", QUERY_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void handle(Set<TreeNode> nodes) {
                generateDmlForSelectedTables(DmlType.UPDATE);
            }
        }, DbTree.NODE_TYPE_TABLE);

        tree.registerAction(new DbTreeAction("Delete", QUERY_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void handle(Set<TreeNode> nodes) {
                generateDmlForSelectedTables(DmlType.DELETE);
            }
        }, DbTree.NODE_TYPE_TABLE);

        tree.registerAction(new DbTreeAction("Drop", FontAwesome.ARROW_DOWN) {
            private static final long serialVersionUID = 1L;

            @Override
            public void handle(Set<TreeNode> nodes) {
                dropSelectedTables();
            }
        }, DbTree.NODE_TYPE_TABLE);

        tree.registerAction(new DbTreeAction("Import", FontAwesome.DOWNLOAD) {
            private static final long serialVersionUID = 1L;

            @Override
            public void handle(Set<TreeNode> nodes) {
                if (nodes.size() > 0) {
                    IDb db = dbTree.getDbForNode(nodes.iterator().next());
                    new DbImportDialog(db.getPlatform(), getTablesFor(nodes)).showAtSize(0.6);
                }
            }
        }, DbTree.NODE_TYPE_TABLE);

        tree.registerAction(new DbTreeAction("Export", FontAwesome.UPLOAD) {
            private static final long serialVersionUID = 1L;

            @Override
            public void handle(Set<TreeNode> nodes) {
                if (nodes.size() > 0) {
                    IDb db = dbTree.getDbForNode(nodes.iterator().next());
                    new DbExportDialog(db.getPlatform(), getTablesFor(nodes),
                            findQueryPanelForDb(db)).showAtSize(0.6);
                }
            }
        }, DbTree.NODE_TYPE_TABLE);

        tree.registerAction(new DbTreeAction("Fill", FontAwesome.BEER) {
            private static final long serialVersionUID = 1L;

            @Override
            public void handle(Set<TreeNode> nodes) {
                if (nodes.size() > 0) {
                    IDb db = dbTree.getDbForNode(nodes.iterator().next());
                    new DbFillDialog(db.getPlatform(), getTablesFor(nodes), findQueryPanelForDb(db))
                            .showAtSize(0.6);
                }

            }
        }, DbTree.NODE_TYPE_TABLE);

        tree.registerAction(new DbTreeAction("Copy Name", FontAwesome.COPY) {
            private static final long serialVersionUID = 1L;

            @Override
            public void handle(Set<TreeNode> nodes) {
                
                for (TreeNode treeNode : nodes) {
                    IDb db = dbTree.getDbForNode(nodes.iterator().next());
                    DatabaseInfo dbInfo = db.getPlatform().getDatabaseInfo();
                    final String quote = dbInfo.getDelimiterToken();
                    final String catalogSeparator = dbInfo.getCatalogSeparator();
                    final String schemaSeparator = dbInfo.getSchemaSeparator();

                    Table table = getTableFor(treeNode);
                    if (table != null) {
                        QueryPanel panel = findQueryPanelForDb(db);
                        panel.appendSql(table.getQualifiedTableName(quote, catalogSeparator, schemaSeparator));
                        contentTabs.setSelectedTab(panel);
                    }
                }
            }
        }, DbTree.NODE_TYPE_TABLE);

        return tree;

    }
    
    protected QueryPanel getQueryPanelForDb(IDb db) {
        if (db != null) {
            Iterator<Component> i = contentTabs.iterator();
            while (i.hasNext()) {
                Component c = i.next();
                if (c instanceof QueryPanel) {
                    QueryPanel panel = (QueryPanel)c;
                    if (panel.getDb().getName().equals(db.getName())) {
                        return panel;
                    }
                }
            }
        }
        return null;
    }

    protected String getTabName(String name) {
        int tabs = contentTabs.getComponentCount();
        String tabName = tabs > 0 ? null : name;
        if (tabName == null) {
            for (int j = 0; j < 10; j++) {
                boolean alreadyUsed = false;
                String suffix = "";
                for (int i = 0; i < tabs; i++) {
                    Tab tab = contentTabs.getTab(i);
                    String currentTabName = tab.getCaption();

                    if (j > 0) {
                        suffix = "-" + j;
                    }
                    if (currentTabName.equals(name + suffix)) {
                        alreadyUsed = true;
                    }
                }

                if (!alreadyUsed) {
                    tabName = name + suffix;
                    break;
                }
            }
        }
        return tabName;
    }
    
    public ISettingsProvider getSettingsProvider() {
        return settingsProvider;
    }
    
    public IDbProvider getDatabaseProvider() {
        return databaseProvider;
    }

    public void refresh() {
        dbTree.refresh();
    }

    public void focus() {
        dbTree.focus();
    }

}
