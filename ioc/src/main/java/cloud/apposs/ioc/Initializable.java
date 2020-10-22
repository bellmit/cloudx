package cloud.apposs.ioc;

/**
 * IOC容器中实现此接口的类在初次创建时会自动调用初始化
 */
public interface Initializable {
    /**
     * 类实例创建时的初始化调用
     */
    void initialize() throws InstantiationException;
}
