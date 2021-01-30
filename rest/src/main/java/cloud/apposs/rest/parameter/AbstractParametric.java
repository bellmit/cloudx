package cloud.apposs.rest.parameter;

/**
 * 详见{@link Parametric}
 * 当然Model类也不用实现此接口，自身实现这些方法也能够实现对应参数的注入，只是减少代码编码
 */
public abstract class AbstractParametric<R, P> implements Parametric<R, P> {
    protected long flow;

    protected R request;

    protected P response;

    @Override
    public long getFlow() {
        return flow;
    }

    /**
     * 流水号注入
     */
    @Override
    public void setFlow(long flow) {
        this.flow = flow;
    }

    @Override
    public R getRequest() {
        return request;
    }

    /**
     * 请求对象注入
     */
    @Override
    public void setRequest(R request) {
        this.request = request;
    }

    @Override
    public P getResponse() {
        return response;
    }

    /**
     * 响应对象注入
     */
    @Override
    public void setResponse(P response) {
        this.response = response;
    }
}
