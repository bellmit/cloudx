package cloud.apposs.netkit.rxio.io.http;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.rxio.io.http.enctype.FormDataEnctypt;
import cloud.apposs.netkit.rxio.io.http.enctype.FormEnctypt;
import cloud.apposs.netkit.rxio.io.http.enctype.FormJsonEnctypt;
import cloud.apposs.netkit.rxio.io.http.enctype.FormUrlEnctypt;
import cloud.apposs.util.StrUtil;
import cloud.apposs.netkit.rxio.io.http.enctype.FormDataEnctypt.FileBuffer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * HTTP表单数据请求，支持GET/POST，
 * 注意对于POST请求只是简单的编码之后HTTP请求过去，
 * 如果需要POST文件则需要设置formEnctype为FORM_ENCTYPE_FORMDATA
 */
public class HttpForm {
	/** 表单参数编码类型，POST专用 */
	private final FormEnctypt formEnctype;
	
	public HttpForm() {
		this(FormEnctypt.FORM_ENCTYPE_URLENCODE, "utf-8");
	}
	
	public HttpForm(int formEnctype) {
		this(formEnctype, "utf-8");
	}
	
	public HttpForm(int formEnctype, String charset) {
		if (formEnctype == FormEnctypt.FORM_ENCTYPE_FORMDATA) {
			this.formEnctype = new FormDataEnctypt(charset);
		} else if (formEnctype == FormEnctypt.FORM_ENCTYPE_JSON) {
			this.formEnctype = new FormJsonEnctypt(charset);
		} else {
			this.formEnctype = new FormUrlEnctypt(charset);
		}
	}

	/**
	 * 添加请求参数
	 * @param  name  参数名称
	 * @param  value 参数值
	 * @return 添加成功返回true
	 */
	public boolean add(String name, Object value) throws IOException {
		if (StrUtil.isEmpty(name) || value == null) {
			return false;
		}
		formEnctype.addParameter(name, value);
		return true;
	}
	
	public boolean add(String name, String value) throws IOException {
		if (StrUtil.isEmpty(name) || StrUtil.isEmpty(value)) {
			return false;
		}
		formEnctype.addParameter(name, value);
		return true;
	}
	
	/**
	 * 添加文件上传，POST专用
	 */
	public boolean add(String name, File value) throws IOException {
		if (StrUtil.isEmpty(name) || value == null) {
			return false;
		}
		formEnctype.addParameter(name, value);
		return true;
	}
	
	/**
	 * 添加文件上传，POST专用
	 */
	public boolean add(String name, FileBuffer value) throws IOException {
		if (StrUtil.isEmpty(name) || value == null) {
			return false;
		}
		formEnctype.addParameter(name, value);
		return true;
	}
	
	public boolean hasParameter(String name) {
		return formEnctype.hasParameter(name);
	}
	
	public Object getArg(String name) {
		return formEnctype.getParameter(name);
	}
	
	public Map<String, Object> getArgs() {
		return formEnctype.getParameters();
	}
	
	/**
	 * 生成请求的Content-Type，POST专用
	 */
	public String getContentType() {
		return formEnctype.getContentType();
	}
	
	/**
	 * 生成请求的Content-Length，POST专用
	 */
	public long getContentLength() {
		return formEnctype.getContentLength();
	}
	
	/**
	 * 生成表单请求的数据，POST专用
	 */
	public boolean addRequestBuffer(List<IoBuffer> bufferList) throws IOException {
		return formEnctype.addRequestBuffer(bufferList);
	}

	@Override
	public String toString() {
		return formEnctype.toString();
	}
}
