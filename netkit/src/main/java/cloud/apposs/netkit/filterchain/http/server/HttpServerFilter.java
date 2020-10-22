package cloud.apposs.netkit.filterchain.http.server;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.WriteRequest;
import cloud.apposs.netkit.filterchain.IoFilterAdaptor;
import cloud.apposs.netkit.filterchain.http.server.template.DefaultHttpTemplate;
import cloud.apposs.netkit.filterchain.http.server.template.HttpTemplate;
import cloud.apposs.util.HttpStatus;
import cloud.apposs.util.LineBuilder;
import cloud.apposs.util.Parser;
import cloud.apposs.util.StrUtil;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Map;

/**
 * HTTP服务端协议过滤器
 */
public class HttpServerFilter extends IoFilterAdaptor {
    public static final String FILTER_CONTEXT = "HttpServerFilterContext";
    public static final float HTTP_PROTOCOL_1_0 = 1.0F;
    public static final float HTTP_PROTOCOL_1_1 = 1.1F;

    /**
     * 是否将HTTP请求的HEADER KEY自动转换成小写，便于提升性能
     */
    private boolean lowerHeaderKey = false;

    /**
     * HTTP错误输出模板
     */
    private HttpTemplate template;
    private String charset;

    public HttpServerFilter() {
        this(new DefaultHttpTemplate(), HttpConstants.DEFAULT_CHARSET, false);
    }

    public HttpServerFilter(String charset) {
        this(new DefaultHttpTemplate(), charset, false);
    }

    public HttpServerFilter(String charset, boolean lowerHeaderKey) {
        this(new DefaultHttpTemplate(), charset, lowerHeaderKey);
    }

    public HttpServerFilter(HttpTemplate template, String charset, boolean lowerHeaderKey) {
        if (template == null) {
            template = new DefaultHttpTemplate();
        }
        this.template = template;
        this.charset = charset;
        this.lowerHeaderKey = lowerHeaderKey;
    }

    @Override
    public void channelRead(NextFilter nextFilter, IoProcessor processor, Object message) throws Exception {
        if (!(message instanceof IoBuffer)) {
            nextFilter.channelRead(processor, message);
            return;
        }

        // 更新会话时间，避免当上传HTTP大文件时超时
        processor.setActionTime(System.currentTimeMillis());
        // 解析HEADER请求数据
        IoBuffer buffer = (IoBuffer) message;
        Context context = getContext(processor);
        // 解析HEADER数据已经结束，但仍然有数据进来，
        // 可能是POST BODY数据还没解析，直接交给下一个过滤器处理
        if (context.isParseComplete()) {
            HttpRequest request = context.getRequest();
            request.setContent(buffer);
            nextFilter.channelRead(processor, request);
            return;
        }

        if (context.parseRequest(buffer)) {
            // 数据解析完整才给下一个链执行，否则继续读取数据
            nextFilter.channelRead(processor, context.getRequest());
        }
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoProcessor processor, IoBuffer buffer) throws Exception {
        // 更新会话时间，避免当下载HTTP大文件时超时
        processor.setActionTime(System.currentTimeMillis());
        nextFilter.filterWrite(processor, buffer);
    }

    /**
     * 数据发送完毕，重置上下文数据，方便在HTTP长连接中下次重新发起请求
     */
    @Override
    public void channelSend(NextFilter nextFilter, IoProcessor processor, WriteRequest writeRequest) throws Exception {
        Context context = (Context) processor.getAttribute(FILTER_CONTEXT);
        if (context != null) {
            if (context.getProtocol() == HTTP_PROTOCOL_1_0) {
                processor.close(true);
            }
            context.reset();
        }
        nextFilter.channelSend(processor, writeRequest);
    }

    @Override
    public void channelClose(NextFilter nextFilter, IoProcessor processor) {
        Context context = (Context) processor.getAttribute(FILTER_CONTEXT);
        if (context != null) {
            context.release();
        }
        nextFilter.channelClose(processor);
    }

    @Override
    public void exceptionCaught(NextFilter nextFilter, IoProcessor processor, Throwable cause) {
        try {
            doGenerateTemplateError(processor, cause);
        } catch (Throwable ignore) {
        }
        super.exceptionCaught(nextFilter, processor, cause);
    }

    public void setTemplate(HttpTemplate template) {
        this.template = template;
    }

    private Context getContext(IoProcessor processor) throws IOException {
        Context context = (Context) processor.getAttribute(FILTER_CONTEXT);

        if (context == null) {
            SocketAddress remoteAddr = processor.getChannel().getRemoteSocketAddress();
            context = new Context(remoteAddr, charset, lowerHeaderKey);
            processor.setAttribute(FILTER_CONTEXT, context);
        }

        return context;
    }

    /**
     * 服务器端处理异常，输出相关异常信息给前端
     */
    private void doGenerateTemplateError(IoProcessor processor, Throwable error) throws IOException {
        String message = template.generateTemplate(error);
        processor.write(message);
        processor.close(false);
    }

    /**
     * 一个请求就是一个上下文实例
     */
    public static final class Context {
        /**
         * 解析得到的请求数据
         */
        private final HttpRequest request;

        /**
         * 请求解析器
         */
        private final HttpHeaderDecoder decoder;

        private float protocol = HTTP_PROTOCOL_1_0;

        public Context(SocketAddress remoteAddr, String charset, boolean lowerHeaderKey) throws IOException {
            this.request = new HttpRequest(remoteAddr);
            this.decoder = new HttpHeaderDecoder(this, charset, lowerHeaderKey);
        }

        public HttpRequest getRequest() {
            return request;
        }

        public float getProtocol() {
            return protocol;
        }

        public void setProtocol(float protocol) {
            this.protocol = protocol;
        }

        public boolean isParseComplete() {
            return decoder.getStatus() == HttpHeaderDecoder.PARSE_STATUS_FINISH;
        }

        /**
         * 解析客户端请求过来的数据
         *
         * @return 全部解析结束返回true
         * @throws Exception 解析有异常时抛出异常并在前端输出HTTP错误提示
         */
        public boolean parseRequest(IoBuffer stream) throws Exception {
            return decoder.parseRequest(stream);
        }

        public void reset() {
            if (decoder != null) {
                decoder.reset();
            }
        }

        public void release() {
            if (request != null) {
                request.release();
            }
        }
    }

    /**
     * HTTP请求解析，包括解析请求纯文本请求和表单提交的大数据分段请求
     */
    public static final class HttpHeaderDecoder {
        /** 解析请求行的各种状态 */
        /**
         * 初始状态，解析http协议
         */
        public static final int PARSE_STATUS_ROTOCOL = 0;
        /**
         * 解析http请求头
         */
        public static final int PARSE_STATUS_HEADER = 1;
        /**
         * 解析http请求头结束
         */
        public static final int PARSE_STATUS_FINISH = 2;

        public static final int DEFAULT_LINE_LENGTH = 128;

        /**
         * 当前解析状态
         */
        private int status = PARSE_STATUS_ROTOCOL;

        private final Context context;

        /**
         * 解析出来的当前行
         */
        private final LineBuilder currentLine;

        /**
         * 是否将HTTP请求的HEADER KEY自动转换成小写，便于提升性能
         */
        private boolean lowerHeaderKey = false;

        public HttpHeaderDecoder(Context context, String charset, boolean lowerHeaderKey) {
            this.context = context;
            this.currentLine = new LineBuilder(DEFAULT_LINE_LENGTH, charset);
            this.lowerHeaderKey = lowerHeaderKey;
        }

        public int getStatus() {
            return status;
        }

        /**
         * 解析请求HEADER行
         */
        public boolean parseRequest(IoBuffer buffer) throws Exception {
            String line = null;
            HttpRequest request = context.getRequest();
            switch (status) {
                case PARSE_STATUS_ROTOCOL:
                    // 解析请求协议，即：GET / HTTP/1.1
                    line = doParseLine(buffer);
                    if (line == null) {
                        // 没解析到完整行，退出等待下次数据请求过来
                        return false;
                    }

                    // 此时可能是HTTP KEEPALIVE长连接，重复请求前先把上一个请求的上下文数据重置
                    context.release();

                    // 获取解析协议
                    String[] protocols = line.split("\\s+");
                    if (protocols.length < 3) {
                        throw new HttpParseException(HttpStatus.HTTP_STATUS_400);
                    }
                    doInitRequestProtocol(request, protocols);
                    doInitContextProtocol(request);
                    status = PARSE_STATUS_HEADER;
                case PARSE_STATUS_HEADER:
                    // 解析请求头，即：Host: FKW.COM
                    do {
                        line = doParseLine(buffer);
                        if (line == null) {
                            // 没解析到完整行，退出等待下次数据请求过来
                            return false;
                        }
                        String[] arrs = line.split(":");
                        if (arrs.length >= 2) {
                            String headerKey = arrs[0].trim();
                            if (lowerHeaderKey) {
                                headerKey = headerKey.toLowerCase();
                            }
                            request.putHeader(headerKey, StrUtil.joinArrayString(arrs, ":", 1));
                        }
                    } while (line.length() > 0);
                    // 已经解析到CRLF最后的空行，Header已经解析完毕，如果有POST BODY数据则交给下一个过滤器处理
                    if (buffer.hasReadableBytes()) {
                        request.setContent(buffer);
                    }
                    status = PARSE_STATUS_FINISH;
            }
            return true;
        }

        /**
         * 解析请求行，成功解析到一行返回true，否则返回false
         */
        private String doParseLine(IoBuffer buffer) throws Exception {
            long index = buffer.readIdx();
            long total = buffer.writeIdx();

            for (; index < total; index++) {
                byte letter = buffer.get();
                if (letter == HttpConstants.CR) {
                    continue;
                }
                if (letter == HttpConstants.LF) {
                    String line = currentLine.toString();
                    currentLine.setLength(0);
                    return line;
                }
                if (currentLine.length() > HttpConstants.MAX_HEADER_LINE) {
                    throw new HttpParseException(HttpStatus.HTTP_STATUS_400, "Max Header Line");
                }
                currentLine.append(letter);
            }

            return null;
        }

        /**
         * 解析请求协议，即：GET / HTTP/1.1
         */
        private void doInitRequestProtocol(HttpRequest request, String[] protocols) {
            String method = protocols[0];
            String uri = protocols[1];
            String protocol = protocols[2];
            String path = uri;
            int pathEndPos = uri.indexOf('?');
            if (pathEndPos > 0) {
                path = uri.substring(0, pathEndPos);
                Map<String, String> parameters = request.getParameters();
                String[] arguments = uri.substring(path.length() + 1).split("&");
                for (int i = 0; i < arguments.length; i++) {
                    String argument = arguments[i];
                    String[] variables = argument.split("=");
                    if (variables.length == 2) {
                        parameters.put(variables[0].trim(), variables[1].trim());
                    }
                }
            }
            request.setMethod(method);
            request.setProtocol(protocol);
            request.setRequestUri(path);
        }

        private void doInitContextProtocol(HttpRequest request) {
            String protocol = request.getProtocol();
            int position = protocol.indexOf("/");
            if (position > 0) {
                context.setProtocol(Parser.parseFloat(protocol.substring(position + 1), HTTP_PROTOCOL_1_0));
            }
        }

        /**
         * 重置解析状态，
         * 等待HTTP KEEPALIVE长连接时下次新请求进行解析
         */
        public void reset() {
            status = PARSE_STATUS_ROTOCOL;
        }
    }
}
