package org.jumpmind.symmetric.ui.sqlexplorer;

import java.util.List;

public interface IDbProvider {

    public List<IDb> getDatabases();
    
}
