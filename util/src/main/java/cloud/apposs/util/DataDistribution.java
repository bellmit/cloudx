package cloud.apposs.util;

/**
 * 数据分布统计服务，支持
 * <pre>
 * 1、平均响应时间统计
 * 2、最大响应时间统计
 * 3、最小响应时间统计
 * 4、75线、95线、99线响应时间定时统计
 * </pre>
 * 参考：
 * https://blog.csdn.net/weixin_37405394/article/details/80795938
 */
public final class DataDistribution implements DataCollector {
	private static final int DEFAULT_PUBLISH_INTERVAL = 60 * 1000;
	
	private DataBuffer current;
    private DataBuffer previous;
    
    /** 数据收集总条数 */
    private long numValue;
    /** 最小数据 */
    private double minValue;
    /** 最大数据 */
    private double maxValue;
    /** 平均数值 */
    private double mean = 0.0;
    /** 标准差数值 */
    private double stddev = 0.0;
    /** 样本数据总数 */
    private int sampleSize = 0;
    
    private final double[] percents;
    /** 数据百分位分布 */
    private final double[] percentiles;
    
    private int publishInterval = DEFAULT_PUBLISH_INTERVAL;
    private boolean publishDaemon = true;
    
    private final DataPublisher publisher;
    
    private final Object swapLock = new Object();
    
    public DataDistribution(int bufferSize, double[] percents) {
    	if (bufferSize <= 0 || percents == null) {
            throw new IllegalArgumentException("bufferSize or percents");
        }
        for (int i = 0; i < percents.length; i++) {
            if (percents[i] < 0.0 || percents[i] > 100.0) {
            	throw new IllegalArgumentException("percents");
            }
        }
    	
    	this.current = new DataBuffer(bufferSize);
    	this.previous = new DataBuffer(bufferSize);
    	this.percents = percents;
        this.percentiles = new double[percents.length];
        this.publisher = new DataPublisher(publishInterval);
    }
    
    @Override
	public void collect(double value) {
		synchronized (swapLock) {
			current.collect(value);
		}
	}
    
    @Override
    public long getTotal() {
		return numValue;
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
    	return mean;
    }
    
    @Override
    public double getStdDev() {
        return stddev;
    }
	
	@Override
	public int getSampleSize() {
		return sampleSize;
	}

	@Override
	public double getPercentile(double percent) {
		for (int i = 0; i < percents.length; i++) {
			if (percents[i] == percent) {
				return percentiles[i];
			}
		}
		return 0.0;
	}

	@Override
	public double[] getPercentiles(double[] percents, double[] percentiles) {
		for (int i = 0; i < percents.length; i++) {
			for (int j = 0; j < this.percents.length; i++) {
				if (percents[i] == this.percents[j]) {
					percentiles[i] = this.percentiles[j];
				}
			}
        }
        return percentiles;
	}

	public double[] getPercentiles() {
        return percentiles;
    }

	public void start() {
    	publisher.setInterval(publishInterval);
    	if (publishDaemon) {
    		publisher.setDaemon(true);
    	}
    	publisher.start();
    }
    
    public void shutdown() {
    	publisher.shutdown();
    }
    
    public void setPublishInterval(int publishInterval) {
    	if (publishInterval > 0) {
    		this.publishInterval = publishInterval;
    	}
	}

	public void setPublishDeamon(boolean publishDeamon) {
		this.publishDaemon = publishDeamon;
	}

	private void calcuate() {
    	synchronized (swapLock) {
    		DataBuffer temp = null;
        	temp = current;
        	current = previous;
        	previous = temp;
        	current.reset();
    	}
    	previous.calucate();
    	previous.getPercentiles(percents, percentiles);
    	
    	numValue = previous.getTotal();
    	minValue = previous.getMinimum();
    	maxValue = previous.getMaximum();
    	mean = previous.getMean();
    	stddev = previous.getStdDev();
    	sampleSize = previous.getSampleSize();
    }
    
    class DataPublisher extends Thread {
    	private int interval;
    	
    	private volatile boolean shutdown = false;
    	
    	public DataPublisher(int interval) {
    		this.interval = interval;
    	}

		public void setInterval(int interval) {
			this.interval = interval;
		}

		@Override
		public void run() {
			while (!shutdown) {
				try {
					Thread.sleep(interval);
				} catch (InterruptedException e) {
					if (shutdown) {
						return;
					}
				}
				calcuate();
			}
		}
		
		public synchronized void shutdown() {
			if (shutdown) {
				return;
			}
			shutdown = true;
			interrupt();
		}
    }
}
