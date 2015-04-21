package org.jumpmind.symmetric.ui.common;

import org.apache.commons.lang.StringUtils;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.PasswordField;

public class ImmediateUpdatePasswordField extends PasswordField {

    private static final long serialVersionUID = 1L;

    String startValue;
    
    boolean initialized = false;
    
    public ImmediateUpdatePasswordField(String caption) {
        super(caption);
        setImmediate(true);
        setNullRepresentation("");        
        setTextChangeEventMode(TextChangeEventMode.LAZY);
        setTextChangeTimeout(200);        
        addTextChangeListener(new TextChangeListener() {
            private static final long serialVersionUID = 1L;
            @Override
            public void textChange(TextChangeEvent event) {
                if (!StringUtils.equals(startValue, event.getText())) {
                    int cursor = getCursorPosition();
                    setValue(event.getText(), false);
                    setCursorPosition(cursor);
                    save();
                    startValue = getValue();
                }
            }
        });
    }
    
    @Override
    public void setValue(String newValue) throws com.vaadin.data.Property.ReadOnlyException {
        super.setValue(newValue);
        if (!initialized) {
            startValue = newValue;
            initialized = true;
        }
    }

    
    protected void save() {
        
    }
}
