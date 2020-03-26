package cloud.apposs.protobuf;

public class ProtoBufException extends RuntimeException {
	private static final long serialVersionUID = -7635858253994920428L;

	public ProtoBufException() {
		super();
	}

	public ProtoBufException(String message, Throwable cause) {
		super(message, cause);
	}

	public ProtoBufException(String message) {
		super(message);
	}

	public ProtoBufException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * 已经没有字节可以解码
	 */
	static ProtoBufException overIndex() {
        return new ProtoBufException("Protocol Buffer Read Over Index");
    }
	
	/**
	 * 解码格式有错误
	 */
	static ProtoBufException malformedVarInt() {
        return new ProtoBufException("CodedInput Encountered A Malformed VarInt");
    }
	
	static ProtoBufException negativeSize() {
		return new ProtoBufException("CodedInput Encountered A Negative Size");
	}
	
	/**
	 * 反射创建对象失败
	 */
	static ProtoBufException reflectInstantiation(Class<?> typeClass, Exception e) {
        return new ProtoBufException("Class[" + typeClass + "] Instantiation fails", e);
    }
	
	/**
	 * 反射调用失败
	 */
	static ProtoBufException reflectCall(Exception e) {
        return new ProtoBufException("Protocol Buffer Reflect Call Fail", e);
    }
}
