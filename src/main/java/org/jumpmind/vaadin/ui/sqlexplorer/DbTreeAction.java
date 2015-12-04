package org.jumpmind.vaadin.ui.sqlexplorer;

import java.util.Set;

import com.vaadin.event.Action;
import com.vaadin.server.Resource;

abstract public class DbTreeAction extends Action {
        
    private static final long serialVersionUID = 1L;

    public DbTreeAction(String caption) {
        super(caption);
    }

    public DbTreeAction(String caption, Resource icon) {
        super(caption, icon);
    }

    abstract public void handle(Set<DbTreeNode> nodes);

}
