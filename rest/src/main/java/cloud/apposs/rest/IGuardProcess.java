package cloud.apposs.rest;

public interface IGuardProcess<R, P> {
    /**
     * 解析获取Guard熔断附加参数，服务于熔断组件，
     * 一般是解析请求中的AID或者AID+REQUEST等组件来实现AID粒度组别的熔断
     *
     * @param  resource 熔断资源，供业务细粒度实现做判断，
     *                  细粒度的资源熔断可判断不同的资源来返回不同的GuardKey，也可以直接返回GuardKey
     * @param  request  HTTP请求体
     * @param  response HTTP响应体
     * @return 返回null则直接以resource作为熔断资源
     */
    Object getGuardKey(String resource, R request, P response);
}
