package cloud.apposs.cachex.memory.jvm;

import java.util.List;

/**
 * 缓存回收策略服务
 */
public interface CacheEvictionPolicy {
	/**
	 * 获取策略名称
	 * 
     * @return LRU, LFU
     */
    String getName();
    
    /**
     * 从缓存列表中取出最符合回收条件的缓存数据
     * 
     * @param  elements 缓存列表
     * @return 队伍回收条件的缓存数据
     */
    Element selectedBasedOnPolicy(List<Element> elements);
    
    /**
     * 匹配缓存节点是否符合回收条件
     * 
     * @param  element1 第一个要匹配的缓存数据
     * @param  element2 第二个要匹配的缓存数据
     * @return 命中第二个缓存数据返回true，否则返回false
     */
    boolean compare(Element element1, Element element2);
}
