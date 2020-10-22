package cloud.apposs.netkit.rxio.io.http.enctype;

import cloud.apposs.netkit.IoBuffer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * POST表单提交不同的编码格式
 */
public interface FormEnctypt {
	public static final int FORM_ENCTYPE_URLENCODE = 0;
	public static final int FORM_ENCTYPE_FORMDATA = 1;
	public static final int FORM_ENCTYPE_JSON = 2;
	
	/**
	 * 添加请求参数
	 * @param  name  参数名称
	 * @param  value 参数值
	 * @return 添加成功返回true
	 */
	boolean addParameter(String name, Object value) throws IOException;
	
	/**
	 * 判断是否已经已经存在指定参数
	 */
	boolean hasParameter(String name);
	
	/**
	 * 获取参数值
	 */
	Object getParameter(String name);
	
	/**
	 * 获取参数列表
	 */
	Map<String, Object> getParameters();
	
	/**
	 * 生成表单Content-Type
	 */
	String getContentType();
	
	/**
	 * 生成表单Content-Length
	 */
	long getContentLength();
	
	/**
	 * 生成表单请求数据
	 * 
	 * @return 有数据生成返回true
	 */
	boolean addRequestBuffer(List<IoBuffer> bufferList) throws IOException;
}
