package cloud.apposs.ioc;

import cloud.apposs.util.SysUtil;

/**
 * bean标签的定义信息，是bean在内存中的描述形式
 */
public final class BeanDefinition {
	/** Bean名称(全路径) */
	private String beanName;
	
	/** Bean类 */
	private Class<?> beanClass;
	
	/** 是否每次获得Bean都会生成一个新的对象，默认为单例 */
	private boolean prototype;
	
	public BeanDefinition(Class<?> beanClass, boolean prototype) {
		SysUtil.checkNotNull(beanClass, "beanClass");
		
		this.beanName = beanClass.getName();
		this.beanClass = beanClass;
		this.prototype = prototype;
	}
	
	public BeanDefinition(Object bean) {
	}

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public Class<?> getBeanClass() {
		return beanClass;
	}

	public void setBeanClass(Class<?> beanClass) {
		this.beanClass = beanClass;
	}

	public boolean isPrototype() {
		return prototype;
	}

	public void setPrototype(boolean prototype) {
		this.prototype = prototype;
	}

	@Override
	public String toString() {
		StringBuilder info = new StringBuilder(1024);
		info.append("beanName:").append(beanName).append("\n");
		info.append("prototype:").append(prototype);
		return info.toString();
	}
}
