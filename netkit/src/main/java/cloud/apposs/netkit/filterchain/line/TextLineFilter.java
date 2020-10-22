package cloud.apposs.netkit.filterchain.line;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.WriteRequest;
import cloud.apposs.netkit.buffer.IoAllocator;
import cloud.apposs.netkit.filterchain.IoFilterAdaptor;

import java.io.IOException;

public class TextLineFilter extends IoFilterAdaptor {
	public static final String FILTER_NAME = "TextLineFilter";
	public static final String DEFAULT_END_STRING = "\r\n";
	public static final String FILTER_CONTEXT = "TextLineFilterContext";
	
	private int bufferSize = 2 * 1024;
	
	private String endString = DEFAULT_END_STRING;
	
	public TextLineFilter() {
		this(FILTER_NAME, DEFAULT_END_STRING);
	}
	
	public TextLineFilter(String endString) {
		this(FILTER_NAME, endString);
	}
	
	public TextLineFilter(String name, String endString) {
		this.name = name;
		this.endString = endString;
	}
	
	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public String getEndString() {
		return endString;
	}

	public void setEndString(String endString) {
		this.endString = endString;
	}

	@Override
	public void channelRead(NextFilter nextFilter, IoProcessor processor,
			Object message) throws Exception {
		if (!(message instanceof IoBuffer)) {
            nextFilter.channelRead(processor, message);
            return;
        }
		
		IoBuffer in = (IoBuffer) message;
		Context context = getContext(processor);
		IoBuffer buffer = context.getBuffer();
		buffer.put(in);
		boolean isEndStringMatched = isEndStringMatched(context, buffer);
		if (isEndStringMatched) {
			nextFilter.channelRead(processor, buffer);
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

	private Context getContext(IoProcessor processor) throws IOException {
        Context context = (Context) processor.getAttribute(FILTER_CONTEXT);
        
        if (context == null) {
            context = new Context(bufferSize, endString);
            processor.setAttribute(FILTER_CONTEXT, context);
        }
        
        return context;
    }
	
	private boolean isEndStringMatched(Context context, IoBuffer buf) throws IOException {
		// 检查是否已经为结束请求
		int matched = 0;
		String endString = context.getEndString();
		for (long i = context.getReadPos(); i < buf.writeIdx(); ++i) {
			byte b = buf.get(i);
			if (b == endString.charAt(matched)) {
				++matched;
				if (matched == endString.length()) {
					buf.writeIdx(i - endString.length() + 1);
					break;
				}
			} else {
				i = i - matched;
				matched = 0;
			}
		}
		// 匹配到行尾
		if (matched == endString.length()) {
			return true;
		}
		
		long newPos = buf.writeIdx() - endString.length();
		newPos = newPos < 0 ? 0 : newPos;
		context.setReadPos(newPos);
		return false;
	}
	
	public static class Context {
		/** 字符串传输默认以什么字符串结尾算一次完整数据接收，例如smtp协议是以\r\n结束一次数据请求 */
		private String endString = DEFAULT_END_STRING;
		
		private IoBuffer buffer;
		
		private long readPos = 0;
		
		public Context(int bufLen, String endString) throws IOException {
			this.buffer = IoAllocator.allocate(bufLen);
			this.endString = endString;
		}
		
		public String getEndString() {
			return endString;
		}
		
		public void setEndString(String endString) {
			this.endString = endString;
		}

		public IoBuffer getBuffer() {
			return buffer;
		}

		public long getReadPos() {
			return readPos;
		}

		public void setReadPos(long readPos) {
			this.readPos = readPos;
		}

		public void reset() {
			readPos = 0;
			if (buffer != null) {
				buffer.reset();
			}
		}
		
		public void release() {
			if (buffer != null) {
				buffer.free();
				buffer = null;
			}
		}
	}
}
