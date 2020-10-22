package cloud.apposs.netkit.filterchain.dns;

import java.io.IOException;

public class ARecord extends Record {
    private int addr;

    @Override
    void rrToWire(IoBufferAccessor accessor) throws IOException {
        accessor.writeU32(((long)addr) & 0xFFFFFFFFL);
    }

    @Override
    void rrFromWire(IoBufferAccessor accessor, int len) throws IOException {
        addr = (int) accessor.readU32();
    }

    @Override
    Record getEmptyInstance() {
        return new ARecord();
    }

    public int getAddr() {
        return addr;
    }

    public String getAddrInString() {
        String part1 = String.valueOf((addr >> 24) & 0xFF);
        String part2 = String.valueOf((addr >> 16) & 0xFF);
        String part3 = String.valueOf((addr >> 8) & 0xFF);
        String part4 = String.valueOf(addr & 0xFF);
        return part1 + "." + part2 + "." + part3 + "." + part4;
    }

	@Override
	public String toString() {
		return getAddrInString();
	}
}
