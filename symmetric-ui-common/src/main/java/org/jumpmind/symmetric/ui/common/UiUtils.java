package org.jumpmind.symmetric.ui.common;

import static org.apache.commons.lang.StringUtils.abbreviate;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.time.FastDateFormat;
import org.jumpmind.db.sql.JdbcSqlTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.aceeditor.AceEditor;

import com.vaadin.data.Container;
import com.vaadin.data.Property;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.Position;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.Table;
import com.vaadin.ui.themes.ValoTheme;

public final class UiUtils {

    final static Logger log = LoggerFactory.getLogger(UiUtils.class);

    static final FastDateFormat DATETIMEFORMAT = FastDateFormat.getDateTimeInstance(
            FastDateFormat.SHORT, FastDateFormat.SHORT);

    static final FastDateFormat TIMEFORMAT = FastDateFormat.getTimeInstance(FastDateFormat.MEDIUM);

    static final String NULL_TEXT = "<null>";

    private UiUtils() {
    }
    
    public static void styleTabSheet(TabSheet tabSheet) {
        tabSheet.setSizeFull();
        tabSheet.addStyleName(ValoTheme.TABSHEET_FRAMED);
        tabSheet.addStyleName(ValoTheme.TABSHEET_COMPACT_TABBAR);
        tabSheet.addStyleName(ValoTheme.TABSHEET_PADDED_TABBAR);        
    }

    public static TabSheet createTabSheet() {
        TabSheet tabSheet = new TabSheet();
        styleTabSheet(tabSheet);
        return tabSheet;
    }

    public static Button createPrimaryButton(String name) {
        return createPrimaryButton(name, null);
    }

    public static Button createPrimaryButton(String name, Button.ClickListener listener) {
        Button button = new Button(name);
        if (listener != null) {
            button.addClickListener(listener);
        }
        button.addStyleName(ValoTheme.BUTTON_PRIMARY);
        return button;
    }

    public static Table createTable() {
        Table table = new Table() {

            private static final long serialVersionUID = 1L;

            @Override
            protected String formatPropertyValue(Object rowId, Object colId, Property<?> property) {
                if (property.getValue() != null) {
                    if (property.getType() == Date.class) {
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss aaa");
                        return df.format((Date) property.getValue());
                    } else if (Number.class.isAssignableFrom(property.getType())) {
                        return property.getValue().toString();
                    }
                }
                return super.formatPropertyValue(rowId, colId, property);
            }

        };
        return table;
    }

    public static AceEditor createAceEditor() {
        AceEditor editor = new AceEditor();
        editor.setSizeFull();
        editor.setImmediate(true);
        ServletContext context = VaadinServlet.getCurrent().getServletContext();
        if (context.getRealPath("/ace") != null) {
            String acePath = context.getContextPath() + "/ace";
            editor.setThemePath(acePath);
            editor.setModePath(acePath);
            editor.setWorkerPath(acePath);
        } else {
            log.warn("Could not find a local version of the ace editor.  "
                    + "You might want to consider installing the ace web artifacts at "
                    + context.getRealPath(""));
        }
        editor.setTextChangeEventMode(TextChangeEventMode.EAGER);
        editor.setHighlightActiveLine(true);
        editor.setShowPrintMargin(false);
        return editor;
    }

    public static void notify(String message) {
        notify(null, message, Type.HUMANIZED_MESSAGE);
    }

    public static void notify(String caption, String message) {
        notify(caption, message, Type.HUMANIZED_MESSAGE);
    }

    public static void notify(String message, Type type) {
        notify(null, message, type);
    }

    public static void notify(String caption, String message, Type type) {
        Notification notification = new Notification(caption, message, type);
        notification.setPosition(Position.TOP_CENTER);
        notification.setStyleName(notification.getStyleName() + " " + ValoTheme.NOTIFICATION_BAR
                + " " + ValoTheme.NOTIFICATION_CLOSABLE);
        if (type == Type.ERROR_MESSAGE) {
            notification.setIcon(FontAwesome.WARNING);
        }
        notification.show(Page.getCurrent());
    }
    
    public static void notify(String message, Throwable ex) {
        notify("An unexpected error occurred", "The message was: " + message
                + ".  See the log file for additional details", Type.ERROR_MESSAGE);
    }

    public static void notify(Throwable ex) {
        notify("An unexpected error occurred", "The message was: " + ex.getMessage()
                + ".  See the log file for additional details", Type.ERROR_MESSAGE);
    }

    public static Object getObject(ResultSet rs, int i) throws SQLException {
        Object obj = JdbcSqlTemplate.getResultSetValue(rs, i, false);
        if (obj instanceof byte[]) {
            obj = new String(Hex.encodeHex((byte[]) obj));
        }

        if (obj instanceof String) {
            obj = abbreviate((String) obj, 1024 * 4);
        }
        return obj;
    }

    public static Table putResultsInTable(final ResultSet rs, int maxResultSize,
            final boolean showRowNumbers, String... excludeValues) throws SQLException {

        final Table table = createTable();
        table.setImmediate(true);
        table.setSortEnabled(true);
        table.setSelectable(true);
        table.setMultiSelect(true);
        table.setColumnReorderingAllowed(true);
        table.setColumnReorderingAllowed(true);
        table.setColumnCollapsingAllowed(true);

        final ResultSetMetaData meta = rs.getMetaData();
        int columns = meta.getColumnCount();
        table.addContainerProperty("#", Integer.class, null);
        Set<String> columnNames = new HashSet<String>();
        Set<Integer> skipColumnIndexes = new HashSet<Integer>();
        for (int i = 1; i <= columns; i++) {
            String realColumnName = meta.getColumnName(i);
            String columnName = realColumnName;
            if (!Arrays.asList(excludeValues).contains(columnName)) {

                int index = 1;
                while (columnNames.contains(columnName)) {
                    columnName = realColumnName + "_" + index++;
                }
                columnNames.add(columnName);

                Class<?> typeClass = Object.class;
                int type = meta.getColumnType(i);
                switch (type) {
                    case Types.FLOAT:
                    case Types.DOUBLE:
                    case Types.NUMERIC:
                    case Types.REAL:
                    case Types.DECIMAL:
                        typeClass = BigDecimal.class;
                        break;
                    case Types.TINYINT:
                    case Types.SMALLINT:
                    case Types.BIGINT:
                    case Types.INTEGER:
                        typeClass = Long.class;
                        break;
                    case Types.VARCHAR:
                    case Types.CHAR:
                    case Types.NVARCHAR:
                    case Types.NCHAR:
                    case Types.CLOB:
                        typeClass = String.class;
                    default:
                        break;
                }
                table.addContainerProperty(i, typeClass, null);
                table.setColumnHeader(i, columnName);
            } else {
                skipColumnIndexes.add(i - 1);
            }

        }
        int rowNumber = 1;
        while (rs.next() && rowNumber <= maxResultSize) {
            Object[] row = new Object[columnNames.size() + 1];
            row[0] = new Integer(rowNumber);
            int rowIndex = 1;
            for (int i = 0; i < columns; i++) {
                if (!skipColumnIndexes.contains(i)) {
                    Object o = getObject(rs, i + 1);
                    int type = meta.getColumnType(i + 1);
                    switch (type) {
                        case Types.FLOAT:
                        case Types.DOUBLE:
                        case Types.REAL:
                        case Types.NUMERIC:
                        case Types.DECIMAL:
                            if (o == null) {
                                o = new BigDecimal(-1);
                            }
                            if (!(o instanceof BigDecimal)) {
                                o = new BigDecimal(castToNumber(o.toString()));
                            }
                            break;
                        case Types.TINYINT:
                        case Types.SMALLINT:
                        case Types.BIGINT:
                        case Types.INTEGER:
                            if (o == null) {
                                o = new Long(-1);
                            }

                            if (!(o instanceof Long)) {
                                o = new Long(castToNumber(o.toString()));
                            }
                            break;
                        default:
                            break;
                    }
                    row[rowIndex] = o == null ? NULL_TEXT : o;
                    rowIndex++;
                }
            }
            table.addItem(row, rowNumber);
            rowNumber++;
        }

        if (rowNumber < 100) {
            table.setColumnWidth("#", 18);
        } else if (rowNumber < 1000) {
            table.setColumnWidth("#", 25);
        } else {
            table.setColumnWidth("#", 30);
        }

        if (!showRowNumbers) {
            table.setColumnCollapsed("#", true);
        }

        return table;
    }

    protected static String castToNumber(String value) {
        if ("NO".equalsIgnoreCase(value) || "FALSE".equalsIgnoreCase(value)) {
            return "0";
        } else if ("YES".equalsIgnoreCase(value) || "TRUE".equalsIgnoreCase(value)) {
            return "1";
        } else {
            return value;
        }
    }
    
    public static String formatDuration(long timeInMs) {
        if (timeInMs > 60000) {
            long minutes = timeInMs / 60000;
            long seconds = (timeInMs - (minutes * 60000)) / 1000;
            return minutes + " m " + seconds + " s";
        } else if (timeInMs > 1000) {
            long seconds = timeInMs / 1000;
            return seconds + " s";
        } else {
            return timeInMs + " ms";
        }
    }

    public static String formatDateTime(Date dateTime) {
        if (dateTime != null) {
            Calendar cal = Calendar.getInstance();
            Calendar ref = Calendar.getInstance();
            ref.setTime(dateTime);
            if (ref.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
                    && ref.get(Calendar.YEAR) == cal.get(Calendar.YEAR)) {
                return TIMEFORMAT.format(dateTime);
            } else {
                return DATETIMEFORMAT.format(dateTime);
            }
        } else {
            return null;
        }
    }

    public static void addItems(List<?> items, Container container) {
        for (Object item : items) {
            container.addItem(item);
        }
    }
    
    public static String getJdbcTypeValue(String type) {
        String value = null;
        if (type.equalsIgnoreCase("CHAR")) {
            value = "";
        } else if (type.equalsIgnoreCase("VARCHAR")) {
            value = "";
        } else if (type.equalsIgnoreCase("LONGVARCHAR")) {
            value = "";
        } else if (type.equalsIgnoreCase("NUMERIC")) {
            value = "0";
        } else if (type.equalsIgnoreCase("DECIMAL")) {
            value = "0.00";
        } else if (type.equalsIgnoreCase("BIT")) {
            value = "0";
        } else if (type.equalsIgnoreCase("BOOLEAN")) {
            value = "0";
        } else if (type.equalsIgnoreCase("TINYINT")) {
            value = "0";
        } else if (type.equalsIgnoreCase("SMALLINT")) {
            value = "0";
        } else if (type.equalsIgnoreCase("INTEGER")) {
            value = "0";
        } else if (type.equalsIgnoreCase("BIGINT")) {
            value = "0";
        } else if (type.equalsIgnoreCase("REAL")) {
            value = "0";
        } else if (type.equalsIgnoreCase("DOUBLE")) {
            value = "0.0";
        } else if (type.equalsIgnoreCase("BINARY")) {
            value = null;
        } else if (type.equalsIgnoreCase("VARBINARY")) {
            value = null;
        } else if (type.equalsIgnoreCase("LONGBINARY")) {
            value = null;
        } else if (type.equalsIgnoreCase("DATE")) {
            value = "'2014-07-08'";
        } else if (type.equalsIgnoreCase("TIME")) {
            value = "'12:00:00'";
        } else if (type.equalsIgnoreCase("TIMESTAMP")) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = new Date();
            value = dateFormat.format(date);
        } else if (type.equalsIgnoreCase("CLOB")) {
            value = "''";
        } else if (type.equalsIgnoreCase("BLOB")) {
            value = "''";
        } else if (type.equalsIgnoreCase("ARRAY")) {
            value = "[]";
        } else if (type.equalsIgnoreCase("DISTINCT")) {
            value = null;
        } else if (type.equalsIgnoreCase("STRUCT")) {
            value = null;
        } else if (type.equalsIgnoreCase("REF")) {
            value = null;
        } else if (type.equalsIgnoreCase("DATALINK")) {
            value = null;
        } else if (type.equalsIgnoreCase("JAVA_OBJECT")) {
            value = null;
        } else {
            value = null;
        }
        return value;
    }
}
