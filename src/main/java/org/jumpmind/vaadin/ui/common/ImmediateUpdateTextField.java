package org.jumpmind.vaadin.ui.common;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.TextField;

public abstract class ImmediateUpdateTextField extends TextField {

    private static final long serialVersionUID = 1L;

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
                  save(event.getText());
            }
        });
    }
    
    protected abstract void save(String text);
}
