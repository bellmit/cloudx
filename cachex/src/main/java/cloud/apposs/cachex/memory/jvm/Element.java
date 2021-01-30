package cloud.apposs.cachex.memory.jvm;

import java.io.Serializable;

/**
 * 数据缓存节点，维护缓存数据、命中次数，缓存时间等，
 * 可以实现一级缓存、二级缓存等
 */
public interface Element extends Serializable, Cloneable {
	/**
	 * 获取缓存Key
	 */
	String getKey();

	/**
	 * 获取缓存值
	 */
	Object getValue();
	
	/**
	 * 获取缓存值
	 * 
	 * @param update 是否更新统计信息，包括访问命中次数等
	 */
	Object getValue(boolean update);

	/**
	 * 获取缓存命中次数
	 */
	long getHitCount();
	
	/**
	 * 获取缓存字节大小
	 */
	int getByteSize();

	/**
	 * 获取缓存创建时间
	 */
	long getCreationTime();

	/**
	 * 获取缓存最近一次访问时间
	 */
	long getLastAccessTime();
	
	/**
	 * 获取缓存过期时间
	 */
	int getExpirationTime();

	/**
	 * 设置缓存过期时间
	 * 
	 * @param  expirationTime 过期时间
	 * @return 原先过期时间，单位毫秒
	 */
	int setExpirationTime(int expirationTime);
	
	/**
	 * 判断缓存是否过期
	 * 
	 * @return 过期返回true
	 */
	boolean isExpired();
}
