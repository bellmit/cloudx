package cloud.apposs.rest.listener.httplog.variable;

import cloud.apposs.rest.Handler;

import java.util.LinkedList;
import java.util.List;

public abstract class AbstractVariableParser<R, P> {
    private static final char ESCAPE_CHAR = '$';
    protected static final int BUF_SIZE = 256;

    protected final String format;

    /**
     * 日志选项解析器
     */
    protected final List<IVariable<R, P>> variableList = new LinkedList<IVariable<R, P>>();

    public AbstractVariableParser(String format) {
        this.format = format;
        if (format != null) {
            this.doParseFormat();
        }
    }

    public void initialize() {
        if (format != null) {
            this.doParseFormat();
        }
    }

    public String parse(R request, P response, Handler handler, Throwable t) {
        StringBuilder output = new StringBuilder(BUF_SIZE);
        for (IVariable<R, P> format : variableList) {
            String message = format.parse(request, response, handler, t);
            if (message != null) {
                output.append(message);
            }
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
                        variableList.add(new LiteralVariable<R, P>(currentLiteral.toString()));
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
                variableList.add(new LiteralVariable<R, P>(currentLiteral.toString()));
            }
        } else if (state == State.FORMAT_STATE) {
            if (currentLiteral.length() != 0) {
                finalizeFormatter(currentLiteral.toString());
            }
        }
    }

    /**
     * 针对不同的日志选项添加不同的解析器
     */
    protected abstract void finalizeFormatter(String option);

    public class State {
        private static final int LITERAL_STATE = 0;
        private static final int FORMAT_STATE = 1;
    }
}
