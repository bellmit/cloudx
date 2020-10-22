package cloud.apposs.netkit.client.pool;

import cloud.apposs.netkit.client.IoClient;

public interface IoClientFactory {
	IoClient createClient(Class<? extends IoClient> clazz) throws Exception;
}
