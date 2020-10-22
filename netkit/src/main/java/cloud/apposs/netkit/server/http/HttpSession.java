package cloud.apposs.netkit.server.http;

import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.netkit.server.ServerHandlerContext;

public final class HttpSession {
	private final HttpRequest request;
	
	private final HttpResponse response;

	public HttpSession(ServerHandlerContext context, HttpRequest request) {
		this.response = new HttpResponse(context);
		this.request = request;
	}
	
	public HttpRequest getRequest() {
		return request;
	}

	public HttpResponse getResponse() {
		return response;
	}
	
	public void close(boolean immediately) {
		response.close(immediately);
	}
}
