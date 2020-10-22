package cloud.apposs.netkit.rxio.io.http;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.buffer.ByteBuf;
import cloud.apposs.util.StrUtil;

import java.io.IOException;
import java.net.Proxy;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Http GET/POST/PUT/DELETE多种请求支持
 */
public class IoHttpMultipy extends IoHttp {
    public IoHttpMultipy(String url, HttpMethod method) throws Exception {
        super(url, method);
    }

    public IoHttpMultipy(String url, HttpMethod method, Proxy proxy) throws Exception {
        super(url, method, proxy);
    }

    @Override
    protected IoBuffer[] doWrapRequest() throws IOException {
        boolean isFormEntity = isFormEntity();
        StringBuilder request = new StringBuilder(128);
        String path = doGetPath();
        request.append(method).append(" " + path + " ");
        request.append(HTTP_PROTOCL_1 + CRLF);
        request.append("User-Agent: " + userAgent + CRLF);
        // 生成Header头
        if (isFormEntity) {
            long contentLength = form.getContentLength();
            if (contentLength > 0) {
                headers.put("Content-Length", Long.toString(contentLength));
                // 生成Header头
                if (!headers.containsKey("Content-Type")) {
                    headers.put("Content-Type", form.getContentType());
                }
            }
        }
        if (!headers.containsKey("Host")) {
            headers.put("Host", uri.getHost());
        }
        if (!headers.containsKey("Accept")) {
            headers.put("Accept", "*/*");
        }
        Set<Map.Entry<String, String>> headerList = headers.entrySet();
        for (Map.Entry<String, String> keyVal : headerList) {
            request.append(keyVal.getKey() + ": " + keyVal.getValue() + CRLF);
        }
        request.append(CRLF);
        // 生成表单提交数据
        List<IoBuffer> bufferList = new LinkedList<IoBuffer>();
        IoBuffer part1 = ByteBuf.wrap(request.toString(), "utf-8");
        bufferList.add(part1);
        if (isFormEntity) {
            form.addRequestBuffer(bufferList);
        }
        IoBuffer[] buffers = new IoBuffer[bufferList.size()];
        bufferList.toArray(buffers);
        return buffers;
    }

    private boolean isFormEntity() {
        return method.equals(HttpMethod.POST) || method.equals(HttpMethod.PUT) || method.equals(HttpMethod.DELETE);
    }

    private String doGetPath() {
        if (method.equals(HttpMethod.GET)) {
            String path = uri.getPath();
            if (StrUtil.isEmpty(path)) {
                path = "/";
            }

            // URL带参和方法带参都支持一起GET传递
            boolean hasQuery = false;
            String query = uri.getRawQuery();
            if (!StrUtil.isEmpty(query)) {
                hasQuery = true;
                path += "?" + query;
            }
            if (form != null) {
                Map<String, Object> arguments = form.getArgs();
                int total = arguments.size();
                if (total > 0) {
                    if (!hasQuery) {
                        path += "?";
                    } else {
                        path += "&";
                    }
                }
                StringBuilder argsQuery = new StringBuilder(64);
                int current = 0;
                for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                    argsQuery.append(entry.getKey()).append("=").append(entry.getValue());
                    if (++current < total) {
                        argsQuery.append("&");
                    }
                }
                path += argsQuery.toString();
            }
            return path;
        }
        String path = uri.getPath();
        if (StrUtil.isEmpty(path)) {
            path = "/";
        }
        String query = uri.getRawQuery();
        if (!StrUtil.isEmpty(query)) {
            path += "?" + query;
        }
        return path;
    }
}
