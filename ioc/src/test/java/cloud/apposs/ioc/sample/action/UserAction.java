package cloud.apposs.ioc.sample.action;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.sample.bean.IProductBean;
import cloud.apposs.ioc.sample.bean.UserBean;

@Component
public class UserAction {
    @Autowired
    private UserBean user;

    private IProductBean product;

    private String userProduct;

    public UserBean getUser() {
        return user;
    }

    public IProductBean getProduct() {
        return product;
    }

    @Autowired
    public void setProduct(IProductBean product) {
        this.product = product;
    }

    /**
     * 同时注入两个参数到方法中
     */
    @Autowired
    public void setUserProduct(UserBean user, IProductBean product) {
        this.userProduct = user + ":" + product;
    }

    public String getUserProduct() {
        return userProduct;
    }
}
