package cloud.apposs.netkit.filterchain.dns;

import java.io.IOException;

/**
 * 资源记录类
 */
public abstract class Record {
    private Name name;

    private int type;

    private int dclass;

    private long ttl;

    protected Record(){}

    public Record(String domain, int type, int dclass) {
        this(new Name(domain), type, dclass);
    }

    public Record(Name name, int type, int dclass) {
        this(name, type, dclass, 0);
    }

    public Record(Name name, int type, int dclass, long ttl) {
        DnsRecordType.checkType(type);
        DClass.check(dclass);
        this.name = name;
        this.type = type;
        this.dclass = dclass;
        this.ttl = ttl;
    }

    /**
     * 子类将资源记录数据转换为协议
     */
    abstract void rrToWire(IoBufferAccessor accessor) throws IOException;

    /**
     * 子类解析资源记录的具体数据
     */
    abstract void rrFromWire(IoBufferAccessor accessor, int len) throws IOException;

    /**
     * 获取一个子类空记录
     */
    abstract Record getEmptyInstance();

    static Record newRecord(Name name, int type, int dclass, long ttl) {
        return new EmptyRecord(name, type, dclass, ttl);
    }

    /**
     * 解析有数据的资源记录
     */
    static Record newRecord(Name name, int type, int dclass, long ttl, int len, IoBufferAccessor accessor) throws IOException {
        Record proto = DnsRecordType.getProto(type);
        Record rec;
        if (proto != null) {
            rec = proto.getEmptyInstance();
        } else {
            rec = new UNKRecord();
        }
        rec.name = name;
        rec.type = type;
        rec.dclass = dclass;
        rec.ttl = ttl;
        accessor.need(len);
        rec.rrFromWire(accessor, len);
        return rec;
    }

    /**
     * 解析资源记录
     * @param isQuery 如果是 QUERY 域的不存在 ttl、length、data
     */
    static Record fromWire(IoBufferAccessor accessor, boolean isQuery) throws IOException {
        Record ret;
        Name name = Name.fromWire(accessor);
        int type = accessor.readU16();
        int dclass = accessor.readU16();
        if (isQuery) {
            ret = new EmptyRecord(name, type, dclass);
        } else {
            long ttl = accessor.readU32();
            int length = accessor.readU16();
            if (length == 0) {
                ret = new EmptyRecord(name, type, dclass, ttl);
            } else {
                ret = newRecord(name, type, dclass, ttl, length, accessor);
            }
        }
        return ret;
    }

    /**
     * 将资源记录解析为协议
     */
    void toWire(IoBufferAccessor accessor, boolean isQuery) throws IOException {
        name.toWire(accessor);
        accessor.writeU16(type);
        accessor.writeU16(dclass);
        if (!isQuery) {
            accessor.writeU32(ttl);
            accessor.writeU16(0);
            long oldIdx = accessor.writeIdx();
            rrToWire(accessor);
            long length = accessor.writeIdx() - oldIdx;
            accessor.writeU16At(length, oldIdx - 2);
        }
    }

}
