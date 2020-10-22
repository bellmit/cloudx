package cloud.apposs.util;

/**
 * 数据采集和统计
 */
public interface DataCollector {
	/**
	 * 数据收集
	 */
	void collect(double value);
	
	/**
	 * 获取收集到的所有数据总数，
	 * 注意，有可能在进行当次计算统计后该数值会清零
	 */
	long getTotal();
	
	/**
	 * 获取样本最大数据
	 */
	double getMinimum();
	
	/**
	 * 获取样本最小数据
	 */
	double getMaximum();
	
	/**
	 * 获取样本平均数据
	 */
	double getMean();
	
	/**
	 * 获取样本数据标准差
	 */
	double getStdDev();
	
	/**
	 * 当次收集的数据样本总数
	 */
	int getSampleSize();
	
	/**
	 * 获取百分位数，耗费CPU计算性能
	 * 
	 * @param percent 百分线，如75线、95线
	 */
	double getPercentile(double percent);
	
	/**
	 * 统计95线数据，耗费CPU计算性能
	 * 
	 * @param percents 百分线，如75线、95线
	 * @param percentiles 统计结果存储
	 */
	double[] getPercentiles(double[] percents, double[] percentiles);
}
