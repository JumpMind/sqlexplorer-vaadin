package org.jumpmind.symmetric.ui.sqlexplorer;

import java.util.HashSet;
import java.util.Set;

import org.jumpmind.symmetric.ui.common.TreeNode;
import org.jumpmind.symmetric.ui.common.UiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.annotations.StyleSheet;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
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

    final static FontAwesome QUERY_ICON = FontAwesome.BOLT;

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

    public SqlExplorer(String configDir, IDbProvider databaseProvider, String user) {
        this(databaseProvider, new DefaultSettingsProvider(configDir), user);
    }

    public SqlExplorer(IDbProvider databaseProvider, ISettingsProvider settingsProvider, String user) {
        this.databaseProvider = databaseProvider;
        this.settingsProvider = settingsProvider;

        setSizeFull();
        addStyleName("sqlexplorer");

        VerticalLayout leftLayout = new VerticalLayout();
        leftLayout.setSizeFull();
        leftLayout.addStyleName(ValoTheme.MENU_ROOT);

        leftLayout.addComponent(buildLeftMenu());

        Panel scrollable = new Panel();
        scrollable.setSizeFull();
        scrollable.addStyleName(ValoTheme.PANEL_BORDERLESS);

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

        contentTabs = UiUtils.createTabSheet();
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

        setSplitPosition(DEFAULT_SPLIT_POS, Unit.PIXELS);
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
                showButton.setVisible(true);
            }
        });
        hideButton.setDescription("Hide Database Explorer");
        hideButton.setIcon(FontAwesome.BARS);

        MenuItem refreshButton = leftMenu.addItem("", new Command() {
            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                dbTree.refresh();
            }
        });
        refreshButton.setIcon(FontAwesome.REFRESH);

        MenuItem queryWindow = leftMenu.addItem("", new Command() {
            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                openQueryWindow(dbTree.getSelected());
            }
        });
        queryWindow.setIcon(QUERY_ICON);

        MenuItem settings = leftMenu.addItem("", new Command() {
            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                SettingsDialog dialog = new SettingsDialog(settingsProvider);
                dialog.showAtSize(.5);
            }
        });
        settings.setIcon(FontAwesome.GEARS);
        return leftMenu;
    }

    protected void addShowButton(MenuBar contentMenuBar) {
        boolean visible = showButton != null ? showButton.isVisible() : false;
        showButton = contentMenuBar.addItem("", new Command() {
            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                setSplitPosition(savedSplitPosition, Unit.PIXELS);
                showButton.setVisible(false);
            }
        });
        showButton.setIcon(FontAwesome.BARS);
        showButton.setDescription("Show Database Explorer");
        showButton.setVisible(visible);
    }

    protected void selectContentTab(IContentTab tab) {
        if (selected != null) {
            selected.unselected();
        }
        contentTabs.setSelectedTab(tab);
        contentMenuBar.removeItems();
        addShowButton(contentMenuBar);
        tab.selected(contentMenuBar);
        selected = tab;
    }

    protected void openQueryWindow(Set<TreeNode> nodes) {
        Set<String> dbNames = new HashSet<String>();
        for (TreeNode node : nodes) {
            IDb db = dbTree.getDbForNode(node);
            String dbName = db.getName();
            if (!dbNames.contains(dbName)) {
                dbNames.add(dbName);
                QueryPanel panel = new QueryPanel(db, settingsProvider, user);
                Tab tab = contentTabs.addTab(panel, getTabName(dbName));
                tab.setClosable(true);
                tab.setIcon(QUERY_ICON);
                selectContentTab(panel);
            }
        }
    }

    protected DbTree buildDbTree() {

        final DbTree tree = new DbTree(databaseProvider, settingsProvider);
        tree.registerAction(new DbTreeAction("Query", QUERY_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void handle(Set<TreeNode> nodes) {
                openQueryWindow(nodes);
            }
        }, DbTree.NODE_TYPE_DATABASE);
        return tree;

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

    public void refresh() {
        dbTree.refresh();
    }

    public void focus() {
        dbTree.focus();
    }

}
