package cloud.apposs.webx.resolver.parameter;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.parameter.BodyParameterResolver;
import cloud.apposs.rest.parameter.Parameter;
import cloud.apposs.util.Param;
import cloud.apposs.webx.WebUtil;
import cloud.apposs.webx.WebXConfig;
import cloud.apposs.webx.WebXConstants;
import cloud.apposs.webx.upload.MultiFormRequest;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class ModelParameterResolver extends BodyParameterResolver<HttpServletRequest, HttpServletResponse> {
    @Override
    @SuppressWarnings("unchecked")
    public Param getParameterValues(Parameter parameter,
                HttpServletRequest request, HttpServletResponse response) throws Exception {
        WebXConfig config = (WebXConfig) request.getAttribute(WebXConstants.REQUEST_ATTRIBUTE_WEBXCONFIG);

        Param param = null;
        if (ServletFileUpload.isMultipartContent(request)) {
            // 表单文件上传需要做特殊解析
            // 注意此处限制上传文件大小只是根据配置来限制，接收完之后再由业务来判断是否超过业务限制上传文件大小
            // 如果要在底层做，则可以定义一个接口，然后在此通过接口来判断，
            // 或者再起一个上传服务业务专门来做处理（即纯粹HttpServletRequest作为参数来解析上传的字节数据）
            MultiFormRequest formRequest = WebUtil.getFormRequest(request, config.getMaxUploadFileSize(), config.getCharset());
            param = new Param();
            param.putAll(formRequest.getParameters());
            // 把MultiFormRequest也打进Model对象中方便业务获取
            param.put(WebXConstants.REQUEST_PARAMETRIC_MULTIFORM_REQUEST, formRequest);
        } else {
            // 普通表单数据则直接解析
            param = WebUtil.getRequestParam(request, config.getCharset());
        }

        Map<String, String> uriVariables = (Map<String, String>) request.getAttribute(WebXConstants.REQUEST_ATTRIBUTE_VARIABLES);
        if (uriVariables != null) {
            param.putAll(uriVariables);
        }

        // 把请求流水号打进Model对象，方便在进行HTTP请求时也把流水号带上
        Object flowValue = request.getAttribute(WebXConstants.REQUEST_PARAMETRIC_FLOW);
        if (flowValue instanceof Long) {
            long flow = (long) flowValue;
            param.put(WebXConstants.REQUEST_PARAMETRIC_FLOW, flow);
        }

        // 把HttpRequest和HttpResponse也打进Model对象中
        param.put(WebXConstants.REQUEST_PARAMETRIC_REQUEST, request);
        param.put(WebXConstants.REQUEST_PARAMETRIC_RESPONSE, response);
        return param;
    }
}
