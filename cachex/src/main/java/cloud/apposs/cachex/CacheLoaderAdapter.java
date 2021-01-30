package cloud.apposs.cachex;

import java.util.List;

import cloud.apposs.cachex.storage.Query;
import cloud.apposs.protobuf.ProtoSchema;
import cloud.apposs.util.Ref;

public class CacheLoaderAdapter<K extends CacheKey, V> implements CacheLoader<K, V> {
    @Override
    public void initialize(CacheX<K, V> cachex) {
        // Do Nothing
    }

    @Override
    public int add(K key, V value, ProtoSchema schema, CacheX<K, V> cachex,
                   Ref<Object> idRef, Object... args) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public int add(List<V> values, ProtoSchema schema, CacheX<K, V> cachex, List<Object> idRefs, Object... args) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public V load(K key, ProtoSchema schema, CacheX<K, V> cachex, Object... args) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public V select(CacheKey<?> key, ProtoSchema schema, Query query, CacheX<K, V> cachex, Object... args) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<V> query(CacheKey<?> key, ProtoSchema schema,
                         Query query, AbstractCacheX<K, V> cachex, Object[] args) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(K key, CacheX<K, V> cachex, Object... args) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(K key, V value, ProtoSchema schema, CacheX<K, V> cachex, Object... args) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hadd(K key, Object field, V value, ProtoSchema schema,
                    CacheX<K, V> cachex, Ref<Object> idRef, Object... args) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<V> hload(K key, ProtoSchema schema,
                         CacheX<K, V> cachex, Object... args) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hdelete(K key, Object field,
                       CacheX<K, V> cachex, Object... args) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hdelete(K key, Object[] fields, CacheX<K, V> cachex, Object... args) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hupdate(K key, Object field, V value, ProtoSchema schema,
                       CacheX<K, V> cachex, Object... args) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getField(V info) {
        throw new UnsupportedOperationException();
    }
}
