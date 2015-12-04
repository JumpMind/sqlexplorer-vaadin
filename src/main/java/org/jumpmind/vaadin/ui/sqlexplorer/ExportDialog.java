package org.jumpmind.vaadin.ui.sqlexplorer;

import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.ResizableWindow;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

public class ExportDialog extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    private TabularResultLayout panel;

    private VerticalLayout exportLayout;

    private OptionGroup oGroup;

    private Button cancelButton;

    private Button exportButton;

    public ExportDialog(TabularResultLayout panel) {
        super("Export");

        this.panel = panel;

        setModal(true);
        setWidth(300, Unit.PIXELS);
        setHeight(130, Unit.PIXELS);
        setClosable(true);

        createExportLayout();
        setContent(exportLayout);
    }

    protected void createExportLayout() {
        exportLayout = new VerticalLayout();
        exportLayout.setSizeFull();
        exportLayout.setMargin(true);
        exportLayout.setSpacing(true);

        HorizontalLayout exportOptionsLayout = new HorizontalLayout();
        exportOptionsLayout.setSpacing(true);
        Label optionGroupLabel = new Label(
                "Export Format <span style='padding-left:0px; color: red'>*</span>",
                ContentMode.HTML);
        exportOptionsLayout.addComponent(optionGroupLabel);

        oGroup = new OptionGroup();
        oGroup.setImmediate(true);
        oGroup.addItem("CSV");
        oGroup.addItem("Excel");
        oGroup.setValue("CSV");

        exportOptionsLayout.addComponent(oGroup);
        exportLayout.addComponent(exportOptionsLayout);
        exportLayout.setExpandRatio(exportOptionsLayout, 1);

        cancelButton = new Button("Cancel", new Button.ClickListener() {
            private static final long serialVersionUID = 1L;

            public void buttonClick(ClickEvent event) {
                UI.getCurrent().removeWindow(ExportDialog.this);
            }
        });

        exportButton = CommonUiUtils.createPrimaryButton("Export",
                new Button.ClickListener() {
                    private static final long serialVersionUID = 1L;

                    public void buttonClick(ClickEvent event) {
                        if (oGroup.getValue().toString().equals("CSV")) {
                            panel.csvExport();
                        } else {
                            panel.excelExport();
                        }
                        UI.getCurrent().removeWindow(ExportDialog.this);
                    }
                });

        exportLayout.addComponent(buildButtonFooter(cancelButton, exportButton));

    }

}
