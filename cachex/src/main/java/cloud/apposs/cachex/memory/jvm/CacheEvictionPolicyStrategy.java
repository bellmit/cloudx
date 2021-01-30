package cloud.apposs.cachex.memory.jvm;

import cloud.apposs.util.StrUtil;

/**
 * 内存回收策略管理器
 */
public class CacheEvictionPolicyStrategy {
	public static final String CACHE_POLICY_LRU = "LRU";
	public static final String CACHE_POLICY_LFU = "LFU";
	
	public static CacheEvictionPolicy getCachePolicy(String policy) {
		if (StrUtil.isEmpty(policy)) {
			return null;
		}
		
		policy = policy.toUpperCase();
		if (CACHE_POLICY_LRU.equals(policy)) {
			return new LruEvictionPolicy();
		} else if (CACHE_POLICY_LFU.equals(policy)) {
			return new LfuEvictionPolicy();
		}
		
		return null;
	}
}
