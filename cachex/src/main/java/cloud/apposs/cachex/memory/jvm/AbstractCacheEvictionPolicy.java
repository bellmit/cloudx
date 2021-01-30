package cloud.apposs.cachex.memory.jvm;

import java.util.List;

public abstract class AbstractCacheEvictionPolicy implements CacheEvictionPolicy {
	@Override
	public Element selectedBasedOnPolicy(List<Element> elements) {
		Element lowestElement = null;
        for (Element element : elements) {
            if (lowestElement == null) {
            	lowestElement = element;
            } else if (compare(lowestElement, element)) {
            	lowestElement = element;
            }
        }
        return lowestElement;
	}
}
