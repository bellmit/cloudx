package cloud.apposs.netkit.filterchain.http.server.decrypt;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.filterchain.http.server.HttpConstants;
import cloud.apposs.netkit.filterchain.http.server.HttpFormFile;
import cloud.apposs.netkit.filterchain.http.server.HttpParseException;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.util.HttpStatus;
import cloud.apposs.util.LineBuilder;
import cloud.apposs.util.Pair;
import cloud.apposs.util.StrUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Form表单DATA解码，主要用于上传文件，一个请求对应一个表单解码器，
 * 参考：https://www.jianshu.com/p/4f9e79eb0163
 * 示例：
 * <pre>
 * Content-Length: 11506
 * Content-Type:multipart/form-data; boundary=----WebKitFormBoundaryrGKCBY7qhFd3TrwA
 *
 * ------WebKitFormBoundaryrGKCBY7qhFd3TrwA
 * Content-Disposition: form-data; name="usrname"
 *
 * QUN
 * ------WebKitFormBoundaryrGKCBY7qhFd3TrwA
 * Content-Disposition: form-data; name="myfile"; filename="LibAntiPrtSc_INFORMATION.log"
 * Content-Type: application/octet-stream
 *
 * <FILE DATA HERE>
 * ------WebKitFormBoundaryrGKCBY7qhFd3TrwA
 * Content-Disposition: form-data; name="mypic"; filename="profile.png"
 * Content-Type: image/png
 *
 * <PNG DATA HERE>
 * ------WebKitFormBoundaryrGKCBY7qhFd3TrwA--
 * </pre>
 */
public class FormDataDectypt implements FormDecrypt {
    private static final int DEFAULT_LINE_KEY_LENGTH = 128;
    private static final int DEFAULT_LINE_VALUE_LENGTH = 256;

    /**
     * 解析表单数据的各种状态
     */
    public static final int FORM_READ_INIT = 0;
    public static final int FORM_READ_FIELD = 1;
    public static final int FORM_READ_VALUE = 2;
    public static final int FORM_READ_FILE = 3;

    /**
     * 解析表单HEADER的各种状态
     */
    public static final int FORM_DECRYPT_START = 0;
    public static final int FORM_DECRYPT_FIELD = 1;
    public static final int FORM_DECRYPT_DISPOSITION = 2;

    /**
     * 文件上传时临时文件相关配置
     */
    private final int threshold;
    private final File directory;

    private final byte[] boundary;

    private int status = FORM_READ_INIT;
    private int fieldStatus = FORM_DECRYPT_START;

    /**
     * 数据解析缓冲区，提升性能，避免频繁的网络数据读取
     */
    private byte[] currentBuffer;
    /**
     * 缓冲区要读取的下一个位置
     */
    private int nextReadBufferIndex = 0;
    /**
     * 当前缓冲区写入的位置
     */
    private int currentWriteBufferLength = 0;

    private final Map<String, String> currentHeaders;
    private final LineBuilder currentKey;
    private final LineBuilder currentValue;

    /**
     * 表单解析全部完毕
     */
    private static final Boolean PARSE_FORM_COMPLETED = new Boolean(true);

    public FormDataDectypt(String contentType, String charset,
                           int bufferSize, int threshold, File directory) throws Exception {
        if (StrUtil.isEmpty(contentType) || contentType.indexOf(";") == -1) {
            throw new HttpParseException(HttpStatus.HTTP_STATUS_400, "Content-Type not set");
        }
        String boundary = doGetRequestBoundary(contentType);
        if (StrUtil.isEmpty(boundary)) {
            throw new HttpParseException(HttpStatus.HTTP_STATUS_400, "Multipart Boundary not set");
        }

        this.threshold = threshold;
        this.directory = directory;
        byte[] boundaries = boundary.getBytes(charset);
		int boundaryPrefixLength = HttpConstants.BOUNDARY_PREFIX.length;
        this.boundary = new byte[boundaries.length + boundaryPrefixLength];
        System.arraycopy(HttpConstants.BOUNDARY_PREFIX, 0, this.boundary, 0, boundaryPrefixLength);
        System.arraycopy(boundaries, 0, this.boundary, boundaryPrefixLength, boundaries.length);
        this.currentBuffer = new byte[Math.max(bufferSize, boundaries.length * 2)];
        this.currentHeaders = new HashMap<String, String>();
        this.currentKey = new LineBuilder(DEFAULT_LINE_KEY_LENGTH, charset);
        this.currentValue = new LineBuilder(DEFAULT_LINE_VALUE_LENGTH, charset);
    }

    /**
     * 开始解析表单数据，解析阶段如下
     * <pre>
     * Content-Length: 11506
     * Content-Type:multipart/form-data; boundary=----WebKitFormBoundaryrGKCBY7qhFd3TrwA
     *
     * ------WebKitFormBoundaryrGKCBY7qhFd3TrwA -- [[FORM_READ_INIT:doParseBoundary here]]
     * Content-Disposition: form-data; name="usrname" -- [[FORM_READ_HEADER:doParseFormHeader here]]
     *
     * QUN -- [[FORM_READ_VALUE:doParseFormValue here]]
     * ------WebKitFormBoundaryrGKCBY7qhFd3TrwA
     * Content-Disposition: form-data; name="myfile"; filename="LibAntiPrtSc_INFORMATION.log" -- [[FORM_READ_FIELD:doParseFormHeader here]]
     * Content-Type: application/octet-stream
     *
     * <FILE DATA HERE> -- [[FORM_READ_VALUE:doParseFormValue here]]
     * ------WebKitFormBoundaryrGKCBY7qhFd3TrwA
     * Content-Disposition: form-data; name="mypic"; filename="profile.png" -- [[FORM_READ_FIELD:doParseFormHeader here]]
     * Content-Type: image/png
     *
     * <PNG DATA HERE> -- [[FORM_READ_VALUE:doParseFormValue here]]
     * ------WebKitFormBoundaryrGKCBY7qhFd3TrwA--
     * </pre>
     */
    @Override
    public boolean parseForm(HttpRequest request) throws Exception {
        Boolean success = null;
        boolean readMore = true;
        IoBuffer buffer = request.getContent();
        for (; ; ) {
            if (readMore && doReadFromBuffer(buffer) == -1) {
                // 已经没有可读取的数据，退出等待接收下次网络数据
                return false;
            }
            switch (status) {
                case FORM_READ_INIT: // ------WebKitFormBoundary7MA4YWxkTrZu0gW
                    success = doParseBoundary(buffer);
                    if (success == null) {
                        // 没解析到完整行，退出等待下次数据请求过来
                        readMore = true;
                        break;
                    }
                    // 表单boundary没匹配上，视为无效请求
                    if (!success) {
                        throw new HttpParseException(HttpStatus.HTTP_STATUS_400, "Invalid Http Boundary");
                    }
                    status = FORM_READ_FIELD;
                    readMore = false;
                    continue;
                case FORM_READ_FIELD: // Content-Disposition: form-data; name="usrname"
                    success = doParseFormField(buffer);
                    if (success == null) {
                        // 没解析到完整数据，继续读取网络数据
                        readMore = true;
                        break;
                    }
                    // 表单字段没正确以CRLFCRLF结尾，视为无效请求
                    if (!success) {
                        throw new HttpParseException(HttpStatus.HTTP_STATUS_400, "Invalid Http Boundary");
                    }
                    if (currentHeaders.containsKey("content-type")) {
                        status = FORM_READ_FILE; // <FIELD VALUE>
                    } else {
                        status = FORM_READ_VALUE; // <FILE DATA>
                    }
                    readMore = false;
                    continue;
                case FORM_READ_VALUE:
                    success = doParseFormValue(request, buffer);
                    if (success == null) {
                        // 没解析到完整数据，继续读取网络数据
                        readMore = true;
                        break;
                    }
                    // 表单值没正确以BOUNDARY+CRLF结尾，视为无效请求
                    if (!success) {
                        throw new HttpParseException(HttpStatus.HTTP_STATUS_400, "Invalid Http Boundary");
                    }
                    // 表单全部解析完毕
                    if (success == PARSE_FORM_COMPLETED) {
                        return true;
                    }
                    // 表单其中一个字段数据已经解析完毕，状态更改为从FIELD开始解析
                    status = FORM_READ_FIELD;
                    readMore = false;
                    continue;
                case FORM_READ_FILE:
                    success = doParseFormFile(request);
                    if (success == null) {
                        // 没解析到完整数据，继续读取网络数据
                        readMore = true;
                        break;
                    }
                    // 表单值没正确以BOUNDARY+CRLF结尾，视为无效请求
                    if (!success) {
                        throw new HttpParseException(HttpStatus.HTTP_STATUS_400, "Invalid Http Boundary");
                    }
                    // 表单全部解析完毕
                    if (success == PARSE_FORM_COMPLETED) {
                        return true;
                    }
                    // 表单其中一个字段数据已经解析完毕，状态更改为从FIELD开始解析
                    status = FORM_READ_FIELD;
                    readMore = false;
                    continue;
            }
        }
    }

    /**
     * 解析请求BOUNDARY，
     * 成功解析到BOUNDARY返回TRUE，数据还没彻底解析完返回NULL，否则返回FALSE
     */
    private Boolean doParseBoundary(IoBuffer buffer) throws Exception {
        // 还没读取到完整行，退出等待下次数据进来
        if (boundary.length > currentWriteBufferLength) {
            return null;
        }
        for (int match = 2, i = nextReadBufferIndex; i < currentWriteBufferLength; i++) {
            byte letter = currentBuffer[i];
            // 没匹配上，证明一开始就不是严格以BOUNDARY开头的，视为非法请求
            if (letter != boundary[match]) {
                return Boolean.FALSE;
            }
            // 匹配上了，判断是否已经全部匹配上
            if (++match == boundary.length) {
                // 判断是否以CRLF结尾
                if (i >= currentWriteBufferLength - 2) {
                    // 读到此处已经没有字节读了，退出等待下次数据进来
                    return null;
                } else {
                    // BOUNDARY没有以CRLF结尾，视为无效请求
                    if (currentBuffer[i + 1] != HttpConstants.CR || currentBuffer[i + 2] != HttpConstants.LF) {
                        return Boolean.FALSE;
                    } else {
                        // 完全以BOUNDARY.CRLF结束，请求正确
                        nextReadBufferIndex = i + 2 + 1;
                        return Boolean.TRUE;
                    }
                }
            }
        }

        return Boolean.FALSE;
    }

    /**
     * 解析表单字段，
     * 成功解析到BOUNDARY返回TRUE，数据还没彻底解析完返回NULL，否则返回FALSE
     * 示例：
     * <pre>
     * Content-Disposition: form-data; name="mypic"; filename="profile.png"
     * Content-Type: image/png
     *
     * </pre>
     */
    private Boolean doParseFormField(IoBuffer buffer) throws Exception {
        // 定位表单字段是否已经以CRLFCRLF结尾
        Pair<Integer, Boolean> result = doGetFormFieldAvailableIndex();
        if (result == null) {
            return null;
        }
        // 开始解析表单字段
        int avaiableIndex = result.first();
        for (int index = nextReadBufferIndex; index <= avaiableIndex; index++, nextReadBufferIndex++) {
            byte letter = currentBuffer[index];
            // 开始解析字段
            switch (fieldStatus) {
                case FORM_DECRYPT_START:
                    // 跳过空行
                    if (letter == ' ' || letter == '\t' || letter == '"' ||
                            letter == HttpConstants.CR || letter == HttpConstants.LF) {
                        break;
                    }

                    if (letter == ':' || letter == '=') {
                        fieldStatus = FORM_DECRYPT_FIELD;
                    } else {
                        currentKey.append(letter);
                    }
                    break;
                case FORM_DECRYPT_FIELD: // 匹配'='或者':'
                    if (letter == ' ' || letter == '\t' || letter == '"' || letter == HttpConstants.CR) {
                        break;
                    }

                    // 以分号或者换行作为一个KEY-VALUE结束
                    if (letter == ';' || letter == HttpConstants.LF) {
                        // 解析Key=Value结束
                        fieldStatus = FORM_DECRYPT_START;
                        doAddFormField();
                    } else {
                        currentValue.append(letter);
                    }
                    break;
            }
        }

        // 数据解析结束
        try {
            // 是否在一开始查找时倒已经匹配到了结尾，如果是则代表已经解析结束了
            boolean matchLine = result.second();
            if (matchLine) {
                fieldStatus = FORM_DECRYPT_START;
                doAddFormField();
                nextReadBufferIndex += HttpConstants.HEADER_SEPARATOR.length;
                return Boolean.TRUE;
            }
            return null;
        } finally {
            // 重置缓冲区并退出继续读取网络数据到缓冲区中继续解析
            doFlipCurrentBuffer();
        }
    }

    private void doAddFormField() {
        if (currentKey.length() > 0) {
            currentHeaders.put(currentKey.toString().toLowerCase(), currentValue.toString());
            currentKey.setLength(0);
            currentValue.setLength(0);
        }
    }

    private Boolean doParseFormValue(HttpRequest request, IoBuffer buffer) throws Exception {
        // 解析表单字段值
        Pair<Integer, Boolean> result = doGetFormValueAvailableIndex();
        if (result == null) {
            return null;
        }
        int avaiableIndex = result.first();
        int offset = nextReadBufferIndex;
        int avaiable = avaiableIndex - offset + 1;
        currentValue.append(currentBuffer, offset, avaiable);

        try {
            // 表单数据已经是完整数据
            Boolean complete = result.second();
            if (complete) {
                String key = currentHeaders.get("name");
                if (key == null) {
                    return Boolean.FALSE;
                }
                request.getParameters().put(key, currentValue.toString());
                nextReadBufferIndex += avaiable + boundary.length;
                currentValue.setLength(0);
                currentHeaders.clear();
                return complete;
            }
            return null;
        } finally {
            // 重置缓冲区并退出继续读取网络数据到缓冲区中继续解析
            doFlipCurrentBuffer();
        }
    }

    private Boolean doParseFormFile(HttpRequest request) throws Exception {
        Pair<Integer, Boolean> result = doGetFormValueAvailableIndex();
        if (result == null) {
            return null;
        }

        int avaiableIndex = result.first();
        int offset = nextReadBufferIndex;
        int avaiable = avaiableIndex - offset + 1;
        avaiable = avaiable > 0 ? avaiable : 0;
        // 表单KEY必须之前就已经解析存在
        String key = currentHeaders.get("name");
        if (key == null) {
            return Boolean.FALSE;
        }
        String filename = currentHeaders.get("filename");

        try {
            // 表单数据已经是完整数据
            Boolean complete = result.second();
            if (complete) {
                HttpFormFile formFile = doGetFormFile(request, key, filename);
                formFile.write(currentBuffer, offset, avaiable);
                nextReadBufferIndex += avaiable + boundary.length;
                currentHeaders.clear();
                return complete;
            } else {
                HttpFormFile formFile = doGetFormFile(request, key, filename);
                formFile.write(currentBuffer, offset, avaiable);
                nextReadBufferIndex += avaiable;
                return null;
            }
        } finally {
            // 重置缓冲区并退出继续读取网络数据到缓冲区中继续解析
            doFlipCurrentBuffer();
        }
    }

    /**
     * 缓冲区已经没有可解析的数据，
     * 把可读的数据迁回头部，等待下次网络接收再处理数据
     */
    private void doFlipCurrentBuffer() throws IOException {
        int start = nextReadBufferIndex;
        int length = currentWriteBufferLength - nextReadBufferIndex;
        // 先将还没读取的数据迁移到字节数组头部
        System.arraycopy(currentBuffer, start, currentBuffer, 0, length);
        nextReadBufferIndex = 0;
        currentWriteBufferLength = length;
    }

    /**
     * 从IoBuffer读取网络数据到buffer字节数组中，
     * 注意在调用前如果buffer字节还有存留字节数据，先把字节数据拷贝到数组前面，示例：
     * <code>
     * System.arraycopy(currentBuffer, nextReadBufferIndex, buffer, 0, (currentWriteBufferLength - nextReadBufferIndex));
     * </code>
     *
     * @return 没有可读网络数据返回-1
     */
    private int doReadFromBuffer(IoBuffer buffer) throws IOException {
        int total = currentBuffer.length;
        int offset = currentWriteBufferLength;
        long readable = buffer.readableBytes();
        if (readable <= 0) {
            return -1;
        }
        int length = (int) (total - offset > readable ? readable : total - offset);
        buffer.get(currentBuffer, offset, length);
        currentWriteBufferLength += length;
        return length;
    }

    /**
     * 获取表单字段可读取的字节位置，判断是否正确以CRLFCRLF结尾，示例：
     * <pre>
     * Content-Disposition: form-data; name="usrname"
     * [AvaiableIndex Here]
     * </pre>
     *
     * @return 没有可读数据返回null
     */
    private Pair<Integer, Boolean> doGetFormFieldAvailableIndex() {
        int match = 0;
        int index = nextReadBufferIndex;
        int total = currentWriteBufferLength;
        for (; index < total; index++) {
            byte letter = currentBuffer[index];
            if (letter == HttpConstants.HEADER_SEPARATOR[0]) {
                // 是匹配到了，但此时可读取的字节和要的字节总数不匹配，直接退出下次重新从网络读取完整数据再匹配
                if (index + HttpConstants.HEADER_SEPARATOR.length > total) {
                    break;
                }
                // 开始遍历之后是否全部匹配
                for (match = 0; match < HttpConstants.HEADER_SEPARATOR.length; match++) {
                    if (currentBuffer[index + match] != HttpConstants.HEADER_SEPARATOR[match]) {
                        break;
                    }
                }
                // 全部匹配到CRLFCRLF，证明已经到达表单字段结尾
                if (match == HttpConstants.HEADER_SEPARATOR.length) {
                    Pair<Integer, Boolean> result = new Pair<Integer, Boolean>(index - 1, true);
                    return result;
                }
            }
        }
        // 没有匹配上则最后以CRLFCRLF字节长度为保留位置，前面的数据先读取
        int avaiable = total - HttpConstants.HEADER_SEPARATOR.length;
        return avaiable < 0 ? null : new Pair<Integer, Boolean>(avaiable, false);
    }

    /**
     * 获取表单数据可读取的字节位置，判断是否正确以BOUNDARY结尾，示例：
     * <pre>
     * MYNAME[AvaiableIndex Here]
     * ------WebKitFormBoundaryrGKCBY7qhFd3TrwA
     * <pre>
     *
     * @return 没有可读数据返回null
     */
    private Pair<Integer, Boolean> doGetFormValueAvailableIndex() {
        int match = 0;
        int start = nextReadBufferIndex;
        int total = currentWriteBufferLength;
        for (; start < total; start++) {
            byte letter = currentBuffer[start];
            if (letter == boundary[0]) { // 获取到BOUNDARY开头的字节
                // 是匹配到了，但此时可读取的字节和BOUNDARY要的字节数不匹配，直接退出下次重新从网络读取完整数据再匹配
                if (start + boundary.length > total) {
                    break;
                }
                // 开始遍历之后是否全部匹配
                for (match = 0; match < boundary.length; match++) {
                    if (currentBuffer[start + match] != boundary[match]) {
                        break;
                    }
                }
                // 全部匹配到BOUNDARY，证明已经到达表单字段数据结尾
                if (match == boundary.length) {
                    int boundaryEnd = start + match + 1;
                    if (total > boundaryEnd) {
                        if (currentBuffer[boundaryEnd - 1] == HttpConstants.BOUNDARY_POSTFIX[0] &&
                                currentBuffer[boundaryEnd] == HttpConstants.BOUNDARY_POSTFIX[1]) {
                            Pair<Integer, Boolean> result = new Pair<Integer, Boolean>(start - 1, PARSE_FORM_COMPLETED);
                            return result;
                        }
                    }
                    Pair<Integer, Boolean> result = new Pair<Integer, Boolean>(start - 1, true);
                    return result;
                }
            }
        }
        // 没有匹配上则最后以CRLFCRLF字节长度为保留位置，前面的数据先读取
        int avaiable = total - boundary.length;
        // 可读的数据小于目前读取数据的位置索引，再读取数据也无意义，退出等待接收下次网络数据
        if (avaiable < 0 || avaiable <= nextReadBufferIndex) {
            return null;
        }
        return new Pair<Integer, Boolean>(avaiable, false);
    }

    /**
     * 截取请求Content-Type中的boundary，示例：
     * Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW
     */
    private static String doGetRequestBoundary(String contentType) {
        String[] boundaries = contentType.split("=");
        if (boundaries.length != 2) {
            return null;
        }
        return boundaries[1].trim();
    }

    private HttpFormFile doGetFormFile(HttpRequest request, String key, String filename) {
        HttpFormFile formFile = request.getFile(key);
        if (formFile == null) {
            formFile = new HttpFormFile(filename, threshold, directory);
            request.addFile(key, formFile);
        }
        return formFile;
    }

    @Override
    public void release() {
        currentBuffer = null;
    }
}
