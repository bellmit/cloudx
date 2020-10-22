package cloud.apposs.netkit.filterchain.dns;

import java.io.IOException;

public class TXTRecord extends Record {
    private String txt;

    @Override
    void rrToWire(IoBufferAccessor accessor) throws IOException {
        byte[] bytes = txt.getBytes();
        accessor.writeBytes(bytes, 0, bytes.length);
    }

    @Override
    void rrFromWire(IoBufferAccessor accessor, int len) throws IOException {
        byte[] bytes = new byte[len];
        accessor.readBytes(bytes, 0, len);
        txt = new String(bytes);
    }

    @Override
    Record getEmptyInstance() {
        return new TXTRecord();
    }

    public String getTxt() {
        return txt;
    }
}
