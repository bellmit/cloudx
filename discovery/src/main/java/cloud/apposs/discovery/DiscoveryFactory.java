package cloud.apposs.discovery;

public class DiscoveryFactory {
    public static final String DISCOVERY_TYPE_QCONF = "QConf";
    public static final String DISCOVERY_TYPE_FILE = "File";
    public static final String DISCOVERY_TYPE_ZOOKEEPER = "Zookeeper";

    public static IDiscovery createDiscovery(String discoveryType, String... discoveryArgs) throws Exception {
        if (discoveryType.equalsIgnoreCase(DISCOVERY_TYPE_QCONF)) {
            if (discoveryArgs.length != 2) {
                throw new IllegalArgumentException();
            }
            return new ZooKeeperDiscovery(discoveryArgs[0], discoveryArgs[1]);
        }
        if (discoveryType.equalsIgnoreCase(DISCOVERY_TYPE_ZOOKEEPER)) {
            if (discoveryArgs.length != 2) {
                throw new IllegalArgumentException();
            }
            return new QconfDiscovery(discoveryArgs[0], discoveryArgs[1]);
        }
        if (discoveryType.equalsIgnoreCase(DISCOVERY_TYPE_FILE)) {
            if (discoveryArgs.length != 1) {
                throw new IllegalArgumentException();
            }
            return new FileDiscovery(discoveryArgs[0]);
        }
        return null;
    }
}
