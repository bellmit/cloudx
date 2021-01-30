package cloud.apposs.guard.node;

/**
 * 数据节点，负责在服务运行时的请求的收集和数据统计
 */
public interface Node {
    /**
     * 正常通过的 QPS 值
     */
    public long passQps();

    /**
     * 阻断的 QPS
     */
    public long blockQps();

    /**
     * 异常的 QPS
     */
    public long exceptionQps();

    /**
     * 只要通过了获取令牌就是 success
     */
    public long successQps();

    /**
     * 所有的请求，pass + block
     */
    public long totalQps() ;

    public void addException(int count);

    public void addBlock(int count);

    public void addPass(int count);

    public void addRespTimeAndSuccCount(long respTime, int count);

    /**
     * 平均响应时间
     */
    public long avgRespTime();
}
