package cloud.apposs.netkit.filterchain.dns;

import java.util.HashMap;
import java.util.Map;

/**
 * 查询类型
 * 
 * 参考：
 * https://en.wikipedia.org/wiki/List_of_DNS_record_types
 */
public class DnsRecordType {
    /**
     * 返回 32 位的 Ip 地址，通常用于根据域名查询 Ip 地址
     */
    public static final int A = 1;

    /** 域名服务器 */
	public static final int NS = 2;

    /** 别名 */
	public static final int CNAME = 5;

	public static final int SOA = 6;

	public static final int WKS = 11;

	public static final int PTR = 12;

	public static final int HINFO = 13;

	public static final int MX = 15;

    public static final int TXT	= 16;

	public static final int AAAA = 28;

	public static final int AXFR = 252;

	public static final int ANY = 255;

    private static final Map<Integer, Record> recordMap = new HashMap<Integer, Record>();

    static {
        recordMap.put(A, new ARecord());
        recordMap.put(NS, new NSRecord());
        recordMap.put(CNAME, new CNameRecord());
        recordMap.put(MX, new MXRecord());
        recordMap.put(TXT, new TXTRecord());
    }

    public static Record getProto(int type) {
        return recordMap.get(type);
    }

	public static void checkType(int type) {
	    if (type < 0 || type > 0xFFFF) {
	        throw new IllegalArgumentException("invalid type:" + type);
        }
    }
}
