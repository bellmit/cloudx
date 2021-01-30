package cloud.apposs.cachex.storage;

import java.util.List;

public class DaoListenerAdapter implements DaoListener {
    @Override
    public void selectStart(String table, String primary, Object identity, Query query) {
    }

    @Override
    public void selectComplete(String table, String primary, Object identity, Query query, Throwable t) {
    }

    @Override
    public void selectStart(String sql, String primary) {
    }

    @Override
    public void selectComplete(String sql, String primary, Throwable t) {
    }

    @Override
    public void queryStart(String table, String primary, Query query) {
    }

    @Override
    public void queryComplete(String table, String primary, Query query, Throwable t) {
    }

    @Override
    public void queryStart(String sql, String primary) {
    }

    @Override
    public void queryComplete(String sql, String primary, Throwable t) {
    }

    @Override
    public void updateStart(String table, Entity entity) {
    }

    @Override
    public void updateComplete(String table, Entity entity, Throwable t) {
    }

    @Override
    public void updateStart(String table, List<Entity> entities) {
    }

    @Override
    public void updateComplete(String table, List<Entity> entities, Throwable t) {
    }

    @Override
    public void updateStart(String table, Updater updater) {
    }

    @Override
    public void updateComplete(String table, Updater updater, Throwable t) {
    }

    @Override
    public void updateStart(String sql) {
    }

    @Override
    public void updateComplete(String sql, Throwable t) {
    }

    @Override
    public void deleteStart(String table, Entity entity) {
    }

    @Override
    public void deleteComplete(String table, Entity entity, Throwable t) {
    }

    @Override
    public void deleteStart(String table, List<Entity> entities) {
    }

    @Override
    public void deleteComplete(String table, List<Entity> entities, Throwable t) {
    }

    @Override
    public void deleteStart(String table, String primary, Object identity) {
    }

    @Override
    public void deleteComplete(String table, String primary, Object identity, Throwable t) {
    }

    @Override
    public void deleteStart(String table, Where where) {
    }

    @Override
    public void deleteComplete(String table, Where where, Throwable t) {
    }

    @Override
    public void insertStart(String table, Entity entity) {
    }

    @Override
    public void insertStart(String table, Entity entity, Throwable t) {
    }

    @Override
    public void insertStart(String table, List<Entity> entities) {
    }

    @Override
    public void insertStart(String table, List<Entity> entities, Throwable t) {
    }
}
