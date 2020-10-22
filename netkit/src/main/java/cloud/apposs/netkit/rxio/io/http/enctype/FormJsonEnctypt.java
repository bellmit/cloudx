package cloud.apposs.netkit.rxio.io.http.enctype;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.buffer.ByteBuf;
import cloud.apposs.util.Param;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * Form表单JSON编码提交
 */
public class FormJsonEnctypt implements FormEnctypt {
	/** 参数编码 */
	private final Charset charset;
	
	/** 参数列表 */
	private final Param parameters = new Param();
	
	public FormJsonEnctypt() {
		this("utf-8");
	}
	
	public FormJsonEnctypt(String charset) {
		this.charset = Charset.forName(charset);
	}
	
	@Override
	public boolean addParameter(String name, Object value) {
		parameters.put(name, value);
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
		return "application/json;charset=" + charset;
	}

	@Override
	public long getContentLength() {
		return parameters.toJson().getBytes(charset).length;
	}

	@Override
	public boolean addRequestBuffer(List<IoBuffer> bufferList) throws IOException {
		if (parameters.size() <= 0) {
			return false;
		}
		bufferList.add(ByteBuf.wrap(parameters.toJson(), charset));
		return true;
	}

	@Override
	public String toString() {
		return parameters.toJson();
	}
}
