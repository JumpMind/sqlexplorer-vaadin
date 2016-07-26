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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.apache.commons.codec.binary.Hex;
import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.DatabaseInfo;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
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
    protected Button downloadButton;
    protected Table table;
    protected IDatabasePlatform platform;

    public ReadOnlyTextAreaDialog(final String title, final String value, Table table, final Object[] primaryKeys,
    		IDatabasePlatform platform, boolean isEncodedInHex, boolean isDownloadable) {
        super(title);
        this.table = table;
        this.platform = platform;

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
        
        if (isDownloadable && table != null) {
        	downloadButton = new Button("Download");
        	Resource resource = new StreamResource(new StreamSource() {

				private static final long serialVersionUID = 1L;

				public InputStream getStream() {
    				return new ByteArrayInputStream(getLobData(title, primaryKeys));
    			}
				
    		}, title);
        	FileDownloader fileDownloader = new FileDownloader(resource);
        	fileDownloader.extend(downloadButton);
        	buttonLayout.addComponent(downloadButton);
        	buttonLayout.setComponentAlignment(downloadButton, Alignment.BOTTOM_CENTER);
        	
        	long fileSize = getLobData(title, primaryKeys).length;
        	String sizeText = fileSize + " Bytes";
        	if (fileSize / 1024 > 0) {
        		sizeText = Math.round(fileSize / 1024.0) + " kB";
        		fileSize /= 1024;
        	}
        	if (fileSize / 1024 > 0) {
        		sizeText = Math.round(fileSize / 1024.0) + " MB";
        		fileSize /= 1024;
        	}
        	if (fileSize / 1024 > 0) {
        		sizeText = Math.round(fileSize / 1024.0) + " GB";
        		fileSize /= 1024;
        	}
        	Label sizeLabel = new Label(sizeText);
        	buttonLayout.addComponent(sizeLabel);
        	buttonLayout.setExpandRatio(sizeLabel, 1.0f);
        	buttonLayout.setComponentAlignment(sizeLabel, Alignment.BOTTOM_CENTER);
        }

        Label spacer = new Label();
        buttonLayout.addComponent(spacer);
        buttonLayout.setExpandRatio(spacer, 1);

        Button closeButton = buildCloseButton();
        buttonLayout.addComponent(closeButton);
        buttonLayout.setComponentAlignment(closeButton, Alignment.BOTTOM_RIGHT);

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
    
    protected byte[] getLobData(String title, Object[] primaryKeys) {
    	ISqlTemplate sqlTemplate = platform.getSqlTemplate();
    	Column lobColumn = table.getColumnWithName(title);
    	String sql = buildSelect(table, lobColumn, table.getPrimaryKeyColumns());
    	if (platform.isBlob(lobColumn.getMappedTypeCode())) {
    		return sqlTemplate.queryForBlob(sql, lobColumn.getJdbcTypeCode(), lobColumn.getJdbcTypeName(), primaryKeys);
    	} else {
    		return sqlTemplate.queryForClob(sql, lobColumn.getJdbcTypeCode(), lobColumn.getJdbcTypeName(), primaryKeys).getBytes();
    	}
    }
    
    protected String buildSelect(Table table, Column lobColumn, Column[] pkColumns) {
        StringBuilder sql = new StringBuilder("select ");
        DatabaseInfo dbInfo = platform.getDatabaseInfo();
        String quote = platform.getDdlBuilder().isDelimitedIdentifierModeOn() ? dbInfo.getDelimiterToken() : "";
        sql.append(quote);
        sql.append(lobColumn.getName());
        sql.append(quote);
        sql.append(",");
        sql.delete(sql.length() - 1, sql.length());
        sql.append(" from ");
        sql.append(table.getQualifiedTableName(quote, dbInfo.getCatalogSeparator(), 
                dbInfo.getSchemaSeparator()));
        sql.append(" where ");
        for (Column col : pkColumns) {
            sql.append(quote);
            sql.append(col.getName());
            sql.append(quote);
            sql.append("=? and ");
        }
        sql.delete(sql.length() - 5, sql.length());
        return sql.toString();
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
        show(title, value, null, null, null, isEncodedInHex, false);
    }
    
    public static void show(String title, String value, Table table, Object[] primaryKeys, IDatabasePlatform platform,
    		boolean isEncodedInHex, boolean isDownloadable) {
        ReadOnlyTextAreaDialog dialog = new ReadOnlyTextAreaDialog(title, value, table, primaryKeys, platform,
        		isEncodedInHex, isDownloadable);
        dialog.showAtSize(.4);
    }

}
