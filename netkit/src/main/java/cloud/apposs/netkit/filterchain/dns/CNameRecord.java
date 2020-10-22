package cloud.apposs.netkit.filterchain.dns;

import java.io.IOException;

public class CNameRecord extends Record {
    private Name cname;

    @Override
    void rrToWire(IoBufferAccessor accessor) throws IOException {
        cname.toWire(accessor);
    }

    @Override
    void rrFromWire(IoBufferAccessor accessor, int len) throws IOException {
        cname = Name.fromWire(accessor);
    }

    @Override
    Record getEmptyInstance() {
        return new CNameRecord();
    }

    public Name getCname() {
        return cname;
    }
}
