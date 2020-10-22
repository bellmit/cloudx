package cloud.apposs.netkit.filterchain.executor;

import java.util.List;

public interface ThreadPoolHandler {
	/**
	 * 决定创建什么类型的线程池组，
	 * 例如有读CMD/写CMD/内部线程池组，
	 * 以不同的线程池组来拆分可以隔离各个CMD指令逻辑处理，
	 * 如果返回NULL则采用默认线程池组处理所有CMD请求
	 */
	List<ThreadPool> createPoolGroups();

	/**
	 * 根据不同的指令返回不同的线程池和线程组来执行指令任务
	 */
	ThreadPoolType getThreadPoolType(Object message);
}
