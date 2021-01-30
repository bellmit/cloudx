package cloud.apposs.okhttp;

public interface IHttpInterceptor {
    void preRequest(IORequest request) throws Exception;
}
