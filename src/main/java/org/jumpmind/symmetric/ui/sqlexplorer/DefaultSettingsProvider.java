package org.jumpmind.symmetric.ui.sqlexplorer;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSettingsProvider implements ISettingsProvider, Serializable {

    private static final long serialVersionUID = 1L;

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
            File file = getSettingsFile();
            if (file.exists() && file.length() > 0) {
                FileInputStream is = null;
                try {
                    is = new FileInputStream(file);
                    XMLDecoder decoder = new XMLDecoder(is);
                    Settings settings = (Settings) decoder.readObject();
                    decoder.close();
                    return settings;
                } catch (Exception ex) {
                    log.error("Failed to load settings", ex);
                    FileUtils.deleteQuietly(file);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
            return new Settings();
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
