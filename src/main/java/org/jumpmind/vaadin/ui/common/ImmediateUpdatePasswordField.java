package org.jumpmind.vaadin.ui.common;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.PasswordField;

public abstract class ImmediateUpdatePasswordField extends PasswordField {

    private static final long serialVersionUID = 1L;

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
                save(event.getText());
            }
        });
    }

    abstract protected void save(String text);
}
