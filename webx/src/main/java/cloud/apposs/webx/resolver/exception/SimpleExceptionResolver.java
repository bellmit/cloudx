package cloud.apposs.webx.resolver.exception;

import cloud.apposs.rest.WebExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 简单异常拦截，仅打印异常堆栈到终端
 */
public class SimpleExceptionResolver implements WebExceptionResolver<HttpServletRequest, HttpServletResponse> {
	@Override
	public Object resolveHandlerException(HttpServletRequest request,
			HttpServletResponse response, Throwable throwable) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		System.out.println("Exception Caught:" + sw.toString());
		return null;
	}
}
