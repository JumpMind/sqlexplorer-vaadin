package org.jumpmind.symmetric.ui.sqlexplorer;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.VerticalSplitPanel;

public class QueryPanel extends VerticalSplitPanel implements IContentTab {

    private static final long serialVersionUID = 1L;

    public QueryPanel(IDb database) {
    }

    @Override
    public void select(MenuBar menuBar) {
        MenuItem executeAtCursorButton = menuBar.addItem("", FontAwesome.PLAY, new Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
                // service.execute(false);
            }
        });
        executeAtCursorButton.setDescription("Run sql under cursor (CTRL+ENTER)");
    }
}
