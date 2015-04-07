package org.jumpmind.symmetric.ui.common;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import org.apache.commons.lang.StringUtils;

import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.TextField;

public class ImmediateUpdateTextField extends TextField {

    private static final long serialVersionUID = 1L;

    String startValue;
    
    boolean initialized = false;
    
    public ImmediateUpdateTextField(String caption) {
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
                    setValue(event.getText());
                    save();
                    startValue = getValue();
                }
            }
        });
        addBlurListener(new BlurListener() {                
            private static final long serialVersionUID = 1L;                
            @Override
            public void blur(BlurEvent event) {
                if (isNotBlank(getState().errorMessage)) {
                    setValue(startValue);
                }
            }
        });
        addShortcutListener(new ShortcutListener("field", KeyCode.ENTER, null) {
            private static final long serialVersionUID = 1L;
            @Override
            public void handleAction(Object sender, Object target) {
                if (!StringUtils.equals(startValue, getValue())) {
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
