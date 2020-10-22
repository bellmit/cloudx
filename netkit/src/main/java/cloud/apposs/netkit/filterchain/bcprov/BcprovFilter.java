package cloud.apposs.netkit.filterchain.bcprov;

import cloud.apposs.netkit.filterchain.IoFilterAdaptor;

import java.security.Provider;
import java.security.Security;

public class BcprovFilter extends IoFilterAdaptor {
	@Override
	public void init() {
		try {
			// 解决https请求中如果密钥过大会报Prime size must be multiple of 64, and can only range from 512 to 1024 (inclusive)
			// 参考：https://stackoverflow.com/questions/6851461/java-why-does-ssl-handshake-give-could-not-generate-dh-keypair-exception
			Class<?> bcProvider = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
			if (bcProvider != null) {
				Security.addProvider((Provider) bcProvider.newInstance());
			}
		} catch (Exception e) {
		}
	}
}
