package org.jumpmind.vaadin.ui.sqlexplorer;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.SelectedTabChangeEvent;
import com.vaadin.ui.TabSheet.SelectedTabChangeListener;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

public class DatabaseInfoPanel extends VerticalLayout implements IInfoPanel {

	private static final long serialVersionUID = 1L;

	Logger log = LoggerFactory.getLogger(DatabaseInfoPanel.class);
	
	IDb db;
	
	Settings settings;
	
	TabSheet tabSheet;
	
	String selectedCaption;
	
	public DatabaseInfoPanel(IDb db, Settings settings, String selectedTabCaption) {
		this.db = db;
		this.settings = settings;
		
		setSizeFull();
		
		tabSheet = CommonUiUtils.createTabSheet();
		tabSheet.setImmediate(true);
		tabSheet.addSelectedTabChangeListener(new SelectedTabChangeListener() {

			private static final long serialVersionUID = 1L;

			@Override
			public void selectedTabChange(SelectedTabChangeEvent event) {
				selectedCaption = tabSheet.getTab(tabSheet.getSelectedTab()).getCaption();
			}
		});
		addComponent(tabSheet);
		
		try {
			Connection c = ((DataSource) db.getPlatform().getDataSource()).getConnection();
			DatabaseMetaData metaData = c.getMetaData();
			
			tabSheet.addTab(createTabData(createTableWithReflection(DatabaseMetaData.class, metaData)), "Meta Data");
			tabSheet.addTab(createTabData(createTableWithReflection(Connection.class, c)), "Connection");
			
			try{
				Table clientInfoProperties = CommonUiUtils.putResultsInTable(metaData.getClientInfoProperties(), Integer.MAX_VALUE, false);
				clientInfoProperties.setSizeFull();
				tabSheet.addTab(createTabData(clientInfoProperties), "Client Info Properties");
			} catch (AbstractMethodError e) {
				log.debug("Could not create Client Info Properties tab", e);
			}
			
			try {
				Table catalogs = CommonUiUtils.putResultsInTable(metaData.getCatalogs(), Integer.MAX_VALUE, false);
				catalogs.setSizeFull();
				tabSheet.addTab(createTabData(catalogs), "Catalogs");
			} catch (AbstractMethodError e) {
				log.debug("Could not create Catalogs tab", e);
			}
			
			try {
				Table schemas = CommonUiUtils.putResultsInTable(metaData.getSchemas(), Integer.MAX_VALUE, false);
				schemas.setSizeFull();
				tabSheet.addTab(createTabData(schemas), "Schemas");
			} catch (AbstractMethodError e) {
				log.debug("Could not create Schemas tab", e);
			}
			
			try {
				Table tableTypes = CommonUiUtils.putResultsInTable(metaData.getTableTypes(), Integer.MAX_VALUE, false);
				tableTypes.setSizeFull();
				tabSheet.addTab(createTabData(tableTypes), "Table Types");
			} catch (AbstractMethodError e) {
				log.debug("Could not create Table Types tab", e);
			}
			
			try {
				Table dataTypes = CommonUiUtils.putResultsInTable(metaData.getTypeInfo(), Integer.MAX_VALUE, false);
				dataTypes.setSizeFull();
				tabSheet.addTab(createTabData(dataTypes), "Data Types");
			} catch (AbstractMethodError e) {
				log.debug("Could not create Data Types tab", e);
			}
			
			try {
				tabSheet.addTab(createTabData(createTableFromString(metaData.getNumericFunctions(), "Numeric Functions")), "Numeric Functions");
			} catch (AbstractMethodError e) {
				log.debug("Could not create Numeric Functions tab", e);
			}
			
			try {
				tabSheet.addTab(createTabData(createTableFromString(metaData.getStringFunctions(), "String Functions")), "String Functions");
			} catch (AbstractMethodError e) {
				log.debug("Could not create String Functions tab", e);
			}
			
			try {
				tabSheet.addTab(createTabData(createTableFromString(metaData.getSystemFunctions(), "System Functions")), "System Functions");
			} catch (AbstractMethodError e) {
				log.debug("Could not create System Functions tab", e);
			}
			
			try {
				tabSheet.addTab(createTabData(createTableFromString(metaData.getTimeDateFunctions(), "Date/Time Functions")), "Date/Time Functions");
			} catch (AbstractMethodError e) {
				log.debug("Could not create Date/Time Functions tab", e);
			}
			
			try {
				tabSheet.addTab(createTabData(createTableFromString(metaData.getSQLKeywords(), "Keywords")), "Keywords");
			} catch (AbstractMethodError e) {
				log.debug("Could not create Keywords tab", e);
			}
		} catch (SQLException e) {
			e.printStackTrace();
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
	
	public AbstractLayout createTabData(Table table) {
		VerticalLayout layout = new VerticalLayout();
		layout.setMargin(true);
		layout.setSizeFull();
		layout.addComponent(table);
		layout.setExpandRatio(table, 1);
		return layout;
	}
	
	private Table createTableWithReflection(Class<?> reflectionClass, Object instance) {
		Table table = CommonUiUtils.createTable();
		table.setSizeFull();
		table.setSortEnabled(true);
        table.setSelectable(true);
        table.setMultiSelect(true);
        
        table.addContainerProperty(1, String.class, null);
        table.setColumnHeader(1, "Property");
        table.setColumnWidth(1, 390);
        table.addContainerProperty(2, Object.class, null);
        table.setColumnHeader(2, "Value");
        
        Method[] methods = reflectionClass.getMethods();
        int rowNumber = 1;
        for (Method method : methods) {
        	if ((method.getReturnType().equals(Integer.TYPE) || method.getReturnType().equals(String.class)
        			|| method.getReturnType().equals(Boolean.TYPE)) && method.getParameterCount() == 0) {
        		try {
					Object value = method.invoke(instance);
					Object[] row = new Object[] {method.getName(), value};
	        		table.addItem(row, rowNumber);
	        		rowNumber++;
				} catch (Exception e) {
					log.debug("Could not invoke method "+method.getName(), e);
				}
        	}
        }
		
		return table;
	}
	
	private Table createTableFromString(String data, String columnName) {
		Table table = CommonUiUtils.createTable();
		table.setSizeFull();
		table.setSortEnabled(true);
        table.setSelectable(true);
        table.setMultiSelect(true);
		
		List<String> values = new ArrayList<String>();
		int lastComma = 0;
		for (int i=0; i<data.length(); i++) {
			if (data.charAt(i) == ',') {
				values.add(data.substring(lastComma, i).trim());
				lastComma = i + 1;
			}
		}
		
		table.addContainerProperty(1, String.class, null);
        table.setColumnHeader(1, columnName);
        for (int i=0; i<values.size(); i++) {
        	table.addItem(new Object[]{values.get(i)}, i);
        }
		
		return table;
	}

	public String getSelectedTabCaption() {
		return selectedCaption;
	}
	
	@Override
	public void selected() {
	}

	@Override
	public void unselected() {
	}
	
}
