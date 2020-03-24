package cloud.apposs.util;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * 动态字符串拼接，底层以字节为单位，服务于网络数据接收
 */
public final class LineBuilder implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final Charset DEFAULT_CHARSET = CharsetUtil.UTF_8;
	
	/** 字节数组 */
	private byte[] value;
	
	private int length;
	
	/** 字符串编码 */
	private Charset charset;
	
	public LineBuilder() {
		this(128);
	}
	
	public LineBuilder(int capacity) {
		this(capacity, DEFAULT_CHARSET);
	}
	
	public LineBuilder(int capacity, String charset) {
		this(capacity, Charset.forName(charset));
	}
	
	public LineBuilder(int capacity, Charset charset) {
		this.value = new byte[capacity];
		this.charset = charset;
	}

	public int length() {
		return length;
	}

	public void setLength(int length) {
		if (length < 0) {
		    throw new StringIndexOutOfBoundsException(length);
		}
		this.length = length;
	}
	
	public LineBuilder append(byte b) {
		int newLength = length + 1;
		if (newLength > value.length) {
			doExpandCapacity(newLength);
		}
		value[length++] = b;
		return this;
	}
	
	public LineBuilder append(String str) {
		return append(str.getBytes(charset));
	}
	
	public LineBuilder append(byte[] src) {
		return append(src, 0, src.length);
	}
	
	public LineBuilder append(byte[] src, int offset, int length) {
		SysUtil.checkNotNull(src, "src");
		
		int newLength = this.length + length;
		if (newLength > value.length) {
			doExpandCapacity(newLength);
		}
		System.arraycopy(src, offset, value, this.length, length);
		this.length = newLength;
		return this;
	}
	
	/**
	 * 字节数组容量不足时自动扩容
	 */
	private void doExpandCapacity(int capacity) {
		int newCapacity = (value.length + 1) << 1;
        if (capacity < 0) {
        	capacity = Integer.MAX_VALUE;
        }
        if (newCapacity > capacity) {
        	capacity = newCapacity;
        }
        value = Arrays.copyOf(value, capacity);
	}
	
	public String toString(String charset) {
		return toString(Charset.forName(charset));
	}
	
	public String toString(Charset charset) {
		return new String(value, 0, length, charset);
	}
	
	@Override
	public String toString() {
		return toString(charset);
	}
}
