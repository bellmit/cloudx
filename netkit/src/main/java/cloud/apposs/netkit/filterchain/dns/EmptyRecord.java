package cloud.apposs.netkit.filterchain.dns;

public class EmptyRecord extends Record {
    public EmptyRecord() {
    }

    public EmptyRecord(Name name, int type, int dclass) {
        this(name, type, dclass, 0);
    }

    public EmptyRecord(Name name, int type, int dclass, long ttl) {
        super(name, type, dclass, ttl);
    }

    @Override
    void rrToWire(IoBufferAccessor accessor) {
    }

    @Override
    void rrFromWire(IoBufferAccessor accessor, int len) {
    }

    @Override
    Record getEmptyInstance() {
        return new EmptyRecord();
    }
}
