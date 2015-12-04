package org.jumpmind.vaadin.ui.sqlexplorer;

public interface ISettingsProvider {

    public void save(Settings settings);
    
    public Settings get();
    
    public Settings load();
    
}
