package cloud.apposs.okhttp;

import cloud.apposs.balance.Peer;
import cloud.apposs.discovery.IDiscovery;
import cloud.apposs.discovery.MemoryDiscovery;
import cloud.apposs.netkit.filterchain.http.client.HttpAnswer;
import cloud.apposs.netkit.rxio.IoAction;
import cloud.apposs.netkit.rxio.IoSubscriber;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class TestOkHttp {
    private static final String COMMON_URL = "https://www.baidu.com";
    private static final String BUS_URL = "http://localhost:8880/product/config";
    private IDiscovery discovery;
    private OkHttp okHttp;

    @Before
    public void before() throws Exception {
        Map<String, List<Peer>> peers = new HashMap<String, List<Peer>>();
        List<Peer> peerList = new LinkedList<Peer>();
        peerList.add(new Peer("106.75.177.38", 14080));
        peerList.add(new Peer("106.75.136.85", 14080));
        peers.put("sid1", peerList);
        discovery = new MemoryDiscovery(peers);
        okHttp = HttpBuilder.builder().discovery(discovery).retryCount(3).retrySleepTime(5000).build();
    }

    /**
     * 测试异步HTTP GET请求
     */
    @Test
    public void testHttpExecuteGet() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        okHttp.execute(COMMON_URL).subscribe(new IoAction<HttpAnswer>() {
            @Override
            public void call(HttpAnswer httpAnswer) throws Exception {
                latch.countDown();
                System.out.println(httpAnswer.getContent());
            }
        }).start();
        latch.await();
    }

    /**
     * 测试异步HTTP POST请求
     */
    @Test
    public void testHttpExecutePost() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        FormEntity formEntity = FormEntity.builder()
                .add("name", "xiaomitv")
                .add("price", 99.9);
        IORequest request = IORequest.builder()
                .url("172.17.2.11:8080/web2/login")
                .post(formEntity);
        okHttp.execute(request).subscribe(new IoAction<HttpAnswer>() {
            @Override
            public void call(HttpAnswer httpAnswer) throws Exception {
                latch.countDown();
                System.out.println(httpAnswer.getContent());
            }
        }).start();
        latch.await();
    }

    /**
     * 测试同步HTTP GET请求
     */
    @Test
    public void testHttpPerformGet() throws Exception {
        System.out.println(okHttp.perform(COMMON_URL));
    }

    /**
     * 测试同步HTTP GET请求
     */
    @Test
    public void testHttpPerformTimeout() throws Exception {
        System.out.println(okHttp.perform(BUS_URL));
    }

    /**
     * 测试异步HTTP GET请求
     */
    @Test
    public void testHttpExecuteRetry() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        okHttp.execute(BUS_URL).subscribe(new IoSubscriber<HttpAnswer>() {
            @Override
            public void onNext(HttpAnswer value) throws Exception {
                System.out.println(value.getContent());
                latch.countDown();
            }
            @Override
            public void onCompleted() {
            }
            @Override
            public void onError(Throwable cause) {
                cause.printStackTrace();
                latch.countDown();
            }
        }).start();
        latch.await();
    }

    @After
    public void after() {
        okHttp.close();
    }
}
