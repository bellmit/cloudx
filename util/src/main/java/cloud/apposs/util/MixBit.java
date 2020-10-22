package cloud.apposs.util;

/**
 * 字节位比对工具
 */
public final class MixBit {
    /**
     * 初始化的数值
     */
    private long value;

    public static MixBit build(long value) {
        return new MixBit(value);
    }

    private MixBit(long value) {
        this.value = value;
    }

    public final boolean matched(long flag) {
        return matched(flag, true);
    }

    /**
     * 判断指定的字节位是否匹配
     *
     * @param flag 标志位，注意标志位一定要只有一个1，其他都为0
     * @param strict 是否判断1的位数是否合法，可以避免调用时传递一个错误的标志位导致的异常
     * @return 匹配时返回true
     */
    public final boolean matched(long flag, boolean strict) {
        if (strict && Long.bitCount(flag) > 1) {
            throw new IllegalArgumentException();
        }
        return (flag & value) == flag;
    }

    public final long active(long flag) {
        return active(flag, true);
    }

    /**
     * 开启某个标志
     * @param flag 标志位
     * @param strict 是否判断1的位数是否合法，可以避免调用时传递一个错误的标志位导致的异常
     */
    public final long active(long flag, boolean strict) {
        if (strict && Long.bitCount(flag) > 1) {
            throw new IllegalArgumentException();
        }
        this.value |= flag;
        return this.value;
    }

    public final long deactive(long flag) {
        return deactive(flag, true);
    }

    /**
     * 关闭某个标志
     * @param flag 标志位
     * @param strict 是否判断1的位数是否合法，可以避免调用时传递一个错误的标志位导致的异常
     */
    public final long deactive(long flag, boolean strict) {
        if (strict && Long.bitCount(flag) > 1) {
            throw new IllegalArgumentException();
        }

        this.value &= ~flag;
        return this.value;
    }

    public final long getValue() {
        return value;
    }

    /**
     * 将数值转换成二进制字符串
     */
    public final String toBinaryString() {
        return Long.toBinaryString(value);
    }

    @Override
    public final String toString() {
        return toBinaryString();
    }
}
