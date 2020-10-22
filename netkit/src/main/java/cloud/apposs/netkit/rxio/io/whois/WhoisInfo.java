package cloud.apposs.netkit.rxio.io.whois;

public class WhoisInfo {
	private final String domain;
	
	private final String info;
	
	private final boolean isReg;
	
	public WhoisInfo(String domain, String info, boolean isReg) {
		this.domain = domain;
		this.info = info;
		this.isReg = isReg;
	}

	public String getDomain() {
		return domain;
	}

	public String getInfo() {
		return info;
	}

	public boolean isReg() {
		return isReg;
	}

	@Override
	public String toString() {
		return info;
	}
}
