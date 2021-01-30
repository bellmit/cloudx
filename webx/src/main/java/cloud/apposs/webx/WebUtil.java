package cloud.apposs.webx;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.filterchain.http.server.HttpConstants;
import cloud.apposs.netkit.rxio.io.http.enctype.FormEnctypt;
import cloud.apposs.okhttp.FormEntity;
import cloud.apposs.okhttp.IORequest;
import cloud.apposs.rest.parameter.Parametric;
import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.MediaType;
import cloud.apposs.util.Param;
import cloud.apposs.util.StrUtil;
import cloud.apposs.util.SysUtil;
import cloud.apposs.webx.upload.FileUploadException;
import cloud.apposs.webx.upload.MultiFormRequest;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.List;

/**
 * WEB操作工具类
 */
public final class WebUtil {
    public static final String REQUEST_METHOD_PUT = "PUT";
    public static final String REQUEST_METHOD_POST = "POST";
    public static final String REQUEST_METHOD_DELETE = "DELTE";

    /**
     * 判断是否为AJAX请求
     */
    public static boolean isAJAX(HttpServletRequest request) {
        return request.getHeader("X-Requested-With") != null;
    }

    /**
     * 获取请求路径
     */
    public static String getRequestPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        if (StrUtil.isEmpty(pathInfo)) {
            pathInfo = "";
        }
        return servletPath + pathInfo;
    }

    /**
     * 从请求中获取所有参数（当参数名重复时，用后者覆盖前者）
     *
     * @param request 请求
     * @param charset 请求参数编码
     */
    @SuppressWarnings("unchecked")
    public static Param getRequestParam(HttpServletRequest request, String charset) throws IOException {
        Param param = new Param();
        String method = request.getMethod().toUpperCase();
        // HTTP请求为PUT/DELETE请求
        if (method.equals(REQUEST_METHOD_PUT) || method.equals(REQUEST_METHOD_DELETE)) {
            String queryString = null;
            try {
                queryString = URLDecoder.decode(SysUtil.getStringFromStream(request.getInputStream()), charset);
            } catch (Exception e) {
            }
            if (StrUtil.isEmpty(queryString)) {
                return param;
            }

            String[] queryArray = StrUtil.toStringArray(queryString, "&", true);
            for (int i = 0; i < queryArray.length; i++) {
                String query = queryArray[i];
                String[] keyVal = StrUtil.toStringArray(query, "=", true);
                if (keyVal.length != 2) {
                    continue;
                }
                String paramKey = keyVal[0];
                String paramValue = keyVal[1];
                param.put(paramKey, paramValue);
            }
            return param;
        }

        if (method.equalsIgnoreCase(REQUEST_METHOD_POST)) {
            String contentType = request.getContentType().toLowerCase();
            // HTTP请求为JSON POST请求
            if (contentType.contains(MediaType.APPLICATION_JSON.toString())) {
                StringBuilder builder = new StringBuilder();
                String line = null;
                BufferedReader reader = request.getReader();
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                Param value = JsonUtil.parseJsonParam(builder.toString());
                if (value != null) {
                    param.putAll(value);
                }
            }
        }

        // HTTP请求为普通GET请求
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramKey = paramNames.nextElement();
            String paramValue = request.getParameter(paramKey);
            param.put(paramKey, paramValue);
        }
        return param;
    }

    /**
     * 302临时跳转
     */
    public static void sendRedirect302(HttpServletRequest request,
                                       HttpServletResponse response, String url) {
        response.setHeader("Location", url);
        response.setStatus(302);
    }

    /**
     * 301永久跳转，对于google等有效，可以把页面权重转移
     */
    public static void sendRedirect301(HttpServletRequest request,
                                       HttpServletResponse response, String url) {
        response.setHeader("Location", url);
        response.setStatus(301);
    }

    /**
     * 转发请求
     */
    public static void sendForward(HttpServletRequest request,
                                   HttpServletResponse response, String url) throws ServletException, IOException {
        request.getRequestDispatcher(url).forward(request, response);
    }

    public static String getCookie(HttpServletRequest request, String key) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return "";
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(key)) {
                String value = cookie.getValue();
                if (!StrUtil.isEmpty(value)) {
                    return value;
                }
            }
        }
        return "";

    }

    public static void setCookie(String key, String value,
                                 HttpServletRequest request, HttpServletResponse response) {
        String domain = request.getHeader("host");
        setCookie(key, value, domain, "/", true, request, response);
    }

    /**
     * 设置Cookie
     */
    public static void setCookie(String key, String value,
                                 String domain, String path, boolean httpOnly,
                                 HttpServletRequest request, HttpServletResponse response) {
        StringBuilder cookie = new StringBuilder();
        cookie.append("Domain=").append(domain).append(";");
        cookie.append("Path=").append(path).append(";");
        if (httpOnly) {
            cookie.append("HttpOnly;");
        }
        response.setHeader("Set-Cookie", cookie.toString());
    }

    public static void response(HttpServletRequest request, HttpServletResponse response,
                MediaType contentType, String content) throws IOException {
        response(request, response, contentType, HttpConstants.DEFAULT_CHARSET, content, false);
    }

    public static void response(HttpServletRequest request, HttpServletResponse response,
                MediaType contentType, String content, boolean flush) throws IOException {
        response(request, response, contentType, HttpConstants.DEFAULT_CHARSET, content, flush);
    }

    /**
     * 响应输出，如果为flush则需要复用Servlet3.0的异步机制进行异步输出
     */
    public static void response(HttpServletRequest request, HttpServletResponse response,
                MediaType contentType, String charset, String content, boolean flush) throws IOException {
        if (flush) {
            AsyncContext context = null;
            try {
                context = (AsyncContext) request.getAttribute(WebXConstants.REQUEST_ATTRIBUTE_ASYNC);
                // 有可能底层出现异常（超时），request资源已经释放，不再响应输出
                if (context == null) {
                    return;
                }
                HttpServletResponse asyncResponse = (HttpServletResponse) context.getResponse();
                asyncResponse.setContentType(contentType + "; charset=" + charset);
                asyncResponse.getWriter().print(content);
            } finally {
                if (context != null) {
                    context.complete();
                }
            }
        } else {
            response.setContentType(contentType + "; charset=" + charset);
            response.getWriter().print(content);
        }
    }

    public static void response(HttpServletRequest request, HttpServletResponse response,
                                MediaType contentType, byte[] content) throws IOException {
        response(request, response, contentType, HttpConstants.DEFAULT_CHARSET, content, false);
    }

    public static void response(HttpServletRequest request, HttpServletResponse response,
                                MediaType contentType, byte[] content, boolean flush) throws IOException {
        response(request, response, contentType, HttpConstants.DEFAULT_CHARSET, content, flush);
    }

    /**
     * 响应输出，如果为flush则需要复用Servlet3.0的异步机制进行异步输出
     */
    public static void response(HttpServletRequest request, HttpServletResponse response,
                                MediaType contentType, String charset, byte[] content, boolean flush) throws IOException {
        if (flush) {
            AsyncContext context = null;
            try {
                context = (AsyncContext) request.getAttribute(WebXConstants.REQUEST_ATTRIBUTE_ASYNC);
                // 有可能底层出现异常（超时），request资源已经释放，不再响应输出
                if (context == null) {
                    return;
                }
                HttpServletResponse asyncResponse = (HttpServletResponse) context.getResponse();
                asyncResponse.setContentType(contentType + "; charset=" + charset);
                asyncResponse.getOutputStream().write(content);
            } finally {
                if (context != null) {
                    context.complete();
                }
            }
        } else {
            response.setContentType(contentType + "; charset=" + charset);
            response.getWriter().print(content);
        }
    }

    /**
     * 基于IoBuffer的字节码直接下载，会有JVM内存压力
     */
    public static void response(HttpServletRequest request, HttpServletResponse response,
                MediaType contentType, String charset, IoBuffer buffer, boolean flush) throws IOException {
        if (flush) {
            AsyncContext context = null;
            try {
                context = (AsyncContext) request.getAttribute(WebXConstants.REQUEST_ATTRIBUTE_ASYNC);
                // 有可能底层出现异常（超时），request资源已经释放，不再响应输出
                if (context == null) {
                    return;
                }
                HttpServletResponse asyncResponse = (HttpServletResponse) context.getResponse();
                asyncResponse.setContentType(contentType + "; charset=" + charset);
                asyncResponse.getWriter().print(buffer.array());
            } finally {
                if (context != null) {
                    context.complete();
                }
            }
        } else {
            response.setContentType(contentType + "; charset=" + charset);
            response.getWriter().print(buffer.array());
        }
    }

    /**
     * 基于文件的直接下载，
     * 参考：https://www.cnblogs.com/xdp-gacl/p/3789624.html
     */
    public static void response(HttpServletRequest request, HttpServletResponse response,
                MediaType contentType, String charset, File file, boolean flush) throws IOException {
        if (flush) {
            AsyncContext context = null;
            InputStream in = null;
            try {
                context = (AsyncContext) request.getAttribute(WebXConstants.REQUEST_ATTRIBUTE_ASYNC);
                // 有可能底层出现异常request资源已经释放，不再响应输出
                if (context == null) {
                    return;
                }
                HttpServletResponse asyncResponse = (HttpServletResponse) context.getResponse();
                asyncResponse.setContentType(contentType + "; charset=" + charset);
                asyncResponse.setCharacterEncoding(charset);
                // 设置响应Header
                for (String key : response.getHeaderNames()) {
                    asyncResponse.setHeader(key, response.getHeader(key));
                }
                // 获取要下载的文件输入流
                in = new FileInputStream(file.getPath());
                int len = 0;
                // 创建数据缓冲区，注意缓存区太大会有JVM内存不足，太小则下载速度不够快
                byte[] buffer = new byte[512 * 1024];
                // 通过response对象获取OutputStream流
                OutputStream out = asyncResponse.getOutputStream();
                // 将FileInputStream流写入到buffer缓冲区
                while ((len = in.read(buffer)) > 0) {
                    // 使用OutputStream将缓冲区的数据输出到客户端浏览器
                    out.write(buffer,0, len);
                }
            } finally {
                if (context != null) {
                    context.complete();
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        } else {
            response.setContentType(contentType + "; charset=" + charset);
            response.setCharacterEncoding(charset);
            InputStream in = null;
            try {
                in = new FileInputStream(file.getPath());
                int len = 0;
                byte[] buffer = new byte[512 * 1024];
                OutputStream out = response.getOutputStream();
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer,0, len);
                }
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }
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
        formEntity.add(WebXConstants.REQUEST_PARAMETER_FLOW, parametric.getFlow());
        return formEntity;
    }

    /**
     * 构造带流水号请求的请求表单，
     * 主要服务于是非Parametric参数接口的业务
     *
     * @param  flow 流水号
     * @param  formEnctype 表单类型，默认为JSON表单类型
     * @return 请求表单
     */
    public static FormEntity buildFormEntity(int flow, int formEnctype) throws IOException {
        FormEntity formEntity = FormEntity.builder(formEnctype);
        formEntity.add(WebXConstants.REQUEST_PARAMETER_FLOW, flow);
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

    /**
     * 获取文件表单数据
     */
    public static MultiFormRequest getFormRequest(HttpServletRequest request) throws FileUploadException {
        return getFormRequest(request, -1, "utf-8");
    }

    /**
     * 获取文件表单数据
     * 参考：https://www.cnblogs.com/xdp-gacl/p/4200090.html
     *
     * @param request HTTP请求
     * @param maxFileSize 最大接收文件大小，-1则为不限制
     * @param charset 请求解析解码
     */
    public static MultiFormRequest getFormRequest(
            HttpServletRequest request, long maxFileSize, String charset) throws FileUploadException {
        try {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            // 设置最大文件上传值
            upload.setFileSizeMax(maxFileSize);
            // 设置最大请求值 (包含文件和表单数据)
            upload.setSizeMax(maxFileSize);
            // 使用ServletFileUpload解析器解析上传数据，
            // 解析结果返回的是一个List<FileItem>集合，每一个FileItem对应一个Form表单的输入项
            List<FileItem> fileItemList = upload.parseRequest(request);
            MultiFormRequest formRequest = new MultiFormRequest();
            for (FileItem fileItem : fileItemList) {
                if (fileItem.isFormField()) {
                    // 表单数据为表单字段
                    String name = fileItem.getFieldName();
                    String value = fileItem.getString(charset);
                    formRequest.addParameter(name, value);
                } else {
                    // 表单数据为上传文件
                    // 得到上传的文件名称，注意：不同的浏览器提交的文件名是不一样的，
                    // 有些浏览器提交上来的文件名是带有路径的，如：c:\a\b\1.txt，而有些只是单纯的文件名，如：1.txt
                    String fileName = fileItem.getFieldName();
                    if (StrUtil.isEmpty(fileName)) {
                        continue;
                    }
                    formRequest.addFile(fileName, fileItem);
                }
            }
            return formRequest;
        } catch (Exception e) {
            throw new FileUploadException(e);
        }
    }
}
