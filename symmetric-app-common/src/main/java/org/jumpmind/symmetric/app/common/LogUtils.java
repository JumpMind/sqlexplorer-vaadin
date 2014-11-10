package org.jumpmind.symmetric.app.common;

import java.io.File;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.ApplicationContext;

public final class LogUtils {

    private LogUtils() {
    }

    public static void initLogging(ApplicationContext ctx) {
        /* Optionally remove existing handlers attached to j.u.l root logger */
        SLF4JBridgeHandler.removeHandlersForRootLogger();

        /*
         * Add SLF4JBridgeHandler to j.u.l's root logger, should be done once
         * during the initialization phase of your application
         */
        SLF4JBridgeHandler.install();

        boolean consoleEnabled = Boolean.parseBoolean(ctx.getEnvironment().getProperty(
                EnvConstants.LOG_TO_CONSOLE_ENABLED, "true"));
        if (!consoleEnabled) {
            org.apache.log4j.Logger.getRootLogger().removeAppender("CONSOLE");
        }

        boolean fileEnabled = Boolean.parseBoolean(ctx.getEnvironment().getProperty(
                EnvConstants.LOG_TO_FILE_ENABLED, "true"));
        if (fileEnabled) {
            String logFile = ctx.getEnvironment().getProperty(EnvConstants.LOG_FILE,
                    "logs/application.log");
            File dir = new File(logFile).getParentFile();
            dir.mkdirs();
            try {
                RollingFileAppender logFileAppender = new RollingFileAppender();
                logFileAppender.setFile(logFile);
                logFileAppender.setMaxBackupIndex(10);
                logFileAppender.setMaxFileSize("10MB");
                logFileAppender.setAppend(true);
                logFileAppender.setLayout(new PatternLayout("%d %-5p [%c{1}] [%t] %m%n"));
                org.apache.log4j.Logger.getRootLogger().addAppender(logFileAppender);
                logFileAppender.activateOptions();
            } catch (Exception ex) {
                System.err.println("Failed to configure the following log file: " + logFile);
                ex.printStackTrace();
            }
        }

    }
}
