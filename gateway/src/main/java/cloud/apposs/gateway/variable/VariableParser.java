package cloud.apposs.gateway.variable;

import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;

import java.util.LinkedList;
import java.util.List;

/**
 * 网关日志选项解析器，各选项定义如下：
 * $http_xxx: http header信息日志
 * $remot_addr: 请求远程地址
 * $remot_port: 请求远程端口
 * $host: 请求域名
 * $request: 请求url
 * $status: 响应状态码
 */
public final class VariableParser {
    private static final char ESCAPE_CHAR = '$';
    protected static final int BUF_SIZE = 256;

    private final String format;

    private final List<IVariable> variableList = new LinkedList<IVariable>();

    public VariableParser(String format) {
        this.format = format;
        if (format != null) {
            this.doParseFormat();
        }
    }

    public String parse(HttpRequest request, HttpResponse response) {
        StringBuilder output = new StringBuilder(BUF_SIZE);
        for (IVariable format : variableList) {
            output.append(format.parse(request, response));
        }
        return output.toString();
    }

    private void doParseFormat() {
        StringBuilder currentLiteral = new StringBuilder(32);
        int state = State.LITERAL_STATE;
        for (int i = 0; i < format.length(); i++) {
            char letter = format.charAt(i);
            switch (state) {
                case State.LITERAL_STATE:
                    if (letter != ESCAPE_CHAR) { // 只是普通字符而已
                        currentLiteral.append(letter);
                        break;
                    }
                    // 匹配关键字$
                    if (currentLiteral.length() != 0) {
                        variableList.add(new LiteralVariable(currentLiteral.toString()));
                    }
                    currentLiteral.setLength(0);
                    state = State.FORMAT_STATE;
                    break;
                case State.FORMAT_STATE:
                    if (letter != '_' && !Character.isLetter(letter)) {
                        // 对应一项日志项已经解析结束，判断是否存在对应IFormatter对象映射
                        finalizeFormatter(currentLiteral.toString());
                        currentLiteral.setLength(0);
                        state = State.LITERAL_STATE;
                    }
                    currentLiteral.append(letter);
                    break;
            }
        }
        // 最后结束项的解析
        if (state == State.LITERAL_STATE) {
            if (currentLiteral.length() != 0) {
                variableList.add(new LiteralVariable(currentLiteral.toString()));
            }
        } else if (state == State.FORMAT_STATE) {
            if (currentLiteral.length() != 0) {
                finalizeFormatter(currentLiteral.toString());
            }
        }
    }

    private void finalizeFormatter(String option) {
        if (option.startsWith("http_")) {
            String header = option.substring(5).replaceAll("_", "-");
            variableList.add(new HttpHeaderVariable(header));
        } else if (option.equals("remote_addr")) {
            variableList.add(new RemoteAddressVariable());
        }  else if (option.equals("remote_port")) {
            variableList.add(new RemotePortVariable());
        } else if (option.equals("host")) {
            variableList.add(new HostVariable());
        } else if (option.equals("request")) {
            variableList.add(new RequestVariable());
        } else if (option.equals("status")) {
            variableList.add(new HttpStatusVariable());
        } else {
            // 所有日志项都没匹配到，那就直接文本输出
            variableList.add(new LiteralVariable("$" + option));
        }
    }

    public class State {
        private static final int LITERAL_STATE = 0;
        private static final int FORMAT_STATE = 1;
    }
}
