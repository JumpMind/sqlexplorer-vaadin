package org.jumpmind.symmetric.ui.sqlexplorer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.IDdlReader;
import org.jumpmind.symmetric.ui.common.TreeNode;
import org.jumpmind.symmetric.ui.common.UiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.peter.contextmenu.ContextMenu;
import org.vaadin.peter.contextmenu.ContextMenu.ContextMenuItem;
import org.vaadin.peter.contextmenu.ContextMenu.ContextMenuOpenedListener.TreeListener;
import org.vaadin.peter.contextmenu.ContextMenu.ContextMenuOpenedOnTreeItemEvent;

import com.vaadin.event.Action;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Tree;

public class DbTree extends Tree {

    private static final long serialVersionUID = 1L;

    final Logger log = LoggerFactory.getLogger(getClass());

    public final static String[] TABLE_TYPES = new String[] { "TABLE", "SYSTEM TABLE",
            "SYSTEM VIEW" };

    public static final String NODE_TYPE_DATABASE = "Database";
    public static final String NODE_TYPE_CATALOG = "Catalog";
    public static final String NODE_TYPE_SCHEMA = "Schema";
    public static final String NODE_TYPE_TABLE = "Table";

    IDatabaseProvider databaseProvider;

    Set<TreeNode> expanded = new HashSet<TreeNode>();

    Set<TreeNode> hasBeenExpanded = new HashSet<TreeNode>();

    Map<String, List<DbTreeAction>> actionsByNodeType = new HashMap<String, List<DbTreeAction>>();

    public DbTree(IDatabaseProvider databaseProvider) {
        this.databaseProvider = databaseProvider;
        setWidth(100, Unit.PERCENTAGE);
        setImmediate(true);
        setMultiSelect(true);
        setSelectable(true);
        setItemStyleGenerator(new StyleGenerator());
        Listener listener = new Listener();
        addCollapseListener(listener);
        addExpandListener(listener);
        addActionHandler(new Handler());
    }

    public void registerAction(DbTreeAction action, String... nodeTypes) {
        for (String nodeType : nodeTypes) {
            List<DbTreeAction> actions = actionsByNodeType.get(nodeType);
            if (actions == null) {
                actions = new ArrayList<DbTreeAction>();
                actionsByNodeType.put(nodeType, actions);
            }
            actions.add(action);
        }
    }

 // TODO delete once we decide to not use this context menu
    protected void configureContextMenu() {
        
        ContextMenu treeContextMenu = new ContextMenu();
        treeContextMenu.addContextMenuTreeListener(new TreeListener() {

            @Override
            public void onContextMenuOpenFromTreeItem(ContextMenuOpenedOnTreeItemEvent event) {
                log.info("Opened at " + event.getItemId());
            }
        });
        ContextMenuItem treeItem1 = treeContextMenu.addItem("Tree test item #1");
        treeItem1.setSeparatorVisible(true);
        treeItem1.addStyleName("treeStyle1");
        treeContextMenu.addItem("Tree test item #2").setEnabled(false);
        treeContextMenu.setAsTreeContextMenu(this);
    }

    public void refresh() {
        hasBeenExpanded.clear();
        List<IDb> databases = databaseProvider.getDatabases();
        Set<TreeNode> expandedItems = new HashSet<TreeNode>(expanded);
        expanded.clear();
        Set<TreeNode> selected = getSelected();
        removeAllItems();
        TreeNode firstNode = null;
        for (IDb database : databases) {
            TreeNode databaseNode = new TreeNode(database.getName(), NODE_TYPE_DATABASE,
                    FontAwesome.DATABASE, null);
            addItem(databaseNode);
            setItemIcon(databaseNode, databaseNode.getIcon());

            if (firstNode == null) {
                firstNode = databaseNode;
            }
        }

        for (TreeNode expandedItem : expandedItems) {
            expandItem(expandedItem);
        }

        if (selected == null || selected.size() == 0) {
            selected = new HashSet<TreeNode>();
            selected.add(firstNode);
        }
        setValue(selected);
        focus();

    }

    @SuppressWarnings("unchecked")
    protected Set<TreeNode> getSelected() {
        return (Set<TreeNode>) getValue();
    }

    @SuppressWarnings("unchecked")
    protected Set<TreeNode> getSelected(String type) {
        HashSet<TreeNode> nodes = new HashSet<TreeNode>();
        Set<TreeNode> selected = (Set<TreeNode>) getValue();
        for (TreeNode treeNode : selected) {
            if (treeNode.getType().equals(type)) {
                nodes.add(treeNode);
            }
        }
        return nodes;
    }

    protected IDb getDbForNode(TreeNode node) {
        while (node.getParent() != null) {
            node = node.getParent();
        }
        String databaseName = node.getName();
        List<IDb> databases = databaseProvider.getDatabases();
        for (IDb database : databases) {
            if (database.getName().equals(databaseName)) {
                return database;
            }
        }
        return null;
    }

    protected void expanded(TreeNode treeNode) {
        if (!hasBeenExpanded.contains(treeNode)) {
            hasBeenExpanded.add(treeNode);

            try {
                IDatabasePlatform platform = getDbForNode(treeNode).getPlatform();
                IDdlReader reader = platform.getDdlReader();

                Collection<?> children = getChildren(treeNode);
                if (children == null || children.size() == 0) {
                    if (treeNode.getType().equals(NODE_TYPE_DATABASE)) {
                        List<TreeNode> nextLevel = new ArrayList<TreeNode>();
                        List<String> catalogs = reader.getCatalogNames();
                        if (catalogs.size() > 0) {
                            if (catalogs.remove(platform.getDefaultCatalog())) {
                                catalogs.add(0, platform.getDefaultCatalog());
                            }
                            for (String catalog : catalogs) {
                                TreeNode catalogNode = new TreeNode(catalog, NODE_TYPE_CATALOG,
                                        FontAwesome.BOOK, treeNode);
                                nextLevel.add(catalogNode);
                            }
                        } else {
                            List<String> schemas = reader.getSchemaNames(null);
                            if (schemas.remove(platform.getDefaultSchema())) {
                                schemas.add(0, platform.getDefaultSchema());
                            }
                            for (String schema : schemas) {
                                TreeNode schemaNode = new TreeNode(schema, NODE_TYPE_SCHEMA,
                                        FontAwesome.BOOK, treeNode);
                                nextLevel.add(schemaNode);
                            }
                        }

                        if (nextLevel.size() == 0) {
                            nextLevel.addAll(getTableTreeNodes(reader, treeNode, null, null));
                        }

                        treeNode.getChildren().addAll(nextLevel);
                        for (TreeNode node : nextLevel) {
                            addTreeNode(node);
                        }
                    } else if (treeNode.getType().equals(NODE_TYPE_CATALOG)) {
                        List<String> schemas = reader.getSchemaNames(treeNode.getName());
                        if (schemas.size() > 0) {
                            if (schemas.remove(platform.getDefaultSchema())) {
                                schemas.add(0, platform.getDefaultSchema());
                            }
                            for (String schema : schemas) {
                                TreeNode schemaNode = new TreeNode(schema, NODE_TYPE_SCHEMA,
                                        FontAwesome.BOOK, treeNode);
                                treeNode.getChildren().add(schemaNode);
                                addTreeNode(schemaNode);
                            }
                        } else {
                            addTableNodes(reader, treeNode, treeNode.getName(), null);
                        }

                    } else if (treeNode.getType().equals(NODE_TYPE_SCHEMA)) {
                        String catalogName = null;
                        TreeNode parent = (TreeNode) getParent(treeNode);
                        if (parent != null && parent.getType().equals(NODE_TYPE_CATALOG)) {
                            catalogName = parent.getName();
                        }
                        addTableNodes(reader, treeNode, catalogName, treeNode.getName());
                    }

                    setChildrenAllowed(treeNode, treeNode.getChildren().size() > 0);
                }
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                UiUtils.notify(ex);
            }
        }
    }

    protected void addTreeNode(TreeNode node) {
        addItem(node);
        setParent(node, node.getParent());
        setItemIcon(node, node.getIcon());
        setChildrenAllowed(node, !node.getType().equals(NODE_TYPE_TABLE));
    }

    protected List<TreeNode> getTableTreeNodes(IDdlReader reader, TreeNode parent,
            String catalogName, String schemaName) {
        List<TreeNode> list = new ArrayList<TreeNode>();
        List<String> tables = reader.getTableNames(catalogName, schemaName, TABLE_TYPES);
        for (String tableName : tables) {
            TreeNode treeNode = new TreeNode(tableName, NODE_TYPE_TABLE, FontAwesome.TABLE, parent);
            if (catalogName != null) {
                treeNode.getProperties().setProperty("catalogName", catalogName);
            }
            if (schemaName != null) {
                treeNode.getProperties().setProperty("schemaName", schemaName);
            }
            list.add(treeNode);
        }
        return list;
    }

    protected void addTableNodes(IDdlReader reader, TreeNode parent, String catalogName,
            String schemaName) {
        List<TreeNode> nodes = getTableTreeNodes(reader, parent, catalogName, schemaName);
        for (TreeNode treeNode : nodes) {
            parent.getChildren().add(treeNode);
            addTreeNode(treeNode);
        }
    }

    class Listener implements CollapseListener, ExpandListener {

        private static final long serialVersionUID = 1L;

        @Override
        public void nodeCollapse(CollapseEvent event) {
            expanded.remove(event.getItemId());
        }

        @Override
        public void nodeExpand(ExpandEvent event) {
            TreeNode node = (TreeNode) event.getItemId();
            expanded.add(node);
            expanded(node);
        }

    }

    class StyleGenerator implements ItemStyleGenerator {
        private static final long serialVersionUID = 1L;

        public String getStyle(Tree source, Object itemId) {
            if (itemId instanceof TreeNode) {
                TreeNode node = (TreeNode) itemId;
                IDatabasePlatform platform = getDbForNode(node).getPlatform();
                if (node.getType().equals(NODE_TYPE_CATALOG)) {
                    String catalog = platform.getDefaultCatalog();
                    if (catalog != null && catalog.equals(node.getName())) {
                        return "bold";
                    }
                } else if (node.getType().equals(NODE_TYPE_SCHEMA)) {
                    String schema = platform.getDefaultSchema();
                    if (schema != null && schema.equals(node.getName())) {
                        return "bold";
                    }
                }
            }
            return null;

        }
    }

    class Handler implements com.vaadin.event.Action.Handler {

        private static final long serialVersionUID = 1L;

        @Override
        public Action[] getActions(Object target, Object sender) {
            if (target instanceof TreeNode) {
                TreeNode treeNode = (TreeNode) target;
                List<DbTreeAction> actions = actionsByNodeType.get(treeNode.getType());
                if (actions != null) {
                    return actions.toArray(new Action[actions.size()]);
                }
            }
            return new Action[0];

        }

        @Override
        public void handleAction(Action action, Object sender, Object target) {
            if (action instanceof DbTreeAction) {
                if (!getSelected().contains(target)) {
                    select(target);
                }
                TreeNode node = (TreeNode) target;
                ((DbTreeAction) action).handle(getSelected(node.getType()));
            }
        }
    }

}
