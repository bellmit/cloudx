package cloud.apposs.netkit.rxio.io.http;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.buffer.ByteBuf;
import cloud.apposs.util.StrUtil;

import java.net.Proxy;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Http Get请求
 */
public class IoHttpGet extends IoHttp {
	public IoHttpGet(String url) throws Exception {
		super(url, HttpMethod.GET);
	}

	public IoHttpGet(String url, Proxy proxy) throws Exception {
		super(url, HttpMethod.GET, proxy);
	}

	@Override
	protected IoBuffer[] doWrapRequest() {
		StringBuilder request = new StringBuilder(128);
		String path = uri.getPath();
		if (StrUtil.isEmpty(path)) {
			path = "/";
		}
		
		// URL带参和方法带参都支持一起GET传递
		boolean hasQuery = false;
		String query = uri.getRawQuery();
		if (!StrUtil.isEmpty(query)) {
			hasQuery = true;
			path += "?" + query;
		}
		if (form != null) {
			Map<String, Object> arguments = form.getArgs();
			int total = arguments.size();
			if (total > 0) {
				if (!hasQuery) {
					path += "?";
				} else {
					path += "&";
				}
			}
			StringBuilder argsQuery = new StringBuilder(64);
			int current = 0;
			for (Entry<String, Object> entry : arguments.entrySet()) {
				argsQuery.append(entry.getKey()).append("=").append(entry.getValue());
				if (++current < total) {
					argsQuery.append("&");
				}
			}
			path += argsQuery.toString();
		}
		
		request.append(method).append(" " + path + " ");
		request.append(HTTP_PROTOCL_1 + CRLF);
		request.append("User-Agent: " + userAgent + CRLF);
		if (!headers.containsKey("Host")) {
			headers.put("Host", uri.getHost());
		}
		if (!headers.containsKey("Accept")) {
			headers.put("Accept", "*/*");
		}
		Set<Entry<String, String>> headerList = headers.entrySet();
		int total = headerList.size();
		int current = 0;
		for (Entry<String, String> keyVal : headerList) {
			request.append(keyVal.getKey() + ": " + keyVal.getValue() + CRLF);
			if (++current >= total) {
				request.append(CRLF);
			}
		}
		IoBuffer[] buffers = new IoBuffer[1];
		buffers[0] = ByteBuf.wrap(request.toString(), "utf-8");
		return buffers;
	}
}
