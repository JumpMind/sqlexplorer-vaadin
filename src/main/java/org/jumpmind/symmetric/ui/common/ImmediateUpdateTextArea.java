package org.jumpmind.symmetric.ui.common;

import org.apache.commons.lang.StringUtils;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.TextArea;

public abstract class ImmediateUpdateTextArea extends TextArea {

    private static final long serialVersionUID = 1L;

    String startValue;
    
    boolean initialized = false;
    
    public ImmediateUpdateTextArea(String caption) {
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
                    save(event.getText());
                }
            }
        });
    }
    
    abstract protected void save(String text);
    
}
