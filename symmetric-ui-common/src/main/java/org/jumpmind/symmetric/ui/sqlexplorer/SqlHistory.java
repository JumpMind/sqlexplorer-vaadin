package org.jumpmind.symmetric.ui.sqlexplorer;

import java.io.Serializable;
import java.util.Date;

public class SqlHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sqlStatement;
    private Date lastExecuteTime;
    private long lastExecuteDuration;
    private String lastExecuteUserId;
    private long executeCount;

    public String getSqlStatement() {
        return sqlStatement;
    }

    public void setSqlStatement(String sqlStatement) {
        this.sqlStatement = sqlStatement;
    }

    public Date getLastExecuteTime() {
        return lastExecuteTime;
    }

    public void setLastExecuteTime(Date lastExecuteTime) {
        this.lastExecuteTime = lastExecuteTime;
    }

    public long getLastExecuteDuration() {
        return lastExecuteDuration;
    }

    public void setLastExecuteDuration(long lastExecuteDuration) {
        this.lastExecuteDuration = lastExecuteDuration;
    }

    public String getLastExecuteUserId() {
        return lastExecuteUserId;
    }

    public void setLastExecuteUserId(String lastExecuteUserId) {
        this.lastExecuteUserId = lastExecuteUserId;
    }

    public long getExecuteCount() {
        return executeCount;
    }

    public void setExecuteCount(long executeCount) {
        this.executeCount = executeCount;
    }

}
