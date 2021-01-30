package cloud.apposs.bootor;

public class CustomService {
    public static void main(String[] args) throws Exception {
        HttpApplication.run(CustomService.class, new CustomConfig(), args);
    }

    /**
     * 可让业务自定义配置
     */
    public static class CustomConfig {
        private String serviceName;

        private String lockLength;

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getLockLength() {
            return lockLength;
        }

        public void setLockLength(String lockLength) {
            this.lockLength = lockLength;
        }
    }
}
