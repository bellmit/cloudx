package cloud.apposs.gateway.configure;

public class ConfigParseException extends Exception{
    private final int lineNo;

    public ConfigParseException(int lineNo, String message) {
        super(message + " in line:" + lineNo);
        this.lineNo = lineNo;
    }

    public int getLineNo() {
        return lineNo;
    }
}
