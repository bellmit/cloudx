package cloud.apposs.util;

import org.junit.Test;

import java.util.Random;

public class TestDataCollector {
    @Test
    public void testDataBuffer() {
        DataBuffer buffer = new DataBuffer(12);
        buffer.collect(1020.1);
        buffer.collect(1123.2);
        buffer.collect(2134.2);
        buffer.collect(2056.7);
        buffer.collect(2358.1);
        buffer.collect(2223.4);
        buffer.collect(6283.5);
        buffer.collect(6104.9);
        buffer.collect(8358.1);
        buffer.collect(2012.3);
        buffer.collect(12821.1);
        buffer.collect(4221.6);
        System.out.println("Maximum:" + buffer.getMaximum());
        System.out.println("Minimum:" + buffer.getMinimum());
        System.out.println("Mean:" + buffer.getMean());
        System.out.println("StdDev:" + buffer.getStdDev());
        buffer.calucate();
        System.out.println("SampleSize:" + buffer.getSampleSize());
        System.out.println("Percentile:" + buffer.getPercentile(85));
    }

    @Test
    public void testDataDistribution() throws Exception {
        double[] percents = new double[4];
        percents[0] = 70.0;
        percents[1] = 75.0;
        percents[2] = 85.0;
        percents[3] = 95.0;
        DataDistribution distribute = new DataDistribution(1024, percents);
        distribute.setPublishDeamon(false);
        distribute.setPublishInterval(1000);
        distribute.start();

        distribute.collect(1020.1);
        distribute.collect(1123.2);
        distribute.collect(2134.2);
        distribute.collect(2056.7);
        distribute.collect(2358.1);
        distribute.collect(2223.4);
        distribute.collect(6283.5);
        distribute.collect(6104.9);
        distribute.collect(8358.1);
        distribute.collect(2012.3);
        distribute.collect(12821.1);
        distribute.collect(4221.6);
        Thread.sleep(1500);
        System.out.println("Maximum:" + distribute.getMaximum());
        System.out.println("Minimum:" + distribute.getMinimum());
        System.out.println("Mean:" + distribute.getMean());
        System.out.println("StdDev:" + distribute.getStdDev());
        System.out.println("SampleSize:" + distribute.getSampleSize());
        System.out.println("Percentile:" + distribute.getPercentile(85));
        distribute.shutdown();
    }

    @Test
    public void testDataDistributionThread() throws Exception {
        double[] percents = new double[4];
        percents[0] = 70.0;
        percents[1] = 75.0;
        percents[2] = 85.0;
        percents[3] = 95.0;
        DataDistribution distribute = new DataDistribution(1024, percents);
        distribute.setPublishDeamon(false);
        distribute.setPublishInterval(1000);
        distribute.start();

        new DataCollectThread1(distribute).start();
        new DataCollectThread2(distribute).start();
    }

    static class DataCollectThread1 extends Thread {
        private final DataDistribution distribute;

        public DataCollectThread1(DataDistribution distribute) {
            this.distribute = distribute;
        }

        @Override
        public void run() {
            Random r = new Random(1459834);
            while (true) {
                for (int i = 0; i < 1000; i++) {
                    double rl = r.nextDouble() * 25.2;
                    distribute.collect(rl);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    static class DataCollectThread2 extends Thread {
        private final DataDistribution distribute;

        public DataCollectThread2(DataDistribution distribute) {
            this.distribute = distribute;
        }

        @Override
        public void run() {
            while (true) {
                System.out.println("================= Start ==================");
                System.out.println("Maximum:" + distribute.getMaximum());
                System.out.println("Minimum:" + distribute.getMinimum());
                System.out.println("Mean:" + distribute.getMean());
                System.out.println("StdDev:" + distribute.getStdDev());
                System.out.println("SampleSize:" + distribute.getSampleSize());
                System.out.println("Percentile:" + distribute.getPercentile(85));
                System.out.println("================== End ===================");
                System.out.println();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
