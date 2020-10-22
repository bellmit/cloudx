package cloud.apposs.netkit.filterchain.dns;

import java.io.IOException;

public class NSRecord extends Record {
    private Name host;

    @Override
    void rrToWire(IoBufferAccessor accessor) throws IOException {
        host.toWire(accessor);
    }

    @Override
    void rrFromWire(IoBufferAccessor accessor, int len) throws IOException {
        host = Name.fromWire(accessor);
    }

    @Override
    Record getEmptyInstance() {
        return new NSRecord();
    }

    public Name getHost() {
        return host;
    }
}
