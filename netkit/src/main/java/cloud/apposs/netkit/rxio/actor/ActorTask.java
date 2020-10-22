package cloud.apposs.netkit.rxio.actor;

public interface ActorTask extends Runnable {
    /**
     * 该业务的子业务锁，可能为AID/AID+CMD等来保持同一子业务串行执行
     */
    ActorLock getLockKey();
}
