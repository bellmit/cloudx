package cloud.apposs.bootor.sample.action;

import cloud.apposs.bootor.BootorConfig;
import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.netkit.filterchain.http.client.HttpAnswer;
import cloud.apposs.netkit.rxio.IoEmitter;
import cloud.apposs.netkit.rxio.IoFunction;
import cloud.apposs.netkit.rxio.RxIo;
import cloud.apposs.okhttp.OkHttp;
import cloud.apposs.rest.ApplicationContext;
import cloud.apposs.rest.WebExceptionResolver;
import cloud.apposs.rest.annotation.Request;
import cloud.apposs.rest.annotation.RestAction;

@RestAction
public class ProductAction {
    private final BootorConfig config;

    private final OkHttp okHttp;

    private final ApplicationContext context;

    /**
     * Action构造方法参数注入
     */
    @Autowired
    public ProductAction(BootorConfig config, OkHttp okHttp, ApplicationContext context) {
        this.config = config;
        this.okHttp = okHttp;
        this.context = context;
    }

    @Request.Read("/product/config")
    public RxIo<String> getConfig() {
        return RxIo.emitter(new IoEmitter<String>() {
            @Override
            public String call() throws Exception {
                return config.toString();
            }
        });
    }

    @Request.Read("/product/baidu")
    public RxIo<String> getBaidu() throws Exception {
        return okHttp.execute("https://www.baidu.com")
            .map(new IoFunction<HttpAnswer, String>() {
                @Override
                public String call(HttpAnswer httpAnswer) throws Exception {
                    return httpAnswer.getContent();
                }
            });
    }

    @Request.Read("/product/resolver")
    public RxIo<String> getResolver() {
        return RxIo.emitter(new IoEmitter<String>() {
            @Override
            public String call() throws Exception {
                return context.getBeanHierarchy(WebExceptionResolver.class).toString();
            }
        });
    }
}
