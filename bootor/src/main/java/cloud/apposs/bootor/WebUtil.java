package cloud.apposs.bootor;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.buffer.ReadOnlyBuf;
import cloud.apposs.netkit.filterchain.http.server.HttpConstants;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.netkit.rxio.io.http.enctype.FormEnctypt;
import cloud.apposs.okhttp.FormEntity;
import cloud.apposs.okhttp.IORequest;
import cloud.apposs.rest.parameter.Parametric;
import cloud.apposs.util.HttpStatus;
import cloud.apposs.util.MediaType;

import java.io.File;
import java.io.IOException;

public final class WebUtil {
    /**
     * 获取请求路径
     */
    public static String getRequestPath(HttpRequest request) {
        String servletPath = request.getRequestUri();
        return servletPath;
    }

    /**
     * 301永久跳转，对于google等有效，可以把页面权重转移
     */
    public static void sendRedirect301(HttpResponse response, String url) {
        response.putHeader("Location", url);
        response.setStatus(HttpStatus.HTTP_STATUS_301);
    }

    /**
     * 302临时跳转
     */
    public static void sendRedirect302(HttpResponse response, String url) {
        response.putHeader("Location", url);
        response.setStatus(HttpStatus.HTTP_STATUS_302);
    }

    /**
     * 字符串响应输出
     */
    public static void response(HttpResponse response, MediaType contentType, String content) throws IOException {
        response(response, contentType, HttpConstants.DEFAULT_CHARSET, content, false);
    }

    /**
     * 字符串响应输出
     */
    public static void response(HttpResponse response, MediaType contentType,
                                String content, boolean flush) throws IOException {
        response(response, contentType, HttpConstants.DEFAULT_CHARSET, content, flush);
    }

    /**
     * 字符串响应输出
     */
    public static void response(HttpResponse response, MediaType contentType,
                                String charset, String content, boolean flush) throws IOException {
        response.setContentType(contentType.getType() + "; charset=" + charset);
        response.write(content, flush);
    }

    /**
     * 字节码响应输出
     */
    public static void response(HttpResponse response, MediaType contentType,
                                String charset, byte[] content, boolean flush) throws IOException {
        response.setContentType(contentType.getType() + "; charset=" + charset);
        response.write(content, flush);
    }

    /**
     * 字节码响应输出
     */
    public static void response(HttpResponse response, MediaType contentType,
                                String charset, IoBuffer buffer, boolean flush) throws IOException {
        response.setContentType(contentType.getType() + "; charset=" + charset);
        response.write(buffer, flush);
    }

    /**
     * 流媒体文件响应输出，采用数据零拷贝输出到网络
     */
    public static void response(HttpResponse response, MediaType contentType,
                                String charset, File file, boolean flush) throws IOException {
        response.setContentType(contentType.getType() + "; charset=" + charset);
        IoBuffer buffer = ReadOnlyBuf.wrap(file);
        response.write(buffer, flush);
    }

    /**
     * 根据Model对象构造请求表单
     *
     * @param  parametric Model对象
     * @return 请求表单
     */
    public static FormEntity buildFormEntity(Parametric parametric) throws IOException {
        return buildFormEntity(parametric, FormEnctypt.FORM_ENCTYPE_JSON);
    }

    /**
     * 根据Model对象构造请求表单
     *
     * @param  parametric Model对象
     * @param  formEnctype 表单类型，默认为JSON表单类型
     * @return 请求表单
     */
    public static FormEntity buildFormEntity(Parametric parametric, int formEnctype) throws IOException {
        if (parametric == null) {
            throw new IllegalArgumentException("parametric");
        }
        FormEntity formEntity = FormEntity.builder(formEnctype);
        formEntity.add(BootorConstants.REQUEST_PARAMETER_FLOW, parametric.getFlow());
        return formEntity;
    }

    /**
     * 根据Model对象构造请求体
     *
     * @param  serviceId 服务注册实例ID
     * @param  url 请求URL
     * @return 请求体
     */
    public static IORequest buildIORequest(String serviceId, String url) {
        return IORequest.builder().serviceId(serviceId).url(serviceId + url);
    }
}
