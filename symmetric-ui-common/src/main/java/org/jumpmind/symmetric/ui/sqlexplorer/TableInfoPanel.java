package org.jumpmind.symmetric.ui.sqlexplorer;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.JdbcSqlTemplate;
import org.jumpmind.symmetric.ui.common.UiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

public class TableInfoPanel extends VerticalLayout implements IContentTab {

    protected static final Logger log = LoggerFactory.getLogger(TableInfoPanel.class);

    private static final long serialVersionUID = 1L;

    public TableInfoPanel(TableName table, IDatabasePlatform databasePlatform, Settings settings) {

        setSizeFull();

        TabSheet tabSheet = UiUtils.createTabSheet();
        addComponent(tabSheet);

        JdbcSqlTemplate sqlTemplate = (JdbcSqlTemplate) databasePlatform.getSqlTemplate();

        tabSheet.addTab(
                create(new ColumnMetaDataTableCreator(sqlTemplate, table, settings), table),
                "Columns");
        tabSheet.addTab(
                create(new PrimaryKeyMetaDataTableCreator(sqlTemplate, table, settings), table),
                "Primary Keys");
        tabSheet.addTab(create(new IndexMetaDataTableCreator(sqlTemplate, table, settings), table),
                "Indexes");
        if (databasePlatform.getDatabaseInfo().isForeignKeysSupported()) {
            tabSheet.addTab(
                    create(new ImportedKeysMetaDataTableCreator(sqlTemplate, table, settings),
                            table), "Imported Keys");
            tabSheet.addTab(
                    create(new ExportedKeysMetaDataTableCreator(sqlTemplate, table, settings),
                            table), "Exported Keys");
        }
    }

    protected AbstractLayout create(AbstractMetaDataTableCreator creator, TableName tableName) {
        Table table = creator.create(tableName);
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
