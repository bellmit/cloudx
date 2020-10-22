package cloud.apposs.netkit.filterchain.dns;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * DNS 协议
 * https://blog.csdn.net/tianxuhong/article/details/74922454
 * https://blog.csdn.net/zhuzeyu22/article/details/52277174
 */
public class DnsMessage {
    /**
     * 协议头部
     */
    private Header header;

    /**
     * 资源记录
     */
    private List<Record>[] sections;

    @SuppressWarnings("unchecked")
	public DnsMessage(Header header) {
        this.header = header;
        sections = new List[4];
    }

    public DnsMessage() {
        this(new Header());
    }

    public DnsMessage(int id) {
        this(new Header(id));
    }

    public static DnsMessage newQuery(String domain, int type) {
        return newQuery(domain, type, DClass.IN, 0);
    }

    public static DnsMessage newQuery(String domain, int type, int  dclass, int ttl) {
        Record r = Record.newRecord(new Name(domain), type, dclass, ttl);
        return newQuery(r);
    }

    public static DnsMessage newQuery(Record... records) {
        DnsMessage m = new DnsMessage();
        m.header.setOpCode(OpCode.QUERY);
        m.header.setRD(true);
        for (Record r : records) {
            m.addRecord(r, Section.QUERY);
        }
        return m;
    }

    /**
     * 根据头部将协议解析为 {@link DnsMessage}
     */
    static DnsMessage fromWire(Header header, IoBufferAccessor accessor) throws IOException {
        DnsMessage answer = new DnsMessage(header);
        try {
            for(int i = 0; i < 4 ; i++) {
                int recordNum = header.getCount(i);
                List<Record> parsedRecords = answer.sections[i] = new LinkedList<Record>();
                for (; recordNum > 0; recordNum--) {
                    parsedRecords.add(Record.fromWire(accessor, i == Section.QUERY));
                }
            }
        } catch (IOException e) {
            // 如果被截断了直接返回
            if (!header.getTC()) {
                throw e;
            }
        }
        return answer;
    }

    /**
     * 添加记录
     */
    public void addRecord(Record r, int section) {
        if (sections[section] == null) {
            sections[section] = new LinkedList<Record>();
        }
        header.incCount(section);
        sections[section].add(r);
    }

    public void toWire(IoBufferAccessor accessor) throws IOException {
        header.toWire(accessor);
        for (int i = 0; i < 4; i++) {
            List<Record> records = sections[i];
            if (records != null) {
                for (Record rec : records) {
                    rec.toWire(accessor, i == Section.QUERY);
                }
            }
        }
    }

    public List<Record> getRecords(int field){
        return sections[field];
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public class Section {
        public static final int QUERY = 0;

        public static final int ANSWER = 1;

        public static final int AUTHORITY = 2;

        public static final int ADDITIONAL = 3;
    }

}
