package org.jumpmind.symmetric.ui.sqlexplorer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.symmetric.io.data.DbExport;
import org.jumpmind.symmetric.io.data.DbExport.Compatible;
import org.jumpmind.symmetric.io.data.DbExport.Format;
import org.jumpmind.symmetric.ui.common.ResizableWindow;
import org.jumpmind.symmetric.ui.common.UiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class DbExportDialog extends ResizableWindow {

    private static final String EXPORT_TO_THE_SQL_EDITOR = "Export to the SQL Editor";

    private static final String EXPORT_AS_A_FILE = "Export as a File";

    private static final long serialVersionUID = 1L;

    final Logger log = LoggerFactory.getLogger(getClass());

    private ComboBox formatSelect;

    private ComboBox compatibilitySelect;

    private CheckBox data;

    private CheckBox createInfo;

    private CheckBox foreignKeys;

    private CheckBox indices;

    private CheckBox quotedIdentifiers;

    private CheckBox dropTables;

    private TextArea whereClauseField;

    private Button previousButton;

    private Button cancelButton;

    private Button selectAllLink;

    private Button selectNoneLink;

    public Button nextButton;

    private Button exportFileButton;

    private Button exportEditorButton;

    private TableSelectionLayout tableSelectionLayout;

    private VerticalLayout optionLayout;

    private FileDownloader fileDownloader;

    private DbExport dbExport;

    private OptionGroup oGroup;

    private QueryPanel queryPanel;

    private IDatabasePlatform databasePlatform;

    public DbExportDialog(IDatabasePlatform databasePlatform, Set<Table> selectedTableSet,
            QueryPanel queryPanel) {
        this(databasePlatform, "Database Export", "Please select from the following tables",
                selectedTableSet, queryPanel);
    }

    public DbExportDialog(IDatabasePlatform databasePlatform, String titleKey,
            String descriptionKey, Set<Table> selectedTableSet, QueryPanel panel) {
        super(titleKey);

        this.databasePlatform = databasePlatform;
        this.queryPanel = panel;

        tableSelectionLayout = new TableSelectionLayout(databasePlatform, selectedTableSet) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void selectionChanged() {
                nextButton.setEnabled(tableSelectionLayout.getSelectedTables().size() > 0);
            }
        };

        createOptionLayout();

        addComponent(tableSelectionLayout, 1);

        addButtons();
    }

    protected void addButtons() {
        selectAllLink = new Button("Select All");
        selectAllLink.addStyleName(ValoTheme.BUTTON_LINK);
        selectAllLink.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = 1L;

            public void buttonClick(ClickEvent event) {
                tableSelectionLayout.selectAll();
            }
        });

        selectNoneLink = new Button("Select None");
        selectNoneLink.addStyleName(ValoTheme.BUTTON_LINK);
        selectNoneLink.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = 1L;

            public void buttonClick(ClickEvent event) {
                tableSelectionLayout.selectNone();
            }
        });

        nextButton = UiUtils.createPrimaryButton("Next");
        nextButton.setEnabled(tableSelectionLayout.getSelectedTables().size() > 0);
        nextButton.addClickListener(new Button.ClickListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event) {
                next();
            }
        });

        cancelButton = new Button("Cancel", new Button.ClickListener() {
            private static final long serialVersionUID = 1L;

            public void buttonClick(ClickEvent event) {
                close();
            }
        });

        previousButton = new Button("Previous", new Button.ClickListener() {
            private static final long serialVersionUID = 1L;

            public void buttonClick(ClickEvent event) {
                previous();
            }
        });
        previousButton.setVisible(false);

        exportFileButton = UiUtils.createPrimaryButton("Export", new Button.ClickListener() {
            private static final long serialVersionUID = 1L;

            public void buttonClick(ClickEvent event) {
                close();
            }
        });
        buildFileDownloader();
        exportFileButton.setVisible(false);

        exportEditorButton = UiUtils.createPrimaryButton("Export", new Button.ClickListener() {
            private static final long serialVersionUID = 1L;

            public void buttonClick(ClickEvent event) {
                exportToEditor();
                close();
            }
        });
        exportEditorButton.setVisible(false);

        addComponent(buildButtonFooter(new Button[] { selectAllLink, selectNoneLink },
                cancelButton, previousButton, nextButton, exportFileButton, exportEditorButton));

    }

    protected void createOptionLayout() {
        optionLayout = new VerticalLayout();
        optionLayout.setMargin(true);
        optionLayout.setSpacing(true);
        optionLayout.setSizeFull();
        optionLayout.addComponent(new Label("Please choose from the following options"));

        FormLayout formLayout = new FormLayout();
        optionLayout.addComponent(formLayout);
        optionLayout.setExpandRatio(formLayout, 1);

        formatSelect = new ComboBox("Format");
        formatSelect.setImmediate(true);
        for (DbExportFormat format : DbExportFormat.values()) {
            formatSelect.addItem(format);
        }
        formatSelect.setNullSelectionAllowed(false);
        formatSelect.setValue(DbExportFormat.SQL);
        formatSelect.addValueChangeListener(new Property.ValueChangeListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent event) {
                DbExportFormat format = (DbExportFormat) formatSelect.getValue();

                switch (format) {
                    case SQL:
                        compatibilitySelect.setEnabled(true);
                        compatibilitySelect.setNullSelectionAllowed(false);
                        setDefaultCompatibility();
                        data.setEnabled(true);
                        foreignKeys.setEnabled(true);
                        indices.setEnabled(true);
                        quotedIdentifiers.setEnabled(true);
                        break;
                    case XML:
                        compatibilitySelect.setEnabled(false);
                        compatibilitySelect.setNullSelectionAllowed(true);
                        compatibilitySelect.setValue(null);
                        data.setEnabled(true);
                        foreignKeys.setEnabled(true);
                        indices.setEnabled(true);
                        quotedIdentifiers.setEnabled(true);
                        break;
                    case CSV:
                        compatibilitySelect.setEnabled(false);
                        compatibilitySelect.setNullSelectionAllowed(true);
                        compatibilitySelect.setValue(null);
                        data.setEnabled(false);
                        foreignKeys.setEnabled(false);
                        indices.setEnabled(false);
                        quotedIdentifiers.setEnabled(false);
                        break;
                    case SYM_XML:
                        compatibilitySelect.setEnabled(false);
                        compatibilitySelect.setNullSelectionAllowed(true);
                        compatibilitySelect.setValue(null);
                        data.setEnabled(false);
                        foreignKeys.setEnabled(false);
                        indices.setEnabled(false);
                        quotedIdentifiers.setEnabled(false);
                        break;
                }
            }
        });
        formatSelect.select(DbExportFormat.SQL);
        formLayout.addComponent(formatSelect);

        compatibilitySelect = new ComboBox("Compatibility");
        for (Compatible compatability : Compatible.values()) {
            compatibilitySelect.addItem(compatability);
        }

        compatibilitySelect.setNullSelectionAllowed(false);
        setDefaultCompatibility();
        formLayout.addComponent(compatibilitySelect);

        createInfo = new CheckBox("Create Tables");
        formLayout.addComponent(createInfo);

        dropTables = new CheckBox("Drop Tables");
        formLayout.addComponent(dropTables);

        data = new CheckBox("Insert Data");
        data.setValue(true);
        formLayout.addComponent(data);

        foreignKeys = new CheckBox("Create Foreign Keys");
        formLayout.addComponent(foreignKeys);

        indices = new CheckBox("Create Indices");
        formLayout.addComponent(indices);

        quotedIdentifiers = new CheckBox("Qualify with Quoted Identifiers");
        formLayout.addComponent(quotedIdentifiers);

        whereClauseField = new TextArea("Where Clause");
        whereClauseField.setColumns(100);
        whereClauseField.setRows(2);
        formLayout.addComponent(whereClauseField);

        oGroup = new OptionGroup("Export Format");
        oGroup.setImmediate(true);
        oGroup.addItem(EXPORT_AS_A_FILE);
        oGroup.addItem(EXPORT_TO_THE_SQL_EDITOR);
        oGroup.setValue(EXPORT_AS_A_FILE);
        oGroup.addValueChangeListener(new Property.ValueChangeListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent event) {
                setExportButtonsEnabled();
            }
        });
        formLayout.addComponent(oGroup);

    }

    protected void setDefaultCompatibility() {
        try {
            compatibilitySelect.setValue(Compatible.valueOf(databasePlatform.getName()
                    .toUpperCase()));
        } catch (Exception ex) {
            compatibilitySelect.setValue(Compatible.ORACLE);
        }
    }

    protected void setExportButtonsEnabled() {
        if (oGroup.getValue().equals(EXPORT_AS_A_FILE)) {
            exportEditorButton.setVisible(false);
            exportFileButton.setVisible(true);
        } else {
            exportFileButton.setVisible(false);
            exportEditorButton.setVisible(true);
        }
    }

    protected void previous() {
        content.removeComponent(optionLayout);
        content.addComponent(tableSelectionLayout, 0);
        content.setExpandRatio(tableSelectionLayout, 1);
        nextButton.setVisible(true);
        previousButton.setVisible(false);
        exportEditorButton.setVisible(false);
        exportFileButton.setVisible(false);
        selectAllLink.setVisible(true);
        selectNoneLink.setVisible(true);
    }

    protected void next() {
        content.removeComponent(tableSelectionLayout);
        content.addComponent(optionLayout, 0);
        content.setExpandRatio(optionLayout, 1);
        nextButton.setVisible(false);
        previousButton.setVisible(true);
        setExportButtonsEnabled();
        selectAllLink.setVisible(false);
        selectNoneLink.setVisible(false);

    }

    protected void createDbExport() {
        dbExport = new DbExport(databasePlatform);
        if (tableSelectionLayout.schemaSelect.getValue() != null) {
            dbExport.setSchema(tableSelectionLayout.schemaSelect.getValue().toString());
        }
        if (tableSelectionLayout.catalogSelect.getValue() != null) {
            dbExport.setCatalog(tableSelectionLayout.catalogSelect.getValue().toString());
        }
        dbExport.setNoCreateInfo(!createInfo.getValue());
        dbExport.setNoData(!data.getValue());
        dbExport.setNoForeignKeys(!foreignKeys.getValue());
        dbExport.setNoIndices(!indices.getValue());
        dbExport.setAddDropTable(dropTables.getValue());
        Format format = DbExport.Format.valueOf(formatSelect.getValue().toString());
        dbExport.setFormat(format);
        if (compatibilitySelect.getValue() != null) {
            Compatible compatible = (Compatible) compatibilitySelect.getValue();
            dbExport.setCompatible(compatible);
        }
        dbExport.setUseQuotedIdentifiers(quotedIdentifiers.getValue());
        dbExport.setWhereClause(whereClauseField.getValue());
    }

    protected void exportToEditor() {
        List<String> list = tableSelectionLayout.getSelectedTables();
        String[] array = new String[list.size()];
        list.toArray(array);

        createDbExport();

        String script;
        try {
            script = dbExport.exportTables(array);
            queryPanel.writeSql(script);
        } catch (IOException e) {
            String msg = "Failed to export to the sql editor";
            log.error(msg, e);
            UiUtils.notify(msg, e);
        }
    }

    protected void buildFileDownloader() {
        if (fileDownloader != null) {
            fileDownloader.remove();
        }
        fileDownloader = new FileDownloader(createResource()) {
            private static final long serialVersionUID = 1L;

            @Override
            public Resource getFileDownloadResource() {
                /* recreate the resource so the file name is regenerated */
                return createResource();
            }
        };
        fileDownloader.extend(exportFileButton);
    }

    private StreamResource createResource() {

        final String format = (String) formatSelect.getValue().toString();
        StreamSource ss = new StreamSource() {
            private static final long serialVersionUID = 1L;

            public InputStream getStream() {

                List<String> list = tableSelectionLayout.getSelectedTables();
                String[] array = new String[list.size()];
                list.toArray(array);

                createDbExport();

                String script;
                try {
                    script = dbExport.exportTables(array);
                    return new ByteArrayInputStream(script.getBytes());
                } catch (IOException e) {
                    String msg = "Failed to export to a file";
                    log.error(msg, e);
                    UiUtils.notify(msg, e);
                }
                return null;
            }
        };
        String datetime = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
        StreamResource sr = new StreamResource(ss, String.format(
                "table-export-%s." + format.toLowerCase(), datetime));
        return sr;
    }

    enum DbExportFormat {

        SQL, XML, CSV, SYM_XML;

    }

}
