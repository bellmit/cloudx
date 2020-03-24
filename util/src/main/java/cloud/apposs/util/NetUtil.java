package cloud.apposs.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

/**
 * 网络、网卡操作工具
 */
public final class NetUtil {
    public static final class Ipv4{
        /**
         * 把IP地址转化为字节数组，
         * 解析异常时抛出{@link IllegalArgumentException}
         */
        public static final byte[] toIpByte(String ip) {
            try {
                return InetAddress.getByName(ip).getAddress();
            } catch (Exception e) {
                throw new IllegalArgumentException("invalid ip address " + ip);
            }
        }
    }

    /**
     * 获取所有IPV4网卡地址
     */
    public static final List<NetInterface> getLocalAddressList() {
        return getLocalAddressList(false);
    }

    /**
     * 获取所有网卡地址
     *
     * @param withIpv6 是否连同IPV6地址也获取
     */
    public static final List<NetInterface> getLocalAddressList(boolean withIpv6) {
        List<NetInterface> addressList = new LinkedList<NetInterface>();
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface iface = netInterfaces.nextElement();
                String netName = iface.getName();
                Enumeration<InetAddress> ias = iface.getInetAddresses();
                NetInterface ninfo = new NetInterface(netName);
                while (ias.hasMoreElements()) {
                    InetAddress ia = ias.nextElement();
                    // 只有ip4，但IP地址的byte>4跳过
                    if (!withIpv6 && ia.getAddress().length > 4) {
                        continue;
                    }
                    ninfo.addAddress(ia);
                }
                addressList.add(ninfo);
            }
        } catch (SocketException e) {
        }
        return addressList;
    }

    /**
     * 获取所有IPV4网卡地址，以{网卡名称:网卡信息}建立索引便于快速定位
     */
    public static final Map<String, NetInterface> getLocalAddressInfo() {
        return getLocalAddressInfo(false);
    }

    /**
     * 获取所有网卡地址，以{网卡名称:网卡信息}建立索引便于快速定位
     *
     * @param withIpv6 是否连同IPV6地址也获取
     */
    public static final Map<String, NetInterface> getLocalAddressInfo(boolean withIpv6) {
        Map<String, NetInterface> addressInfo = new HashMap<String, NetInterface>();
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface iface = netInterfaces.nextElement();
                String netName = iface.getName();
                Enumeration<InetAddress> ias = iface.getInetAddresses();
                NetInterface ninfo = new NetInterface(netName);
                while (ias.hasMoreElements()) {
                    InetAddress ia = ias.nextElement();
                    // 只有ip4，但IP地址的byte>4跳过
                    if (!withIpv6 && ia.getAddress().length > 4) {
                        continue;
                    }
                    ninfo.addAddress(ia);
                }
                if (!ninfo.isEmpty()) {
                    addressInfo.put(netName, ninfo);
                }
            }
        } catch (SocketException e) {
        }
        return addressInfo;
    }

    public static final class NetInterface {
        public static final String NETINTERFACE_NAME = "name";

        private final String name;

        private final List<InetAddress> addresses;

        public NetInterface(String name) {
            this.name = name;
            this.addresses = new LinkedList<InetAddress>();
        }

        public String getName() {
            return name;
        }

        public boolean isEmpty() {
            return addresses.isEmpty();
        }

        public List<InetAddress> getAddresses() {
            return addresses;
        }

        public void addAddress(InetAddress address) {
            addresses.add(address);
        }

        public InetAddress getLocalAddress() {
            return addresses.isEmpty() ? null : addresses.get(0);
        }

        @Override
        public String toString() {
            return "NetInterface{" +
                    "name='" + name + '\'' +
                    ", addresses=" + addresses +
                    '}';
        }
    }
}
