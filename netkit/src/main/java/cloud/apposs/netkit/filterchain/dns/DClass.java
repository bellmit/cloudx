package cloud.apposs.netkit.filterchain.dns;

public class DClass {
    /** Internet */
    public static final int IN = 1;

    public static void check(int dclass){
        if (dclass < 0 || dclass > 0xFFFF) {
            throw new IllegalArgumentException("invalid dclass:" + dclass);
        }
    }
}
