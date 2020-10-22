package cloud.apposs.netkit.rxio.io.http;

import java.util.HashMap;
import java.util.Map;

public enum HttpMethod {
	GET, POST, HEAD, PUT, DELETE;

	private static final Map<String, HttpMethod> methodMap = new HashMap<String, HttpMethod>();

	static {
	    methodMap.put("GET", GET);
        methodMap.put("POST", POST);
        methodMap.put("PUT", PUT);
        methodMap.put("DELETE", DELETE);
        methodMap.put("HEAD", HEAD);
    }

    public static HttpMethod getHttpMethod(String method) {
	    return methodMap.get(method);
    }
}
