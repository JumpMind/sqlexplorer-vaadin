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

    private VerticalLayout panel;

    private VerticalLayout exportLayout;

    private OptionGroup oGroup;

    private Button cancelButton;

    private Button exportButton;

    public ExportDialog(VerticalLayout panel) {
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
                            if (panel instanceof TabularResultLayout) {
                            	((TabularResultLayout) panel).csvExport();
                            } else if (panel instanceof TriggerTableLayout) {
                            	((TriggerTableLayout) panel).csvExport();
                            }
                        } else {
                        	if (panel instanceof TabularResultLayout) {
                            	((TabularResultLayout) panel).excelExport();
                            } else if (panel instanceof TriggerTableLayout) {
                            	((TriggerTableLayout) panel).excelExport();
                            }
                        }
                        UI.getCurrent().removeWindow(ExportDialog.this);
                    }
                });

        exportLayout.addComponent(buildButtonFooter(cancelButton, exportButton));

    }

}
