package cloud.apposs.webx.resource.jstl;

import cloud.apposs.util.StrUtil;
import cloud.apposs.webx.resource.Resource;
import cloud.apposs.webx.resource.ResourceManager;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;

/**
 * CSS样式输出
 */
public class ResourceCssTag extends SimpleTagSupport {
	private String key;
	
	public void setKey(String key) {
		this.key = key;
	}

	@Override
    public void doTag() throws JspException, IOException {
		String cssPath = ResourceManager.getResPath(Resource.RES_TYPE_CSS, key);
		if (!StrUtil.isEmpty(cssPath)) {
			String cssPathOutput = "<link type=\"text/css\" href=\"//" + cssPath + "\" rel=\"stylesheet\" />";
			getJspContext().getOut().write(cssPathOutput);
		}
    }
}
