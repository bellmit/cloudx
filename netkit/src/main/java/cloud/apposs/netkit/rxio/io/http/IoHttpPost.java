package cloud.apposs.netkit.rxio.io.http;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.buffer.ByteBuf;
import cloud.apposs.util.StrUtil;

import java.io.IOException;
import java.net.Proxy;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Http Post请求，
 * 参考：https://www.jianshu.com/p/4f9e79eb0163
 */
public class IoHttpPost extends IoHttp {
    public IoHttpPost(String url) throws Exception {
        super(url, HttpMethod.POST);
    }

    public IoHttpPost(String url, Proxy proxy) throws Exception {
        super(url, HttpMethod.POST, proxy);
    }

    protected IoHttpPost(String url, HttpMethod method) throws Exception {
        super(url, method);
    }

    protected IoHttpPost(String url, HttpMethod method, Proxy proxy) throws Exception {
        super(url, method, proxy);
    }

    @Override
    protected IoBuffer[] doWrapRequest() throws IOException {
        StringBuilder request = new StringBuilder(128);
        String path = uri.getPath();
        if (StrUtil.isEmpty(path)) {
            path = "/";
        }

        // URL带参和方法带参都支持一起POST传递
        String query = uri.getRawQuery();
        if (!StrUtil.isEmpty(query)) {
            String[] urlArgs = query.split("&");
            for (int i = 0; i < urlArgs.length; i++) {
                String urlArg = urlArgs[i];
                String[] argKeyValue = urlArg.split("=");
                if (argKeyValue.length != 2) {
                    continue;
                }
                if (form.hasParameter(argKeyValue[0])) {
                    continue;
                }
                form.add(argKeyValue[0], argKeyValue[1]);
            }
        }

        long contentLength = form.getContentLength();
        if (contentLength > 0) {
            headers.put("Content-Length", Long.toString(contentLength));
        }
        // 生成Header头
        request.append(method).append(" " + path + " ");
        request.append(HTTP_PROTOCL_1 + CRLF);
        request.append("User-Agent: " + userAgent + CRLF);
        if (!headers.containsKey("Content-Type")) {
            headers.put("Content-Type", form.getContentType());
        }
        if (!headers.containsKey("Host")) {
            headers.put("Host", uri.getHost());
        }
        if (!headers.containsKey("Accept")) {
            headers.put("Accept", "*/*");
        }
        Set<Entry<String, String>> headerList = headers.entrySet();
        for (Entry<String, String> keyVal : headerList) {
            request.append(keyVal.getKey() + ": " + keyVal.getValue() + CRLF);
        }
        request.append(CRLF);

        // 表单POST数据编码
        List<IoBuffer> bufferList = new LinkedList<IoBuffer>();
        IoBuffer part1 = ByteBuf.wrap(request.toString(), "utf-8");
        bufferList.add(part1);
        form.addRequestBuffer(bufferList);
        IoBuffer[] buffers = new IoBuffer[bufferList.size()];
        bufferList.toArray(buffers);
        return buffers;
    }
}
