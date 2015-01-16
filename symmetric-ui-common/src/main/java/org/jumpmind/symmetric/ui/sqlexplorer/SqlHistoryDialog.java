package org.jumpmind.symmetric.ui.sqlexplorer;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.symmetric.ui.common.DateColumnFormatter;
import org.jumpmind.symmetric.ui.common.DurationFormatter;
import org.jumpmind.symmetric.ui.common.ResizableWindow;
import org.jumpmind.symmetric.ui.common.CommonUiUtils;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class SqlHistoryDialog extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    private final com.vaadin.ui.Table table;

    private String filterCriteria = null;

    private QueryPanel queryPanel;

    private ISettingsProvider settingsProvider;

    public SqlHistoryDialog(ISettingsProvider settingsProvider, QueryPanel queryPanel) {
        super("Sql History");
        this.settingsProvider = settingsProvider;
        this.queryPanel = queryPanel;

        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setSizeFull();
        mainLayout.setMargin(true);
        mainLayout.setSpacing(true);
        addComponent(mainLayout, 1);        
        
        final List<SqlHistory> sqlHistories = settingsProvider.get().getSqlHistory();

        HorizontalLayout searchPanel = new HorizontalLayout();
        searchPanel.setWidth(100, Unit.PERCENTAGE);
        searchPanel.setSpacing(true);
        
        Label spacer = new Label();
        searchPanel.addComponent(spacer);
        searchPanel.setExpandRatio(spacer, 1);

        final TextField filterField = new TextField();
        filterField.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        filterField.setIcon(FontAwesome.SEARCH);
        filterField.setInputPrompt("Filter Sql");
        filterField.setImmediate(true);
        filterField.setTextChangeEventMode(TextChangeEventMode.LAZY);
        filterField.setTextChangeTimeout(200);

        searchPanel.addComponent(filterField);
        mainLayout.addComponent(searchPanel);

        table = CommonUiUtils.createTable();
        table.setImmediate(true);
        table.setSortEnabled(true);
        table.setSelectable(true);
        table.setMultiSelect(true);
        table.setColumnCollapsingAllowed(false);

        final BeanContainer<String, SqlHistory> container = new BeanContainer<String, SqlHistory>(
                SqlHistory.class);

        filterField.addTextChangeListener(new TextChangeListener() {
            private static final long serialVersionUID = 1L;

            public void textChange(TextChangeEvent event) {
                filterCriteria = (String) event.getText();
                container.removeAllItems();
                for (int i = 0; i < sqlHistories.size(); i++) {
                    SqlHistory temp = sqlHistories.get(i);
                    if (StringUtils.containsIgnoreCase(temp.getSqlStatement(), filterCriteria)
                            || filterCriteria.equals("")
                            || StringUtils.containsIgnoreCase(
                                    String.valueOf(temp.getExecuteCount()), filterCriteria)
                            || StringUtils.containsIgnoreCase(CommonUiUtils
                                    .formatDuration(temp.getLastExecuteDuration()), filterCriteria)
                            || StringUtils.containsIgnoreCase(
                                    CommonUiUtils.formatDateTime(temp.getLastExecuteTime()),
                                    filterCriteria)
                            || StringUtils.containsIgnoreCase(temp.getLastExecuteUserId(),
                                    filterCriteria)) {
                        container.addBean(temp);
                    }
                }
            }
        });

        container.setBeanIdProperty("sqlStatement");

        table.setContainerDataSource(container);
        table.setColumnCollapsingAllowed(true);

        table.setColumnHeader("sqlStatement", "SQL");

        table.setColumnHeader("lastExecuteTime", "Time");
        table.addGeneratedColumn("lastExecuteTime", new DateColumnFormatter());

        table.setColumnHeader("lastExecuteDuration", "Duration");
        table.addGeneratedColumn("lastExecuteDuration", new DurationFormatter());
        table.setColumnCollapsed("lastExecuteDuration", true);

        table.setColumnHeader("lastExecuteUserId", "User");
        table.setColumnCollapsed("lastExecuteUserId", true);

        table.setColumnHeader("executeCount", "Count");

        table.setVisibleColumns(new Object[] { "sqlStatement", "executeCount", "lastExecuteTime",
                "lastExecuteDuration", "lastExecuteUserId" });

        table.setColumnExpandRatio("sqlStatement", 1);
        table.setColumnWidth("executeCount", 55);
        table.setColumnWidth("lastExecuteTime", 150);

        table.addItemClickListener(new ItemClickEvent.ItemClickListener() {

            private static final long serialVersionUID = 1L;

            public void itemClick(ItemClickEvent event) {
                if (event.isDoubleClick()) {
                    select();
                }
            }
        });

        table.setSizeFull();

        mainLayout.addComponent(table);
        mainLayout.setExpandRatio(table, 1);

        container.addAll(sqlHistories);
        
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = 1L;

            public void buttonClick(ClickEvent event) {
                close();
            }
        });

        Button applyButton = CommonUiUtils.createPrimaryButton("Select");
        applyButton.setClickShortcut(KeyCode.ENTER);
        applyButton.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = 1L;

            public void buttonClick(ClickEvent event) {
                select();
            }
        });
        
        addComponent(buildButtonFooter(cancelButton, applyButton));

        if (sqlHistories.size() > 0) {
            Set<String> toSelect = new HashSet<String>();
            toSelect.add((String) table.getItemIds().iterator().next());
            table.setValue(toSelect);
        }

    }

    protected void select() {
        @SuppressWarnings("unchecked")
        Collection<String> values = (Collection<String>) table.getValue();
        if (values != null && values.size() > 0) {
            String delimiter = settingsProvider.get().getProperties().get(Settings.SQL_EXPLORER_DELIMITER);
            for (String sql : values) {
                queryPanel.writeSql(sql + (sql.trim().endsWith(delimiter) ? "" : delimiter));
            }
            close();
        }
    }
}
