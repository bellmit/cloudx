package cloud.apposs.logger;

/**
 * 设置输出内容的长度等，不够长的会用空格补齐，使输出内容变得整齐
 */
public final class FormatInfo {
    /** 字符占用最小长度，不足时用空格补齐 */
    private int minChar = -1;

    /** 字符占用最大长度，超过时截取到该最大长度并用...结尾 */
    private int maxChar = 0x7FFFFFFF;

    /** 补充空格是左对齐补充还是右对齐补充 */
    private boolean leftAlign = false;

    public int getMinChar() {
        return minChar;
    }

    public void setMinChar(int minChar) {
        this.minChar = minChar;
    }

    public int getMaxChar() {
        return maxChar;
    }

    public void setMaxChar(int maxChar) {
        this.maxChar = maxChar;
    }

    public boolean isLeftAlign() {
        return leftAlign;
    }

    public void setLeftAlign(boolean leftAlign) {
        this.leftAlign = leftAlign;
    }

    public void reset() {
        minChar = -1;
        maxChar = 0x7FFFFFFF;
        leftAlign = false;
    }
}
