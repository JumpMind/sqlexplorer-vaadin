package org.jumpmind.vaadin.ui.sqlexplorer;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import org.apache.commons.dbcp.BasicDataSource;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.Configuration.ClassList;
import org.eclipse.jetty.webapp.WebAppContext;
import org.h2.Driver;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.UI;

@Title("SQL Explorer Demo")
@Theme("valo")
@PreserveOnRefresh
@Push
public class DemoUI extends UI implements IDbProvider {

    static final Logger log = LoggerFactory.getLogger(DemoUI.class);

    private static final long serialVersionUID = 1L;

    List<IDb> dbList;

    @Override
    protected void init(VaadinRequest request) {
        dbList = new ArrayList<IDb>();
        dbList.add(new DB("DATABASE1"));
        dbList.add(new DB("DATABASE2"));
        SqlExplorer explorer = new SqlExplorer("build", this, "admin", 300);
        setContent(explorer);
        explorer.refresh();
    }

    @Override
    public List<IDb> getDatabases() {
        return dbList;
    }

    @WebServlet(urlPatterns = "/*")
    @VaadinServletConfiguration(ui = DemoUI.class, productionMode = true, widgetset = "org.jumpmind.vaadin.ui.sqlexplorer.AppWidgetSet")
    public static class DemoUIServlet extends VaadinServlet {
        private static final long serialVersionUID = 1L;
    }

    public static class DB implements IDb {

        private static final long serialVersionUID = 1L;

        String name;
        IDatabasePlatform databasePlatform;

        public DB(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public IDatabasePlatform getPlatform() {
            if (databasePlatform == null) {
                BasicDataSource ds = new BasicDataSource();
                ds.setDriverClassName(Driver.class.getName());
                ds.setUrl("jdbc:h2:mem:" + name);
                databasePlatform = JdbcDatabasePlatformFactory.createNewPlatformInstance(ds, new SqlTemplateSettings(), true, false);
            }
            return databasePlatform;
        }
    }

    public static void main(String[] args) throws Exception {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        Server server = new Server(9090);
        ClassList classlist = Configuration.ClassList.setServerDefault(server);
        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration", "org.eclipse.jetty.annotations.AnnotationConfiguration");
        WebAppContext webapp = new WebAppContext();
        webapp.setParentLoaderPriority(true);
        webapp.setConfigurationDiscovered(true);
        webapp.setContextPath("/");
        webapp.setResourceBase("src/main/webapp");
        webapp.setWar("src/main/webapp");
        webapp.addServlet(DemoUIServlet.class, "/*");
        server.setHandler(webapp);
        server.start();
        log.info("Browse http://localhost:9090 to see the demo");
        server.join();
    }

}
