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
package org.jumpmind.vaadin.ui.common;

import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ReadOnlyTextAreaDialog extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    final Logger log = LoggerFactory.getLogger(getClass());
    protected TextArea textField;
    protected AbstractSelect displayBox;

    public ReadOnlyTextAreaDialog(String title, final String value, boolean isEncodedInHex) {
        super(title);

        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setMargin(true);
        wrapper.setSizeFull();
        textField = new TextArea();
        textField.setSizeFull();
        textField.setWordwrap(false);
        wrapper.addComponent(textField);
        addComponent(wrapper, 1);

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.addStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);
        buttonLayout.setSpacing(true);
        buttonLayout.setWidth(100, Unit.PERCENTAGE);
        addComponent(buttonLayout);

        if (isEncodedInHex) {
            displayBox = new ComboBox("Display As");
            displayBox.addItem("Hex");
            displayBox.addItem("Text");
            displayBox.addItem("Decimal");
            displayBox.setNullSelectionAllowed(false);
            displayBox.select("Hex");
            displayBox.addValueChangeListener(new ValueChangeListener() {

                private static final long serialVersionUID = 1L;

                @Override
                public void valueChange(ValueChangeEvent event) {
                    updateTextField((String) displayBox.getValue(), value);
                }
            });
            buttonLayout.addComponent(displayBox);
        }

        Label spacer = new Label();
        buttonLayout.addComponent(spacer);
        buttonLayout.setExpandRatio(spacer, 1);

        buttonLayout.addComponent(buildCloseButton());

        textField.setValue(value);
        textField.addTextChangeListener(new TextChangeListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void textChange(TextChangeEvent event) {
                if (displayBox != null) {
                    updateTextField((String) displayBox.getValue(), value);
                } else {
                    textField.setValue(value);
                }
            }
        });
    }
    
    @Override
    protected void grabFocus() {        
    }
    
    @Override
    public void show() {
        super.show();
        selectAll();
    }
    
    public void selectAll() {
        textField.focus();
        textField.selectAll();      
    }

    protected void updateTextField(String display, String value) {
        if (display.equals("Hex")) {
            textField.setValue(value);
        } else if (display.equals("Text")) {
            try {
                byte[] bytes = Hex.decodeHex(value.toCharArray());
                textField.setValue(new String(bytes));
            } catch (Exception e) {
                log.warn("Failed to decode hex string for display", e);
            }
        } else if (display.equals("Decimal")) {
            try {
                byte[] bytes = Hex.decodeHex(value.toCharArray());
                String newValue = "";
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    if (!newValue.equals("")) {
                        newValue += " ";
                    }
                    newValue += buffer.get() & 0xff;
                }
                textField.setValue(newValue);
            } catch (Exception e) {
                log.warn("Failed to decode hex string for display", e);
            }
        }
    }

    public static void show(String title, String value, boolean isEncodedInHex) {
        ReadOnlyTextAreaDialog dialog = new ReadOnlyTextAreaDialog(title, value, isEncodedInHex);
        dialog.showAtSize(.4);
    }

}
