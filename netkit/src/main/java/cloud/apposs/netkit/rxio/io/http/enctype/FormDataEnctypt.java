package cloud.apposs.netkit.rxio.io.http.enctype;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.buffer.ByteBuf;
import cloud.apposs.netkit.buffer.ReadOnlyBuf;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Form表单Form-Data编码提交
 */
public class FormDataEnctypt implements FormEnctypt {
	public static final String CRLF = "\r\n";
	private final static String DOUBLE_HYPHENS = "--";
	private final static char[] MULTIPART_CHARS = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

	/** 参数编码 */
	private final Charset charset;
	
	/** 参数列表 */
	private final Map<String, Object> parameters = new HashMap<String, Object>();
	
	/** 参数编码列表 */
	private final List<MultipartEntity> entityList = new LinkedList<MultipartEntity>();
	
	/** 表单数据分隔符 */
	private final String boundary;
	
	public FormDataEnctypt() {
		this("utf-8");
	}
	
	public FormDataEnctypt(String charset) {
		this.charset = Charset.forName(charset);
		this.boundary = doGenerateBoundary();
	}
	
	@Override
	public boolean addParameter(String name, Object value) throws IOException {
		parameters.put(name, value);
		if (value instanceof File) {
			File file = (File) value;
			entityList.add(new MultipartEntityFile(name, file));
		} else if (value instanceof FileBuffer) {
			FileBuffer file = (FileBuffer) value;
			entityList.add(new MultipartEntityBuffer(name, file));
		} else {
			entityList.add(new MultipartEntityString(name, value.toString()));
		}
		return true;
	}

	@Override
	public Object getParameter(String name) {
		return parameters.get(name);
	}

	@Override
	public Map<String, Object> getParameters() {
		return parameters;
	}

	@Override
	public boolean hasParameter(String name) {
		return parameters.containsKey(name);
	}
	
	@Override
	public String getContentType() {
		return "multipart/form-data;boundary=" + boundary;
	}

	@Override
	public long getContentLength() {
		long length = 0;
		for (MultipartEntity entity : entityList) {
			length += entity.length();
		}
		length += tailBoundary().length();
		return length;
	}
	
	private String headBoundary() {
        return DOUBLE_HYPHENS + boundary;
    }
	
	private String tailBoundary() {
        return DOUBLE_HYPHENS + boundary + DOUBLE_HYPHENS + CRLF;
    }

	@Override
	public boolean addRequestBuffer(List<IoBuffer> bufferList) throws IOException {
		if (entityList.isEmpty()) {
			return false;
		}
		for (MultipartEntity entity : entityList) {
			IoBuffer[] buffers = entity.content();
			for (int i = 0; i < buffers.length; i++) {
				bufferList.add(buffers[i]);
			}
		}
		bufferList.add(ByteBuf.wrap(tailBoundary()));
		return true;
	}
	
	private static String doGenerateBoundary() {
        final StringBuilder buffer = new StringBuilder(64);
        final Random rand = new Random();
        // a random size from 30 to 40
        final int count = rand.nextInt(11) + 30;
        buffer.append(DOUBLE_HYPHENS);
        for (int i = 0; i < count; i++) {
            buffer.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
        }
        return buffer.toString();
    }
	
	/**
	 * 参数FORM编码
	 */
	interface MultipartEntity {
		/** 获取参数字节大小 */
		long length();
		
		/**
		 * 添加编码参数内容到发送字节中
		 */
		IoBuffer[] content() throws IOException;
	}
	
	/**
	 * 普通参数FORM编码，格式如下：
	 * <pre>
	 * ------WebKitFormBoundary0I1SO9CGBmiC7NZG
	 * Content-Disposition: form-data; name="username"
	 * 
	 * googleuser
	 * </pre>
	 */
	class MultipartEntityString implements MultipartEntity {
		private final byte[] content;
		
		public MultipartEntityString(String name, String value) {
			StringBuilder info = new StringBuilder(64);
			info.append(headBoundary()).append(CRLF);
			info.append("Content-Disposition: form-data;");
			info.append(" name=\"").append(name).append("\"").append(CRLF).append(CRLF);
			info.append(value).append(CRLF);
			this.content = info.toString().getBytes(charset);
		}

		@Override
		public long length() {
			return content.length;
		}

		@Override
		public IoBuffer[] content() throws IOException {
			IoBuffer[] buffers = new IoBuffer[1];
			buffers[0] = ByteBuf.wrap(content);
			return buffers;
		}
	}
	
	/**
	 * 二进制文件参数FORM编码，格式如下：
	 * <pre>
	 * ------WebKitFormBoundary0I1SO9CGBmiC7NZG
	 * Content-Disposition: form-data; name="file"; filename="user.tdb"
	 * Content-Type: application/octet-stream
	 * 
	 * [STREAM DATA HERE]
	 * </pre>
	 */
	class MultipartEntityFile implements MultipartEntity {
		private final File file;
		
		private final byte[] content;
		
		private final ReadOnlyBuf buffer;
		
		public MultipartEntityFile(String name, File file) throws IOException {
			StringBuilder info = new StringBuilder(64);
			info.append(headBoundary()).append(CRLF);
			info.append("Content-Disposition: form-data;");
			info.append(" name=\"").append(name).append("\"; filename=\"")
				.append(file.getName()).append("\"").append(CRLF);
			info.append("Content-Type: application/octet-stream").append(CRLF).append(CRLF);
			this.content = info.toString().getBytes(charset);
			this.file = file;
			this.buffer = new ReadOnlyBuf(file);
		}

		@Override
		public long length() {
			return content.length + file.length() + CRLF.length();
		}
		
		@Override
		public IoBuffer[] content() throws IOException {
			IoBuffer[] buffers = new IoBuffer[3];
			buffers[0] = ByteBuf.wrap(content);
			buffers[1] = buffer;
			buffers[2] = ByteBuf.wrap(CRLF);
			return buffers;
		}
	}
	
	/**
	 * 二进制数据参数FORM编码，格式如下：
	 * <pre>
	 * ------WebKitFormBoundary0I1SO9CGBmiC7NZG
	 * Content-Disposition: form-data; name="file"; filename="user.tdb"
	 * Content-Type: application/octet-stream
	 * 
	 * SQLite format 3   @         
	 * tableuseruserCREATE TABLE user(
	 *     id INTEGER PRIMARY KEY AUTOINCREMENT,
	 *     name VARCHAR(50) NOT NULL,
	 *     class INT(11) DEFAULT NULL
	 * )
	 * </pre>
	 */
	class MultipartEntityBuffer implements MultipartEntity {
		private final byte[] content;
		
		private final FileBuffer file;
		
		public MultipartEntityBuffer(String name, FileBuffer file) {
			StringBuilder info = new StringBuilder(64);
			info.append(headBoundary()).append(CRLF);
			info.append("Content-Disposition: form-data;");
			info.append(" name=\"").append(name).append("\"; filename=\"")
				.append(file.getName()).append("\"").append(CRLF);
			info.append("Content-Type: application/octet-stream").append(CRLF).append(CRLF);
			this.content = info.toString().getBytes(charset);
			this.file = file;
		}

		@Override
		public long length() {
			return content.length + file.length() + CRLF.length();
		}
		
		@Override
		public IoBuffer[] content() throws IOException {
			IoBuffer[] buffers = new IoBuffer[3];
			buffers[0] = ByteBuf.wrap(content);
			buffers[1] = file.getBuffer();
			buffers[2] = ByteBuf.wrap(CRLF);
			return buffers;
		}
	}
	
	public static class FileBuffer {
		private final String name;
		
		private final IoBuffer buffer;

		public FileBuffer(String name, byte[] buffer) {
			this(name, new ByteBuf(buffer));
		}
		
		public FileBuffer(String name, ByteBuffer buffer) {
			this(name, new ByteBuf(buffer));
		}
		
		public FileBuffer(String name, IoBuffer buffer) {
			this.name = name;
			this.buffer = buffer;
		}

		public String getName() {
			return name;
		}

		public IoBuffer getBuffer() {
			return buffer;
		}
		
		public long length() {
			return buffer.readableBytes();
		}
	}
}
