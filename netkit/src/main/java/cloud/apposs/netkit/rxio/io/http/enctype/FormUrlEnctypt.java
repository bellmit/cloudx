package cloud.apposs.netkit.rxio.io.http.enctype;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.buffer.ByteBuf;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Form表单URL编码提交
 */
public class FormUrlEnctypt implements FormEnctypt {
	/** 参数编码 */
	private final Charset charset;
	
	/** 参数列表 */
	private final Map<String, Object> parameters = new HashMap<String, Object>();
	
	private StringBuilder encodeUrlValue = new StringBuilder(48);
	
	public FormUrlEnctypt() {
		this("utf-8");
	}
	
	public FormUrlEnctypt(String charset) {
		this.charset = Charset.forName(charset);
	}
	
	@Override
	public boolean addParameter(String name, Object value) {
		parameters.put(name, value.toString());
		String encodeValue = value.toString();
		try {
			encodeValue = URLEncoder.encode(encodeValue, charset.toString());
		} catch (UnsupportedEncodingException e) {
		}
		if (encodeUrlValue.length() <= 0) {
			encodeUrlValue.append(name).append("=").append(encodeValue);
		} else {
			encodeUrlValue.append("&").append(name).append("=").append(encodeValue);
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
		return "application/x-www-form-urlencoded;charset=" + charset;
	}

	@Override
	public long getContentLength() {
		return encodeUrlValue.toString().getBytes(charset).length;
	}

	@Override
	public boolean addRequestBuffer(List<IoBuffer> bufferList) throws IOException {
		if (parameters.size() <= 0) {
			return false;
		}
		bufferList.add(ByteBuf.wrap(encodeUrlValue.toString(), "utf-8"));
		return true;
	}

	@Override
	public String toString() {
		return encodeUrlValue.toString();
	}
}
