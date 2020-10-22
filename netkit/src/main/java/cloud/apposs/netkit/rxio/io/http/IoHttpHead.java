package cloud.apposs.netkit.rxio.io.http;

import java.net.Proxy;

/**
 * Http Head请求
 */
public class IoHttpHead extends IoHttp {
	public IoHttpHead(String url) throws Exception {
		super(url, HttpMethod.HEAD);
	}

	public IoHttpHead(String url, Proxy proxy) throws Exception {
		super(url, HttpMethod.HEAD, proxy);
	}
}
