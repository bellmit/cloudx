package cloud.apposs.netkit.filterchain.http.server;

import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.WriteRequest;
import cloud.apposs.netkit.filterchain.IoFilterAdaptor;
import cloud.apposs.netkit.filterchain.http.server.decrypt.FormDataDectypt;
import cloud.apposs.netkit.filterchain.http.server.decrypt.FormDecrypt;
import cloud.apposs.netkit.filterchain.http.server.decrypt.FormJsonDectypt;
import cloud.apposs.netkit.filterchain.http.server.decrypt.FormUrlDectypt;
import cloud.apposs.util.HttpStatus;
import cloud.apposs.util.MediaType;
import cloud.apposs.util.Parser;
import cloud.apposs.util.StrUtil;
import cloud.apposs.util.SysUtil;

import java.io.File;

/**
 * HTTP表单POST数据解码过滤器，
 * 之所以不在{@link HttpServerFilter}中进行表单解码是因为如果表单是上传一个大文件数据，
 * 那么如果服务接入熔断时进行CMD指令判断时会先读取所有文件数据再进行熔断处理，这样会导致其实已经所有数据进来了无法立即熔断保护好系统，
 * 采用新增Filter的方式可以实现在熔断过滤后再解析表单数据，实现热插拔，示例如下
 * <pre>
 * // 先解析HTTP HEADER请求
 * Server.filterChain.addFilter(new HttpServerFilter());
 * // 解析HEADER头部数据之后就可以用熔断来做指令判断了，不用读取所有表单数据再通过熔断过滤来判断
 * Server.filterChain.addFilter(new SentinelFilter());
 * // 熔断检测系统通过之后最后才解析表单数据
 * Server.filterChain.addFilter(new HttpDecryptFilter());
 * </pre>
 */
public class HttpDecryptFilter extends IoFilterAdaptor {
	public static final String FILTER_CONTEXT = "HttpDecryptFilterContext";
	
	private final String charset;
	
	/** 文件上传时临时文件相关配置 */
	private static int threshold;
	private static File directory;
	
	public HttpDecryptFilter() {
		this(HttpConstants.DEFAULT_CHARSET, HttpConstants.DEFAULT_TMP_DIRECTORY);
	}
	
	public HttpDecryptFilter(String charset) {
		this(charset, HttpConstants.DEFAULT_TMP_DIRECTORY);
	}
	
	public HttpDecryptFilter(String charset, String directory) {
		SysUtil.checkNotNull(charset, "charset");
		this.charset = charset;
		HttpDecryptFilter.threshold = HttpConstants.DEFAULT_FILE_LIMIT;
		HttpDecryptFilter.directory = new File(directory);
	}
	
	@Override
	public void channelRead(NextFilter nextFilter, IoProcessor processor,
			Object message) throws Exception {
		if (!(message instanceof HttpRequest)) {
            nextFilter.channelRead(processor, message);
            return;
        }
		
		HttpRequest request = (HttpRequest) message;
		Context context = getContext(processor, request);
		
		// 无需进行表单数据解析，直接交给下一个过滤链处理
		if (context.isNoNeedDecrypt()) {
			nextFilter.channelRead(processor, request);
			return;
		}
		// 表单数据已经解析完毕，丢掉数据包避免重复调用业务逻辑
		if (context.isDecryptComplete()) {
			return;
		}
		
		// 解析Form表单数据
		if (context.parseForm()) {
			// 数据解析完整才给下一个链执行，否则继续读取数据
			nextFilter.channelRead(processor, context.getRequest());
		}
	}
	
	/**
	 * 数据发送完毕，重置上下文数据，方便在HTTP长连接中下次重新发起请求
	 */
	@Override
	public void channelSend(NextFilter nextFilter, IoProcessor processor,
			WriteRequest writeRequest) throws Exception {
		Context context = (Context) processor.getAttribute(FILTER_CONTEXT);
        if (context != null) {
            context.reset();
        }
		nextFilter.channelSend(processor, writeRequest);
	}
	
	@Override
	public void channelClose(NextFilter nextFilter, IoProcessor processor) {
		Context context = (Context) processor.getAttribute(FILTER_CONTEXT);
		if (context != null) {
			context.release();
		}
		nextFilter.channelClose(processor);
	}
	
	private Context getContext(IoProcessor processor, HttpRequest request) throws Exception {
        Context context = (Context) processor.getAttribute(FILTER_CONTEXT);
        
        if (context == null) {
        	int bufferSize = (processor.getBufferSize() * 3) / 2;
            context = new Context(request, bufferSize, charset);
            processor.setAttribute(FILTER_CONTEXT, context);
        }
        
        return context;
    }
	
	/**
	 * 一个请求就是一个上下文实例
	 */
	public static final class Context {
		/** 解析表单数据的各种状态 */
		public static final int FORM_STATUS_INIT 	= 0;
		public static final int FORM_STATUS_FINISH 	= 1;
		private int status = FORM_STATUS_INIT;
		
		private final HttpRequest request;
		private final FormDecrypt decryptor;
		
		private Context(HttpRequest request, int bufferSize, String charset) throws Exception {
			this.request = request;
			// 判断Http请求是否有表单数据
			if (!request.isHeaderContains("content-type", true)) {
				// 无表单数据要解析，直接标记为无需解析
				decryptor = null;
			} else {
				decryptor = doCreateFormDecrypt(request, bufferSize, charset);
			}
		}
		
		public HttpRequest getRequest() {
			return request;
		}
		
		public boolean isNoNeedDecrypt() {
			return decryptor == null;
		}

		public boolean isDecryptComplete() {
			return status == FORM_STATUS_FINISH;
		}
		
		public boolean parseForm() throws Exception {
			if (request.getContent() == null) {
				return false;
			}
			boolean complete = decryptor.parseForm(request);
			if (complete) {
				status = FORM_STATUS_FINISH;
			}
			return complete;
		}
		
		public void reset() {
			status = FORM_STATUS_INIT;
		}

		public void release() {
			if (decryptor != null) {
				decryptor.release();
			}
		}
		
		private static FormDecrypt doCreateFormDecrypt(
				HttpRequest request, int bufferSize, String charset) throws Exception {
			String contentType = request.getHeader("content-type", true);
			if (StrUtil.isEmpty(contentType)) {
				throw new HttpParseException(HttpStatus.HTTP_STATUS_400, "Invalid Header Content-Type");
			}
			long contentLength = Parser.parseLong(request.getHeader("content-length", true), -1);
			if (contentLength < 0) {
				throw new HttpParseException(HttpStatus.HTTP_STATUS_400, "Invalid Header Content-Length");
			}
			
			// 根据表单Content-Type决定采用哪个FormDecryptor进行解码
			if (MediaType.APPLICATION_FORM_URLENCODED.matchType(contentType)) {
				return new FormUrlDectypt(contentLength, charset);
			}
			if (MediaType.MULTIPART_FORM_DATA.matchType(contentType)) {
				return new FormDataDectypt(contentType, charset, bufferSize, threshold, directory);
			}
			if (MediaType.APPLICATION_JSON.matchType(contentType)) {
				return new FormJsonDectypt(contentLength, charset);
			}
			// 没有匹配的表单Content-Type，直接抛出异常，视为不支持
			throw new HttpParseException(HttpStatus.HTTP_STATUS_501, "Invalid Header Content-Type");
		}
	}
}
