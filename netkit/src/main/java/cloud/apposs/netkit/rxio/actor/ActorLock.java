package cloud.apposs.netkit.rxio.actor;

/**
 * 异步锁，持有这把锁的业务只有当手动释放时，其他拥有同类型锁的业务才能得到锁继续执行
 */
public final class ActorLock {
    private final Object key;

    private Actor.TaskLock lock;

    public ActorLock(Object key) {
        this.key = key;
    }

    public void setLock(Actor.TaskLock lock) {
        this.lock = lock;
    }

    public void unlock() {
        lock.release();
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return key.equals(obj);
    }

    @Override
    public String toString() {
        return key.toString();
    }
}
