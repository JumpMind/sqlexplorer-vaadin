package org.jumpmind.symmetric.ui.sqlexplorer;

import java.util.Set;

import org.jumpmind.symmetric.ui.common.TreeNode;

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

    abstract public void handle(Set<TreeNode> nodes);

}
