package cloud.apposs.netkit.rxio.io.http;

import java.net.Proxy;

/**
 * Http Delete请求
 */
public class IoHttpDelete extends IoHttpPost {
    public IoHttpDelete(String url) throws Exception {
        super(url, HttpMethod.DELETE);
    }

    public IoHttpDelete(String url, Proxy proxy) throws Exception {
        super(url, HttpMethod.DELETE, proxy);
    }
}
