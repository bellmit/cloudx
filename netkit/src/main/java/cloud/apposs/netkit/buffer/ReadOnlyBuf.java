package cloud.apposs.netkit.buffer;

import cloud.apposs.netkit.EventChannel;
import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.util.SysUtil;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * 文件引用只读缓存，内部仅保留文件的引用，仅用于大文件数据发送业务，提升服务的性能，
 * 底层禁止往该Buffer写入数据避免文件内容被覆写，
 * 废除flip逻辑，采用readIdx和writeIdx来维护数据的读取与写入<br>
 * 内存结构如下：
 * <pre>
 * +-------------------+------------------+------------------+
 * | discardable bytes |  readable bytes  |  writable bytes  |
 * |                   |     (CONTENT)    |                  |
 * +-------------------+------------------+------------------+
 * |                   |                  |                  |
 * 0      <=        readIdx     <=     writeIdx    <=    capacity
 * </pre>
 */
public class ReadOnlyBuf implements IoBuffer {
    public static final int DEFAULT_RAM_SIZE = 10 * 1024 * 1024;

    private long writeIdx = 0;

    private long readIdx = 0;

    /**
     * 当前缓冲最大容量
     */
    private final long capacity;

    /**
     * 允许每次进行零拷贝传输时传输的数据大小也不能超过此值
     */
    private final int ramsize;

    /**
     * 持有的文件引用
     */
    private final File file;
    private FileChannel fchannel;

    /**
     * 文件读写次数，用于统计
     */
    private int fwcnt = 0;
    private int frcnt = 0;

    public static IoBuffer wrap(File file) throws IOException {
        return new ReadOnlyBuf(file);
    }

    public static IoBuffer wrap(File file, int ramsize) throws IOException {
        return new ReadOnlyBuf(file, ramsize);
    }

    public ReadOnlyBuf(File file) throws IOException {
        this(file, DEFAULT_RAM_SIZE);
    }

    public ReadOnlyBuf(File file, int ramsize) throws IOException {
        if (file == null || ramsize <= 0) {
            throw new IllegalArgumentException();
        }
        if (!file.exists()) {
            throw new FileNotFoundException(file.getPath());
        }
        this.capacity = file.length();
        this.ramsize = ramsize;
        this.writeIdx = file.length();
        this.file = file;
    }

    @Override
    public long readIdx() {
        return readIdx;
    }

    @Override
    public long writeIdx() {
        return writeIdx;
    }

    @Override
    public long capacity() {
        return capacity;
    }

    @Override
    public IoBuffer readIdx(long readIdx) {
        if (readIdx < 0 || readIdx > writeIdx) {
            throw new IndexOutOfBoundsException(String.format(
                    "readerIndex: %d (expected: 0 <= readerIndex <= writerIndex(%d))", readIdx, writeIdx));
        }
        this.readIdx = readIdx;
        return this;
    }

    @Override
    public IoBuffer writeIdx(long writeIdx) {
        if (writeIdx < readIdx || writeIdx > capacity()) {
            throw new IndexOutOfBoundsException(String.format(
                    "writerIndex: %d (expected: readerIndex(%d) <= writerIndex <= capacity(%d))",
                    writeIdx, readIdx, capacity()));
        }
        this.writeIdx = writeIdx;
        return this;
    }

    @Override
    public long readableBytes() {
        return writeIdx - readIdx;
    }

    @Override
    public long writableBytes() {
        return capacity() - writeIdx;
    }

    @Override
    public boolean hasReadableBytes() {
        return readableBytes() > 0;
    }

    @Override
    public boolean hasWritableBytes() {
        return writableBytes() > 0;
    }

    @Override
    public byte get() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte get(long index) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IoBuffer get(byte[] dst, long offset, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IoBuffer get(byte[] dst) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] array() throws IOException {
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException();
        }
        return array(0, (int) length);
    }

    @Override
    public byte[] array(long offset, int length) throws IOException {
        if (offset > Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException();
        }

        InputStream is = null;
        try {
            is = new FileInputStream(file);
            byte[] bytes = new byte[is.available()];
            is.read(bytes, (int) offset, length);
            return bytes;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    @Override
    public String string() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String string(Charset charset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String string(long offset, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String string(long offset, int length, Charset charset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer buffer() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer buffer(long offset, int length) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer buffer(long offset, int length, boolean direct)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IoBuffer put(byte b) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IoBuffer put(byte[] src) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IoBuffer put(byte[] src, int offset, int length) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IoBuffer put(ByteBuffer src) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IoBuffer put(ByteBuffer src, int offset, int length)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IoBuffer put(IoBuffer src) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IoBuffer put(IoBuffer src, long offset, long length)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IoBuffer put(String src) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IoBuffer put(String src, Charset charset) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IoBuffer put(File src) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IoBuffer put(File src, long offset, long length) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IoBuffer put(FileChannel src) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public IoBuffer put(FileChannel src, long offset, long length)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long channelRecv(EventChannel channel) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long channelSend(EventChannel channel) throws IOException {
        SysUtil.checkNotNull(channel, "channel");

        // 判断所有数据是否发送完毕
        if (!hasReadableBytes()) {
            return 0;
        }

        int sendLen = 0;
        if (fchannel == null) {
            fchannel = new FileInputStream(file).getChannel();
        }
        // 零拷贝将文件数据发送到网络，避免一开始传输量过大也会导致OOM
        int trans = (int) (ramsize > (capacity - readIdx) ? (capacity - readIdx) : ramsize);
        sendLen = (int) channel.transferFrom(fchannel, readIdx, trans);
        if (sendLen > 0) {
            readIdx += sendLen;
            frcnt++;
        }

        return sendLen;
    }

    @Override
    public void expand(int length) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long flush(String file) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long flush(File file) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        frcnt = 0;
        fwcnt = 0;
        readIdx = 0;
        writeIdx = 0;

        if (fchannel != null) {
            try {
                fchannel.position(0);
            } catch (IOException e) {
            }
        }
    }

    @Override
    public void free() {
        if (fchannel != null) {
            try {
                fchannel.close();
            } catch (IOException e) {
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder info = new StringBuilder();
        info.append(getClass().getSimpleName() + "[");
        info.append("capacity=" + capacity());
        info.append(" readIdx=" + readIdx());
        info.append(" writeIdx=" + writeIdx());
        info.append(" frcnt=" + frcnt);
        info.append(" fwcnt=" + fwcnt);
        info.append("]");
        return info.toString();
    }
}
