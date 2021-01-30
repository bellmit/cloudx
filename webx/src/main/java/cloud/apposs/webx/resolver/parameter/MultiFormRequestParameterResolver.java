package cloud.apposs.webx.resolver.parameter;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.rest.parameter.Parameter;
import cloud.apposs.rest.parameter.ParameterResolver;
import cloud.apposs.rest.parameter.RequestParameterMissingException;
import cloud.apposs.webx.WebUtil;
import cloud.apposs.webx.WebXConfig;
import cloud.apposs.webx.WebXConstants;
import cloud.apposs.webx.upload.MultiFormRequest;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class MultiFormRequestParameterResolver implements ParameterResolver<HttpServletRequest, HttpServletResponse> {
    @Override
    public boolean supportsParameter(Parameter parameter) {
        return MultiFormRequest.class.isAssignableFrom(parameter.getType());
    }

    @Override
    public Object resolveArgument(
            Parameter parameter, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 判断上传文件的内容是否为 multipart 类型
        boolean multipart = ServletFileUpload.isMultipartContent(request);
        if (!multipart) {
            throw new RequestParameterMissingException(parameter);
        }
        WebXConfig config = (WebXConfig) request.getAttribute(WebXConstants.REQUEST_ATTRIBUTE_WEBXCONFIG);
        return WebUtil.getFormRequest(request, config.getMaxUploadFileSize(), config.getCharset());
    }
}
