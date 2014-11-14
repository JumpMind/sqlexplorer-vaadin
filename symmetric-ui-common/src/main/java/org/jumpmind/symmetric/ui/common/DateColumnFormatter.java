package org.jumpmind.symmetric.ui.common;

import java.util.Date;

import com.vaadin.data.Property;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;

public class DateColumnFormatter implements Table.ColumnGenerator {

    private static final long serialVersionUID = 1L;

    public Component generateCell(Table source, Object itemId, Object columnId) {
        Property<?> prop = source.getItem(itemId).getItemProperty(columnId);
        if (prop.getType().equals(Date.class)) {
            return new Label(UiUtils.formatDateTime((Date) prop.getValue()));
        }
        return null;
    }

}
