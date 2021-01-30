package cloud.apposs.cachex;

import cloud.apposs.cachex.storage.Dao;
import cloud.apposs.cachex.CacheXConfig.ElasticSearchConfig;
import cloud.apposs.cachex.storage.Entity;
import cloud.apposs.cachex.storage.Metadata;
import cloud.apposs.cachex.storage.SqlBuilder;
import cloud.apposs.cachex.storage.Where;
import cloud.apposs.util.Ref;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

public class TestEsDao {
    public static final String DIALECT = SqlBuilder.DIALECT_ELASTICSEARCH;
//    public static final String ESHOSTS = "172.17.2.30:19201, 172.17.2.30:19202";
    public static final String ESHOSTS = "172.17.2.30:9200";
//    public static final String ESHOSTS = "192.168.1.5:9200";

    public static final String TABLE = "user";

    private Dao dao;

    @Before
    public void before() throws Exception {
        ElasticSearchConfig esConfig = new ElasticSearchConfig();
        esConfig.setHosts(ESHOSTS);
        CacheXConfig config = new CacheXConfig();
        config.setDialect(DIALECT);
        config.setDevelop(true);
        config.setEsConfig(esConfig);
        dao = new Dao(config);
    }

    @Test
    public void testExists() throws Exception {
        Metadata metadata = new Metadata(TABLE);
        TestCase.assertTrue(dao.exist(metadata));
    }

    @Test
    public void testCreate() throws Exception {
        Metadata metadata = new Metadata(TABLE);
        metadata.addColumn("id", Metadata.COLUMN_TYPE_INT);
        metadata.addColumn("name", Metadata.COLUMN_TYPE_STRING);
        metadata.addColumn("class", Metadata.COLUMN_TYPE_INT);
        TestCase.assertTrue(dao.create(metadata, true));
    }

    @Test
    public void testInsert() throws Exception {
        Entity e = new Entity("id");
        e.setInt("class", 100);
        e.setString("name", "way100");
        Ref<Object> idRef = new Ref<Object>();
        int count = dao.insert(TABLE, e, idRef);
        TestCase.assertTrue(count > 0);
    }

    @Test
    public void testInsertBatch() throws Exception {
        Entity e0 = new Entity("id");
        e0.setInt("class", 101);
        e0.setString("name", "way101");
        Entity e1 = new Entity("id");
        e1.setInt("class", 102);
        e1.setString("name", "way102");
        List<Entity> all = new LinkedList<Entity>();
        all.add(e0);
        all.add(e1);
        List<Object> idList = new LinkedList<Object>();
        int count = dao.insert(TABLE, all, idList);
        TestCase.assertTrue(all.size() == count);
    }

    @Test
    public void testSelect() throws Exception {
        Entity datas = dao.select(TABLE);
        System.out.println(datas);
    }

    @Test
    public void testQuery() throws Exception {
        long start = System.currentTimeMillis();
        List<Entity> datas = dao.query(TABLE);
        System.out.println(datas);
        System.out.println("Execute Time:" + (System.currentTimeMillis() - start));
    }

    @Test
    public void testDeleteWhere() throws Exception {
        Where where = new Where("id", Where.EQ, 17);
        int count = dao.delete(TABLE, where);
        System.out.println(count);
    }

    @Test
    public void testExecuteSelect() throws Exception {
        SqlBuilder builder = dao.getBuilder();
        Entity entity = builder.executeSelect("/" + TABLE + "/_doc/_search", null, (Object[]) null);
        System.out.println(entity);
    }

    @After
    public void after() throws Exception {
        if (dao != null) {
            dao.shutdown();
        }
    }
}
