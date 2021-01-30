package cloud.apposs.balance;

import cloud.apposs.util.DataDistribution;

/**
 * {@link Peer}后端在负载均衡请求中的各种状态记录
 */
public final class PeerStats {
	/** 1000 requests/sec for 1 minute */
	private static final int DEFAULT_BUFFER_SIZE = 60 * 1000;
	
	private final Peer peer;
	
	/** 当前节点的请求总数 */
	private volatile long totalRequest = 0L;
	
	/** 当前活跃的连接数 */
	private volatile int activeRequest = 0;
	
	/** 连续连接失败计数 */
	private volatile long peerFailureCounts = 0L;
	
	private static final double[] PERCENTS = makePercentValues();
	
	private DataDistribution dataDist = new DataDistribution(DEFAULT_BUFFER_SIZE, PERCENTS);

	public PeerStats(Peer peer) {
		this.peer = peer;
		this.dataDist.start();
	}
	
	public void shutdown() {
		dataDist.shutdown();
	}

	public Peer getPeer() {
		return peer;
	}

	public long getTotalRequest() {
		return totalRequest;
	}

	public void incrementNumRequests() {
		this.totalRequest += 1;
	}
	
	public void incrementActiveRequestsCount() {
		activeRequest += 1;
    }

    public void decrementActiveRequestsCount() {
        if (activeRequest-- < 0) {
        	activeRequest = 0;
        }
    }
    
    public void incrementPeerFailureCount() {
		peerFailureCounts += 1;
    }
    
    public void noteResponseTime(double responseTime) {
    	if (responseTime < 0) {
    		return;
    	}
    	dataDist.collect(responseTime);
    }

	public int getActiveRequest() {
		return activeRequest;
	}

	public double getResponseTimeMin() {
		return dataDist.getMinimum();
	}
	
	public double getResponseTimeAvg() {
		return dataDist.getMean();
	}
	
	public double getResponseTime10thPercentile() {
        return getResponseTimePercentile(Percent.TEN);
    }
	
	private double getResponseTimePercentile(Percent p) {
        return dataDist.getPercentiles()[p.ordinal()];
    }
	
	private static double[] makePercentValues() {
        Percent[] percents = Percent.values();
        double[] p = new double[percents.length];
        for (int i = 0; i < percents.length; i++) {
            p[i] = percents[i].getValue();
        }
        return p;
    }
	
	private enum Percent {
        TEN(10), TWENTY_FIVE(25), FIFTY(50), SEVENTY_FIVE(75), NINETY(90),
        NINETY_FIVE(95), NINETY_EIGHT(98), NINETY_NINE(99), NINETY_NINE_POINT_FIVE(99.5);

        private double value;

        Percent(double value) {
            this.value = value;
        }

        public double getValue() {
            return value;
        }
    }
}
