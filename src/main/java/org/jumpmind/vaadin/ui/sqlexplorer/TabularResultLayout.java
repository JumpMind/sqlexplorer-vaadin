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

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.jumpmind.vaadin.ui.sqlexplorer.Settings.SQL_EXPLORER_MAX_RESULTS;
import static org.jumpmind.vaadin.ui.sqlexplorer.Settings.SQL_EXPLORER_SHOW_ROW_NUMBERS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.DatabaseInfo;
import org.jumpmind.db.platform.IDdlReader;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.ReadOnlyTextAreaDialog;
import org.jumpmind.vaadin.ui.sqlexplorer.SqlRunner.ISqlRunnerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.addon.contextmenu.ContextMenu;
import com.vaadin.addon.tableexport.CsvExport;
import com.vaadin.addon.tableexport.DefaultTableHolder;
import com.vaadin.addon.tableexport.ExcelExport;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class TabularResultLayout extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    final String ACTION_SELECT = "Select From";

    final String ACTION_INSERT = "Insert";

    final String ACTION_UPDATE = "Update";

    final String ACTION_DELETE = "Delete";

    final Logger log = LoggerFactory.getLogger(getClass());

    String tableName;

    String catalogName;

    String schemaName;

    Grid grid;

    org.jumpmind.db.model.Table resultTable;

    String sql;

    ResultSet rs;

    IDb db;

    ISqlRunnerListener listener;

    Settings settings;
    
    Table table;

    boolean showSql = true;

    public TabularResultLayout(IDb db, String sql, ResultSet rs, ISqlRunnerListener listener, Settings settings, boolean showSql)
            throws SQLException {
        this.sql = sql;
        this.showSql = showSql;
        this.db = db;
        this.rs = rs;
        this.listener = listener;
        this.settings = settings;
        createTabularResultLayout();
    }

    public String getSql() {
        return sql;
    }

    public void setShowSql(boolean showSql) {
        this.showSql = showSql;
    }

    protected void createTabularResultLayout() {
        this.setSizeFull();
        this.setSpacing(false);

        HorizontalLayout resultBar = new HorizontalLayout();
        resultBar.setWidth(100, Unit.PERCENTAGE);
        resultBar.setMargin(new MarginInfo(false, true, false, true));

        HorizontalLayout leftBar = new HorizontalLayout();
        leftBar.setSpacing(true);
        final Label resultLabel = new Label("", ContentMode.HTML);
        leftBar.addComponent(resultLabel);

        final Label sqlLabel = new Label("", ContentMode.TEXT);
        sqlLabel.setWidth(800, Unit.PIXELS);
        leftBar.addComponent(sqlLabel);

        resultBar.addComponent(leftBar);
        resultBar.setComponentAlignment(leftBar, Alignment.MIDDLE_LEFT);
        resultBar.setExpandRatio(leftBar, 1);

        MenuBar rightBar = new MenuBar();
        rightBar.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
        rightBar.addStyleName(ValoTheme.MENUBAR_SMALL);

        MenuItem refreshButton = rightBar.addItem("", new Command() {
            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                listener.reExecute(sql);
            }
        });
        refreshButton.setIcon(FontAwesome.REFRESH);

        MenuItem exportButton = rightBar.addItem("", new Command() {
            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                new ExportDialog(TabularResultLayout.this).show();
            }
        });
        exportButton.setIcon(FontAwesome.UPLOAD);

        resultBar.addComponent(rightBar);
        resultBar.setComponentAlignment(rightBar, Alignment.MIDDLE_RIGHT);

        this.addComponent(resultBar);

        try {
            grid = putResultsInGrid(settings.getProperties().getInt(SQL_EXPLORER_MAX_RESULTS));
            grid.setSizeFull();

            ContextMenu menu = new ContextMenu(grid, true);
            menu.addItem(ACTION_SELECT, new ContextMenu.Command() {

                private static final long serialVersionUID = 1L;

                @Override
                public void menuSelected(com.vaadin.addon.contextmenu.MenuItem selectedItem) {
                    handleAction(ACTION_SELECT);
                }
            });
            menu.addItem(ACTION_INSERT, new ContextMenu.Command() {

                private static final long serialVersionUID = 1L;

                @Override
                public void menuSelected(com.vaadin.addon.contextmenu.MenuItem selectedItem) {
                    handleAction(ACTION_INSERT);
                }
            });
            menu.addItem(ACTION_UPDATE, new ContextMenu.Command() {

                private static final long serialVersionUID = 1L;

                @Override
                public void menuSelected(com.vaadin.addon.contextmenu.MenuItem selectedItem) {
                    handleAction(ACTION_UPDATE);
                }
            });
            menu.addItem(ACTION_DELETE, new ContextMenu.Command() {

                private static final long serialVersionUID = 1L;

                @Override
                public void menuSelected(com.vaadin.addon.contextmenu.MenuItem selectedItem) {
                    handleAction(ACTION_DELETE);
                }
            });

            grid.addItemClickListener(new ItemClickListener() {

                private static final long serialVersionUID = 1L;

                @SuppressWarnings("unchecked")
				@Override
                public void itemClick(ItemClickEvent event) {
                    MouseButton button = event.getButton();
                    if (button == MouseButton.LEFT) {
                        Object object = event.getPropertyId();
                        if (object != null && !object.toString().equals("")) {
                            if (event.isDoubleClick()) {
                                Object prop = event.getPropertyId();
                                String header = grid.getColumn(prop).getHeaderCaption();
                                Property<?> p = event.getItem().getItemProperty(prop);
                                if (p != null) {
                                    String data = String.valueOf(p.getValue());
                                	Object[] primaryKeys = null;
                                    boolean binary = resultTable != null ? resultTable.getColumnWithName(header).isOfBinaryType() : false;
                                    boolean isLob = resultTable != null ? resultTable.getColumnWithName(header).isLob() : false;
                                    if (isLob) {
                                    	primaryKeys = ((HashMap<Object, List<Object>>) grid.getData()).get(p.getValue()).toArray();
                                    }
                                    if (binary) {
                                        ReadOnlyTextAreaDialog.show(header, data.toUpperCase(), table, primaryKeys, db.getPlatform(), binary, isLob);
                                    } else {
                                        ReadOnlyTextAreaDialog.show(header, data, table, primaryKeys, db.getPlatform(), binary, isLob);
                                    }
                                }

                            } else {
                                Object row = event.getItemId();
                                if (!grid.getSelectedRows().contains(row)) {
                                    grid.deselectAll();
                                    grid.select(row);
                                } else {
                                    grid.deselect(row);
                                }
                            }
                        }
                    }
                }
            });

            this.addComponent(grid);
            this.setExpandRatio(grid, 1);

            int count = (grid.getContainerDataSource().getItemIds().size());
            int maxResultsSize = settings.getProperties().getInt(SQL_EXPLORER_MAX_RESULTS);
            if (count >= maxResultsSize) {
                resultLabel.setValue("Limited to <span style='color: red'>" + maxResultsSize + "</span> rows;");
            } else {
                resultLabel.setValue(count + " rows returned;");
            }
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
            CommonUiUtils.notify(ex);
        }

        if (showSql) {
            sqlLabel.setValue(StringUtils.abbreviate(sql, 200));
        }

    }

    protected void handleAction(String action) {
        try {
            DatabaseInfo dbInfo = db.getPlatform().getDatabaseInfo();
            final String quote = db.getPlatform().getDdlBuilder().isDelimitedIdentifierModeOn() ? dbInfo.getDelimiterToken() : "";
            final String catalogSeparator = dbInfo.getCatalogSeparator();
            final String schemaSeparator = dbInfo.getSchemaSeparator();

            String[] columnHeaders = CommonUiUtils.getHeaderCaptions(grid);
            Collection<Object> selectedRowsSet = grid.getSelectedRows();
            Iterator<Object> setIterator = selectedRowsSet.iterator();
            while (setIterator.hasNext()) {
                List<Object> typeValueList = new ArrayList<Object>();
                int row = (Integer) setIterator.next();
                Item item = grid.getContainerDataSource().getItem(row);
                Iterator<?> iterator = item.getItemPropertyIds().iterator();
                iterator.next();

                for (int i = 1; i < columnHeaders.length; i++) {
                    Object typeValue = item.getItemProperty(iterator.next()).getValue();
                    if (typeValue instanceof String) {
                        if ("<null>".equals(typeValue) || "".equals(typeValue)) {
                            typeValue = "null";
                        } else {
                            typeValue = "'" + typeValue + "'";
                        }
                    } else if (typeValue instanceof java.util.Date) {
                        typeValue = "{ts " + "'" + FormatUtils.TIMESTAMP_FORMATTER.format(typeValue) + "'" + "}";
                    }
                    typeValueList.add(typeValue);
                }

                if (action.equals(ACTION_SELECT)) {
                    StringBuilder sql = new StringBuilder("SELECT ");

                    for (int i = 1; i < columnHeaders.length; i++) {
                        if (i == 1) {
                            sql.append(quote).append(columnHeaders[i]).append(quote);
                        } else {
                            sql.append(", ").append(quote).append(columnHeaders[i]).append(quote);
                        }
                    }

                    sql.append(" FROM " + org.jumpmind.db.model.Table.getFullyQualifiedTableName(catalogName, schemaName, tableName, quote,
                            catalogSeparator, schemaSeparator));

                    sql.append(" WHERE ");

                    int track = 0;
                    for (int i = 0; i < resultTable.getColumnCount(); i++) {
                        Column col = resultTable.getColumn(i);
                        if (col.isPrimaryKey()) {
                            if (track == 0) {
                                sql.append(col.getName() + "=" + typeValueList.get(i));
                            } else {
                                sql.append(" and ").append(quote).append(col.getName()).append(quote).append("=")
                                        .append(typeValueList.get(i));
                            }
                            track++;
                        }
                    }
                    sql.append(";");
                    listener.writeSql(sql.toString());
                } else if (action.equals(ACTION_INSERT)) {
                    StringBuilder sql = new StringBuilder();
                    sql.append("INSERT INTO ").append(org.jumpmind.db.model.Table.getFullyQualifiedTableName(catalogName, schemaName,
                            tableName, quote, catalogSeparator, schemaSeparator)).append(" (");

                    for (int i = 1; i < columnHeaders.length; i++) {
                        if (i == 1) {
                            sql.append(quote + columnHeaders[i] + quote);
                        } else {
                            sql.append(", " + quote + columnHeaders[i] + quote);
                        }
                    }
                    sql.append(") VALUES (");
                    boolean first = true;
                    for (int i = 1; i < columnHeaders.length; i++) {
                        if (first) {
                            first = false;
                        } else {
                            sql.append(", ");
                        }
                        sql.append(typeValueList.get(i - 1));
                    }
                    sql.append(");");
                    listener.writeSql(sql.toString());

                } else if (action.equals(ACTION_UPDATE)) {
                    StringBuilder sql = new StringBuilder("UPDATE ");
                    sql.append(org.jumpmind.db.model.Table.getFullyQualifiedTableName(catalogName, schemaName, tableName, quote,
                            catalogSeparator, schemaSeparator) + " SET ");
                    for (int i = 1; i < columnHeaders.length; i++) {
                        if (i == 1) {
                            sql.append(quote).append(columnHeaders[i]).append(quote).append("=");
                        } else {
                            sql.append(", ").append(quote).append(columnHeaders[i]).append(quote).append("=");
                        }

                        sql.append(typeValueList.get(i - 1));
                    }

                    sql.append(" WHERE ");

                    int track = 0;
                    for (int i = 0; i < resultTable.getColumnCount(); i++) {
                        Column col = resultTable.getColumn(i);
                        if (col.isPrimaryKey()) {
                            if (track == 0) {
                                sql.append(quote).append(col.getName()).append(quote).append("=").append(typeValueList.get(i));
                            } else {
                                sql.append(" and ").append(quote).append(col.getName()).append(quote).append("=")
                                        .append(typeValueList.get(i));
                            }
                            track++;
                        }
                    }
                    sql.append(";");
                    listener.writeSql(sql.toString());

                } else if (action.equals(ACTION_DELETE)) {
                    StringBuilder sql = new StringBuilder("DELETE FROM ");
                    sql.append(org.jumpmind.db.model.Table.getFullyQualifiedTableName(catalogName, schemaName, tableName, quote,
                            catalogSeparator, schemaSeparator)).append(" WHERE ");
                    int track = 0;
                    for (int i = 0; i < resultTable.getColumnCount(); i++) {
                        Column col = resultTable.getColumn(i);
                        if (col.isPrimaryKey()) {
                            if (track == 0) {
                                sql.append(quote).append(col.getName()).append(quote).append("=").append(typeValueList.get(i));
                            } else {
                                sql.append(" and ").append(quote).append(col.getName()).append(quote).append("=")
                                        .append(typeValueList.get(i));
                            }
                            track++;
                        }
                    }
                    sql.append(";");
                    listener.writeSql(sql.toString());
                }
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            Notification.show("There are an error while attempting to perform the action.  Please check the log file for further details.");
        }
    }

    protected static String getTypeValue(String type) {
        String value = null;
        if (type.equalsIgnoreCase("CHAR")) {
            value = "''";
        } else if (type.equalsIgnoreCase("VARCHAR")) {
            value = "''";
        } else if (type.equalsIgnoreCase("LONGVARCHAR")) {
            value = "''";
        } else if (type.equalsIgnoreCase("DATE")) {
            value = "''";
        } else if (type.equalsIgnoreCase("TIME")) {
            value = "''";
        } else if (type.equalsIgnoreCase("TIMESTAMP")) {
            value = "{ts ''}";
        } else if (type.equalsIgnoreCase("CLOB")) {
            value = "''";
        } else if (type.equalsIgnoreCase("BLOB")) {
            value = "''";
        } else if (type.equalsIgnoreCase("ARRAY")) {
            value = "[]";
        } else {
            value = "";
        }
        return value;
    }

    protected Grid putResultsInGrid(int maxResultSize) throws SQLException {
        String parsedSql = sql;
        String first = "";
        String second = "";
        String third = "";
        parsedSql = parsedSql.substring(parsedSql.toUpperCase().indexOf("FROM ") + 5, parsedSql.length());
        parsedSql = parsedSql.trim();
        String separator = ".";
        if (parsedSql.contains(separator)) {
            first = parsedSql.substring(0, parsedSql.indexOf(separator) + separator.length() - 1);
            parsedSql = parsedSql.substring(parsedSql.indexOf(separator) + separator.length(), parsedSql.length());
            if (parsedSql.contains(separator)) {
                second = parsedSql.substring(0, parsedSql.indexOf(separator) + separator.length() - 1);
                parsedSql = parsedSql.substring(parsedSql.indexOf(separator) + separator.length(), parsedSql.length());
                if (parsedSql.contains(separator)) {
                    third = parsedSql.substring(0, parsedSql.indexOf(separator) + separator.length() - 1);
                    parsedSql = parsedSql.substring(parsedSql.indexOf(separator) + separator.length(), parsedSql.length());
                } else {
                    third = parsedSql;
                }
            } else {
                second = parsedSql;
            }
        } else {
            first = parsedSql;
        }
        if (!third.equals("")) {
            tableName = third;
            schemaName = second;
            catalogName = first;
        } else if (!second.equals("")) {
            if (db.getPlatform().getDefaultCatalog() != null) {
                IDdlReader reader = db.getPlatform().getDdlReader();
                List<String> catalogs = reader.getCatalogNames();
                if (catalogs.contains(first)) {
                    catalogName = first;
                } else if (db.getPlatform().getDefaultSchema() != null) {
                    Iterator<String> iterator = catalogs.iterator();
                    while (iterator.hasNext()) {
                        List<String> schemas = reader.getSchemaNames(iterator.next());
                        if (schemas.contains(first)) {
                            schemaName = first;
                        }
                    }
                }
            } else if (db.getPlatform().getDefaultSchema() != null) {
                schemaName = first;
            }
            tableName = second;
        } else if (!first.equals("")) {
            tableName = parsedSql;
        }

        if (isNotBlank(tableName)) {
            if (tableName.contains(" ")) {
                tableName = tableName.substring(0, tableName.indexOf(" "));
            }
            if (isBlank(schemaName)) {
                schemaName = null;
            }
            if (isBlank(catalogName)) {
                catalogName = null;
            }
            String quote = "\"";
            if (catalogName != null && catalogName.contains(quote)) {
                catalogName = catalogName.replaceAll(quote, "");
                catalogName = catalogName.trim();
            }
            if (schemaName != null && schemaName.contains(quote)) {
                schemaName = schemaName.replaceAll(quote, "");
                schemaName = schemaName.trim();
            }
            if (tableName != null && tableName.contains(quote)) {
                tableName = tableName.replaceAll(quote, "");
                tableName = tableName.trim();
            }
            try {
                resultTable = db.getPlatform().getTableFromCache(catalogName, schemaName, tableName, false);
                if (resultTable != null) {
                    tableName = resultTable.getName();
                    if (isNotBlank(catalogName) && isNotBlank(resultTable.getCatalog())) {
                        catalogName = resultTable.getCatalog();
                    }
                    if (isNotBlank(schemaName) && isNotBlank(resultTable.getSchema())) {
                        schemaName = resultTable.getSchema();
                    }

                }
            } catch (Exception e) {
                log.debug("Failed to lookup table: " + tableName, e);
            }
        }

        int columnCount = rs.getMetaData().getColumnCount();
        for (int i=0; i<columnCount; i++) {
        	int type = rs.getMetaData().getColumnType(i+1);
        	if (type == Types.BLOB || type == Types.CLOB || type == Types.NCLOB) {
                table = db.getPlatform().getTableFromCache(catalogName, schemaName, tableName, true);
                break;
        	}
        }
        List<Integer> pkcolumns = new ArrayList<Integer>();
        if (table != null) {
        	for (Column pkcolumn : table.getPrimaryKeyColumns()) {
        		pkcolumns.add(table.getColumnIndex(pkcolumn));
        	}
        }
        
        TypedProperties properties = settings.getProperties();
        return CommonUiUtils.putResultsInGrid(rs, pkcolumns, properties.getInt(SQL_EXPLORER_MAX_RESULTS), properties.is(SQL_EXPLORER_SHOW_ROW_NUMBERS),
                getColumnsToExclude());

    }

    protected String[] getColumnsToExclude() {
        return new String[0];
    }

    public void csvExport() {
        if (grid != null) {
            CsvExport csvExport = new CsvExport(new DefaultTableHolder(grid));
            csvExport.excludeCollapsedColumns();
            csvExport.setDisplayTotals(false);
            csvExport.setExportFileName(db.getName() + "-export.csv");
            csvExport.setReportTitle(sql);
            csvExport.export();
        }
    }

    public void excelExport() {
        if (grid != null) {
            ExcelExport excelExport = new ExcelExport(new DefaultTableHolder(grid));
            excelExport.excludeCollapsedColumns();
            excelExport.setDisplayTotals(false);
            excelExport.setExportFileName(db.getName() + "-export.xls");
            excelExport.setReportTitle(sql);
            excelExport.export();
        }
    }
}
