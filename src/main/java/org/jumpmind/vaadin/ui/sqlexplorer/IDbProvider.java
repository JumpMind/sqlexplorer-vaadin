package org.jumpmind.vaadin.ui.sqlexplorer;

import java.util.List;

public interface IDbProvider {

    public List<IDb> getDatabases();
    
}
