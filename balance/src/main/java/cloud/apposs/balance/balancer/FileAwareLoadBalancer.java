package cloud.apposs.balance.balancer;

import cloud.apposs.balance.ILoadBalancer;
import cloud.apposs.balance.LbConfig;
import cloud.apposs.balance.Peer;
import cloud.apposs.balance.discovery.PollingDiscovery.DiscoveryAction;
import cloud.apposs.logger.Logger;
import cloud.apposs.util.IoUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * 基于配置文件定时读取更新的负载均衡
 */
public class FileAwareLoadBalancer extends DynamicLoadBalancer {
	public FileAwareLoadBalancer(String filename, FileAwarePeerParser parser) {
		this(new LbConfig(), filename, parser);
	}
	
	public FileAwareLoadBalancer(LbConfig config, String filename, FileAwarePeerParser parser) {
		super(config, new FileAwareDiscoveryAction(filename, parser));
	}
	
	static class FileAwareDiscoveryAction implements DiscoveryAction {
		private final File file;
		
		private long lastModified;
		
		private final FileAwarePeerParser parser;
		
		public FileAwareDiscoveryAction(String filename, FileAwarePeerParser parser) {
			if (filename == null) {
				throw new IllegalArgumentException("filename");
			}
			if (parser == null) {
				throw new IllegalArgumentException("parser");
			}
			
			File file = new File(filename);
			if (!file.exists()) {
				throw new IllegalArgumentException("file " + filename + " not exist");
			}
			this.file = file;
			this.parser = parser;
		}

		@Override
		public void discover(ILoadBalancer balancer) throws Exception {
			if (!file.exists()) {
				throw new FileNotFoundException(file.getAbsolutePath());
			}
			long lastModified = file.lastModified();
			if (this.lastModified == lastModified) {
				return;
			}
			
			FileInputStream in = null;
			try {
				in = new FileInputStream(file);
				List<Peer> allPeers = parser.parseServerList(in);
				if (allPeers != null) {
					balancer.addPeers(allPeers);
				}
			} finally {
				IoUtil.close(in);
			}
			
			this.lastModified = lastModified;
		}

		@Override
		public void cause(Throwable cause) {
			Logger.error(cause, "file discovery fail", file.getName());
		}
	}
	
	public interface FileAwarePeerParser {
		List<Peer> parseServerList(InputStream content);
	}
}
