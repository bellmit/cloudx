package cloud.apposs.netkit.filterchain.dns;

import cloud.apposs.netkit.IoBuffer;

import java.io.IOException;
import java.util.Random;

/**
 * 头部格式
 * |0----------------15--16---------------31|
 * |   TransactionId   |        Flags       |
 * |     Questions     |     Answers Rrs    |
 * |   Authority Rrs   |   Additional Rrs   |
 * |----------------------------------------|
 */
public class Header {
    private static Random random = new Random();
    /** 会话 Id */
    private int id = -1;
    
    /**
     * 标志
     * <pre>
     * | QR | opcode | AA | TC | RD | RA | (zero) | rcode |
     *   1      4      1    1    1    1      3       4
     * </pre>
     */
    private int flags = 0;
    
    /**
     * 资源数数组
     */
    private int[] counts = new int[4];

    public Header() {
    }

    public Header(int id) {
    	this.id = id;
    }

    public Header(IoBufferAccessor accessor) throws IOException {
        this(accessor.readU16());
        flags = accessor.readU16();
        for(int i = 0; i < counts.length; i++) {
            counts[i] = accessor.readU16();
        }
    }

    public Header(IoBuffer buffer) throws IOException {
        this(new IoBufferAccessor(buffer));
    }

    void toWire(IoBufferAccessor buffer) throws IOException {
        buffer.writeU16(getId());
        buffer.writeU16(flags);
        for (int count : counts) {
            buffer.writeU16(count);
        }
    }

    public int getId() {
        if (id >= 0) {
            return id;
        }
        synchronized (this) {
            if (id < 0)
                id = random.nextInt(0xffff);
            return id;
        }
    }

    public void setId(int id) {
        if (id < 0 || id > 0xffff) {
            throw new IllegalArgumentException("DNS message ID " + id + " is out of range");
        }
        this.id = id;
    }

    public Header setQR(boolean val) {
        setFlag(0, val);
        return this;
    }

    public boolean getQR() {
        return ((flags >> 15) & 0x1) == 1;
    }

    public Header setOpCode(int val) {
        if (val < 0 || val > 0xF) {
            throw new IllegalArgumentException("DNS OpCode " + val + " is out of range");
        }
        flags &= 0x87FF;
        flags |= (val << 11);
        return this;
    }

    public int getOpCode() {
        return (flags >> 11) & 0xF;
    }

    public Header setAA(boolean val) {
        setFlag(5, val);
        return this;
    }

    public boolean getAA() {
        return ((flags >> 10) & 0x1) == 1;
    }

    public Header setTC(boolean val) {
        setFlag(6, val);
        return this;
    }

    public boolean getTC() {
        return ((flags >> 9) & 0x1) == 1;
    }

    public Header setRD(boolean val) {
        setFlag(7, val);
        return this;
    }

    public boolean getRD() {
        return ((flags >> 8) & 0x1) == 1;
    }

    public Header setRA(boolean val) {
        setFlag(8, val);
        return this;
    }

    public boolean getRA() {
        return ((flags >> 7) & 0x1) == 1;
    }

    public Header setRCode(int val) {
        if (val < 0 || val > 0xF) {
            throw new IllegalArgumentException("DNS RCode " + val + " is out of range");
        }
        flags = (flags & 0xFFF0) + val;
        return this;
    }

    public int getRCode() {
        return flags & 0xF;
    }

    public Header setCount(int field, int val) {
        if (val < 0) {
            throw new IllegalArgumentException("count should more than or equals 0");
        }
        counts[field] = val;
        return this;
    }

    public int getCount(int field) {
        return counts[field];
    }

    public Header incCount(int field) {
        counts[field]++;
        return this;
    }

    public Header decCount(int field) {
        counts[field]--;
        return this;
    }

    public Header setFlag (int bit, boolean val) {
        if (val) {
            flags |= (1 << (15 - bit));
        } else {
            flags &= ~(1 << (15 - bit));
        }
        return this;
    }

    public Header setFlag (int bit) {
        setFlag(bit, true);
        return this;
    }
}
