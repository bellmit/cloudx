package cloud.apposs.netkit.filterchain.http.server.template;

/**
 * HTTP错误的模板输出，
 * 可由各业务自定义，例如输出堆栈信息或者线上线上只输出网络异常信息
 */
public interface HttpTemplate {
	/**
	 * 自定义HTTP错误的模板输出
	 */
	String generateTemplate(Throwable error);
}
