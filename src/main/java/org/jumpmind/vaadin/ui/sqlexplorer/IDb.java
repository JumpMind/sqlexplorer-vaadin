package org.jumpmind.vaadin.ui.sqlexplorer;

import org.jumpmind.db.platform.IDatabasePlatform;

public interface IDb  {

    public String getName();
    
    public IDatabasePlatform getPlatform();

}
