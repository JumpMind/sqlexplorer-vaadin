package org.jumpmind.symmetric.ui.sqlexplorer;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSettingsProvider implements ISettingsProvider {

    final Logger log = LoggerFactory.getLogger(getClass());

    String dir;

    Settings settings;

    public DefaultSettingsProvider(String dir) {
        this.dir = dir;
    }

    protected File getSettingsFile() {
        return new File(dir, "sqlexplorer-settings.xml");
    }

    @Override
    public void save(Settings settings) {
        synchronized (getClass()) {
            File file = getSettingsFile();
            FileOutputStream os = null;
            try {
                os = new FileOutputStream(file, false);
                XMLEncoder encoder = new XMLEncoder(os);
                encoder.writeObject(settings);
                encoder.close();
                this.settings = settings;
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            } finally {
                IOUtils.closeQuietly(os);
            }
        }
    }

    public Settings load() {
        synchronized (getClass()) {
            File historyFile = getSettingsFile();
            if (historyFile.exists()) {
                FileInputStream is = null;
                try {
                    is = new FileInputStream(historyFile);
                    XMLDecoder decoder = new XMLDecoder(is);
                    settings = (Settings) decoder.readObject();
                    decoder.close();
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            } else {
                settings = new Settings();
            }
            return settings;
        }
    }

    @Override
    public Settings get() {
        if (settings == null) {
            settings = load();
        }
        return settings;
    }

}
