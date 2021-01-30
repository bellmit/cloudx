package cloud.apposs.balance;

import cloud.apposs.balance.balancer.FileAwareLoadBalancer;
import cloud.apposs.balance.balancer.FileAwareLoadBalancer.FileAwarePeerParser;
import cloud.apposs.balance.balancer.QconfAwareLoadBalancer;
import cloud.apposs.balance.balancer.ResourceAwareLoadBalancer;
import cloud.apposs.balance.balancer.ResourceAwareLoadBalancer.ResourceAwarePeerParser;
import cloud.apposs.balance.balancer.ZooKeeperAwareLoadBalancer;
import cloud.apposs.balance.ping.SocketPing;
import cloud.apposs.balance.rule.ChannelRule;
import cloud.apposs.balance.rule.LessConnRule;
import cloud.apposs.balance.rule.RandomRule;
import cloud.apposs.util.IoUtil;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class TestLoadBalancer {
    private static final String LB_CFG_FILE = "lbProxy.conf";
    private static final String ZOOKEEPER_SERVER = "192.168.1.6:2081";

    @Test
    public void testSimpleLB() {
        int aid = 100;
        ILoadBalancer balancer = new BaseLoadBalancer();
        Peer peer1 = new Peer("192.168.1.1", 1001);
        Peer peer2 = new Peer("192.168.1.2", 1002);
        balancer.addPeers(peer1, peer2);

        for (int i = 0; i < 10; i++) {
            Peer peer = balancer.choosePeer(aid);
            System.out.println(peer);
        }
    }

    @Test
    public void testRandomLB() {
        int aid = 100;
        ILoadBalancer balancer = new BaseLoadBalancer();
        Peer peer1 = new Peer("192.168.1.1", 1001);
        Peer peer2 = new Peer("192.168.1.2", 1002);
        balancer.addPeers(peer1, peer2);
        balancer.setRule(new RandomRule());

        for (int i = 0; i < 10; i++) {
            Peer peer = balancer.choosePeer(aid);
            System.out.println(peer);
        }
    }

    @Test
    public void testLessConnLB() {
        int aid = 100;
        ILoadBalancer balancer = new BaseLoadBalancer();
        Peer peer1 = new Peer("192.168.1.1", 1001);
        Peer peer2 = new Peer("192.168.1.2", 1002);
        balancer.addPeers(peer1, peer2);
        balancer.setRule(new LessConnRule());

        for (int i = 0; i < 10; i++) {
            Peer peer = balancer.choosePeer(aid);
            System.out.println(peer);
        }
    }

    @Test
    public void testChannelLB() {
        ILoadBalancer balancer = new BaseLoadBalancer();
        Peer peer1 = new Peer("192.168.1.1", 1001);
        peer1.addMetadata("traffic", true);
        Peer peer2 = new Peer("192.168.1.2", 1002);
        peer2.addMetadata("master", true);
        Peer peer3 = new Peer("192.168.1.3", 1003);
        peer3.addMetadata("slave", true);
        Peer peer4 = new Peer("192.168.1.4", 1004);
        peer4.addMetadata("traffic", true);
        peer4.addMetadata("slave", true);
        balancer.addPeers(peer1, peer2, peer3, peer4);
        balancer.setRule(new ChannelRule());

        ChannelRule.IPeerChannel key1 = new ChannelRule.IPeerChannel() {
            @Override
            public String getChannel() {
                return "master";
            }
        };
        ChannelRule.IPeerChannel key2 = new ChannelRule.IPeerChannel() {
            @Override
            public String getChannel() {
                return "slave";
            }
        };
        ChannelRule.IPeerChannel key3 = new ChannelRule.IPeerChannel() {
            @Override
            public String getChannel() {
                return "traffic";
            }
        };
        for (int i = 0; i < 10; i++) {
            Peer matchPeer1 = balancer.choosePeer(key1);
            Peer matchPeer2 = balancer.choosePeer(key2);
            Peer matchPeer3 = balancer.choosePeer(key3);
            Assert.assertTrue("192.168.1.2".equals(matchPeer1.getHost()));
            System.out.println("slave peer:" + matchPeer2);
            Assert.assertTrue("192.168.1.1".equals(matchPeer3.getHost()) || "192.168.1.4".equals(matchPeer3.getHost()));
        }
    }

    @Test
    public void testFileAwareLB() throws Exception {
        LbConfig config = new LbConfig();
        config.setAutoPing(true);
        ILoadBalancer balancer = new FileAwareLoadBalancer(config, LB_CFG_FILE, new FileAwarePeerParser() {
            @Override
            public List<Peer> parseServerList(InputStream content) {
                List<Peer> peerList = new LinkedList<Peer>();
                List<String> lineList = IoUtil.readLines(content);
                for (String line : lineList) {
                    String[] peerSplit = line.split(":");
                    peerList.add(new Peer(peerSplit[0], Integer.parseInt(peerSplit[1])));
                }
                return peerList;
            }
        });
        balancer.setPing(new SocketPing());

        Thread.sleep(100);
        int aid = 100;
        for (int i = 0; i < 10; i++) {
            Peer peer = balancer.choosePeer(aid);
            System.out.println(peer);
        }
        balancer.shutdown();
    }

    @Test
    public void testResourceAwareLB() throws Exception {
        LbConfig config = new LbConfig();
        config.setAutoPing(true);
        ILoadBalancer balancer = new ResourceAwareLoadBalancer(config, LB_CFG_FILE, new ResourceAwarePeerParser() {
            @Override
            public List<Peer> parseServerList(InputStream content) {
                List<Peer> peerList = new LinkedList<Peer>();
                List<String> lineList = IoUtil.readLines(content);
                for (String line : lineList) {
                    String[] peerSplit = line.split(":");
                    peerList.add(new Peer(peerSplit[0], Integer.parseInt(peerSplit[1])));
                }
                return peerList;
            }
        });

        // 因为底层是用PollingDiscovery定时从其他服务获取可用节点，所以启动时先让定时线程跑起来寻找服务先
        Thread.sleep(100);
        int aid = 100;
        for (int i = 0; i < 10; i++) {
            Peer peer = balancer.choosePeer(aid);
            System.out.println(peer);
        }
        balancer.shutdown();
    }

    @Test
    public void testZooKeeperLB() throws Exception {
        ILoadBalancer balancer = new ZooKeeperAwareLoadBalancer(ZOOKEEPER_SERVER, "/registry/sid1",
                new ZooKeeperAwareLoadBalancer.ZooKeeperAwarePeerParser() {
                    @Override
                    public List<Peer> parseServerList(List<String> addressList) {
                        List<Peer> peerList = new LinkedList<Peer>();
                        for (String addressStr : addressList) {
                            Param addressInfo = JsonUtil.parseJsonParam(addressStr);
                            peerList.add(new Peer(addressInfo.getString("host"), addressInfo.getInt("port")));
                        }
                        return peerList;
                    }

                    @Override
                    public void exceptionCaught(Throwable cause) {
                        cause.printStackTrace();
                    }
                });
        int aid = 100;
        for (int i = 0; i < 10; i++) {
            Peer peer = balancer.choosePeer(aid);
            System.out.println(peer);
        }
        balancer.shutdown();
    }

    @Test
    public void testQConfLB() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ILoadBalancer balancer = new QconfAwareLoadBalancer("center", "/registry/sid1",
                new QconfAwareLoadBalancer.QconfAwarePeerParser() {
                    @Override
                    public List<Peer> parseServerList(List<String> addressList) {
                        List<Peer> peerList = new LinkedList<Peer>();
                        for (String addressStr : addressList) {
                            Param addressInfo = JsonUtil.parseJsonParam(addressStr);
                            peerList.add(new Peer(addressInfo.getString("host"), addressInfo.getInt("port")));
                        }
                        latch.countDown();
                        return peerList;
                    }

                    @Override
                    public void exceptionCaught(Throwable cause) {
                        cause.printStackTrace();
                        latch.countDown();
                    }
                });
        latch.await();
        int aid = 100;
        for (int i = 0; i < 10; i++) {
            Peer peer = balancer.choosePeer(aid);
            System.out.println(peer);
        }
        balancer.shutdown();
    }
}
