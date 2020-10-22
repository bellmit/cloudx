package cloud.apposs.netkit.filterchain.http.server.decrypt;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.filterchain.http.server.HttpConstants;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.util.LineBuilder;

/**
 * Form表单URL解码，一个请求对应一个表单解码器
 * Content-Type: application/x-www-form-urlencoded
 */
public class FormUrlDectypt implements FormDecrypt {
	/** 解析表单数据的各种状态 */
	public static final int FORM_DECRYPT_START 			= 0;
	public static final int FORM_DECRYPT_FIELD 			= 1;
	public static final int FORM_DECRYPT_DISPOSITION	= 2;
	
	private int status = FORM_DECRYPT_START;
	
	private long contentLength;
	private long currentContentPosition = 0;
	
	private LineBuilder currentKey;
	private LineBuilder currentValue;
	
	public FormUrlDectypt(long contentLength, String charset) {
		if (contentLength <= 0) {
			throw new IllegalArgumentException("Content-Length");
		}
		this.contentLength = contentLength;
		this.currentKey = new LineBuilder(128, charset);
		this.currentValue = new LineBuilder(128, charset);
	}
	
	@Override
	public boolean parseForm(HttpRequest request) throws Exception {
		IoBuffer buffer = request.getContent();
		long index = buffer.readIdx();
		long total = buffer.writeIdx();
		for (;index < total; index++) {
			currentContentPosition++;
			byte letter = buffer.get();
			switch (status) {
			case FORM_DECRYPT_START:
				if (letter == ' ' || letter == '\t' || letter == '&') {
					break;
				}
				
				if (letter == '=') {
					status = FORM_DECRYPT_FIELD;
				} else {
					currentKey.append(letter);
				}
				break;
			case FORM_DECRYPT_FIELD: // 匹配'='
				if (letter == ' ' || letter == '\t' || letter == HttpConstants.LF) {
					// 表单数据解析结束
					break;
				}
				
				if (letter == '&') {
					// 解析Key=Value结束
					status = FORM_DECRYPT_START;
					doAddParameter(request);
				} else {
					currentValue.append(letter);
				}
				break;
			}
		}
		// Content-Length指定长度的数据已经读取完毕
		boolean complete = currentContentPosition >= contentLength;
		if (complete) {
			doAddParameter(request);
		}
		return complete;
	}
	
	@Override
	public void release() {
	}

	private void doAddParameter(HttpRequest request) {
		if (currentKey.length() > 0 && currentValue.length() > 0) {
			request.getParameters().put(currentKey.toString(), currentValue.toString());
			// 逻辑清空缓存，等待下一个KEY&VALUE解析
			currentKey.setLength(0);
			currentValue.setLength(0);
		}
	}
}
