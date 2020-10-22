package cloud.apposs.netkit.filterchain.dns;

import java.io.IOException;

/**
 * MX记录
 * http://help.dnsmadeeasy.com/managed-dns/dns-record-types/mx-record/
 * https://kb.intermedia.net/Article/903
 */
public class MXRecord extends Record{
    private int priority;

    private Name sever;

    @Override
    void rrToWire(IoBufferAccessor accessor) throws IOException {
        accessor.writeU16(priority);
        sever.toWire(accessor);
    }

    @Override
    void rrFromWire(IoBufferAccessor accessor, int len) throws IOException {
        priority = accessor.readU16();
        sever = Name.fromWire(accessor);
    }

    @Override
    Record getEmptyInstance() {
        return new MXRecord();
    }

    public int getPriority() {
        return priority;
    }

    public Name getSever() {
        return sever;
    }
}
