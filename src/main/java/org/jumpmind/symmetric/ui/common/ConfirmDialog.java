package org.jumpmind.symmetric.ui.common;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.Serializable;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

public class ConfirmDialog extends Window {

    private static final long serialVersionUID = 1L;

    public ConfirmDialog(String caption, String text, final IConfirmListener confirmListener) {
        this(caption, text, true, confirmListener);
    }

    public ConfirmDialog(String caption, String text, boolean focusOnOk, final IConfirmListener confirmListener) {
        setCaption(caption);
        setModal(true);
        setResizable(true);
        setWidth(300, Unit.PIXELS);
        setHeight(200, Unit.PIXELS);
        setClosable(false);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setSpacing(true);
        layout.setMargin(true);
        setContent(layout);

        if (isNotBlank(text)) {
            Label textLabel = new Label(text);
            layout.addComponent(textLabel);
            layout.setExpandRatio(textLabel, 1);
        }

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);
        buttonLayout.setSpacing(true);
        buttonLayout.setWidth(100, Unit.PERCENTAGE);

        Label spacer = new Label(" ");
        buttonLayout.addComponent(spacer);
        buttonLayout.setExpandRatio(spacer, 1);

        Button cancelButton = new Button("Cancel");
        cancelButton.setClickShortcut(KeyCode.ESCAPE);
        cancelButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event) {
                UI.getCurrent().removeWindow(ConfirmDialog.this);
            }
        });
        buttonLayout.addComponent(cancelButton);

        Button okButton = new Button("Ok");
        okButton.setStyleName(ValoTheme.BUTTON_PRIMARY);
        okButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event) {
                if (confirmListener.onOk()) {
                    UI.getCurrent().removeWindow(ConfirmDialog.this);
                }
            }
        });

        buttonLayout.addComponent(okButton);

        layout.addComponent(buttonLayout);

        if (!focusOnOk) {
            cancelButton.focus();
        } else {
            okButton.focus();
        }

    }

    public static void show(String caption, String text, IConfirmListener listener) {
        ConfirmDialog dialog = new ConfirmDialog(caption, text, listener);
        UI.getCurrent().addWindow(dialog);
    }

    public static interface IConfirmListener extends Serializable {
        public boolean onOk();
    }

}
