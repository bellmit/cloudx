package cloud.apposs.rest.parameter;

/**
 * {@link cloud.apposs.rest.annotation.Model}参数对象注入，
 * 被注解的参数如果实现此接口则连同流水号等内部属性注入，减少代码的编写
 * 具体参看{@link BodyParameterResolver}和其实对应的继承类 ModelParameterResolver，
 * 之所以设计参数以对象的形式传递有以下几个好处
 * 1. 提升开发效率
 * 2. 通过@Model参数可直接判断要传递的参数是什么
 * 3. 由框架来协助进行参数校验
 */
public interface Parametric<R, P> {
    /** 请求流水号 */
    long getFlow();
    void setFlow(long flow);

    /**
     * HTTP请求对象
     */
    R getRequest();
    void setRequest(R request);

    /**
     * HTTP响应对象
     */
    P getResponse();
    void setResponse(P response);
}
