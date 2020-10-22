package cloud.apposs.netkit.filterchain.http.client;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.WriteRequest;
import cloud.apposs.netkit.buffer.IoAllocator;
import cloud.apposs.netkit.filterchain.IoFilterAdaptor;
import cloud.apposs.netkit.rxio.io.http.HttpMethod;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * http协议过滤器
 * 参考：
 * https://imququ.com/post/transfer-encoding-header-in-http.html
 * http://www.cnblogs.com/xuehaoyue/p/6639029.html
 */
public class HttpFilter extends IoFilterAdaptor {
	public static final String FILTER_CONTEXT = "HttpFilterContext";
	/** http结束标识符 */
	private static final String HTTP_END_STRING = "\r\n";
	/** header结束标识符 */
	private static final String HEADER_END_STRING = "\r\n\r\n";
	/** chunk分块结束标识符 */
	private static final String CHUNK_END_STRING = "0\r\n\r\n";
	
	private int bufferSize = 1024 * 30;
	
	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public HttpFilter(IoProcessor processor, String url, HttpMethod method) throws IOException {
        Context context = new Context(bufferSize, url, method);
        processor.setAttribute(FILTER_CONTEXT, context);
	}
	
	@Override
	public void channelRead(NextFilter nextFilter,
			IoProcessor processor, Object message) throws Exception {
		if (!(message instanceof IoBuffer)) {
            nextFilter.channelRead(processor, message);
            return;
        }
		
		final IoBuffer in = (IoBuffer) message;
		final Context context = getContext(processor);
		context.getBuffer().put(in);
		
		// 先获取http服务器请求头再决定接下来读取多少字节
		if (!context.isHeaderComplete()) {
			if (!isEndStringMatched(context, HEADER_END_STRING)) {
				return;
			}
			parseHeaders(context);
		}
		
		if (parseBody(context)) {
			nextFilter.channelRead(processor, context.getResponse());
		}
	}
	
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
	
	private Context getContext(IoProcessor processor) {
        return (Context) processor.getAttribute(FILTER_CONTEXT);
    }
	
	private boolean isEndStringMatched(Context context, String endString) throws IOException {
		int matched = 0;
		long readPos = context.getReadPosition();
		IoBuffer buf = context.getBuffer();
		try {
			long total = buf.writeIdx();
			while (readPos < total) {
				byte b = buf.get(readPos++);
				if (b == endString.charAt(matched)) {
					++matched;
					if (matched == endString.length()) {
						return true;
					}
				} else {
					readPos = readPos - matched;
					matched = 0;
				}
			}
		} finally {
			context.setReadPosition(readPos);
		}
		
		return false;
	}
	
	private void parseHeaders(Context context) throws IOException {
		IoBuffer buf = context.getBuffer();
		byte[] headerBytes = buf.array(0, (int) context.getReadPosition());
		InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(headerBytes));
		BufferedReader br = new BufferedReader(isr);
		HttpAnswer response = context.getResponse();
		String line = br.readLine();
		if (line == null) {
			return;
		}
		
		if (response.getStatus() == null) {
			parseStatus(response, line);
			line = br.readLine();
		}
		
		while (line != null) {
		    String[] keyValue = line.split(":");
		    if (keyValue.length == 2) {
		    	String key = keyValue[0].trim().toLowerCase();
		    	String value = keyValue[1].trim().toLowerCase();
		    	response.putHeader(key, value);
		    	
		    	if (key.equals("content-length")) {
		    		context.setContentLength(Integer.parseInt(value));
		    	} else if (key.equals("transfer-encoding")) {
		    		context.setChunked(value.equals("chunked"));
		    	}
		    }
		    line = br.readLine();
		}
	}
	
	private void parseStatus(HttpAnswer response, String line) {
        String[] splits = line.split("\\s");
        if (splits.length > 2) {
        	HttpAnswer.Status status = response.getRawStatus();
        	status.setVersion(splits[0]);
        	try {
        		status.setCode(Integer.parseInt(splits[1]));        		
        	} catch(NumberFormatException e) {
        	}
            status.setDescription(splits[2]);
        }
    }
	
	private boolean parseBody(Context context) throws IOException {
		// Http Head请求不请求内容体
		if (HttpMethod.HEAD == context.getMethod()) {
            return true;
        }
		
		if (context.getContentLength() >= 0) {
			if (context.getContentLength() == 0) {
				return true;
			}
			
			IoBuffer totalBuf = context.getBuffer();
			long readPos = context.getReadPosition();
			long bodyLen = totalBuf.writeIdx() - readPos;
			if (bodyLen < 0 || bodyLen < context.getContentLength()) {
				return false;
			}
			HttpAnswer response = context.getResponse();
			response.write(totalBuf.array(readPos, (int) bodyLen));
			return true;
		} else if (context.isChunked()) {
			IoBuffer totalBuf = context.getBuffer();
			while (totalBuf.writeIdx() > context.getReadPosition()) {
				long readPos = context.getReadPosition();
				// 读取所有分块结束，退出
				if (isTrunkEnd(totalBuf, readPos)) {
					break;
				}
				// 多分块读取，参考http://www.cnblogs.com/xuehaoyue/p/6639029.html
				if (context.getChunkSize() == -1) {
					long chunkSizeStart = readPos;
					// 获取分块chunk大小
					if (!isEndStringMatched(context, HTTP_END_STRING)) {
						return false;
					}
					
					int length = (int) (context.getReadPosition() - chunkSizeStart - HTTP_END_STRING.length());
					String chunkSizeStr = new String(totalBuf.array(chunkSizeStart, length));
					context.setChunkSize(Integer.parseInt(chunkSizeStr, 16));
				}
				readPos = context.getReadPosition();
				long bodyLen = totalBuf.writeIdx() - readPos;
				long chunkSize = context.getChunkSize();
				if (bodyLen < 0 || bodyLen < chunkSize) {
					return false;
				}
				HttpAnswer response = context.getResponse();
				response.write(totalBuf.array(readPos, (int) chunkSize));
				context.setReadPosition(readPos + 2 + chunkSize);
				context.setChunkSize(-1);
			}
			// 判断是否还有下一分块数据，如果有继续读取
			if (!isTrunkEnd(totalBuf, context.getReadPosition())) {
				return false;
			}
			return true;
		}
		
		// http协议传输中content-length/chunk必须有一个，不然无法知道如何读取完整数据
		throw new IOException("header not contains content-length nor chunked");
	}
	
	/**
	 * 检查是否已经chunk分块是否结束
	 */
	private boolean isTrunkEnd(IoBuffer buf, long readPos) throws IOException {
		int matched = 0;
		long pos = readPos;
		while (pos < buf.writeIdx()) {
			byte b = buf.get(pos++);
			if (b == CHUNK_END_STRING.charAt(matched)) {
				++matched;
				if (matched == CHUNK_END_STRING.length()) {
					return true;
				}
			} else {
				return false;
			}
		}
		
		return false;
	}
	
	public static class Context {
		private IoBuffer buffer;
		/** 读取到的响应数据 */
		private HttpAnswer response;
		/** 请求的Http Method */
		private final HttpMethod method;

		private long readPosition = 0;
		
		private long contentLength = -1;
		private boolean chunked = false;
		private long chunkSize = -1;
		
		public Context(int bufLen, String url, HttpMethod method) throws IOException {
			this.buffer = IoAllocator.allocate(bufLen);
			this.response = new HttpAnswer(url);
			this.method = method;
		}

		public IoBuffer getBuffer() {
			return buffer;
		}
		
		public HttpAnswer getResponse() {
			return response;
		}

		public HttpMethod getMethod() {
			return method;
		}

		public boolean isHeaderComplete() {
			return contentLength > 0 || chunked;
		}

		public long getReadPosition() {
			return readPosition;
		}

		public void setReadPosition(long readPosition) {
			this.readPosition = readPosition;
		}

		public long getContentLength() {
			return contentLength;
		}

		public void setContentLength(long contentLength) {
			this.contentLength = contentLength;
		}

		public boolean isChunked() {
			return chunked;
		}

		public void setChunked(boolean chunked) {
			this.chunked = chunked;
		}
		
		public long getChunkSize() {
			return chunkSize;
		}

		public void setChunkSize(long chunkSize) {
			this.chunkSize = chunkSize;
		}
		
		public void reset() {
			readPosition = 0;
			contentLength = -1;
			chunked = false;
			chunkSize = -1;
			if (buffer != null) {
				buffer.reset();
			}
		}
		
		public void release() {
			if (buffer != null) {
				buffer.free();
			}
		}
	}
}
