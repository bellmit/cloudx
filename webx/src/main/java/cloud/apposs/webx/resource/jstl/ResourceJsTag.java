package cloud.apposs.webx.resource.jstl;

import cloud.apposs.util.StrUtil;
import cloud.apposs.webx.resource.Resource;
import cloud.apposs.webx.resource.ResourceManager;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;

/**
 * JS样式输出
 */
public class ResourceJsTag extends SimpleTagSupport {
	private String key;
	
	public void setKey(String key) {
		this.key = key;
	}

	@Override
    public void doTag() throws JspException, IOException {
		String jsPath = ResourceManager.getResPath(Resource.RES_TYPE_JS, key);
		if (!StrUtil.isEmpty(jsPath)) {
			String jsPathOutput = "<script type=\"text/javascript\" src=\"//" + jsPath + "\"></script>";
			getJspContext().getOut().write(jsPathOutput);
		}
    }
}
