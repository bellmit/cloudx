package cloud.apposs.webx.resource;

public abstract class AbstractResource implements Resource {
	public static final String KEY_CSS_PREFIX = "css_";
	public static final String KEY_JS_PREFIX = "js_";
	
	@Override
	public int getResType(String key) {
		if (key.startsWith(KEY_CSS_PREFIX)) {
			return RES_TYPE_CSS;
		} else if (key.startsWith(KEY_JS_PREFIX)) {
			return RES_TYPE_JS;
		}
		return -1;
	}

	@Override
	public void loadResList() {
		// DO NOTHING
	}
}
