package cloud.apposs.netkit.filterchain.dns;

import cloud.apposs.util.StrUtil;

import java.io.IOException;

public class Name {
    public final static int MAX_LABEL_LENGTH = 63;

    public final static int MAX_NAME_LENGTH = 255;

    public final static int MAX_LABEL_COUNT = 127;

    public static final int LABEL_MASK = 0xC0;

    public static final int LABEL_NORMAL = 0x00;

    public static final int LABEL_COMPRESSION = 0xC0;

    private final static byte[] EMPTY_LABEL = {0};

    /** DNS 协议的字节数组 */
    private byte[] name;

    /** label 数量 */
    private int labels;

    private Name(){
    }

    /**
     * 构造 name 数组
     * @param domain 域名
     */
    public Name(String domain) {
        if (StrUtil.isEmpty(domain)) {
            throw new NullPointerException("name str is null");
        }
        byte[] label = new byte[MAX_LABEL_LENGTH + 1];
        boolean escaped = false;
        int digit = 0;
        int intval = 0;
        int pos = 1;
        for(int i = 0, length = domain.length(); i < length; i++) {
            byte letter = (byte) domain.charAt(i);
            if (escaped) {
                if (letter >= 0 && letter <= 9 && digit < 3) {
                    digit++;
                    intval *= 10;
                    intval += (letter - '0');
                    if (intval > 255) {
                        throw new IllegalArgumentException("escape int val over limit:" + intval);
                    }
                    if (digit < 3) {
                        continue;
                    }
                    letter = (byte) intval;
                } else if (digit > 0 && digit < 3) {
                    throw new IllegalArgumentException("");
                }
                label[pos++] = letter;
                escaped = false;
            } else if (letter == '\\') {
                escaped = true;
                digit = 0;
                intval = 0;
            } else if (letter == '.') {
                label[0] = (byte) (pos - 1);
                append(label, 0, 1);
                pos = 1;
            } else {
                if (pos > MAX_LABEL_LENGTH) {
                    throw new IllegalArgumentException("label length over limit");
                }
                label[pos++] = letter;
            }
        }
        if ((digit > 0 && digit < 3) || escaped) {
            throw new IllegalArgumentException("bad escaped");
        }
        if (pos != 1) {
            label[0] = (byte) (pos - 1);
            append(label, 0, 1);
        }
        append(EMPTY_LABEL, 0, 1);
    }

    /**
     * 往 name 数组添加 label
     * 
     * @param src 需要添加的 label 数据
     * @param start 起始 label 的下标
     * @param labelCount 需要添加的 label 数量
     */
    private void append(byte[] src, int start, int labelCount) {
        int oldLength = getLength();
        int aLength = 0;
        for (int pos = start, i = 0; i < labelCount; i++) {
            int len = src[pos];
            if (len > MAX_LABEL_LENGTH) {
                throw new IllegalArgumentException("label length over limit");
            }
            len++;
            aLength += len;
            pos += len;
        }
        int newLength = oldLength + aLength;
        if (newLength > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
            		String.format("name length over limit;name=%s;target=%s", 
            				new String(name), new String(src, start, aLength)));
        }
        int newLabels = getLabels() + labelCount;
        if (newLabels > MAX_LABEL_COUNT) {
            throw new IllegalArgumentException("label count over limit");
        }
        byte[] newName = new byte[newLength];
        if (oldLength != 0) {
            System.arraycopy(name, 0, newName, 0, oldLength);
        }
        System.arraycopy(src, start, newName, oldLength, aLength);
        name = newName;
        setLabels(newLabels);
    }

    public void toWire(IoBufferAccessor accessor) throws IOException {
        accessor.writeBytes(name, 0, getLength());
    }

    public int getLength() {
        if (name == null) {
            return 0;
        }
        return name.length;
    }

    public int getLabels() {
        return labels;
    }

    public void setLabels(int labels) {
        this.labels = labels;
    }
    
    public static Name fromWire(IoBufferAccessor accessor) throws IOException {
        Name name = new Name();
        boolean isSaveState = false;
        boolean isDone = false;
        byte[] label = new byte[MAX_LABEL_LENGTH + 1];

        // Name 最后一个字节以 0x00 结尾
        while (!isDone) {
            int len = accessor.readU8();
            switch (len & LABEL_MASK) {
                case LABEL_COMPRESSION : {
                    int pos = accessor.readU8();
                    pos += ((len & ~LABEL_MASK) << 8);
                    if (pos >= accessor.writeIdx() - 2) {
                        throw new IOException("bad compression");
                    }
                    if (!isSaveState) {
                        accessor.save();
                        isSaveState = true;
                    }
                    accessor.readIdx(pos);
                    break;
                }
                case LABEL_NORMAL : {
                    if (name.getLabels() + 1 > MAX_LABEL_COUNT) {
                        throw new IOException("label count over limit");
                    }
                    if (len == 0) {
                        name.append(EMPTY_LABEL, 0, 1);
                        isDone = true;
                    } else {
                        label[0] = (byte) len;
                        accessor.readBytes(label, 1, len);
                        name.append(label, 0, 1);
                    }
                    break;
                }
                default: {
                    throw new IOException("bad label type");
                }
            }
        }
        if (isSaveState) {
            accessor.restore();
        }
        return name;
    }

    @Override
    public String toString() {
        return this.toString(true);
    }

    public String toString(boolean omitFinalDot) {
        if (getLabels() == 1 && name[0] == 0) {
            return ".";
        }
        StringBuilder ret = new StringBuilder();
        for (int idx = 0; idx < name.length;) {
            int len = name[idx++];
            if (len == 0) {
                break;
            }
            ret.append(new String(name, idx, len)).append(".");
            idx += len;
        }
        if (omitFinalDot) {
            int len = ret.length();
            ret.delete(len - 1, len);
        }
        return ret.toString();
    }
}
