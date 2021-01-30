package cloud.apposs.registry;

import cloud.apposs.util.FileUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestRegistry {
    private static final String FILE_RESOURCE = "C://registry.conf";
    private static final String ZOOKEEPER_SERVER = "192.168.1.6:2081";
    private static final int REGISTRY_MODE = 3;

    private IRegistry registry = null;

    @Before
    public void before() {
        if (REGISTRY_MODE == 1) {
            FileUtil.create(FILE_RESOURCE);
            registry = new FileRegistry(FILE_RESOURCE);
        } else if (REGISTRY_MODE == 2) {
            registry = new ZookeeperRegistry(ZOOKEEPER_SERVER);
        } else if (REGISTRY_MODE == 3) {
            registry = new CuratorRegistry(ZOOKEEPER_SERVER);
        }
    }

    @Test
    public void testRegistry() throws Exception {
        ServiceInstance serviceInstance1 = new ServiceInstance("sid1", "1.1.1.1", 8091);
        ServiceInstance serviceInstance2 = new ServiceInstance("sid2", "2.2.2.1", 8092);
        ServiceInstance serviceInstance3 = new ServiceInstance("sid1", "1.1.1.2", 8091);
        ServiceInstance serviceInstance4 = new ServiceInstance("sid2", "2.2.2.2", 8092);
        Assert.assertTrue(registry.registInstance(serviceInstance1));
        Assert.assertTrue(registry.registInstance(serviceInstance2));
        Assert.assertTrue(registry.registInstance(serviceInstance3));
        Assert.assertTrue(registry.registInstance(serviceInstance4));
    }

    @Test
    public void testDeregistry() throws Exception {
        ServiceInstance serviceInstance1 = new ServiceInstance("sid1", "1.1.1.1", 8091);
        ServiceInstance serviceInstance2 = new ServiceInstance("sid2", "2.2.2.1", 8092);
        Assert.assertTrue(registry.deregistInstance(serviceInstance1));
        Assert.assertTrue(registry.deregistInstance(serviceInstance2));
    }

    @After
    public void after() {
        if (registry != null) {
            registry.release();
        }
    }
}
