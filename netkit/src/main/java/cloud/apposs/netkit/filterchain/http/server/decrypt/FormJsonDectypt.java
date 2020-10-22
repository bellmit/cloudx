package cloud.apposs.netkit.filterchain.http.server.decrypt;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.filterchain.http.server.HttpConstants;
import cloud.apposs.netkit.filterchain.http.server.HttpParseException;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.util.HttpStatus;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.LineBuilder;
import cloud.apposs.util.Param;

/**
 * Form表单JSON解码，一个请求对应一个表单解码器
 * Content-Type: application/json;charset=utf-8
 * 参考：
 * https://blog.csdn.net/baichoufei90/article/details/84030479
 */
public class FormJsonDectypt implements FormDecrypt {
	private long contentLength;
	private long currentReadPosition = 0;
	
	/** 解析出来的JSON数据 */
	private LineBuilder jsonLine;
	
	public FormJsonDectypt(long contentLength, String charset) {
		this.contentLength = contentLength;
		this.jsonLine = new LineBuilder(128, charset);
	}
	
	@Override
	public boolean parseForm(HttpRequest request) throws Exception {
		IoBuffer buffer = request.getContent();
		long index = buffer.readIdx();
		long total = buffer.writeIdx();
		
		for (;index < total; index++) {
			currentReadPosition++;
			byte letter = buffer.get();
			if (jsonLine.length() > HttpConstants.MAX_HEADER_LINE) {
				throw new HttpParseException(HttpStatus.HTTP_STATUS_400, "Max Request Line");
			}
			jsonLine.append(letter);
		}
		
		// Content-Length指定长度的数据已经读取完毕
		boolean complete = currentReadPosition >= contentLength;
		if (complete) {
			Param param = JsonUtil.parseJsonParam(jsonLine.toString());
			request.getParam().putAll(param);
		}
		return complete;
	}

	@Override
	public void release() {
	}
}
