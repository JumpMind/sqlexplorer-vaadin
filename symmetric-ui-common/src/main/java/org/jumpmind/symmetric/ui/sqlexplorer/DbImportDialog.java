package org.jumpmind.symmetric.ui.sqlexplorer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.symmetric.io.data.DbImport;
import org.jumpmind.symmetric.io.data.DbImport.Format;
import org.jumpmind.symmetric.ui.common.ResizableWindow;
import org.jumpmind.symmetric.ui.common.CommonUiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.ChangeEvent;
import com.vaadin.ui.Upload.ChangeListener;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.VerticalLayout;

public class DbImportDialog extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    final Logger log = LoggerFactory.getLogger(getClass());

    private Set<Table> selectedTablesSet;

    private Table selectedTable;

    private VerticalLayout importLayout;

    private AbstractSelect formatSelect;

    private CheckBox force;

    private CheckBox ignore;

    private CheckBox replace;

    private AbstractSelect schemaSelect;

    private AbstractSelect catalogSelect;

    private AbstractSelect listOfTablesSelect;

    private TextField commitField;

    private CheckBox alter;

    private CheckBox alterCase;

    private Button cancelButton;

    private Button importButton;

    private DbImport dbImport;

    private Upload upload;

    private Format format;

    private boolean fileSelected = false;

    private IDatabasePlatform databasePlatform;

    private File file;

    private FileOutputStream out;

    public DbImportDialog(IDatabasePlatform databasePlatform) {
        this(databasePlatform, new HashSet<Table>(0));
    }
    
    public DbImportDialog(IDatabasePlatform databasePlatform, Set<Table> selectedTableSet) {
        super("Database Import");

        this.selectedTablesSet = selectedTableSet;
        this.databasePlatform = databasePlatform;

        createImportLayout();
    }

    protected void createImportLayout() {
        importLayout = new VerticalLayout();
        importLayout.setMargin(true);
        importLayout.setSpacing(true);

        importLayout.addComponent(new Label("Please select from the following options"));

        FormLayout formLayout = new FormLayout();
        importLayout.addComponent(formLayout);
        importLayout.setExpandRatio(formLayout, 1);

        formatSelect = new ComboBox("Format");
        for (DbImportFormat format : DbImportFormat.values()) {
            formatSelect.addItem(format);
        }
        formatSelect.setNullSelectionAllowed(false);
        formatSelect.setValue(DbImportFormat.SQL);
        formatSelect.addValueChangeListener(new Property.ValueChangeListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent event) {
                DbImportFormat format = (DbImportFormat) formatSelect.getValue();

                switch (format) {
                    case SQL:
                        listOfTablesSelect.setEnabled(false);
                        alter.setEnabled(false);
                        alterCase.setEnabled(false);
                        break;
                    case XML:
                        listOfTablesSelect.setEnabled(false);
                        alter.setEnabled(true);
                        alterCase.setEnabled(true);
                        break;
                    case CSV:
                        listOfTablesSelect.setEnabled(true);
                        alter.setEnabled(false);
                        alterCase.setEnabled(false);
                        importButton.setEnabled(importButtonEnable());
                        break;
                    case SYM_XML:
                        listOfTablesSelect.setEnabled(false);
                        alter.setEnabled(false);
                        alterCase.setEnabled(false);
                        break;
                }
            }
        });
        formLayout.addComponent(formatSelect);

        catalogSelect = new ComboBox("Catalog");
        catalogSelect.setImmediate(true);
        CommonUiUtils.addItems(getCatalogs(), catalogSelect);
        catalogSelect.select(databasePlatform.getDefaultCatalog());
        catalogSelect.setNullSelectionAllowed(false);
        formLayout.addComponent(catalogSelect);

        schemaSelect = new ComboBox("Schema");
        schemaSelect.setImmediate(true);
        CommonUiUtils.addItems(getSchemas(), schemaSelect);
        if (selectedTablesSet.iterator().hasNext()) {
            schemaSelect.select(selectedTablesSet.iterator().next().getSchema());
        } else {
            schemaSelect.select(databasePlatform.getDefaultSchema());
        }
        schemaSelect.setNullSelectionAllowed(false);
        schemaSelect.addValueChangeListener(new ValueChangeListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent event) {
                populateListOfTablesSelect();
            }
        });
        formLayout.addComponent(schemaSelect);

        listOfTablesSelect = new ComboBox("Tables");
        populateListOfTablesSelect();
        listOfTablesSelect.setEnabled(false);
        listOfTablesSelect.setNullSelectionAllowed(false);

        if (!this.selectedTablesSet.isEmpty()) {
            if (this.selectedTablesSet.size() == 1) {
                this.selectedTable = this.selectedTablesSet.iterator().next();
                listOfTablesSelect.select(this.selectedTable.getName());
                this.selectedTablesSet.clear();
            } else {
                List<Table> list = new ArrayList<Table>(this.selectedTablesSet);
                listOfTablesSelect.select(list.get(0).getName());
                this.selectedTable = list.get(0);
                this.selectedTablesSet.clear();
            }
        }
        formLayout.addComponent(listOfTablesSelect);

        commitField = new TextField("Rows to Commit");
        commitField.addTextChangeListener(new TextChangeListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void textChange(TextChangeEvent event) {
                commitField.setValue(event.getText());
                if (fileSelected) {
                    importButton.setEnabled(importButtonEnable());
                }
            }
        });
        commitField.setImmediate(true);
        commitField.setTextChangeEventMode(TextChangeEventMode.EAGER);
        commitField.setValue("10000");
        formLayout.addComponent(commitField);

        force = new CheckBox("Force");
        formLayout.addComponent(force);

        ignore = new CheckBox("Ignore");
        formLayout.addComponent(ignore);

        replace = new CheckBox("Replace");
        formLayout.addComponent(replace);

        alter = new CheckBox("Alter");
        alter.setEnabled(false);
        formLayout.addComponent(alter);

        alterCase = new CheckBox("Alter Case");
        alterCase.setEnabled(false);
        formLayout.addComponent(alterCase);

        upload = new Upload("File", new Receiver() {

            private static final long serialVersionUID = 1L;

            @Override
            public OutputStream receiveUpload(String filename, String mimeType) {
                try {
                    file = File.createTempFile("dbimport", formatSelect.getValue().toString());
                    out = new FileOutputStream(file);
                    return new BufferedOutputStream(new FileOutputStream(file));
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                    CommonUiUtils.notify("Failed to import " + filename, e);
                }
                return null;
            }
        });
        upload.addSucceededListener(new SucceededListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void uploadSucceeded(SucceededEvent event) {
                createDbImport();
                try {
                    doDbImport();
                } catch (FileNotFoundException e) {
                    log.warn(e.getMessage(), e);
                    Notification.show(e.getMessage());
                }
                deleteFileAndResource();
                close();
            }
        });
        upload.addChangeListener(new ChangeListener() {

            private static final long serialVersionUID = 1L;

            public void filenameChanged(ChangeEvent event) {
                fileSelected = true;
                importButton.setEnabled(importButtonEnable());
            }
        });
        upload.setButtonCaption(null);
        formLayout.addComponent(upload);

        cancelButton = new Button("Cancel", new Button.ClickListener() {
            private static final long serialVersionUID = 1L;

            public void buttonClick(ClickEvent event) {
                UI.getCurrent().removeWindow(DbImportDialog.this);
            }
        });

        importButton = CommonUiUtils.createPrimaryButton("Import", new Button.ClickListener() {
            private static final long serialVersionUID = 1L;

            public void buttonClick(ClickEvent event) {
                upload.submitUpload();
            }
        });
        importButton.setEnabled(false);

        addComponent(importLayout, 1);
        addComponent(buildButtonFooter(cancelButton, importButton));
    }

    protected void deleteFileAndResource() {
        try {
            out.close();
            file.delete();
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
            Notification.show(e.getMessage());
        }
    }

    protected void doDbImport() throws FileNotFoundException {
        if (format.toString().equals("CSV")) {
            dbImport.importTables(new BufferedInputStream(new FileInputStream(file)),
                    listOfTablesSelect.getValue().toString());
        } else {
            dbImport.importTables(new BufferedInputStream(new FileInputStream(file)));
        }
    }

    protected void createDbImport() {
        dbImport = new DbImport(databasePlatform);

        format = DbImport.Format.valueOf(formatSelect.getValue().toString());
        dbImport.setFormat(format);
        dbImport.setCatalog((String)catalogSelect.getValue());
        dbImport.setSchema((String)schemaSelect.getValue());
        dbImport.setCommitRate(Long.parseLong(commitField.getValue()));
        dbImport.setForceImport(force.getValue());
        dbImport.setIgnoreCollisions(ignore.getValue());
        dbImport.setIgnoreMissingTables(ignore.getValue());
        dbImport.setAlterTables(alter.getValue());
        dbImport.setAlterCaseToMatchDatabaseDefaultCase(alterCase.getValue());

    }

    protected boolean importButtonEnable() {
        if (formatSelect.getValue() != null) {
                    if (!commitField.getValue().equals("")) {
                        if (formatSelect.getValue().toString().equals("CSV")) {
                            if (listOfTablesSelect.getValue() != null) {
                                return true;
                            }
                        } else {
                            return true;
                        }
            }
        }
        return false;
    }

    protected void populateListOfTablesSelect() {
        listOfTablesSelect.removeAllItems();
        List<String> tables = getTables();

        for (String table : tables) {
            listOfTablesSelect.addItem(table);
        }
    }

    public String getSelectedSchema() {
        String schemaName = (String) schemaSelect.getValue();
        if (schemaName != null && schemaName.equals(databasePlatform.getDefaultSchema())) {
            schemaName = null;
        }
        return StringUtils.isBlank(schemaName) ? null : schemaName;
    }

    public String getSelectedCatalog() {
        String catalogName = (String) catalogSelect.getValue();
        if (catalogName != null && catalogName.equals(databasePlatform.getDefaultCatalog())) {
            catalogName = null;
        }
        return StringUtils.isBlank(catalogName) ? null : catalogName;
    }

    public List<String> getSchemas() {
        return databasePlatform.getDdlReader().getSchemaNames(null);
    }

    public List<String> getCatalogs() {
        return databasePlatform.getDdlReader().getCatalogNames();
    }

    public List<String> getTables() {
        return databasePlatform.getDdlReader().getTableNames((String) catalogSelect.getValue(),
                (String) schemaSelect.getValue(), new String[] { "TABLE" });
    }

    enum DbImportFormat {

        SQL, XML, CSV, SYM_XML;

    }
}
