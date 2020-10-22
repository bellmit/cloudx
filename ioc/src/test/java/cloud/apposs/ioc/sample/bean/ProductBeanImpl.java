package cloud.apposs.ioc.sample.bean;

import cloud.apposs.ioc.annotation.Component;

@Component
public class ProductBeanImpl implements IProductBean {
	@Override
	public int getProductId() {
		return 100;
	}
}
