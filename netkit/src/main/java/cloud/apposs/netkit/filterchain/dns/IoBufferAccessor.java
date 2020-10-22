package cloud.apposs.netkit.filterchain.dns;

import cloud.apposs.netkit.IoBuffer;

import java.io.IOException;

public class IoBufferAccessor {
    private IoBuffer buffer;

    private long savedWriteIdx;

    private long savedReadIdx;

    public IoBufferAccessor(IoBuffer buffer) {
        this.buffer = buffer;
    }

    public void writeU16(long data) throws IOException {
        byte[] bys = new byte[2];
        bys[0] = (byte) ((data >>> 8) & 0xFF);
        bys[1] = (byte) (data & 0xFF);
        buffer.put(bys);
    }

    public void writeU16At(long data, long idx) throws IOException {
        save();
        buffer.writeIdx(idx);
        writeU16(data);
        restore();
    }

    public void writeU32(long data) throws IOException {
        byte[] bys = new byte[4];
        bys[0] = (byte) ((data >>> 24) & 0xFF);
        bys[1] = (byte) ((data >>> 16) & 0xFF);
        bys[2] = (byte) ((data >>> 8) & 0xFF);
        bys[3] = (byte) (data & 0xFF);
        buffer.put(bys);
    }

    public void writeBytes(byte[] src, int offset, int len) throws IOException {
        buffer.put(src, offset, len);
    }

    public int readU8() throws IOException {
        need(1);
        return buffer.get() & 0xFF;
    }

    public int readU16() throws IOException {
        need(2);
        int b1 = buffer.get() & 0xFF;
        int b2 = buffer.get() & 0xFF;
        return (b1 << 8) + b2;
    }

    public long readU32() throws IOException {
        need(4);
        int b1 = buffer.get() & 0xFF;
        int b2 = buffer.get() & 0xFF;
        int b3 = buffer.get() & 0xFF;
        int b4 = buffer.get() & 0xFF;
        return (b1 << 24) + (b2 << 16) + (b3 << 8) + b4;
    }

    public void readBytes(byte[] target, int offset, int len) throws IOException {
        need(len);
        long readIdx = buffer.readIdx();
        byte[] src = buffer.array(readIdx, len);
        System.arraycopy(src, 0, target, offset, len);
        buffer.readIdx(readIdx + len);
    }

    public IoBufferAccessor readIdx(long idx) {
        buffer.readIdx(idx);
        return this;
    }

    public long readIdx(){
        return buffer.readIdx();
    }

    public long writeIdx() {
        return buffer.writeIdx();
    }

    public IoBufferAccessor writeIdx(long idx) {
        buffer.writeIdx(idx);
        return this;
    }

    /**
     * 需要读取 size 个字节，如果不足则抛异常
     */
    public void need(int size) throws IOException {
        if (buffer.readableBytes() < size) {
            throw new IOException("The rest of bytes less than need");
        }
    }
    
    public void save() {
        savedReadIdx = readIdx();
        savedWriteIdx = writeIdx();
    }

    public void restore() {
        writeIdx(savedWriteIdx);
        readIdx(savedReadIdx);
    }

    public IoBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(IoBuffer buffer) {
        this.buffer = buffer;
    }
}
