package cloud.apposs.netkit.filterchain.http.server.decrypt;

import cloud.apposs.netkit.filterchain.http.server.HttpRequest;

/**
 * HTTP表单数据解码器
 */
public interface FormDecrypt {
	/**
	 * 解析表单数据，将对应解析后的数据添加到HttpRequest中，全部数据解析结束返回true
	 */
	boolean parseForm(HttpRequest request) throws Exception;

	/**
	 * 请求结束，释放资源
	 */
	void release();
}
