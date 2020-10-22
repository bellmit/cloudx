package cloud.apposs.netkit;

import cloud.apposs.netkit.buffer.Allocator;
import cloud.apposs.netkit.buffer.IoAllocator;
import cloud.apposs.netkit.buffer.SimpleAllocator;
import cloud.apposs.netkit.buffer.ZeroCopyAllocator;
import cloud.apposs.netkit.filterchain.http.client.HttpAnswer;
import cloud.apposs.netkit.rxio.IoSubscriber;
import cloud.apposs.netkit.rxio.OnSubscribeIo;
import cloud.apposs.netkit.rxio.RxIo;
import cloud.apposs.netkit.rxio.io.http.IoHttp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestCdnReq {
    public static final String TMP_FILE = "C://myimg.jpg";
    public static final String TMP_FILE2 = "C://myimg.txt";
    public static final String DIR = System.getProperty("user.dir") + "/res/";
    public static final String IP_LIST = DIR + "IP.iplist";
    public static final boolean USE_ZERO_COPY = false;

    public static void main(String[] args) throws Exception {
        Allocator allocator = null;
        if (USE_ZERO_COPY) {
            allocator = new ZeroCopyAllocator(12, "D://Tmp/");
        } else {
            allocator = new SimpleAllocator();
        }
        IoAllocator.setAllocator(allocator);

        testRxIoHttpImgRequest();
    }

    public static void testRxIoHttpImgRequest() throws Exception {
        final EventLoopGroup group = new EventLoopGroup(4);
        group.start();

        long start = System.currentTimeMillis();
        final String url = "https://9556608.s21i.faiusr.com/4/ABUIABAEGAAg3MnS2AUo0OHOhwIwiAY4wwI.png";
//		final String url = "http://www.baidu.com";
        RxIo<HttpAnswer> request = RxIo.create(new OnSubscribeIo<HttpAnswer>(group, new IoHttp(url)));
        request.subscribe(new IoSubscriber<HttpAnswer>() {
            @Override
            public void onNext(HttpAnswer response) throws IOException {
                System.err.println("----------------- " + Thread.currentThread() + url + " ----------------");
                System.err.println("----------------- " + response.getStatus() + " ----------------");
                Map<String, String> headers = response.getHeaders();
                for (String key : headers.keySet()) {
                    System.out.println(key + ":" + headers.get(key));
                }
                saveToFile(response.getContent());
                saveToFile(response.getBytes());
                group.shutdown();
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
                group.shutdown();
            }
        }).start();
        long exeTime = System.currentTimeMillis() - start;
        System.err.println("----------------- " + Thread.currentThread() + " execute time:" + exeTime + " ----------------");
    }

    @SuppressWarnings("unchecked")
    public static void testRxIoHttpImgCdn() throws Exception {
        List<String> ips = new ArrayList<String>(300);
        int line = 1;
        File ipListFile = new File(IP_LIST);
        BufferedReader br = new BufferedReader(new FileReader(ipListFile));
        String str;
        while ((str = br.readLine()) != null) {
            line++;
            String ip = str.trim();
            if (ip != null && !ip.trim().isEmpty()) {
                ips.add(ip);
            }
        }

        final EventLoopGroup group = new EventLoopGroup(4);
        group.start();

        final long start = System.currentTimeMillis();
        final String url = "https://757722.s21i.faiusr.com/4/ABUIABAEGAAg6uyU3wUo1r3bswcwxAE4xAE.png";
        String host = url.split("/")[2];
        List<RxIo<HttpAnswer>> ioHttpList = new ArrayList<RxIo<HttpAnswer>>();
        for (final String ip : ips) {
            String newUrl = url.replace(host, ip);
            IoHttp ioHttp = new IoHttp(newUrl);
            ioHttp.setRecvTimeout(3000);
            ioHttp.setConnectTimeout(3000);
            ioHttp.addHeader("Host", host);
            ioHttpList.add(RxIo.create(new OnSubscribeIo<HttpAnswer>(group, ioHttp)));
        }
        RxIo<HttpAnswer>[] ioHttpArray = new RxIo[ioHttpList.size()];
        ioHttpList.toArray(ioHttpArray);
        RxIo<HttpAnswer> batch = RxIo.merge(ioHttpArray);
        batch.subscribe(new IoSubscriber<HttpAnswer>() {
            @Override
            public void onNext(HttpAnswer response) {
                System.err.println("----------------- " + Thread.currentThread() + response.getUrl() + " ----------------");
                if (response.getStatus() == 200) {
                    System.err.println("----------------- found ----------------");
                }
//        		System.out.println(response.getHeaders());
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("exceptio caught");
                e.printStackTrace();
            }

            @Override
            public void onCompleted() {
                group.shutdown();
                long exeTime = System.currentTimeMillis() - start;
                System.err.println("----------------- " + Thread.currentThread() + " total execute time:" + exeTime + " ----------------");
            }
        }).start();
        long exeTime = System.currentTimeMillis() - start;
        System.err.println("----------------- " + Thread.currentThread() + " execute time:" + exeTime + " ----------------");
    }

    private static void saveToFile(String content) {
        File file = new File(TMP_FILE2);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes(Charset.forName("utf-8")));
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveToFile(byte[] content) {
        File file = new File(TMP_FILE);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
