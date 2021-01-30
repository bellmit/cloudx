package cloud.apposs.registry;

import cloud.apposs.util.FileUtil;
import cloud.apposs.util.StrUtil;

public final class RegistryFactory {
    public static final String REGISTRY_TYPE_FILE = "FILE";
    public static final String REGISTRY_TYPE_ZOOKEEPER = "ZOOKEEPER";
    public static final String REGISTRY_TYPE_CURATOR = "CURATOR";

    public static IRegistry createRegistry(String registryType, String... args) {
        if (StrUtil.isEmpty(registryType)) {
            return null;
        }
        if (registryType.equalsIgnoreCase(REGISTRY_TYPE_FILE)) {
            String file = args[0];
            FileUtil.create(file);
            return new FileRegistry(file);
        }

        if (registryType.equalsIgnoreCase(REGISTRY_TYPE_ZOOKEEPER)) {
            String zkServer = args[0];
            return new ZookeeperRegistry(zkServer);
        }

        if (registryType.equalsIgnoreCase(REGISTRY_TYPE_CURATOR)) {
            String zkServer = args[0];
            return new CuratorRegistry(zkServer);
        }

        return null;
    }
}
