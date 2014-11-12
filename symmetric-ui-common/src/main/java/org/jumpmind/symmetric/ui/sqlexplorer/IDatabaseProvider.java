package org.jumpmind.symmetric.ui.sqlexplorer;

import java.util.List;

public interface IDatabaseProvider {

    public List<IDb> getDatabases();
    
}
