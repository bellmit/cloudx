package cloud.apposs.netkit.server.fai;

public class NKDef {
	private NKDef() {
	}

	public static final class Protocol {
		public static final class Cmd {
			public static final int STAT = 1;
			public static final int GC = 2;
			public static final int HELO = 3;
			public static final int CLEAR_CACHE = 4;
			public static final int CLEAR_ACCT = 5;
			public static final int FLUSH_CONF = 6;
		}

		public static final class Key {
			public static final int STAT = 0;
			public static final int HELO = 0;
		}
	}
}
