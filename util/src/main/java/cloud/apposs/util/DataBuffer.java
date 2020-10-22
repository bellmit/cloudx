package cloud.apposs.util;

import java.util.Arrays;

/**
 * 数据采集服务，包括对数据的存储采集和数据归档统计，支持
 * <pre>
 * 1、平均响应时间统计
 * 2、最大响应时间统计
 * 3、最小响应时间统计
 * 4、75线、95线、99线响应时间定时统计
 * </pre>
 * 参考：
 * https://testerhome.com/topics/11423
 */
public class DataBuffer implements DataCollector {
    /** 数据收集总条数 */
    private long numValue;
    /** 所有数据总和 */
    private double sumValue;
    /** 最小数据 */
    private double minValue;
    /** 最大数据 */
    private double maxValue;
    /** 平方数据 */
    private double sumSquareValue;
    
    /** 数据采集存储 */
    private double[] buffer;
	
	private int position;
	
	private int limit;
	
	/** 样本统计的开始、结束时间 */
	private long startMillis;
    private long finishMillis;
	
	public DataBuffer(int capacity) {
		this.buffer = new double[capacity];
		this.position = 0;
		this.limit = 0;
		this.startMillis = System.currentTimeMillis();
		this.numValue = 0L;
		this.sumValue = 0.0;
		this.minValue = 0.0;
		this.maxValue = 0.0;
		this.sumSquareValue = 0;
	}
	
	@Override
	public void collect(double value) {
		// 更新统计数据
		numValue++;
		sumValue += value;
		sumSquareValue += value * value;
		if (numValue == 1) {
            minValue = value;
            maxValue = value;
        } else if (value < minValue) {
            minValue = value;
        } else if (value > maxValue) {
            maxValue = value;
        }
		
		// 添加采集数据
		buffer[position++] = value;
		if (position >= buffer.length) {
			// 缓存统计数据已经到达最大容量，下次统计数据从索引0开始，
			// 丢失多余的数据来避免收集数据过多爆内存
			position = 0;
			limit = buffer.length;
		} else if (position > limit) {
			limit = position;
		}
	}
	
	@Override
    public double getMinimum() {
        return minValue;
    }

    @Override
    public double getMaximum() {
        return maxValue;
    }
    
    @Override
    public double getMean() {
    	if (numValue <= 0) {
    		return 0;
    	}
    	return sumValue / numValue;
    }
    
    @Override
    public double getStdDev() {
        return Math.sqrt(getVariance());
    }
    
    public double getVariance() {
        if (numValue < 2) {
            return 0.0;
        } else if (sumValue == 0.0) {
            return 0.0;
        } else {
            double mean = getMean();
            return (sumSquareValue / numValue) - mean * mean;
        }
    }
	
    @Override
	public long getTotal() {
		return numValue;
	}

	@Override
	public int getSampleSize() {
		return limit;
	}
	
	@Override
	public double getPercentile(double percent) {
        return computePercentile(percent);
    }
	
	@Override
	public double[] getPercentiles(double[] percents, double[] percentiles) {
        for (int i = 0; i < percents.length; i++) {
            percentiles[i] = computePercentile(percents[i]);
        }
        return percentiles;
    }
	
	/**
	 * 获取样本采集的时间，即该样本还未做数据归档统计
	 */
	public long getSampleIntervalMillis() {
        return (finishMillis - startMillis);
    }
	
	/**
	 * 对数据进行统计，
	 * 会耗费CPU计算性能，所以需要弄成定时任务来统计更新
	 */
	public void calucate() {
		finishMillis = System.currentTimeMillis();
		Arrays.sort(buffer, 0, limit);
	}
	
	/**
	 * 重置数据统计状态，重新进行数据收集
	 */
	public void reset() {
		position = 0;
		limit = 0;
		startMillis = System.currentTimeMillis();
		numValue = 0L;
		sumValue = 0.0;
		minValue = 0.0;
		maxValue = 0.0;
		sumSquareValue = 0;
	}
	
	/**
	 * 计算指定如95线下的数据统计，耗费CPU计算性能
	 * 算法参考：
	 * https://cnx.org/contents/223y7Xzw@12/Percentiles
	 */
	private double computePercentile(double percent) {
		if (limit <= 0) {
            return 0.0;
        } else if (percent <= 0.0) {
            return buffer[0];
        } else if (percent >= 100.0) {
            return buffer[limit - 1];
        }
		
		double index = (percent / 100.0) * limit;
        int iLow = (int) Math.floor(index);
        int iHigh = (int) Math.ceil(index);
        assert 0 <= iLow && iLow <= index && index <= iHigh && iHigh <= limit;
        assert (iHigh - iLow) <= 1;
        if (iHigh >= limit) {
            return buffer[limit - 1];
        } else if (iLow == iHigh) {
            return buffer[iLow];
        } else {
            return buffer[iLow] + (index - iLow) * (buffer[iHigh] - buffer[iLow]);
        }
	}

	@Override
	public String toString() {
		StringBuilder info = new StringBuilder(48);
		info.append("{Collector:");
		info.append("total=").append(getTotal()).append(",");
		info.append("minimum=").append(getMinimum()).append(",");
		info.append("maximum=").append(getMaximum()).append(",");
		info.append("mean=").append(getMean()).append(",");
		info.append("sampleSize=").append(getSampleSize());
		info.append("}");
		return info.toString();
	}
}
