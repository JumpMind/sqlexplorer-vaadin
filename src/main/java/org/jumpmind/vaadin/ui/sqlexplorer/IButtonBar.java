package org.jumpmind.vaadin.ui.sqlexplorer;


public interface IButtonBar {
    
    public void setExecuteScriptButtonEnabled(boolean enabled);
    
    public void setExecuteAtCursorButtonEnabled(boolean enabled);
    
    public void setCommitButtonEnabled(boolean enabled);
    
    public void setRollbackButtonEnabled(boolean enabled);

}
