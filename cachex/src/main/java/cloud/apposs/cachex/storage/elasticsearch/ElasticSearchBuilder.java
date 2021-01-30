package cloud.apposs.cachex.storage.elasticsearch;

import cloud.apposs.cachex.CacheXConfig;
import cloud.apposs.cachex.CacheXConfig.ElasticSearchConfig;
import cloud.apposs.cachex.storage.Entity;
import cloud.apposs.cachex.storage.Metadata;
import cloud.apposs.cachex.storage.Query;
import cloud.apposs.cachex.storage.SqlBuilder;
import cloud.apposs.cachex.storage.Updater;
import cloud.apposs.cachex.storage.Where;
import cloud.apposs.logger.Logger;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import cloud.apposs.util.Parser;
import cloud.apposs.util.Ref;
import cloud.apposs.util.StrUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 参考：
 * https://www.cnblogs.com/slowcity/p/11727579.html
 * https://www.jianshu.com/p/a621bee0ce1c
 * https://blog.csdn.net/paditang/article/details/78802799
 */
public class ElasticSearchBuilder implements SqlBuilder {
    private final static Map<Integer, String> TYPES = new HashMap<Integer, String>();
    static {
        TYPES.put(Metadata.COLUMN_TYPE_INT, "integer");
        TYPES.put(Metadata.COLUMN_TYPE_LONG, "long");
        TYPES.put(Metadata.COLUMN_TYPE_STRING, "text");
        TYPES.put(Metadata.COLUMN_TYPE_DATE, "date");
    }
    protected static final Map<String, ConditionBuilder> CONDITION_BUILDERS = new HashMap<String, ConditionBuilder>();
    static {
        CONDITION_BUILDERS.put(Where.EQ, new TermQueryBuilder());
        CONDITION_BUILDERS.put(Where.NE, new MustNotQueryBuilder());
    }

    private final CacheXConfig config;

    private final RestHighLevelClient client;

    /** 开发模式下输出查询语句 */
    protected boolean develop = false;

    public ElasticSearchBuilder(CacheXConfig config) {
        ElasticSearchConfig esConfig = config.getEsConfig();
        String hosts = esConfig.getHosts();
        if (StrUtil.isEmpty(hosts)) {
            throw new IllegalArgumentException("EsConfig hosts not configed");
        }
        String[] rawHosts = hosts.split(",");
        HttpHost[] httpHosts = new HttpHost[rawHosts.length];
        for (int i = 0; i < rawHosts.length; i++) {
            String rawHost = rawHosts[i].trim();
            String[] rawHostPort = rawHost.split(":");
            if (rawHostPort.length == 2) {
                httpHosts[i] = new HttpHost(rawHostPort[0], Parser.parseInt(rawHostPort[1]), esConfig.getSchema());
            } else {
                httpHosts[i] = new HttpHost(rawHost, ElasticSearchConfig.DEFAULT_ES_PORT, esConfig.getSchema());
            }
        }
        this.config = config;
        this.develop = config.isDevelop();
        RestClientBuilder builder = RestClient.builder(httpHosts);
        this.client = new RestHighLevelClient(builder);
    }

    @Override
    public String getDialect() {
        return SqlBuilder.DIALECT_ELASTICSEARCH;
    }

    @Override
    public Entity select(String table, String primary, Object identity, ProtoSchema schema, Query query) throws Exception {
        if (identity == null) {
            throw new IllegalArgumentException("identity is null");
        }
        GetRequest request = new GetRequest(table, "_doc", identity.toString());
        GetResponse response = client.get(request, RequestOptions.DEFAULT);

        Entity entity = null;
        Map<String, Object> source = response.getSource();
        if (source != null) {
            entity = new Entity();
            entity.setIdentity(response.getId());
            entity.putAll(source);
        }

        return entity;
    }

    @Override
    public List<Entity> query(String table, String primary, ProtoSchema schema, Query query) throws Exception {
        SearchSourceBuilder search = new SearchSourceBuilder();
        SearchRequest request = new SearchRequest(table);
        request.source(search);
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        SearchHit[] hits = response.getHits().getHits();
        List<Entity> entities = new LinkedList<Entity>();
        for (int i = 0; i < hits.length; i++) {
            SearchHit hit = hits[i];
            Entity entity = new Entity();
            entity.setIdentity(hit.getId());
            entity.putAll(hit.getSourceAsMap());
            entities.add(entity);
        }
        return entities;
    }

    @Override
    public int update(String table, Entity entity, ProtoSchema schema) throws Exception {
        UpdateRequest request = new UpdateRequest(table, "_doc", entity.getIdentity().toString());
        request.doc(entity);
        UpdateResponse response = client.update(request, RequestOptions.DEFAULT);
        return response.getGetResult().isExists() ? 1 : 0;
    }

    @Override
    public int update(String table, List<Entity> entities, ProtoSchema schema) throws Exception {
        BulkRequest request = new BulkRequest();
        for (Entity entity : entities) {
            String id = entity.getIdentity().toString();
            UpdateRequest updateRequest = new UpdateRequest(table, "_doc", id);
            updateRequest.doc(entity);
            request.add(updateRequest);
        }
        BulkResponse response = client.bulk(request, RequestOptions.DEFAULT);
        return response.getItems().length;
    }

    @Override
    public int update(String table, Updater updater) throws Exception {
        QueryBuilder query = doGenerateQueryBuilder(updater.where(), false);
        UpdateByQueryRequest request = new UpdateByQueryRequest(table);
        request.setQuery(query);
        StringBuilder scriptText = new StringBuilder();
        List<Updater.Data> fieldDatas = updater.getDataList();
        int dataSize = fieldDatas.size();
        for (int i = 0; i < dataSize; i++) {
            Updater.Data data = fieldDatas.get(i);
            String dataKey = data.getKey();
            String dataOp = data.getOperation();
            Object dataVal = data.getValue();
            if (!StrUtil.isEmpty(dataOp)) {
                scriptText.append("ctx._source." + dataKey + dataOp + dataVal);
            } else {
                scriptText.append("ctx._source." + dataKey + "=" + dataVal);
            }
            if (i < dataSize - 1) {
                scriptText.append(", ");
            }
        }
        request.setScript(new Script(ScriptType.INLINE, "painless", scriptText.toString(), Collections.emptyMap()));
        BulkByScrollResponse response = client.updateByQuery(request, RequestOptions.DEFAULT);
        return (int) response.getStatus().getUpdated();
    }

    @Override
    public int insert(String table, Entity entity, ProtoSchema schema, Ref<Object> idRef) throws Exception {
        IndexRequest request = new IndexRequest(table, "_doc");
        request.source(entity);
        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
        idRef.value(response.getId());
        return 1;
    }

    @Override
    public int insert(String table, List<Entity> entities, ProtoSchema schema, List<Object> idList) throws Exception {
        BulkRequest request = new BulkRequest();
        for (Entity entity : entities) {
            IndexRequest indexRequest = new IndexRequest(table, "_doc");
            indexRequest.source(entity);
            request.add(indexRequest);
        }
        BulkResponse response = client.bulk(request, RequestOptions.DEFAULT);
        BulkItemResponse[] items = response.getItems();
        if (idList != null) {
            for (int i = 0; i < items.length; i++) {
                BulkItemResponse item = items[i];
                idList.add(item.getId());
            }
        }
        return items.length;
    }

    @Override
    public int delete(String table, Entity entity) throws Exception {
        return delete(table, null, entity.getIdentity());
    }

    @Override
    public int delete(String table, String primary, Object identity) throws Exception {
        DeleteRequest request = new DeleteRequest(table, "_doc", identity.toString());
        DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
        return response.getResult() == DocWriteResponse.Result.DELETED ? 1 : 0;
    }

    @Override
    public int delete(String table, List<Entity> entities) throws Exception {
        BulkRequest request = new BulkRequest();
        for (Entity entity : entities) {
            String id = entity.getIdentity().toString();
            DeleteRequest deleteRequest = new DeleteRequest(table, "_doc", id);
            request.add(deleteRequest);
        }
        BulkResponse response = client.bulk(request, RequestOptions.DEFAULT);
        return response.getItems().length;
    }

    @Override
    public int delete(String table, Where where) throws Exception {
        QueryBuilder query = doGenerateQueryBuilder(where, false);
        if (develop) {
            Logger.info("Execute Delete Query Sql:%s", ((AbstractQueryBuilder<?>)query).toString(false));
        }
        DeleteByQueryRequest request = new DeleteByQueryRequest(table);
        request.setQuery(query);
        BulkByScrollResponse response = client.deleteByQuery(request, RequestOptions.DEFAULT);
        return (int) response.getStatus().getUpdated();
    }

    /**
     * 生成ES查询
     *
     * @param where 查询条件
     * @param scoring 是否要进行打分评估，只有查询语句需要查询打分，其他条件不需要，可以提升查询性能
     */
    private QueryBuilder doGenerateQueryBuilder(Where where, boolean scoring) {
        BoolQueryBuilder query = QueryBuilders.boolQuery();
        List<Where.Condition> conditionList = where.getConditionList();
        for (Where.Condition condition : conditionList) {
            Where whereNext = condition.getWhere();
            if (whereNext != null && !whereNext.isEmpty()) {
                QueryBuilder queryBuilder = doGenerateQueryBuilder(whereNext, scoring);
                if (condition.isAnd()) {
                    if (scoring) {
                        query.must(query);
                    } else {
                        query.filter(queryBuilder);
                    }
                } else {
                    query.should(queryBuilder);
                }
            } else {
                String key = condition.getKey();
                String operation = condition.getOperation();
                Object value = condition.getValue();
                ConditionBuilder conditionBuilder = CONDITION_BUILDERS.get(operation);
                if (conditionBuilder == null) {
                    throw new UnsupportedOperationException("unsupported operation '" + operation+ "'");
                }
                QueryBuilder queryBuilder = conditionBuilder.operationBuild(key, value);
                if (condition.isAnd()) {
                    if (scoring) {
                        query.must(query);
                    } else {
                        query.filter(queryBuilder);
                    }
                } else {
                    query.should(queryBuilder);
                }
            }
        }
        return query;
    }

    @Override
    public Entity executeSelect(String url, String primary, Object... args) throws Exception {
        Response response = doExecuteHttp(url, args);
        Param param = JsonUtil.parseJsonParam(EntityUtils.toString(response.getEntity()));
        return new Entity(param);
    }

    @Override
    public List<Entity> executeQuery(String url, String primary, Object... args) throws Exception {
        Response response = doExecuteHttp(url, args);
        Param param = JsonUtil.parseJsonParam(EntityUtils.toString(response.getEntity()));
        return Collections.singletonList(new Entity(param));
    }

    @Override
    public int executeUpdate(String url, Object... args) throws Exception {
        Response response = doExecuteHttp(url, args);
        return response.getStatusLine().getStatusCode();
    }

    private Response doExecuteHttp(String url, Object... args) throws Exception {
        if (args != null) {
            if (args.length > 1 || !(args[0] instanceof Param)) {
                throw new IllegalArgumentException("args");
            }
        }

        Request request = null;
        if (args != null) {
            request = new Request("POST", url);
            Param params = (Param) args[0];
            HttpEntity httpEntity = new NStringEntity(params.toString(), ContentType.APPLICATION_JSON);
            request.setEntity(httpEntity);
        } else {
            request = new Request("GET", url);
        }

        return client.getLowLevelClient().performRequest(request);
    }

    @Override
    public boolean create(Metadata metadata, boolean dropIfExist) throws Exception {
        String index = metadata.getTable();
        if (dropIfExist && exist(metadata)) {
            DeleteIndexRequest request = new DeleteIndexRequest(index);
            client.indices().delete(request, RequestOptions.DEFAULT);
        }

        // 创建索引
        CreateIndexRequest request = new CreateIndexRequest(index);
        // 设置分片
        ElasticSearchConfig esConfig = config.getEsConfig();
        request.settings(Settings.builder().put("index.number_of_shards", esConfig.getNumberOfShards())
                .put("index.number_of_replicas", esConfig.getNumberOfReplicas()));
        // 设置索引mapping
        Map<String, Object> mapping = new HashMap<String, Object>();
        Map<String, Object> properties = new HashMap<String, Object>();
        List<Metadata.Column> columnList = metadata.getColumnList();
        for (Metadata.Column column : columnList) {
            String colType = TYPES.get(column.getType());
            if (StrUtil.isEmpty(colType)) {
                throw new IllegalStateException("Metadata column type['" + column.getType() + "'] not matched");
            }
            String colName = column.getName();
            Map<String, Object> columnMap = new HashMap<String, Object>();
            columnMap.put("type", colType);
            properties.put(colName, columnMap);
        }
        mapping.put("properties", properties);
        request.mapping(mapping);
        // 请求创建索引
        CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
        return response.isAcknowledged();
    }

    @Override
    public boolean exist(Metadata metadata) throws Exception {
        String index = metadata.getTable();
        GetIndexRequest request = new GetIndexRequest(index);
        request.local(false);
        if (config.isDevelop()) {
            request.humanReadable(true);
        }
        return client.indices().exists(request, RequestOptions.DEFAULT);
    }

    /**
     * ES查询条件
     */
    public interface ConditionBuilder {
        /**
         * 根据键值生成对应的ES QueryBuilder
         */
        QueryBuilder operationBuild(String key, Object value);
    }

    public static class TermQueryBuilder implements ConditionBuilder {
        @Override
        public QueryBuilder operationBuild(String key, Object value) {
            return QueryBuilders.termQuery(key, value);
        }
    }

    public static class MustNotQueryBuilder implements ConditionBuilder {
        @Override
        public QueryBuilder operationBuild(String key, Object value) {
            BoolQueryBuilder query = QueryBuilders.boolQuery();
            return query.mustNot(QueryBuilders.termQuery(key, value));
        }
    }

    @Override
    public void shutdown() {
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
            }
        }
    }
}
