package cloud.apposs.netkit.filterchain.dns;

import java.io.IOException;

/**
 * 未知资源记录
 */
public class UNKRecord extends Record{
    private byte[] data;

    @Override
    void rrToWire(IoBufferAccessor accessor) throws IOException {
        accessor.writeBytes(data, 0, data.length);
    }

    @Override
    void rrFromWire(IoBufferAccessor accessor, int len) throws IOException {
        data = new byte[len];
        accessor.readBytes(data, 0, len);
    }

    @Override
    Record getEmptyInstance() {
        return new UNKRecord();
    }

    public byte[] getData() {
        return data;
    }
}
