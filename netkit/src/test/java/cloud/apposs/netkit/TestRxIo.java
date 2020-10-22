package cloud.apposs.netkit;

import cloud.apposs.logger.Level;
import cloud.apposs.netkit.buffer.Allocator;
import cloud.apposs.netkit.buffer.ByteBuf;
import cloud.apposs.netkit.buffer.IoAllocator;
import cloud.apposs.netkit.buffer.SimpleAllocator;
import cloud.apposs.netkit.buffer.ZeroCopyAllocator;
import cloud.apposs.netkit.filterchain.dns.ARecord;
import cloud.apposs.netkit.filterchain.dns.CNameRecord;
import cloud.apposs.netkit.filterchain.dns.DnsMessage;
import cloud.apposs.netkit.filterchain.dns.DnsRecordType;
import cloud.apposs.netkit.filterchain.dns.Record;
import cloud.apposs.netkit.filterchain.http.client.HttpAnswer;
import cloud.apposs.netkit.filterchain.logging.LoggingFilter;
import cloud.apposs.netkit.rxio.IoAction;
import cloud.apposs.netkit.rxio.IoEmitter;
import cloud.apposs.netkit.rxio.IoFunction;
import cloud.apposs.netkit.rxio.IoSubscriber;
import cloud.apposs.netkit.rxio.OnSubscribeIo;
import cloud.apposs.netkit.rxio.RxIo;
import cloud.apposs.netkit.rxio.SafeIoSubscriber;
import cloud.apposs.netkit.rxio.actor.Actor;
import cloud.apposs.netkit.rxio.actor.ActorLock;
import cloud.apposs.netkit.rxio.io.dns.IoDns;
import cloud.apposs.netkit.rxio.io.http.HttpForm;
import cloud.apposs.netkit.rxio.io.http.IoHttp;
import cloud.apposs.netkit.rxio.io.http.IoHttpGet;
import cloud.apposs.netkit.rxio.io.http.IoHttpHead;
import cloud.apposs.netkit.rxio.io.http.IoHttpPost;
import cloud.apposs.netkit.rxio.io.http.enctype.FormDataEnctypt;
import cloud.apposs.netkit.rxio.io.http.enctype.FormEnctypt;
import cloud.apposs.netkit.rxio.io.mail.IoMail;
import cloud.apposs.netkit.rxio.io.mail.MailResult;
import cloud.apposs.netkit.rxio.io.whois.IoWhois;
import cloud.apposs.netkit.rxio.io.whois.WhoisInfo;
import cloud.apposs.util.Errno;
import cloud.apposs.util.FileUtil;
import cloud.apposs.util.Pair;
import cloud.apposs.util.StandardResult;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class TestRxIo {
    public static final String DIR = System.getProperty("user.dir") + "/res/";
    public static final String TXTFILE = DIR + "IP.iplist";
    public static final String LARGEFILE = DIR + "largefile.rar";
    public static final String LARGEFILE2 = "E:\\Download\\CentOS-7-x86_64-DVD-1511.iso";
    public static final boolean USE_ZERO_COPY = false;

    public static void main(String[] form) throws Exception {
        Allocator allocator = null;
        if (USE_ZERO_COPY) {
            allocator = new ZeroCopyAllocator(12, "D://Tmp/");
        } else {
            allocator = new SimpleAllocator();
        }
        IoAllocator.setAllocator(allocator);

        testRxIoHttpExecutor();
    }

    @Test
    public void testRxIoGetWhois() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final EventLoopGroup group = new EventLoopGroup();
        group.start();

        long start = System.currentTimeMillis();
        String domain = "faisco.com";
        InetSocketAddress proxyAddr = new InetSocketAddress("172.16.3.92", 14080);
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, proxyAddr);
        IoProcessor ioWhois = new IoWhois(domain, proxy);
        ioWhois.getFilterChain().add(new LoggingFilter(Level.INFO));
        // 通过whois查询域名的注册商
        RxIo<WhoisInfo> request = RxIo.create(new OnSubscribeIo<WhoisInfo>(group, ioWhois));
        request.subscribe(new IoSubscriber<WhoisInfo>() {
            @Override
            public void onNext(WhoisInfo info) {
                System.err.println("--------------faisco.com-------------");
                System.out.println(info.getInfo());
                latch.countDown();
                group.shutdown();
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
                latch.countDown();
                group.shutdown();
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("exception caught");
                e.printStackTrace();
            }
        }).start();
        latch.await();
        System.err.println("execute time:" + (System.currentTimeMillis() - start));
    }

    @Test
    public void testRxIoHttpRequest() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final EventLoopGroup group = new EventLoopGroup(4);
        group.start();

        final long start = System.currentTimeMillis();
        final String url = "http://127.0.0.1:8880/";
        IoHttp ioHttp = new IoHttp(url);
        ioHttp.getFilterChain().add(new LoggingFilter(Level.INFO));
        RxIo<HttpAnswer> request = RxIo.http(group, ioHttp);
        request.subscribe(new IoSubscriber<HttpAnswer>() {
            @Override
            public void onNext(HttpAnswer response) throws IOException {
                System.err.println("----------------- " + Thread.currentThread() + url + " ----------------");
                System.err.println("----------------- " + response.getStatus() + " ----------------");
        		System.out.println(response.getContent());
            }

            @Override
            public void onCompleted() {
                long exeTime = System.currentTimeMillis() - start;
                System.out.println("execute complete:" + exeTime);
                latch.countDown();
                group.shutdown();
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("exception caught");
                e.printStackTrace();
            }
        }).start();
        latch.await();
        long exeTime = System.currentTimeMillis() - start;
        System.err.println("----------------- " + Thread.currentThread() + " execute time:" + exeTime + " ----------------");
    }

    public static void testRxIoHttpGetParameterRequest() throws Exception {
        final EventLoopGroup group = new EventLoopGroup(4);
        group.start();

        long start = System.currentTimeMillis();
        final String url = "http://itil.aaa.cn/slow.jsp?_name=aa&test=bb";
        IoHttpGet ioHttp = new IoHttpGet(url);
        HttpForm form = new HttpForm();
        form.add("myname1", "mavalue1");
        form.add("myname2", "mavalue2");
        ioHttp.setForm(form);
        RxIo<HttpAnswer> request = RxIo.create(new OnSubscribeIo<HttpAnswer>(group, ioHttp));
        request.subscribe(new IoSubscriber<HttpAnswer>() {
            @Override
            public void onNext(HttpAnswer response) throws IOException {
                System.err.println("----------------- " + Thread.currentThread() + url + " ----------------");
                System.err.println("----------------- " + response.getStatus() + " ----------------");
                System.out.println(response.getContent());
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
                group.shutdown();
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("exception caught");
                e.printStackTrace();
            }
        }).start();
        long exeTime = System.currentTimeMillis() - start;
        System.err.println("----------------- " + Thread.currentThread() + " execute time:" + exeTime + " ----------------");
    }

    public static void testRxIoHttpPostParameterRequest() throws Exception {
        final EventLoopGroup group = new EventLoopGroup(4);
        group.start();

        long start = System.currentTimeMillis();
//		final String url = "http://itil.aaa.cn/slow.jsp?_name=aa&test=bb";
        final String url = "http://127.0.0.1:8080/SpringMvc/normalUpload?_name=wayken";
        IoHttpPost ioHttp = new IoHttpPost(url);
        HttpForm form = new HttpForm();
        form.add("_myarg1", "AA");
        form.add("_myarg2", "中文");
        ioHttp.setForm(form);
        RxIo<HttpAnswer> request = RxIo.create(new OnSubscribeIo<HttpAnswer>(group, ioHttp));
        request.subscribe(new IoSubscriber<HttpAnswer>() {
            @Override
            public void onNext(HttpAnswer response) throws IOException {
                System.err.println("----------------- " + Thread.currentThread() + url + " ----------------");
                System.err.println("----------------- " + response.getStatus() + " ----------------");
                System.out.println(response.getContent());
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
                group.shutdown();
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("exception caught");
                e.printStackTrace();
            }
        }).start();
        long exeTime = System.currentTimeMillis() - start;
        System.err.println("----------------- " + Thread.currentThread() + " execute time:" + exeTime + " ----------------");
    }

    public static void testRxIoHttpPostJsonRequest() throws Exception {
        final EventLoopGroup group = new EventLoopGroup(4);
        group.start();

        long start = System.currentTimeMillis();
        final String url = "http://127.0.0.1:8080/SpringMvc/jsonUpload?_name=wayken";
        IoHttpPost ioHttp = new IoHttpPost(url);
        HttpForm form = new HttpForm(FormEnctypt.FORM_ENCTYPE_JSON);
        form.add("_myarg1", "AA");
        form.add("_myarg2", "中文");
        ioHttp.setForm(form);
        RxIo<HttpAnswer> request = RxIo.create(new OnSubscribeIo<HttpAnswer>(group, ioHttp));
        request.subscribe(new IoSubscriber<HttpAnswer>() {
            @Override
            public void onNext(HttpAnswer response) throws IOException {
                System.err.println("----------------- " + Thread.currentThread() + url + " ----------------");
                System.err.println("----------------- " + response.getStatus() + " ----------------");
                System.out.println(response.getContent());
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
                group.shutdown();
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("exception caught");
                e.printStackTrace();
            }
        }).start();
        long exeTime = System.currentTimeMillis() - start;
        System.err.println("----------------- " + Thread.currentThread() + " execute time:" + exeTime + " ----------------");
    }

    public static void testRxIoHttpPostFormRequest() throws Exception {
        final EventLoopGroup group = new EventLoopGroup(4);
        group.start();

        long start = System.currentTimeMillis();
        final String url = "http://172.17.1.221:8080/SpringMvc/fileUpload2";
        IoHttpPost ioHttp = new IoHttpPost(url);
        HttpForm form = new HttpForm(FormEnctypt.FORM_ENCTYPE_FORMDATA);
        form.add("fname", "AA");
        form.add("lname", "中文");
        form.add("file", new File(LARGEFILE2));
        ioHttp.setRecvTimeout(60 * 60 * 1000);
        ioHttp.setSendTimeout(60 * 60 * 1000);
        ioHttp.setForm(form);
        RxIo<HttpAnswer> request = RxIo.create(new OnSubscribeIo<HttpAnswer>(group, ioHttp));
        request.subscribe(new IoSubscriber<HttpAnswer>() {
            @Override
            public void onNext(HttpAnswer response) throws IOException {
                System.err.println("----------------- " + Thread.currentThread() + url + " ----------------");
                System.err.println("----------------- " + response.getStatus() + " ----------------");
                System.out.println(response.getContent());
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
                group.shutdown();
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("exception caught");
                e.printStackTrace();
            }
        }).start();
        long exeTime = System.currentTimeMillis() - start;
        System.err.println("----------------- " + Thread.currentThread() + " execute time:" + exeTime + " ----------------");
    }

    public static void testRxIoHttpPostBufferRequest() throws Exception {
        final EventLoopGroup group = new EventLoopGroup(4);
        group.start();

        long start = System.currentTimeMillis();
        final String url = "http://127.0.0.1:8080/SpringMvc/fileUpload2";
        IoHttpPost ioHttp = new IoHttpPost(url);
        HttpForm form = new HttpForm(FormEnctypt.FORM_ENCTYPE_FORMDATA);
        form.add("fname", "AA");
        form.add("lname", "中文");
        File file = new File(TXTFILE);
        byte[] content = FileUtil.readByte(file);
        form.add("file", new FormDataEnctypt.FileBuffer(file.getName(), content));
        ioHttp.setForm(form);
        RxIo<HttpAnswer> request = RxIo.create(new OnSubscribeIo<HttpAnswer>(group, ioHttp));
        request.subscribe(new IoSubscriber<HttpAnswer>() {
            @Override
            public void onNext(HttpAnswer response) throws IOException {
                System.err.println("----------------- " + Thread.currentThread() + url + " ----------------");
                System.err.println("----------------- " + response.getStatus() + " ----------------");
                System.out.println(response.getContent());
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
                group.shutdown();
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("exception caught");
                e.printStackTrace();
            }
        }).start();
        long exeTime = System.currentTimeMillis() - start;
        System.err.println("----------------- " + Thread.currentThread() + " execute time:" + exeTime + " ----------------");
    }

    public static void testRxIoHttpHeadRequest() throws Exception {
        final EventLoopGroup group = new EventLoopGroup(4);
        group.start();

        long start = System.currentTimeMillis();
        final String url = "http://www.baidu.com";
        RxIo<HttpAnswer> request = RxIo.create(new OnSubscribeIo<HttpAnswer>(group, new IoHttpHead(url)));
        request.subscribe(new IoSubscriber<HttpAnswer>() {
            @Override
            public void onNext(HttpAnswer response) throws IOException {
                System.err.println("----------------- " + Thread.currentThread() + url + " ----------------");
                System.err.println("----------------- " + response.getStatus() + " ----------------");
                System.out.println("----------------- " + response.getHeaders() + " ----------------");
                System.out.println(response.getContent().length());
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
                group.shutdown();
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("exception caught");
                e.printStackTrace();
            }
        }).start();
        long exeTime = System.currentTimeMillis() - start;
        System.err.println("----------------- " + Thread.currentThread() + " execute time:" + exeTime + " ----------------");
    }

    @Test
    public void testRxIoHttpsRequest() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final EventLoopGroup group = new EventLoopGroup(4);
        group.start();

        long start = System.currentTimeMillis();
        final String url = "https://www.taobao.com";
        RxIo<HttpAnswer> request = RxIo.create(new OnSubscribeIo<HttpAnswer>(group, new IoHttp(url)));
        request.subscribe(new IoSubscriber<HttpAnswer>() {
            @Override
            public void onNext(HttpAnswer response) throws IOException {
                System.err.println("----------------- " + Thread.currentThread() + url + " ----------------");
                System.out.println(response.getContent());
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
                latch.countDown();
                group.shutdown();
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("exception caught");
                e.printStackTrace();
            }
        }).start();
        latch.await();
        long exeTime = System.currentTimeMillis() - start;
        System.err.println("----------------- " + Thread.currentThread() + " execute time:" + exeTime + " ----------------");
    }

    @Test
    public void testRxIoSimpleExecutor() throws Exception {
        final Executor executor = getNamedExecutor("ExecutorOnThread");

        CountDownLatch latch = new CountDownLatch(1);
        RxIo.emitter(new IoEmitter<String>() {
            @Override
            public String call() throws Exception {
                System.err.println("----------------- 1." + Thread.currentThread() + " ----------------");
                return "This is From Executor-" + Thread.currentThread().getName();
            }
        }).subscribeOn(executor)
        .subscribe(new IoSubscriber<String>() {
            @Override
            public void onNext(String value) throws Exception {
                System.err.println("----------------- 2." + Thread.currentThread() + " ----------------");
                System.out.println(value);
                Thread.sleep(2000);
                System.out.println("sleep in");
            }

            @Override
            public void onCompleted() {
                System.err.println("----------------- 3." + Thread.currentThread() + " ----------------");
                System.out.println("complete");
                latch.countDown();
                ((ExecutorService) executor).shutdown();
            }

            @Override
            public void onError(Throwable cause) {
            }
        }).start();
        latch.await();
    }

    public static void testRxIoHttpExecutor() throws Exception {
        final EventLoopGroup group = new EventLoopGroup(4);
        group.start();
        final Executor executor = getNamedExecutor("ExecutorOnThread");

        long start = System.currentTimeMillis();
        final String url1 = "http://www.baidu.com";
        final String url2 = "http://www.fkw.com";
        RxIo.http(group, url1).executOn(executor).map(new IoFunction<HttpAnswer, String>() {
            @Override
            public String call(HttpAnswer resp) throws IOException {
                System.err.println("----------------- Map1 " + Thread.currentThread() + url1 + " ----------------");
                System.out.println(resp.getHeaders().toString());
                return resp.getContent();
            }
        }).request(new IoFunction<String, RxIo<HttpAnswer>>() {
            @Override
            public RxIo<HttpAnswer> call(String t) throws Exception {
                System.err.println("----------------- Request " + Thread.currentThread() + url1 + " ----------------");
                return RxIo.http(group, url2).executOn(executor);
            }
        }).map(new IoFunction<HttpAnswer, String>() {
            @Override
            public String call(HttpAnswer resp) throws Exception {
                System.err.println("----------------- Map2 " + Thread.currentThread() + url2 + " ----------------");
                return resp.getHeaders().toString();
            }
        }).subscribe(new IoSubscriber<String>() {
            @Override
            public void onNext(String response) {
                System.err.println("----------------- Subscribe " + Thread.currentThread() + url2 + " ----------------");
                System.out.println(response);
                group.shutdown();
                ((ExecutorService) executor).shutdown();
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
                group.shutdown();
                ((ExecutorService) executor).shutdown();
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("exception caught");
                e.printStackTrace();
                group.shutdown();
                ((ExecutorService) executor).shutdown();
            }
        }).start();
        long exeTime = System.currentTimeMillis() - start;
        System.err.println("----------------- " + Thread.currentThread() + " execute time:" + exeTime + " ----------------");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRxIoHttpMerge() throws Exception {
        final EventLoopGroup group = new EventLoopGroup(1);
        group.start();
        CountDownLatch latch = new CountDownLatch(1);

        long start = System.currentTimeMillis();
        final String url1 = "https://www.baidu.com";
        final String url2 = "http://www.faisco.com";
        final String url3 = "https://www.taobao.com";
        final String url4 = "https://www.jd.com";
        final String url5 = "http://www.wayken.icoc.cc";
        final String url6 = "http://jz.fkw.com";
        RxIo<HttpAnswer> http1 = RxIo.http(group, url1);
        RxIo<HttpAnswer> http2 = RxIo.http(group, url2);
        RxIo<HttpAnswer> http3 = RxIo.http(group, url3);
        RxIo<HttpAnswer> http4 = RxIo.http(group, url4);
        RxIo<HttpAnswer> http5 = RxIo.http(group, url5);
        RxIo<HttpAnswer> http6 = RxIo.http(group, url6);

        RxIo<HttpAnswer> batch = RxIo.merge(http1, http2, http3, http4, http5, http6);
        batch.subscribe(new IoSubscriber<HttpAnswer>() {
            @Override
            public void onNext(HttpAnswer response) {
                System.err.println("----------------- " + Thread.currentThread() + response.getUrl() + " ----------------");
                System.out.println(response.getHeaders());
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("complete");
                latch.countDown();
                group.shutdown();
            }
        }).start();
        latch.await();
        long exeTime = System.currentTimeMillis() - start;
        System.err.println("----------------- " + Thread.currentThread() + " execute time:" + exeTime + " ----------------");
    }

    /**
     * 模拟订单下单三个TRY请求，当三个请求都通过之后才算事务TRY成功
     */
    @Test
    public void testRxIoHttpFlat() throws Exception {
        final EventLoopGroup group = new EventLoopGroup(1);
        group.start();
        CountDownLatch latch = new CountDownLatch(1);

        final String url1 = "https://www.baidu.com";
        final String url2 = "https://www.faisco.com";
        final String url3 = "https://www.fkw.com";
        RxIo<HttpAnswer> http1 = RxIo.http(group, url1);
        RxIo<HttpAnswer> http2 = RxIo.http(group, url2);
        RxIo<HttpAnswer> http3 = RxIo.http(group, url3);
        List<RxIo<HttpAnswer>> httpList = new LinkedList<RxIo<HttpAnswer>>();
        httpList.add(http1);
        httpList.add(http2);
        httpList.add(http3);
        RxIo.flat(httpList, new IoFunction<HttpAnswer, Boolean>() {
            @Override
            public Boolean call(HttpAnswer httpAnswer) throws Exception {
//                if (httpAnswer.getUrl().contains("faisco")) {
//                    return false;
//                }
                System.out.println(httpAnswer.getUrl());
                return true;
            }
        }).subscribe(new IoSubscriber<Boolean>() {
            @Override
            public void onNext(Boolean value) throws Exception {
                System.out.println("all execute result: " + value);
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }

            @Override
            public void onError(Throwable cause) {
                System.out.println(3);
            }
        }).start();
        latch.await();
    }

    @SuppressWarnings("unchecked")
    public static void testRxIoHttpSlowBatch() throws Exception {
        final EventLoopGroup group = new EventLoopGroup(1);
        group.start();

        final long start = System.currentTimeMillis();
        List<RxIo<HttpAnswer>> requeses = new ArrayList<RxIo<HttpAnswer>>();
        for (int i = 0; i < 100; i++) {
            requeses.add(RxIo.http(group, "http://itil.aaa.cn/slow.jsp"));
        }

        RxIo<HttpAnswer>[] seqArray = new RxIo[requeses.size()];
        requeses.toArray(seqArray);
        RxIo<HttpAnswer> batch = RxIo.merge(seqArray);
        batch.subscribe(new IoSubscriber<HttpAnswer>() {
            @Override
            public void onNext(HttpAnswer response) {
                System.err.println("----------------- " + Thread.currentThread() + response.getUrl() + " ----------------");
                System.out.println(response.getHeaders());
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onCompleted() {
                long exeTime = System.currentTimeMillis() - start;
                System.err.println("----------------- Complete Rxio " + Thread.currentThread() + " execute time:" + exeTime + " ----------------");
                group.shutdown();
            }
        }).start();
        long exeTime = System.currentTimeMillis() - start;
        System.err.println("----------------- " + Thread.currentThread() + " execute time:" + exeTime + " ----------------");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRxIoWhoisBatch() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final EventLoopGroup group = new EventLoopGroup(1);
        group.start();

        final String domain1 = "baidu.com";
        final String domain2 = "faisco.biz";
        final String domain3 = "abce1111.org";
        final String domain4 = "jd.com";
        RxIo<WhoisInfo> whois1 = RxIo.create(group, new IoWhois(domain1));
        RxIo<WhoisInfo> whois2 = RxIo.create(group, new IoWhois(domain2));
        RxIo<WhoisInfo> whois3 = RxIo.create(group, new IoWhois(domain3));
        RxIo<WhoisInfo> whois4 = RxIo.create(group, new IoWhois(domain4));

        RxIo<WhoisInfo> batch = RxIo.merge(whois1, whois2, whois3, whois4);
        batch.subscribe(new IoSubscriber<WhoisInfo>() {
            @Override
            public void onNext(WhoisInfo info) {
                System.err.println(info.getDomain() + ":" + info.isReg());
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onCompleted() {
                group.shutdown();
                latch.countDown();
            }
        }).start();
        latch.await();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRxIoWhoisMergeList() throws Exception {
        final EventLoopGroup group = new EventLoopGroup(1);
        group.start();

        CountDownLatch latch = new CountDownLatch(1);
        final String domain1 = "baidu.com";
        final String domain2 = "faisco.biz";
        final String domain3 = "abce1111.org";
        final String domain4 = "jd.com";
        RxIo<WhoisInfo> whois1 = RxIo.create(group, new IoWhois(domain1));
        RxIo<WhoisInfo> whois2 = RxIo.create(group, new IoWhois(domain2));
        RxIo<WhoisInfo> whois3 = RxIo.create(group, new IoWhois(domain3));
        RxIo<WhoisInfo> whois4 = RxIo.create(group, new IoWhois(domain4));

        RxIo<List<WhoisInfo>> batch = RxIo.mergeList(whois1, whois2, whois3, whois4);
        batch.subscribe(new IoAction<List<WhoisInfo>>() {
            @Override
            public void call(List<WhoisInfo> list) throws Exception {
                for (WhoisInfo info : list) {
                    System.out.println(info.getDomain() + ":" + info.isReg());
                }
                latch.countDown();
                group.shutdown();
            }
        }).start();
        latch.await();
    }

    @Test
    public void testRxIoHttpProxy() throws Exception {
        final EventLoopGroup group = new EventLoopGroup(4);
        group.start();

        CountDownLatch latch = new CountDownLatch(1);
        final String url = "http://myip.ipip.net";
        RxIo.create(new OnSubscribeIo<HttpAnswer>(group, new IoHttp(url)))
                .request(new IoFunction<HttpAnswer, RxIo<HttpAnswer>>() {
                    @Override
                    public RxIo<HttpAnswer> call(HttpAnswer response) throws Exception {
                        System.err.println("----------------- " + Thread.currentThread() + url + " ----------------");
                        System.out.println(response.getContent());
                        InetSocketAddress proxyAddr = new InetSocketAddress("172.16.3.84", 14080);
                        Proxy proxy = new Proxy(Proxy.Type.SOCKS, proxyAddr);
                        return RxIo.create(group, new IoHttp(url, proxy));
                    }
                })
                .subscribe(new IoSubscriber<HttpAnswer>() {
                    @Override
                    public void onNext(HttpAnswer response) throws IOException {
                        System.err.println("----------------- " + Thread.currentThread() + url + " ----------------");
                        System.out.println(response.getContent());
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("execute complete");
                        latch.countDown();
                        group.shutdown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.err.println("exception caught");
                        e.printStackTrace();
                    }
                }).start();
        latch.await();
    }

    @Test
    public void testRxIoHttpProxy2() throws Exception {
        final EventLoopGroup group = new EventLoopGroup(4);
        group.start();

        CountDownLatch latch = new CountDownLatch(1);
        final String url = "http://myip.ipip.net";
        InetSocketAddress proxyAddr = new InetSocketAddress("172.16.3.84", 14080);
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, proxyAddr);
        RxIo.create(new OnSubscribeIo<HttpAnswer>(group, new IoHttpPost(url, proxy)))
                .subscribe(new IoSubscriber<HttpAnswer>() {
                    @Override
                    public void onNext(HttpAnswer response) throws IOException {
                        System.out.println("----------------- " + Thread.currentThread() + url + " ----------------");
                        System.out.println(response.getContent());
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("execute complete");
                        latch.countDown();
                        group.shutdown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.err.println("exception caught");
                        e.printStackTrace();
                    }
                }).start();
        latch.await();
    }

    public static void testRxIoSendMail() throws Exception {
        final EventLoopGroup group = new EventLoopGroup(4);
        group.start();

        long start = System.currentTimeMillis();
        IoMail mail = new IoMail("127.0.0.1", 465);
        IoBuffer buffer = new ByteBuf(12, false);
        buffer.put("HelloMail");
        mail.setData(buffer);
        RxIo<MailResult> request = RxIo.create(new OnSubscribeIo<MailResult>(group, mail));
        request.subscribe(new IoSubscriber<MailResult>() {
            @Override
            public void onNext(MailResult response) {
                System.err.println("----------------- " + Thread.currentThread() + " ----------------");
                System.out.println(response.isSuccess() + ":" + response.getResponse());
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
                group.shutdown();
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("exception caught");
                e.printStackTrace();
            }
        }).start();
        long exeTime = System.currentTimeMillis() - start;
        System.err.println("----------------- " + Thread.currentThread() + " execute time:" + exeTime + " ----------------");
    }

    public static void testRxIoEmitter() {
        RxIo.create(new RxIo.OnSubscribe<String>() {
            @Override
            public void call(SafeIoSubscriber<? super String> t) throws Exception {
                t.onNext("This is a Emitter String");
                t.onCompleted();
            }
        }).map(new IoFunction<String, Integer>() {
            @Override
            public Integer call(String t) {
                System.out.println("recv str " + t);
                return t.hashCode();
            }
        }).match(new IoFunction<Integer, Errno>() {
            @Override
            public Errno call(Integer t) throws Exception {
                return Errno.OK;
            }
        }).subscribe(new IoSubscriber<Integer>() {
            @Override
            public void onNext(Integer value) {
                System.out.println(value);
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Test
    public void testRxIoFrom() throws Exception {
        RxIo.from(1, 2, 3)
            .map(new IoFunction<Integer, String>() {
                @Override
                public String call(Integer t) {
                    return "AA:" + t;
                }
            }).subscribe(new IoSubscriber<String>() {
            @Override
            public void onNext(String value) {
                System.out.println(value);
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Test
    public void testRxIoIterator() throws Exception {
        List<Pair<Integer, String>> dataList = new LinkedList<Pair<Integer, String>>();
        dataList.add(new Pair<Integer, String>(1, "one"));
        dataList.add(new Pair<Integer, String>(2, "two"));
        dataList.add(new Pair<Integer, String>(3, "three"));
        RxIo.from(dataList)
            .map(new IoFunction<Pair<Integer,String>, String>() {
                @Override
                public String call(Pair<Integer, String> t) throws Exception {
                    return "AA:" + t.first() + "-" + t.second();
                }
            }).subscribe(new IoSubscriber<String>() {
            @Override
            public void onNext(String value) {
                System.out.println(value);
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Test
    public void testRxIoRetry() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        RxIo.create(new RxIo.OnSubscribe<Integer>() {
            @Override
            public void call(SafeIoSubscriber<? super Integer> t) throws Exception {
                t.onNext(1001 / 0);
            }
        }).retry(new IoFunction<Throwable, RxIo<Integer>>() {
            @Override
            public RxIo<Integer> call(Throwable throwable) throws Exception {
                return RxIo.from(1, 12, 14).sleep(scheduler, 1200);
            }
        }).subscribe(new IoSubscriber<Integer>() {
            @Override
            public void onNext(Integer value) {
                System.out.println(value);
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("execute error");
                e.printStackTrace();
            }
        }).start();
        latch.await();
    }

    /**
     * 模拟当第一个HTTP超时时再重试执行HTTP请求并且先睡眠一段时间
     */
    @Test
    public void testRxIoHttpRetry() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        EventLoopGroup group = new EventLoopGroup(4);
        group.start();

        final String url1 = "http://localhost:8880/product/config";
        final String url2 = "http://www.baidu.com";
        RxIo.http(group, url1).retry(new IoFunction<Throwable, RxIo<HttpAnswer>>() {
            @Override
            public RxIo<HttpAnswer> call(Throwable throwable) throws Exception {
                System.out.println("request fail");
                throwable.printStackTrace();
                return RxIo.http(group, url1).sleep(scheduler, 5000);
            }
        }).subscribe(new IoSubscriber<HttpAnswer>() {
            @Override
            public void onNext(HttpAnswer value) throws IOException {
                System.out.println(value.getContent());
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("execute error");
                e.printStackTrace();
            }
        }).start();
        latch.await();
    }

    @Test
    public void testRxIoSleep() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        long startTime = System.currentTimeMillis();
        RxIo.create(new RxIo.OnSubscribe<String>() {
            @Override
            public void call(SafeIoSubscriber<? super String> t) throws Exception {
                t.onNext("This is a Emitter String");
                t.onCompleted();
            }
        })
        .sleep(scheduler, 200)
        .map(new IoFunction<String, String>() {
            @Override
            public String call(String s) throws Exception {
                System.out.println("map in " + s + ", Cost Time:" + (System.currentTimeMillis() - startTime));
                return s;
            }
        })
        .sleep(scheduler, 1200)
        .subscribe(new IoAction<String>() {
            @Override
            public void call(String s) throws Exception {
                System.out.println(s + ", Cost Time:" + (System.currentTimeMillis() - startTime));
                latch.countDown();
            }
        }).start();
        latch.await();
    }

    @Test
    public void testRxIoRepeat() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        long startTime = System.currentTimeMillis();
        RxIo.from("AA", "BB")
        .repeat(2)
        .map(new IoFunction<String, String>() {
            @Override
            public String call(String s) throws Exception {
                System.out.println("map in " + s + ", Cost Time:" + (System.currentTimeMillis() - startTime));
                return s;
            }
        })
        .subscribe(new IoSubscriber<String>() {
            @Override
            public void onNext(String value) {
                System.out.println(value);
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }
        }).start();
        latch.await();
    }

    @Test
    public void testRxIoLock() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Actor actor = new Actor();
        long startTime = System.currentTimeMillis();
        Object aid = 854;
        ActorLock lock = Actor.createLock(aid);
        RxIo.lock(lock, actor, new RxIo.OnSubscribe<String>() {
            @Override
            public void call(SafeIoSubscriber<? super String> t) throws Exception {
                System.out.println("Foo in 1");
                t.onNext("This is a Emitter Lock String");
            }
        }).map(new IoFunction<String, String>() {
            @Override
            public String call(String s) throws Exception {
                System.out.println("Foo in 2");
                return s + ": On Map";
            }
        }).subscribe(new IoAction<String>() {
            @Override
            public void call(String s) throws Exception {
                System.out.println(s + ", Cost Time:" + (System.currentTimeMillis() - startTime));
                latch.countDown();
            }
        }).start();
        latch.await();
    }

    @Test
    public void testThreadRxIoLock() throws Exception {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        Actor actor = new Actor(2);

        // 第一个用户
        ActorLock lockKey = Actor.createLock(854);
        List<Thread> orderTheadList = new ArrayList<Thread>();
        for (int i = 0; i < threadCount; i++) {
            int index = i;
            orderTheadList.add(new Thread(new Runnable() {
                @Override
                public void run() {
                    Random random = new Random();
                    int time = random.nextInt(2000);
                    // 模拟EventLoop多线程下RxIo响应式编程
                    RxIo.lock(lockKey, actor, new RxIo.OnSubscribe<String>() {
                        @Override
                        public void call(SafeIoSubscriber<? super String> t) throws Exception {
                            // 模拟即使有请求进来，但耗时比较久，其他同lockkey的请求也是要阻塞保证串行执行
                            try {
                                Thread.sleep(time);
                            } catch (InterruptedException e) {
                            }
                            // 模拟HTTP请求结束会调用此方法触发数据的发送
                            t.onNext("This is a Emitter Lock String");
                        }
                    }).subscribe(new IoAction<String>() {
                        @Override
                        public void call(String s) throws Exception {
                            // 输出时会按顺序输出0:MsgXX,1:MsgXX，即使同lockkey队列方法中执行的耗时不同，依然也是顺序执行
                            System.out.println(index + ";Msg=" + s + ";LockKey=" + lockKey + ";SleepTime=" + time);
                            latch.countDown();
                        }
                    }).start();
                }
            }));
        }
        for (int i = 0; i < orderTheadList.size(); i++) {
            orderTheadList.get(i).start();
            Thread.sleep(10);
        }
        latch.await();
    }

    public static void testRxIoSubscribeOn() {
        long start = System.currentTimeMillis();

        final Executor executor = getNamedExecutor("ExecutorOnThread");
        RxIo.create(new RxIo.OnSubscribe<String>() {
            @Override
            public void call(SafeIoSubscriber<? super String> t) throws Exception {
                System.err.println("----------------- Emitter " + Thread.currentThread() + " ----------------");
                t.onNext("This is a Emitter String");
                t.onCompleted();
            }
        })
        .map(new IoFunction<String, String>() {
            @Override
            public String call(String t) {
                System.err.println("----------------- Map " + Thread.currentThread() + " ----------------");
                return "AA:" + t;
            }
        })
        .subscribeOn(executor)
        .subscribe(new IoSubscriber<String>() {
            @Override
            public void onNext(String value) {
                System.err.println("----------------- OnSubcriber " + Thread.currentThread() + " ----------------");
                System.out.println("subscribe recv from:" + value);
                ((ExecutorService) executor).shutdown();
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
                ((ExecutorService) executor).shutdown();
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                ((ExecutorService) executor).shutdown();
            }
        }).start();

        long exeTime = System.currentTimeMillis() - start;
        System.err.println("----------------- " + Thread.currentThread() + " execute time:" + exeTime + " ----------------");
    }

    @Test
    public void testRxIsDomainReg() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final EventLoopGroup group = new EventLoopGroup(4);
        group.start();

        final String domain = "fkw.com";
        // 先通过whois查询域名是否注册，再输出注册信息
        RxIo.create(new OnSubscribeIo<WhoisInfo>(group, new IoWhois(domain)))
        .map(new IoFunction<WhoisInfo, String>() {
            @Override
            public String call(WhoisInfo info) {
                if (info.isReg()) {
                    return "domain " + info.getDomain() + " is reg";
                }
                return "domain is " + info.getDomain() + " not reg";
            }
        })
        .subscribe(new IoSubscriber<String>() {
            @Override
            public void onNext(String value) {
                System.out.println("subscriber:" + value);
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
                latch.countDown();
                group.shutdown();
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }
        }).start();
        latch.await();
    }

    @Test
    public void testRxIoGetDnsCname() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final EventLoopGroup group = new EventLoopGroup();
        group.start();

        String domain = "www.baidu.com";
        IoDns ioDns = new IoDns(domain, DnsRecordType.CNAME);
        ioDns.setBufferDirect(false);
        ioDns.getFilterChain().add(new LoggingFilter(Level.INFO));
        RxIo<DnsMessage> request = RxIo.create(new OnSubscribeIo<DnsMessage>(group, ioDns));
        request.subscribe(new IoSubscriber<DnsMessage>() {
            @Override
            public void onNext(DnsMessage value) {
                List<String> cnameList = new LinkedList<String>();
                for (Record answer : value.getRecords(DnsMessage.Section.ANSWER)) {
                    if (answer instanceof CNameRecord) {
                        CNameRecord rec = (CNameRecord) answer;
                        cnameList.add(rec.getCname().toString());
                    }
                }
                System.out.println(cnameList);
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
                latch.countDown();
                group.shutdown();
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("exception caught");
                e.printStackTrace();
            }
        }).start();
        latch.await();
    }

    @Test
    public void testRxIoGetDnsAType() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final EventLoopGroup group = new EventLoopGroup();
        group.start();

        String domain = "www.baidu.com";
        IoDns ioDns = new IoDns(domain, DnsRecordType.A);
        ioDns.setBufferDirect(false);
        ioDns.getFilterChain().add(new LoggingFilter(Level.INFO));
        RxIo<DnsMessage> request = RxIo.create(new OnSubscribeIo<DnsMessage>(group, ioDns));
        request.subscribe(new IoSubscriber<DnsMessage>() {
            @Override
            public void onNext(DnsMessage value) {
                List<String> ipList = new LinkedList<String>();
                for (Record answer : value.getRecords(DnsMessage.Section.ANSWER)) {
                    if (answer instanceof ARecord) {
                        ARecord rec = (ARecord) answer;
                        ipList.add(rec.getAddrInString());
                    }
                }
                System.out.println(ipList);
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
                latch.countDown();
                group.shutdown();
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("exception caught");
                e.printStackTrace();
            }
        }).start();
        latch.await();
    }

    @Test
    public void testRxIoGetDnsHttp() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final EventLoopGroup group = new EventLoopGroup(4);
        group.start();
        final long start = System.currentTimeMillis();

        String domain = "www.baidu.com";
        final String url = "http://" + domain;
        IoDns ioDns = new IoDns(domain, DnsRecordType.A);
        ioDns.setBufferDirect(false);
        ioDns.getFilterChain().add(new LoggingFilter(Level.INFO));
        RxIo.create(new OnSubscribeIo<DnsMessage>(group, ioDns))
        // 先通过DNS解析IP，再通过IP连接HTTP服务器获取响应数据，最后进行业务处理
        .map(new IoFunction<DnsMessage, String>() {
            @Override
            public String call(DnsMessage value) throws IOException {
                for (Record answer : value.getRecords(DnsMessage.Section.ANSWER)) {
                    if (answer instanceof ARecord) {
                        ARecord rec = (ARecord) answer;
                        return rec.getAddrInString();
                    }
                }
                throw new IOException("dns resolver fail");
            }
        }).request(new IoFunction<String, RxIo<HttpAnswer>>() {
            @Override
            public RxIo<HttpAnswer> call(String domainIp) throws Exception {
                String host = url.split("/")[2];
                String newUrl = url.replace(host, domainIp);
                IoHttp ioHttp = new IoHttp(newUrl);
                ioHttp.addHeader("Host", host);
                return RxIo.http(group, ioHttp);
            }
        }).subscribe(new IoSubscriber<HttpAnswer>() {
            @Override
            public void onNext(HttpAnswer response) throws IOException {
                System.err.println("----------------- " + Thread.currentThread() + url + " ----------------");
                System.err.println("----------------- " + response.getStatus() + " ----------------");
                System.out.println(response.getContent());
            }

            @Override
            public void onCompleted() {
                long exeTime = System.currentTimeMillis() - start;
                System.out.println("execute complete:" + exeTime);
                latch.countDown();
                group.shutdown();
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("exception caught");
                e.printStackTrace();
            }
        }).start();
        latch.await();
    }

    @Test
    public void testRxIoFullCall() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final EventLoopGroup group = new EventLoopGroup(4);
        group.start();
        final Executor executor = getNamedExecutor("ExecutorOnThread");

        // 先请求url1，再请求url2，最后做逻辑处理
        // 应用场景：传单通过微信api获取用户tokenid，再根据tokenid获取用户头像等
        long start = System.currentTimeMillis();
        final String url1 = "http://www.baidu.com";
        final String url2 = "http://www.fkw.com";
        // 先http异步请求，再配置线程池对请求回来的网络数据进行处理
        RxIo.http(group, url1).executOn(executor).map(new IoFunction<HttpAnswer, String>() {
            // 进行map数据转换，一般是作业务逻辑处理
            @Override
            public String call(HttpAnswer resp) throws IOException {
                System.err.println("----------------- Map1 " + Thread.currentThread() + url1 + " ----------------");
                System.out.println(resp.getHeaders().toString());
                return resp.getContent();
            }
        }).request(new IoFunction<String, RxIo<HttpAnswer>>() {
            // 继续异步请求外网
            @Override
            public RxIo<HttpAnswer> call(String t) throws Exception {
                System.err.println("----------------- Request " + Thread.currentThread() + url1 + " ----------------");
                // 异步外网请求会在EventLoop中执行，所以接下来的业务逻辑处理还需要在线程池处理的话依然要开启executOn
                return RxIo.http(group, url2).executOn(executor);
            }
        }).map(new IoFunction<HttpAnswer, String>() {
            @Override
            public String call(HttpAnswer resp) throws Exception {
                System.err.println("----------------- Map2 " + Thread.currentThread() + url2 + " ----------------");
                return resp.getHeaders().toString();
            }
        }).match(new IoFunction<String, Errno>() {
            // 判断请求的数据是否正常，不正常返回Errno.Error，onError接口会做最终的异常处理
            @Override
            public Errno call(String t) throws Exception {
                if (t == null) {
                    return Errno.ERROR;
                }
                return Errno.OK;
            }
        }).subscribe(new IoSubscriber<String>() {
            // 真正的业务逻辑处理，包括数据输出或者降级处理
            @Override
            public void onNext(String response) {
                System.err.println("----------------- Subscribe " + Thread.currentThread() + url2 + " ----------------");
                System.out.println(response);
                group.shutdown();
                ((ExecutorService) executor).shutdown();
            }

            @Override
            public void onCompleted() {
                System.out.println("execute complete");
                latch.countDown();
                group.shutdown();
                ((ExecutorService) executor).shutdown();
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("exception caught");
                e.printStackTrace();
                group.shutdown();
                ((ExecutorService) executor).shutdown();
            }
        }).start();
        latch.await();
        long exeTime = System.currentTimeMillis() - start;
        System.err.println("----------------- " + Thread.currentThread() + " execute time:" + exeTime + " ----------------");
    }

    @Test
    public void testRxIoStandardResult() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        RxIo.create(new RxIo.OnSubscribe<String>() {
            @Override
            public void call(SafeIoSubscriber<? super String> t) throws Exception {
                t.onNext("This is a Emitter String");
                t.onCompleted();
            }
        }).handle(new IoFunction<String, StandardResult>() {
            @Override
            public StandardResult call(String s) throws Exception {
                return StandardResult.error(Errno.ERROR);
            }
        }).subscribe(new IoSubscriber<StandardResult>() {
            @Override
            public void onNext(StandardResult value) throws Exception {
                System.out.println(value.toJson());
            }

            @Override
            public void onCompleted() {
                System.out.println("----------------- Request Complete -----------------");
                latch.countDown();
            }

            @Override
            public void onError(Throwable cause) {
                System.err.println("Exception Caught");
                cause.printStackTrace();
            }
        }).start();
        latch.await();
    }

    /**
     * 用指定的名称新建一个线程
     */
    public static Executor getNamedExecutor(final String name) {
        return Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable, name);
            }
        });
    }
}
