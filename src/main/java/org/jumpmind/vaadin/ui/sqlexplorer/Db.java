package org.jumpmind.vaadin.ui.sqlexplorer;

import org.jumpmind.db.platform.IDatabasePlatform;

public class Db implements IDb {

    String name;
    IDatabasePlatform platform;

    public Db(String name, IDatabasePlatform platform) {
        this.name = name;
        this.platform = platform;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IDatabasePlatform getPlatform() {
        return platform;
    }

}
