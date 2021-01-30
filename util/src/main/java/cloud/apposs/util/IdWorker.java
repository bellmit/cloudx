package cloud.apposs.util;

/**
 * 基于雪花算法的全局ID生成器，
 * 主要应用于业务如订单一类的ID唯一递增，并适用于异地机房双活设计，即保证不同机房间产生的ID也是唯一
 * 内存结构如下：
 * <pre>
 * +---+------------------------------------------------+-------------+---------------+
 * | 0 | 00000000 00000000 00000000 00000000 00000000 0 | 00000000 00 | 00000000 0000 |
 * +---+------------------------------------------------+-------------+---------------+
 * | 1bit | 41bit                                       | 10bit       | 12bit         |
 * 1. 1bit，不用，因为二进制中最高位是符号位，1表示负数，0表示正数。生成的id一般都是用整数，所以最高位固定为0
 * 2. 41bit-时间戳，用来记录时间戳，毫秒级
 * 3. 10bit-工作机器id，用来记录工作机器id
 * 4. 12bit-序列号，序列号，用来记录同毫秒内产生的不同id
 * </pre>
 * 参考：
 * <pre>
 * https://www.cnblogs.com/flgb/p/13934367.html
 * https://blog.csdn.net/fyj13925475957/article/details/106456731
 * https://www.cnblogs.com/chaos-li/p/11302820.html
 * </pre>
 */
public class IdWorker {
    /** 机器id所占的位数 */
    public static final long WORKER_ID_BITS = 5L;
    /** 数据标识id所占的位数 */
    public static final long IDC_BITS = 5L;
    /** 序列在id中占的位数 */
    public static final long SEQUENCE_BITS = 12L;

    /** 开始时间戳 (2015-01-01) */
    private final long twepoch = 1420041600000L;

    /** 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数) */
    private final long maxWorkerId = -1L ^ (-1L << WORKER_ID_BITS);

    /** 支持的最大数据标识id，结果是31 */
    private final long maxIdcId = -1L ^ (-1L << IDC_BITS);

    /** 机器ID向左移12位 */
    private final long workerIdShift = SEQUENCE_BITS;

    /** 数据标识id向左移17位(12+5) */
    private final long idcIdShift = SEQUENCE_BITS + WORKER_ID_BITS;

    /** 时间戳向左移22位(5+5+12) */
    private final long timestampLeftShift = SEQUENCE_BITS + WORKER_ID_BITS + IDC_BITS;

    /** 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095) */
    private final long sequenceMask = -1L ^ (-1L << SEQUENCE_BITS);

    /** 工作机器ID(0~31) */
    private long workerId;

    /** 数据中心ID(0~31) */
    private long idcId;

    /** 毫秒内序列(0~4095) */
    private long sequence = 0L;

    /** 上次生成ID的时间戳 */
    private long lastTimestamp = -1L;

    public static IdWorker builder(long workerId, long idcId) {
        return new IdWorker(workerId, idcId);
    }

    /**
     * 构造函数
     *
     * @param workerId 工作ID (0~31)，一般指服务器
     * @param idcId 数据中心ID (0~31)，一般指所在机房
     */
    public IdWorker(long workerId, long idcId) {
        if (workerId > maxWorkerId || workerId < 0) {
            String message = String.format("worker Id can't be greater than %d or less than 0", maxWorkerId);
            throw new IllegalArgumentException(message);
        }
        if (idcId > maxIdcId || idcId < 0) {
            String message = String.format("IDC Id can't be greater than %d or less than 0", maxIdcId);
            throw new IllegalArgumentException(message);
        }
        this.workerId = workerId;
        this.idcId = idcId;
    }

    /**
     * 获得下一个ID (该方法是线程安全的)
     */
    public synchronized long nextId() {
        long timestamp = doGenerateTimestamp();

        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < lastTimestamp) {
            String message = String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - timestamp);
            throw new RuntimeException(message);
        }

        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            // 毫秒内序列溢出
            if (sequence == 0) {
                // 阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 时间戳改变，毫秒内序列重置
            sequence = 0L;
        }

        // 上次生成ID的时间戳
        lastTimestamp = timestamp;

        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - twepoch) << timestampLeftShift)
                | (idcId << idcIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     *
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 当前时间戳
     */
    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = doGenerateTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = doGenerateTimestamp();
        }
        return timestamp;
    }

    /**
     * 返回以毫秒为单位的当前时间
     *
     * @return 当前时间(毫秒)
     */
    protected long doGenerateTimestamp() {
        return System.currentTimeMillis();
    }
}
