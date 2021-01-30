package cloud.apposs.cachex.storage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DaoListenerSupport {
    /**
     * 创建一个监听列表
     */
    private static final List<DaoListener> listenerList = new CopyOnWriteArrayList<DaoListener>();

    public void addListener(DaoListener listener) {
        listenerList.add(listener);
    }

    public void selectStart(String table, String primary, Object identity, Query query) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.selectStart(table, primary, identity, query);
        }
    }

    public void selectComplete(String table, String primary, Object identity, Query query, Throwable t) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.selectComplete(table, primary, identity, query, t);
        }
    }

    public void selectStart(String sql, String primary) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.selectStart(sql, primary);
        }
    }

    public void selectComplete(String sql, String primary, Throwable t) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.selectComplete(sql, primary, t);
        }
    }

    public void queryStart(String table, String primary, Query query) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.queryStart(table, primary, query);
        }
    }

    public void queryComplete(String table, String primary, Query query, Throwable t) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.queryComplete(table, primary, query, t);
        }
    }

    public void queryStart(String sql, String primary) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.queryStart(sql, primary);
        }
    }

    public void queryComplete(String sql, String primary, Throwable t) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.queryComplete(sql, primary, t);
        }
    }

    public void updateStart(String table, Entity entity) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.updateStart(table, entity);
        }
    }

    public void updateComplete(String table, Entity entity, Throwable t) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.updateComplete(table, entity, t);
        }
    }

    public void updateStart(String table, List<Entity> entities) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.updateStart(table, entities);
        }
    }

    public void updateComplete(String table, List<Entity> entities, Throwable t) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.updateComplete(table, entities, t);
        }
    }

    public void updateStart(String table, Updater updater) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.updateStart(table, updater);
        }
    }

    public void updateComplete(String table, Updater updater, Throwable t) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.updateComplete(table, updater, t);
        }
    }

    public void updateStart(String sql) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.updateStart(sql);
        }
    }

    public void updateComplete(String sql, Throwable t) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.updateComplete(sql, t);
        }
    }

    public void deleteStart(String table, Entity entity) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.deleteStart(table, entity);
        }
    }

    public void deleteComplete(String table, Entity entity, Throwable t) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.deleteComplete(table, entity, t);
        }
    }

    public void deleteStart(String table, String primary, Object identity) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.deleteStart(table, primary, identity);
        }
    }

    public void deleteComplete(String table, String primary, Object identity, Throwable t) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.deleteComplete(table, primary, identity, t);
        }
    }

    public void deleteStart(String table, List<Entity> entities) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.deleteStart(table, entities);
        }
    }

    public void deleteComplete(String table, List<Entity> entities, Throwable t) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.deleteComplete(table, entities, t);
        }
    }

    public void deleteStart(String table, Where where) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.deleteStart(table, where);
        }
    }

    public void deleteComplete(String table, Where where, Throwable t) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.deleteComplete(table, where, t);
        }
    }

    public void insertStart(String table, Entity entity) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.insertStart(table, entity);
        }
    }

    public void insertComplete(String table, Entity entity, Throwable t) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.insertStart(table, entity, t);
        }
    }

    public void insertStart(String table, List<Entity> entities) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.insertStart(table, entities);
        }
    }

    public void insertComplete(String table, List<Entity> entities, Throwable t) {
        for (int i = 0; i < listenerList.size(); i++) {
            DaoListener listener = listenerList.get(i);
            listener.insertStart(table, entities, t);
        }
    }
}
