package cloud.apposs.netkit.buffer;

import cloud.apposs.netkit.EventChannel;
import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.util.SysUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * 内存字节缓存，提供堆内和堆外内存存储，废除flip逻辑，采用readIdx和writeIdx来维护数据的读取与写入<br>
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
public class ByteBuf implements IoBuffer {
    private int writeIdx = 0;

    private int readIdx = 0;

    private int capacity;

    private ByteBuffer buffer;

    public static ByteBuf wrap(byte[] buffer) {
        return new ByteBuf(buffer);
    }

    public static ByteBuf wrap(String str) {
        return new ByteBuf(str, Charset.forName("utf-8"));
    }

    public static ByteBuf wrap(String str, String charset) {
        return new ByteBuf(str, Charset.forName(charset));
    }

    public static ByteBuf wrap(String str, Charset charset) {
        return new ByteBuf(str, charset);
    }

    public ByteBuf(int capacity, boolean direct) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity");
        }

        this.capacity = capacity;
        if (direct) {
            this.buffer = ByteBuffer.allocateDirect(capacity);
        } else {
            this.buffer = ByteBuffer.allocate(capacity);
        }
    }

    public ByteBuf(byte[] buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer");
        }

        this.buffer = ByteBuffer.wrap(buffer);
        this.capacity = buffer.length;
        this.writeIdx = buffer.length;
    }

    public ByteBuf(String str, Charset charset) {
        if (str == null) {
            throw new IllegalArgumentException("str");
        }

        byte[] buffer = str.getBytes(charset);
        this.buffer = ByteBuffer.wrap(buffer);
        this.capacity = buffer.length;
        this.writeIdx = buffer.length;
    }

    public ByteBuf(ByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer");
        }

        this.buffer = buffer;
        this.capacity = buffer.capacity();
        this.readIdx = buffer.position();
        this.writeIdx = buffer.limit();
    }

    @Override
    public long readIdx() {
        return readIdx;
    }

    @Override
    public IoBuffer readIdx(long readIdx) {
        if (readIdx < 0 || readIdx > writeIdx) {
            throw new IndexOutOfBoundsException(String.format(
                    "readerIndex: %d (expected: 0 <= readerIndex <= writerIndex(%d))", readIdx, writeIdx));
        }
        if (readIdx > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("readIdx over limit");
        }
        this.readIdx = (int) readIdx;
        return this;
    }

    @Override
    public IoBuffer writeIdx(long writeIdx) {
        if (writeIdx < readIdx || writeIdx > capacity()) {
            throw new IndexOutOfBoundsException(String.format(
                    "writerIndex: %d (expected: readerIndex(%d) <= writerIndex <= capacity(%d))",
                    writeIdx, readIdx, capacity()));
        }
        if (writeIdx > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("writeIdx over limit");
        }
        this.writeIdx = (int) writeIdx;
        return this;
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
    public byte get() {
        if (readIdx >= writeIdx) {
            throw new IndexOutOfBoundsException();
        }

        return get(readIdx++);
    }

    @Override
    public byte get(long index) {
        if (index < 0 || index > writeIdx) {
            throw new IndexOutOfBoundsException();
        }

        return buffer.get((int) index);
    }

    @Override
    public IoBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    @Override
    public IoBuffer get(byte[] dst, long offset, int length) {
        if (offset < 0 || length < 0 ||
                offset > Integer.MAX_VALUE || length > writeIdx) {
            throw new IndexOutOfBoundsException();
        }
        int oldPos = buffer.position();
        int oldLimit = buffer.limit();
        try {
            buffer.position(readIdx);
            buffer.limit(writeIdx);
            buffer.get(dst, (int) offset, length);
        } finally {
            buffer.limit(oldLimit);
            buffer.position(oldPos);
        }
        readIdx += length;
        return this;
    }

    @Override
    public byte[] array() {
        return array(readIdx, (int) readableBytes());
    }

    @Override
    public byte[] array(long offset, int length) {
        if (offset < 0 || length < 0 ||
                offset > Integer.MAX_VALUE || (offset + length) > writeIdx) {
            throw new IndexOutOfBoundsException();
        }

        return getBufArray(buffer, (int) offset, length);
    }

    @Override
    public String string() {
        return string(readIdx, (int) readableBytes(), Charset.forName("utf-8"));
    }

    @Override
    public String string(Charset charset) {
        return string(readIdx, (int) readableBytes(), charset);
    }

    @Override
    public String string(long offset, int length) {
        return string(offset, length, Charset.forName("utf-8"));
    }

    @Override
    public String string(long offset, int length, Charset charset) {
        if (offset < 0 || length < 0 ||
                offset > Integer.MAX_VALUE || (offset + length) > writeIdx) {
            throw new IndexOutOfBoundsException();
        }

        byte[] bufs = ByteBuf.getBufArray(buffer, (int) offset, (int) length);
        return new String(bufs, 0, (int) length, charset);
    }

    @Override
    public ByteBuffer buffer() throws IOException {
        return buffer(readIdx, writeIdx - readIdx, false);
    }

    @Override
    public ByteBuffer buffer(long offset, int length) {
        return buffer(offset, length, false);
    }

    @Override
    public ByteBuffer buffer(long offset, int length, boolean direct) {
        if (offset < 0 || length < 0 ||
                offset > Integer.MAX_VALUE || (offset + length) > writeIdx) {
            throw new IndexOutOfBoundsException();
        }

        ByteBuffer newBuf = null;
        if (direct) {
            newBuf = ByteBuffer.allocateDirect(length);
        } else {
            newBuf = ByteBuffer.allocate(length);
        }
        long end = offset + length;
        for (long i = offset; i < end; i++) {
            newBuf.put(get(i));
        }

        return newBuf;
    }

    @Override
    public IoBuffer put(byte b) throws IOException {
        autoExpandIfBufferFull(1);
        buffer.put(b);
        writeIdx++;
        return this;
    }

    @Override
    public IoBuffer put(byte[] src) {
        return put(src, 0, src.length);
    }

    @Override
    public IoBuffer put(byte[] src, int offset, int length) {
        if (src == null || offset < 0 || length < 0) {
            throw new IllegalArgumentException();
        }
        if ((offset + length) > src.length) {
            throw new IndexOutOfBoundsException();
        }

        autoExpandIfBufferFull(length);
        buffer.put(src, offset, length);
        writeIdx += length;
        return this;
    }

    @Override
    public IoBuffer put(ByteBuffer src) {
        return put(src, src.position(), src.limit());
    }

    @Override
    public IoBuffer put(ByteBuffer src, int offset, int length) {
        if (src == null || offset < 0 || length < 0) {
            throw new IllegalArgumentException();
        }
        if ((offset + length) > src.limit()) {
            throw new IndexOutOfBoundsException();
        }

        autoExpandIfBufferFull(length);
        byte[] bytes = getBufArray(src, offset, length);
        buffer.put(bytes);
        src.position((int) (offset + length));
        writeIdx += length;
        return this;
    }

    @Override
    public IoBuffer put(IoBuffer src) throws IOException {
        return put(src, src.readIdx(), src.readableBytes());
    }

    @Override
    public IoBuffer put(IoBuffer src, long offset, long length) throws IOException {
        if (src == null || offset < 0 || length < 0 ||
                offset > Integer.MAX_VALUE || length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }
        if ((offset + length) > src.writeIdx()) {
            throw new IndexOutOfBoundsException();
        }

        byte[] bytes = src.array(offset, (int) length);
        autoExpandIfBufferFull((int) length);
        buffer.put(bytes);
        src.readIdx((int) (offset + length));
        writeIdx += length;
        return this;
    }

    @Override
    public IoBuffer put(File src) throws IOException {
        return put(src, 0, src.length());
    }

    @Override
    public IoBuffer put(File src, long offset, long length) throws IOException {
        if (src == null || offset < 0 || length < 0 ||
                offset > Integer.MAX_VALUE || length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }
        if ((offset + length) > src.length()) {
            throw new IndexOutOfBoundsException();
        }

        FileChannel channel = null;
        try {
            channel = new FileInputStream(src).getChannel();
            return put(channel, offset, length);
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                }
                channel = null;
            }
        }
    }

    @Override
    public IoBuffer put(FileChannel src) throws IOException {
        return put(src, 0, src.size());
    }

    @Override
    public IoBuffer put(FileChannel src, long offset, long length) throws IOException {
        if (src == null || offset < 0 || length < 0 ||
                offset > Integer.MAX_VALUE || length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }
        if ((offset + length) > src.size()) {
            throw new IndexOutOfBoundsException();
        }

        autoExpandIfBufferFull((int) length);
        int limit = (int) (buffer.limit() > length ? length : buffer.limit());
        int writeLen = 0;
        while (writeLen < limit) {
            writeLen += src.read(buffer, offset);
            offset += writeLen;
        }
        writeIdx += writeLen;

        return this;
    }

    @Override
    public IoBuffer put(String src) {
        return put(src, Charset.forName("utf-8"));
    }

    @Override
    public IoBuffer put(String src, Charset charset) {
        byte[] bytes = src.getBytes(charset);
        autoExpandIfBufferFull(bytes.length);
        buffer.put(bytes);
        writeIdx += bytes.length;
        return this;
    }

    @Override
    public long channelRecv(EventChannel channel) throws IOException {
        SysUtil.checkNotNull(channel, "channel");

        int recvLen = 0;
        try {
            recvLen = channel.recv(buffer);
            return recvLen;
        } finally {
            if (recvLen > 0) {
                writeIdx += recvLen;
            }
        }
    }

    @Override
    public long channelSend(EventChannel channel) throws IOException {
        SysUtil.checkNotNull(channel, "channel");

        // 判断所有数据是否发送完毕
        if (!hasReadableBytes()) {
            return 0;
        }

        int sendLen = 0;
        try {
            buffer.position(readIdx);
            buffer.limit(writeIdx);
            sendLen = channel.send(buffer);
            return sendLen;
        } finally {
            if (sendLen > 0) {
                readIdx += sendLen;
            }
        }
    }

    @Override
    public long flush(String file) throws IOException {
        return flush(new File(file));
    }

    @Override
    public long flush(File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file, false);
        FileChannel channel = fos.getChannel();
        int oldPos = buffer.position();
        int oldLimit = buffer.limit();
        try {
            buffer.position(readIdx);
            buffer.limit(writeIdx);
            return channel.write(buffer);
        } finally {
            buffer.limit(oldLimit);
            buffer.position(oldPos);
            fos.close();
            channel.close();
        }
    }

    @Override
    public void reset() {
        readIdx = 0;
        writeIdx = 0;
        if (buffer != null) {
            buffer.clear();
        }
    }

    @Override
    public void free() {
        reset();
    }

    public void expand() {
        autoExpandIfBufferFull(0);
    }

    @Override
    public void expand(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length");
        }
        autoExpandIfBufferFull(length);
    }

    /**
     * 在所需空间不足情况下自动扩展空间容量
     */
    private void autoExpandIfBufferFull(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size");
        }

        int end = buffer.position() + size;
        if (end <= buffer.capacity()) {
            return;
        }

        int oldPos = buffer.position();
        capacity = end > (buffer.capacity() << 1) ? end : (buffer.capacity() << 1);
        byte[] bufNew = new byte[capacity];
        byte[] bufOld = getBufArray(buffer, 0, buffer.limit());
        System.arraycopy(bufOld, 0, bufNew, 0, bufOld.length);
        buffer = ByteBuffer.wrap(bufNew);
        buffer.position(oldPos);
        if (end > buffer.limit()) {
            buffer.limit(end);
        }
    }

    public static byte[] getBufArray(ByteBuffer buf) {
        return getBufArray(buf, 0, buf.limit());
    }

    /**
     * 使用DirectBuffer可以提高网络io时的性能，减少jvm中java内存和native内存之间的转换损耗
     * 但DirectBuffer不支持直接array()方法来得到java的内存byte[]，因此这里必须做一次转换
     */
    public static byte[] getBufArray (ByteBuffer buf,int offset, int length){
        if (buf.isDirect()) {
            int oldPos = buf.position();
            int oldLimit = buf.limit();
            try {
                byte[] bufTmp = new byte[length];
                buf.position(offset);
                buf.limit(offset + length);
                buf.get(bufTmp);
                return bufTmp;
            } finally {
                buf.limit(oldLimit);
                buf.position(oldPos);
            }
        } else {
            byte[] bufNew = new byte[length];
            System.arraycopy(buf.array(), offset, bufNew, 0, length);
            return bufNew;
        }
    }

    @Override
    public String toString () {
        StringBuilder info = new StringBuilder();
        info.append(getClass().getSimpleName() + "[");
        info.append("capacity=" + capacity());
        info.append(" readIdx=" + readIdx());
        info.append(" writeIdx=" + writeIdx());
        info.append("]");
        return info.toString();
    }
}
