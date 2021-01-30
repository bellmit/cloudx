package cloud.apposs.discovery;

import cloud.apposs.balance.Peer;
import cloud.apposs.registry.ServiceInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TestDiscovery {
    private static final String DIR = System.getProperty("user.dir") + "/target/classes/";
    private static final String ZOOKEEPER_SERVER = "172.17.2.39:2081";
    private static final int MODE_ZOOKEEPER = 0;
    private static final int MODE_QCONF = 1;
    private static final int MODE_FILE = 2;
    private static final int MODE_MEMORY = 3;

    private IDiscovery discovery;

    @Before
    public void before() throws Exception {
        int mode = 2;
        if (mode == MODE_ZOOKEEPER) {
            discovery = new ZooKeeperDiscovery(ZOOKEEPER_SERVER, "/registry");
        } else if (mode == MODE_QCONF) {
            discovery = new QconfDiscovery("center", "/registry");
        } else if (mode == MODE_FILE) {
            discovery = new FileDiscovery(DIR + "proxy.conf");
        } else if (mode == MODE_MEMORY) {
            Map<String, List<Peer>> peers = new HashMap<String, List<Peer>>();
            List<Peer> peerList = new LinkedList<Peer>();
            peerList.add(new Peer("106.75.177.38", 14080));
            peerList.add(new Peer("106.75.136.85", 14080));
            peers.put("sid1", peerList);
            discovery = new MemoryDiscovery(peers);
        }
        discovery.start();
    }

    @Test
    public void testDiscovery() throws Exception {
        ServiceInstance instance1 = discovery.choose("sid1", 854);
        ServiceInstance instance2 = discovery.choose("sid1", 854);
        ServiceInstance instance3 = discovery.choose("sid2", 854);

        Assert.assertNotNull(instance1);
        Assert.assertNotNull(instance2);
        System.out.println(instance1);
        System.out.println(instance3);
    }

    @After
    public void after() {
        if (discovery != null) {
            discovery.shutdown();
        }
    }
}
