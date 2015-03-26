package org.jumpmind.symmetric.ui.sqlexplorer;

import static org.jumpmind.symmetric.ui.sqlexplorer.Settings.SQL_EXPLORER_AUTO_COMMIT;
import static org.jumpmind.symmetric.ui.sqlexplorer.Settings.SQL_EXPLORER_DELIMITER;
import static org.jumpmind.symmetric.ui.sqlexplorer.Settings.SQL_EXPLORER_EXCLUDE_TABLES_REGEX;
import static org.jumpmind.symmetric.ui.sqlexplorer.Settings.SQL_EXPLORER_MAX_RESULTS;
import static org.jumpmind.symmetric.ui.sqlexplorer.Settings.SQL_EXPLORER_RESULT_AS_TEXT;
import static org.jumpmind.symmetric.ui.sqlexplorer.Settings.SQL_EXPLORER_SHOW_ROW_NUMBERS;

import java.text.DecimalFormat;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.ui.common.CommonUiUtils;
import org.jumpmind.symmetric.ui.common.ResizableWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class SettingsDialog extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private TextField rowsToFetchField;

    private CheckBox autoCommitBox;

    private TextField delimiterField;

    private TextField excludeTablesWithPrefixField;

    private CheckBox resultAsTextBox;

    private CheckBox showRowNumbersBox;

    ISettingsProvider settingsProvider;

    public SettingsDialog(ISettingsProvider settingsProvider) {
        super("Settings");
        this.settingsProvider = settingsProvider;
        setWidth(400, Unit.PIXELS);
        addComponent(createSettingsLayout(), 1);
        addComponent(createButtonLayout());
    }

    protected AbstractLayout createSettingsLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        FormLayout settingsLayout = new FormLayout();

        Settings settings = settingsProvider.get();
        TypedProperties properties = settings.getProperties();

        rowsToFetchField = new TextField("Max Results");
        rowsToFetchField.setColumns(6);
        rowsToFetchField.setValidationVisible(true);
        rowsToFetchField.setConverter(Integer.class);              
        rowsToFetchField.setValue(properties.getProperty(SQL_EXPLORER_MAX_RESULTS, "100"));
        settingsLayout.addComponent(rowsToFetchField);

        delimiterField = new TextField("Delimiter");
        delimiterField.setValue(properties.getProperty(SQL_EXPLORER_DELIMITER, ";"));
        settingsLayout.addComponent(delimiterField);

        excludeTablesWithPrefixField = new TextField("Hide Tables (regex)");
        excludeTablesWithPrefixField.setValue(properties
                .getProperty(SQL_EXPLORER_EXCLUDE_TABLES_REGEX));
        settingsLayout.addComponent(excludeTablesWithPrefixField);

        resultAsTextBox = new CheckBox("Result As Text");
        String resultAsTextValue = (properties.getProperty(SQL_EXPLORER_RESULT_AS_TEXT, "false"));
        if (resultAsTextValue.equals("true")) {
            resultAsTextBox.setValue(true);
        } else {
            resultAsTextBox.setValue(false);
        }
        settingsLayout.addComponent(resultAsTextBox);

        autoCommitBox = new CheckBox("Auto Commit");
        String autoCommitValue = (properties.getProperty(SQL_EXPLORER_AUTO_COMMIT, "true"));
        if (autoCommitValue.equals("true")) {
            autoCommitBox.setValue(true);
        } else {
            autoCommitBox.setValue(false);
        }
        settingsLayout.addComponent(autoCommitBox);

        showRowNumbersBox = new CheckBox("Show Row Numbers");
        String showRowNumbersValue = (properties.getProperty(SQL_EXPLORER_SHOW_ROW_NUMBERS, "true"));
        if (showRowNumbersValue.equals("true")) {
            showRowNumbersBox.setValue(true);
        } else {
            showRowNumbersBox.setValue(false);
        }
        settingsLayout.addComponent(showRowNumbersBox);
        layout.addComponent(settingsLayout);
        return layout;

    }

    protected AbstractLayout createButtonLayout() {
        Button saveButton = CommonUiUtils.createPrimaryButton("Save", new Button.ClickListener() {
            private static final long serialVersionUID = 1L;

            public void buttonClick(ClickEvent event) {
                save();
                UI.getCurrent().removeWindow(SettingsDialog.this);
            }
        });

        return buildButtonFooter(new Button("Cancel", new CloseButtonListener()), saveButton);
    }

    protected void save() {
        Settings settings = settingsProvider.get();
        TypedProperties properties = settings.getProperties();

        try {
            rowsToFetchField.validate();            
            properties.setProperty(SQL_EXPLORER_MAX_RESULTS, new DecimalFormat().parse(rowsToFetchField.getValue()).intValue());
            properties.setProperty(SQL_EXPLORER_AUTO_COMMIT,
                    String.valueOf(autoCommitBox.getValue()));
            properties.setProperty(SQL_EXPLORER_DELIMITER, delimiterField.getValue());
            properties.setProperty(SQL_EXPLORER_RESULT_AS_TEXT,
                    String.valueOf(resultAsTextBox.getValue()));
            properties.setProperty(SQL_EXPLORER_SHOW_ROW_NUMBERS,
                    String.valueOf(showRowNumbersBox.getValue()));
            properties.setProperty(SQL_EXPLORER_EXCLUDE_TABLES_REGEX,
                    excludeTablesWithPrefixField.getValue());
            settingsProvider.save(settings);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            CommonUiUtils.notify(ex);
        }
    }
}
