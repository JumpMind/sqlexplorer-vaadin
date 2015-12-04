package org.jumpmind.symmetric.ui.sqlexplorer;

public interface ISettingsProvider {

    public void save(Settings settings);
    
    public Settings get();
    
    public Settings load();
    
}
