package cloud.apposs.webx;

import cloud.apposs.rest.RestConstants;

public final class WebXConstants extends RestConstants {
    /** Web.XML配置的WebX框架配置文件路径 */
    public static final String INIT_CONFIG_LOCATION = "configLocation";
    /** Web.XML配置的WebX框架配置实现类，实现框架文件零配置 */
    public static final String INIT_CONFIG_CLASS = "configClass";
    /** 默认WebX框架配置文件路径 */
    public static final String WEB_PATH = "/WEB-INF/";
    public static final String CONFIG_FILE_SUFFIX = "-mvc.xml";

    // 对象注入属性
    public static final String REQUEST_PARAMETRIC_FLOW = "flow";
    public static final String REQUEST_PARAMETRIC_REQUEST = "request";
    public static final String REQUEST_PARAMETRIC_RESPONSE = "response";
    public static final String REQUEST_PARAMETRIC_MULTIFORM_REQUEST = "multiFormRequest";

    /** 请求流水号参数 */
    public static final String REQUEST_PARAMETER_FLOW = "_flow";
    public static final String REQUEST_ATTRIBUTE_ERRNO = "errno";

	public static final String REQUEST_ATTRIBUTE_WEBXCONFIG = "AttrWebXConfig";
    public static final String REQUEST_ATTRIBUTE_VARIABLES = "AttrVariable";
    public static final String REQUEST_ATTRIBUTE_START_TIME = "AttrStartTime";
    public static final String REQUEST_ATTRIBUTE_ASYNC = "AttrAsync";
    public static final String REDIRECT_URL_PREFIX = "redirect:";
    public static final String FORWARD_URL_PREFIX = "forward:";
    public static final String WEBX_VERSION = "v1.1.0.RELEASE";
}
