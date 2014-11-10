package org.jumpmind.symmetric.app.common.persist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Types;
import java.util.Date;
import java.util.List;

import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.Row;
import org.jumpmind.db.util.ResettableBasicDataSource;
import org.jumpmind.symmetric.app.common.TestUtils;
import org.jumpmind.symmetric.app.common.persist.JdbcPersistenceManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JdbcPersistenceManagerTest {

    JdbcPersistenceManager manager;

    Table testTableA;

    @Before
    public void setup() throws Exception {
        manager = new JdbcPersistenceManager(TestUtils.createDatabasePlatform());

        testTableA = new Table("A");
        testTableA.addColumn(new Column("ID", true, Types.INTEGER, -1, -1));
        testTableA.addColumn(new Column("LAST_UPDATE_TIME", false, Types.TIMESTAMP, -1, -1));
        testTableA.addColumn(new Column("NOTE", false, Types.VARCHAR, 100, -1));

        manager.getDatabasePlatform().alterCaseToMatchDatabaseDefaultCase(testTableA);
        manager.getDatabasePlatform().createTables(true, true, testTableA);

    }

    @After
    public void tearDown() throws Exception {
        ResettableBasicDataSource ds = manager.databasePlatform.getDataSource();
        ds.close();
    }

    @Test
    public void testInsert() {
        Date date = new Date();
        manager.insert(new A(1, date, "Hello"), null, null, "A");

        Row row = getRow(1);
        assertEquals(1, row.get("id"));
        assertEquals("Hello", row.get("note"));
        assertEquals(date, row.get("last_update_time"));
    }

    @Test
    public void testUpdate() {
        Date date = new Date();
        A a = new A(1, date, "Hello");
        manager.insert(a, null, null, "A");

        Row row = getRow(1);
        assertEquals("Hello", row.get("note"));

        a.setNote("Goodbye");
        manager.update(a, null, null, "A");

        row = getRow(1);
        assertEquals("Goodbye", row.get("note"));

    }

    @Test
    public void testSave() {
        Date date = new Date();
        A a = new A(1, date, "Hello");
        assertTrue(manager.save(a));

        date = new Date();
        a.setLastUpdateTime(date);
        assertFalse(manager.save(a));

        Row row = getRow(1);
        assertEquals(date, row.get("last_update_time"));
    }

    @Test
    public void testDelete() {
        Date date = new Date();
        A a = new A(999, date, "Hello");
        assertTrue(manager.save(a));

        assertNotNull(getRow(999));

        manager.delete(a);

        assertNull(getRow(999));

    }

    @Test
    public void testFindAll() {
        for (int i = 0; i < 100; i++) {
            manager.save(new A(i + 1, new Date(), "Test"));
        }

        List<A> as = manager.find(A.class, null, null, "A");
        assertEquals(100, as.size());

        for (int i = 0; i < 100; i++) {
            assertEquals(i + 1, as.get(i).getId());
        }
    }
    
    @Test
    public void testRefresh() {
        for (int i = 55; i < 73; i++) {
            manager.save(new A(i + 1, new Date(), "Test"));
        }        
        
        A a60 = new A(60, null, null);
        manager.refresh(a60, null, null, "a");
        
        assertEquals(60, a60.getId());
        assertEquals("Test", a60.getNote());
        assertNotNull(a60.getLastUpdateTime());
    }

    protected Row getRow(int id) {
        ISqlTemplate template = manager.getDatabasePlatform().getSqlTemplate();
        return template.queryForRow("select * from a where id=?", id);
    }

    public static class A {
        protected int id;
        protected Date lastUpdateTime;
        protected String note;

        public A() {
        }
        
        public A(int id, Date lastUpdateTime, String note) {
            super();
            this.id = id;
            this.lastUpdateTime = lastUpdateTime;
            this.note = note;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public Date getLastUpdateTime() {
            return lastUpdateTime;
        }

        public void setLastUpdateTime(Date lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

    }

}
