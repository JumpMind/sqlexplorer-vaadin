package org.jumpmind.symmetric.ui.common;

import com.vaadin.server.Resource;
import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.themes.ValoTheme;

public class TabbedApplicationPanel extends TabSheet {

    private static final long serialVersionUID = 1L;

    protected Tab mainTab;
    
    public TabbedApplicationPanel() {
        setSizeFull();
        addStyleName(ValoTheme.TABSHEET_FRAMED);
        addStyleName(ValoTheme.TABSHEET_COMPACT_TABBAR);

        addSelectedTabChangeListener(new SelectedTabChangeListener() {
            private static final long serialVersionUID = 1L;        
            @Override
            public void selectedTabChange(SelectedTabChangeEvent event) {
                Component selected = event.getTabSheet().getSelectedTab();
                if (selected instanceof IUiPanel) {
                    ((IUiPanel)selected).showing();
                }
            }
        });
        
        setCloseHandler(new CloseHandler() {            
            private static final long serialVersionUID = 1L;
            @Override
            public void onTabClose(TabSheet tabsheet, Component tabContent) {
                if (tabContent instanceof IUiPanel) {
                    if (((IUiPanel)tabContent).closing()) {
                        tabsheet.removeComponent(tabContent);
                    }
                } else {
                    tabsheet.removeComponent(tabContent);
                }
            }
        });
    }

    public void setMainTab(String caption, Resource icon, Component component) {
        component.setSizeFull();
        this.mainTab = addTab(component, caption, icon, 0);
    }

    public void addCloseableTab(String caption, Resource icon, Component component) {
        component.setSizeFull();
        Tab tab = addTab(component, caption, icon, 1);
        tab.setClosable(true);
        setSelectedTab(tab);
    }

}
