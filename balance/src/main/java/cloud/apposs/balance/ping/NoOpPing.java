package cloud.apposs.balance.ping;

import cloud.apposs.balance.IPing;
import cloud.apposs.balance.Peer;

/**
 * 不做任何PING检测，永远返回检测OK
 */
public class NoOpPing implements IPing {
    @Override
    public boolean isAlive(Peer server) {
        return true;
    }
}
