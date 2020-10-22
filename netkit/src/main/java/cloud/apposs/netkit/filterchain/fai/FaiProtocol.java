package cloud.apposs.netkit.filterchain.fai;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.buffer.ByteBuf;
import cloud.apposs.netkit.buffer.IoAllocator;

import java.io.IOException;
import java.nio.ByteBuffer;

public class FaiProtocol {
    /**
     * 数据包头
     */
    private Head head = new Head();

    /**
     * 数据包体
     */
    private IoBuffer body;

    /**
     * 当前是否在接收数据包头部，数据发送是先发送数据包头，再发送数据包体
     */
    private boolean recvHead = true;
    private boolean recvHeadEx = true;

    protected boolean onRead(IoBuffer buf) throws IOException {
        // 先接收包头数据
        if (recvHead && !recvHead(buf)) {
            return false;
        }
        if (recvHeadEx && !recvHeadEx(buf)) {
            return false;
        }
        // 接收包体数据
        return recvBody(buf);
    }

    /**
     * 阻塞获取head数据
     *
     * @return 数据是否接收完整
     */
    private boolean recvHead(IoBuffer buf) throws IOException {
        ByteBuffer headBuf = head.getHeadBuf();
        // 接收数据还不完整，先缓存下来
        if (headBuf.remaining() > buf.writeIdx()) {
            headBuf.put(buf.array());
            buf.readIdx(buf.writeIdx());
            return false;
        }
        int readHeadLen = Head.HEADER_LEN - headBuf.position();
        headBuf.put(buf.array(0, readHeadLen));
        buf.readIdx(readHeadLen);
        // 接收包头数据完毕，解析包头信息
        head.decode();
        recvHead = false;
        return true;
    }

    /**
     * 阻塞获取head扩展数据
     *
     * @return 数据是否接收完整
     */
    private boolean recvHeadEx(IoBuffer buf) {
        int headLenEx = head.getHeadExLen();
        if (headLenEx <= 0) {
            return true;
        }

        // 接收包头扩展数据完毕，解析包头扩展信息，目前没什么用
        head.decodeEx();
        recvHeadEx = false;
        return true;
    }

    /**
     * 非阻塞获取包体数据
     */
    private boolean recvBody(IoBuffer buf) throws IOException {
        // 接收包体数据
        int bodyLen = head.getBodyLen();
        if (bodyLen <= 0) {
            // 没有包体数据，直接返回
            return true;
        }
        if (body == null) {
            body = IoAllocator.allocate(bodyLen, true);
        }
        long needBodyLen = bodyLen - body.writeIdx();
        // 只接收部分数据，先缓存下来退出等待下次数据接收
        if (buf.readableBytes() < needBodyLen) {
            body.put(buf, buf.readIdx(), (int) buf.readableBytes());
            return false;
        }

        body.put(buf, buf.readIdx(), needBodyLen);

        // 完整接收数据
        return true;
    }

    public void body(ByteBuffer buffer) throws IOException {
        if (buffer == null) {
            return;
        }
        buffer.flip();
        if (body == null) {
            body = IoAllocator.allocate(buffer.limit());
        }
        body.put(buffer);
    }

    public ByteBuffer body() throws IOException {
        ByteBuffer buffer = body.buffer();
        buffer.flip();
        return buffer;
    }

    public IoBuffer buffer() throws IOException {
        int bodyLen = 0;
        if (body != null) {
            bodyLen = (int) body.writeIdx();
        }
        IoBuffer buf = IoAllocator.allocate(Head.HEADER_LEN + bodyLen);
        head.setBodyLen(bodyLen);
        ByteBuffer headBuf = head.encode();
        buf.put(headBuf);
        ByteBuffer headExBuf = head.encodeEx();
        if (headExBuf != null) {
            buf.put(headExBuf);
        }
        if (body != null) {
            buf.put(body);
        }
        return buf;
    }

    public int aid() {
        return head.getAid();
    }

    public void aid(int aid) {
        head.setAid(aid);
    }

    public int cmd() {
        return head.getCmd();
    }

    public void cmd(int cmd) {
        head.setCmd((short) cmd);
    }

    public int flow() {
        return head.getFlow();
    }

    public void flow(int flow) {
        head.setFlow(flow);
    }

    public short getWid() {
        return head.getWid();
    }

    public void setWid(short wid) {
        head.setWid(wid);
    }

    public int result() {
        return head.getResult();
    }

    public void result(int result) {
        head.setResult((short) result);
    }

    public Object getLimitKey() {
        return head.getAid();
    }

    public void reset() {
        if (body != null) {
            body.reset();
        }
    }

    public void clear() {
        if (body != null) {
            body.free();
        }
    }

    public static class Head {
        /**
         * 包头大小，固定值
         */
        public static final int HEADER_LEN = 32;
        /**
         * 一个特别的数字，用于解包时校验是否合法包。（此字段貌似必要性不大的）
         */
        public static final short MAGIC = 0x1122;
        /**
         * 协议版本号，如果head的格式有变化，例如扩展成40个字节，此时解包的地方就可以根据version来做兼容处理
         */
        public static final byte VERSION = 0x1;

        /**
         * 包头扩展的长度，如果有特殊协议需要在原包头和包体之间扩展填充一些数据，可以使用。
         * （此字段基本没有太大作用，要扩展时可以用协议好，或者直接在包体中加，这里有点过度设计）
         */
        public byte headExLen = (byte) 0;
        /**
         * 包体长度
         */
        private int bodyLen = 0;

        /**
         * 接收到的包头数据
         */
        private ByteBuffer headBuf = ByteBuffer.allocate(Head.HEADER_LEN);
        private ByteBuffer headBufEx;

        /**
         * 特殊标志，例如当前是否需要keep-alive，是否send-only（无需回包）
         */
        private short flag = 0;
        /**
         * 流水号，例如一个页面请求时，会生成一个flow，然后这个请求过程中，所有的数据包都带有该flow，这样再调试问题时会非常方便
         */
        private int flow = 0;
        /**
         * 用户aid
         */
        private int aid = 0;
        /**
         * 协议命令号
         */
        private short cmd = 0;
        /**
         * 处理结果
         */
        private short result = 0;
        /**
         * 用户wid，目前用于建站产品的多语言方案，不同的wid表示不同的语言版本
         */
        private short wid = 0;

        public int getFlow() {
            return flow;
        }

        public void setFlow(int flow) {
            this.flow = flow;
        }

        public int getAid() {
            return aid;
        }

        public void setAid(int aid) {
            this.aid = aid;
        }

        public short getCmd() {
            return cmd;
        }

        public void setCmd(short cmd) {
            this.cmd = cmd;
        }

        public short getWid() {
            return wid;
        }

        public void setWid(short wid) {
            this.wid = wid;
        }

        public short getResult() {
            return result;
        }

        public void setResult(short result) {
            this.result = result;
        }

        public int getBodyLen() {
            return bodyLen;
        }

        public void setBodyLen(int bodyLen) {
            this.bodyLen = bodyLen;
        }

        public ByteBuffer getHeadBuf() {
            return headBuf;
        }

        public ByteBuffer getHeadBufEx() {
            return headBufEx;
        }

        public void setHeadBufEx(ByteBuffer headBufEx) {
            this.headBufEx = headBufEx;
        }

        public void decode() throws IOException {
            headBuf.rewind();
            if (headBuf.limit() != Head.HEADER_LEN) {
                throw new IOException("decode head error;len=" + headBuf.limit());
            }
            short magic = headBuf.getShort();
            if (magic != MAGIC) {
                throw new IOException("decode head error;magic=" + magic);
            }
            byte version = headBuf.get();
            if (version != VERSION) {
                throw new IOException("decode head error;version=" + version);
            }
            headExLen = headBuf.get();
            bodyLen = headBuf.getInt();
            if (bodyLen < 0) {
                throw new IOException("decode head error;bodyLen=" + bodyLen);
            }
            short chksum = headBuf.getShort();
            flag = headBuf.getShort();
            flow = headBuf.getInt();
            aid = headBuf.getInt();
            cmd = headBuf.getShort();
            result = headBuf.getShort();
            wid = headBuf.getShort();
            // 保留字段
            short reserved1 = headBuf.getShort();
            int reserved2 = headBuf.getInt();
            if (reserved1 != 0 || reserved2 != 0) {
                String error = String.format("decode head error;reserved1=%x;reserved2=%x", reserved1, reserved2);
                throw new IOException(error);
            }

            // 对head中除chksum外的数据计算校验值，解包时校对该值是否一致，如果不一致则数据传输过程中有损坏
            headBuf.putShort(8, (short) 0);
            byte[] bufTmp = ByteBuf.getBufArray(headBuf);
            if (chksum != genChksum(bufTmp, HEADER_LEN)) {
                throw new IOException("decode head checksum error;chksum=" + chksum);
            }
            headBuf.position(HEADER_LEN);
        }

        public ByteBuffer encode() {
            ByteBuffer buf = ByteBuffer.allocate(HEADER_LEN);
            short chksum = 0;
            buf.putShort(MAGIC);
            buf.put(VERSION);
            buf.put(headExLen);
            buf.putInt(bodyLen);
            buf.putShort(chksum);
            buf.putShort(flag);
            buf.putInt(flow);
            buf.putInt(aid);
            buf.putShort(cmd);
            buf.putShort(result);
            buf.putShort(wid);
            short reserved1 = 0;
            buf.putShort(reserved1);
            int reserved2 = 0;
            buf.putInt(reserved2);

            byte[] array = ByteBuf.getBufArray(headBuf);
            chksum = genChksum(array, HEADER_LEN);
            buf.putShort(8, chksum);

            buf.position(HEADER_LEN);
            buf.rewind();

            return buf;
        }

        public ByteBuffer encodeEx() {
            if (headExLen <= 0) {
                return null;
            }
            ByteBuffer buf = ByteBuffer.allocate(headExLen);
            for (int i = 0; i < headExLen; ++i) {
                buf.put((byte) 0);
            }
            buf.rewind();
            return buf;
        }

        public boolean decodeEx() {
            return true;
        }

        public byte getHeadExLen() {
            return headExLen;
        }

        private short genChksum(byte[] buf, int checkLen) {
            int sum = 0;
            int len = checkLen / 2;
            int mod = checkLen % 2;
            // Our algorithm is simple, using a 32 bit accumulator (sum), we add
            // sequential 16 bit words to it, and at the end, fold back all the
            // carry bits from the top 16 bits into the lower 16 bits.
            for (int i = 0; i < len; i++) {
                sum += buf[i];
            }
            // 4mop up an odd byte, if necessary
            if (mod == 1) {
                byte b = buf[checkLen - 1];
                sum += b;
            }
            // 4add back carry outs from top 16 bits to low 16 bits
            // add hi 16 to low 16
            sum = (sum >>> 16) + (sum & 0xffff);
            // add carry
            sum += (sum >>> 16);
            // truncate to 16 bits
            return (short) ~((short) sum);
        }
    }
}
