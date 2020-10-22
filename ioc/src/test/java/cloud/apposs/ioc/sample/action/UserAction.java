package cloud.apposs.ioc.sample.action;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.ioc.annotation.Inject;
import cloud.apposs.ioc.sample.bean.IProductBean;
import cloud.apposs.ioc.sample.bean.UserBean;

@Component
public class UserAction {
    @Inject
    private UserBean user;

    private IProductBean product;

    private String userProduct;

    public UserBean getUser() {
        return user;
    }

    public IProductBean getProduct() {
        return product;
    }

    @Inject
    public void setProduct(IProductBean product) {
        this.product = product;
    }

    /**
     * 同时注入两个参数到方法中
     */
    @Inject
    public void setUserProduct(UserBean user, IProductBean product) {
        this.userProduct = user + ":" + product;
    }

    public String getUserProduct() {
        return userProduct;
    }
}
