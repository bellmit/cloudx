package cloud.apposs.netkit.rxio.io.http;

import java.net.Proxy;

/**
 * Http Put请求
 */
public class IoHttpPut extends IoHttpPost {
    public IoHttpPut(String url) throws Exception {
        super(url, HttpMethod.PUT);
    }

    public IoHttpPut(String url, Proxy proxy) throws Exception {
        super(url, HttpMethod.PUT, proxy);
    }
}
