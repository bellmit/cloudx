package cloud.apposs.cachex;

import cloud.apposs.cachex.storage.Dao;
import cloud.apposs.cachex.storage.Entity;
import cloud.apposs.cachex.storage.Metadata;
import cloud.apposs.cachex.storage.SqlBuilder;
import cloud.apposs.util.Convertor;
import cloud.apposs.util.Ref;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;

public class TestSqliteDao {
    public static final String SQLITE_URL = "jdbc:sqlite://C:/fstg.tdb";
    public static final String TABLE = "fstg";

    private Dao dao;

    @Before
    public void before() throws Exception {
        CacheXConfig.DbConfig dbCfg = new CacheXConfig.DbConfig();
        dbCfg.setJdbcUrl(SQLITE_URL);
        dbCfg.setTestConnectionOnCheckout(false);
        dbCfg.setPoolOperationWatch(false);
        CacheXConfig config = new CacheXConfig();
        config.setDevelop(true);
        config.setDialect(SqlBuilder.DIALECT_SQLITE);
        config.setDbConfig(dbCfg);
        dao = new Dao(config);
    }

    @Test
    public void testCreate() throws Exception {
        Metadata metadata = new Metadata(TABLE);
        metadata.addColumn("aid", Metadata.COLUMN_TYPE_INT, 11);
        metadata.addColumn("app", Metadata.COLUMN_TYPE_INT, 11, true);
        metadata.addColumn("fileId", Metadata.COLUMN_TYPE_STRING, 64, true);
        metadata.addColumn("fileName", Metadata.COLUMN_TYPE_STRING, 64, true);
        metadata.addColumn("folder", Metadata.COLUMN_TYPE_INT, 11, true, "0");
        metadata.addColumn("filePath", Metadata.COLUMN_TYPE_STRING, 128, true);
        metadata.addColumn("flag", Metadata.COLUMN_TYPE_LONG, 11, true, "0");
        metadata.addColumn("createTime", Metadata.COLUMN_TYPE_DATE, -1, true);
        metadata.addPrimaryIndex("aid, app, fileId, folder");
        metadata.addIndex("fileId");
        metadata.addIndex("app");
        metadata.addIndex("folder");
        TestCase.assertTrue(dao.create(metadata, true));
    }

    @Test
    public void testInsert() throws Exception {
        Entity e = new Entity("id");
        e.setInt("aid", 854);
        e.setInt("app", 21);
        e.setString("fileId", "384d70279617ef42f298c264c2060d13850199c7");
        e.setString("fileName", "myjpg.png");
        e.setString("filePath", "/fdfs/00/01");
        e.setCalendar("createTime", Calendar.getInstance());
        Ref<Object> idRef = new Ref<Object>();
        int count = dao.insert(TABLE, e, idRef);
        TestCase.assertTrue(count > 0);
    }

    @Test
    public void testQuery() throws Exception {
        long start = System.currentTimeMillis();
        List<Entity> datas = dao.query(TABLE);
        System.out.println(datas);
        System.out.println("Execute Time:" + (System.currentTimeMillis() - start));
    }

    @After
    public void after() {
        if (dao != null) {
            dao.shutdown();
        }
    }
}
