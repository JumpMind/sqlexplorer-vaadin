/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.vaadin.ui.sqlexplorer;

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
