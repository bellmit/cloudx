package cloud.apposs.bootor.sample.action;

import cloud.apposs.bootor.BootorConfig;
import cloud.apposs.bootor.WebUtil;
import cloud.apposs.bootor.sample.bean.MyBean;
import cloud.apposs.ioc.Initializable;
import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.netkit.EventLoopGroup;
import cloud.apposs.netkit.filterchain.http.client.HttpAnswer;
import cloud.apposs.netkit.filterchain.http.server.HttpFormFile;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.netkit.rxio.IoAction;
import cloud.apposs.netkit.rxio.IoEmitter;
import cloud.apposs.netkit.rxio.IoFunction;
import cloud.apposs.netkit.rxio.RxIo;
import cloud.apposs.okhttp.IORequest;
import cloud.apposs.okhttp.OkHttp;
import cloud.apposs.rest.FileStream;
import cloud.apposs.rest.annotation.Async;
import cloud.apposs.rest.annotation.GuardCmd;
import cloud.apposs.rest.annotation.Model;
import cloud.apposs.rest.annotation.Request;
import cloud.apposs.rest.annotation.RestAction;
import cloud.apposs.rest.annotation.Variable;
import cloud.apposs.rest.annotation.WriteCmd;
import cloud.apposs.util.MediaType;
import cloud.apposs.util.Param;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@RestAction
public class UserAction implements Initializable {
    private BootorConfig config;

    private EventLoopGroup loop;

    private OkHttp okHttp;

    /** 构造器注入配置 */
    @Autowired
    public UserAction(BootorConfig config) throws IOException {
        this.config = config;
        loop = new EventLoopGroup(2);
        loop.start(true);
    }

    /** 方法注入HTTP组件 */
    @Autowired
    public void setOkHttp(OkHttp okHttp) {
        this.okHttp = okHttp;
    }

    @Override
    public void initialize() {
        System.out.println("UserAction initialize");
    }

    @Request("/")
    public String root() {
        return "Hello Index Html";
    }

    @Request.Get(value = "/show")
    public String show() {
        return "HTTP GET SHOW";
    }

    /**
     * http://127.0.0.1:8880/usr?id=1&name=qun
     */
    @Request(value = "/usr", method = Request.Method.GET)
    public String usr(@Variable("id") int id, @Variable("name") String name) {
        return "Usr Info:" + name;
    }

    /**
     * http://127.0.0.1:8880/search?xxx=xxx
     */
    @Request(value = "/search", method = Request.Method.GET)
    public String search(HttpRequest request, HttpResponse response) {
        return "Search Info:" + request.getHeaders();
    }

    /**
     * http://127.0.0.1:8880/product/110/mengniu
     */
    @Request.Post("/product/{id}/{name}")
    public String product(@Variable("id") int id, @Variable("name") String name) {
        return "Product Info:" + id + "-" + name;
    }

    /**
     * curl localhost/search/12/television
     */
    @Request("/search/{id}/{name}")
    public String search(Param info) {
        return "Search Info2:" + info.getString("name");
    }

    /**
     * curl -v -d "id=16&name=myproduct" localhost/pr
     */
    @Request(value = "/pr", method = Request.Method.POST)
    public String pdetail(Param info) {
        return "Product Detail:" + info.getString("name");
    }

    /**
     * curl -v -d "id=16&name=myproduct2" localhost/pr/18
     */
    @Request(value = "/pr/{id}", method = Request.Method.POST)
    public String pdetail(@Variable("id") int id, Param info) {
        return "Product Detail:" + info.getString("name");
    }

    /**
     * 测试host+path的匹配
     */
    @Request(value = "/", host = "www.mydomain.com")
    public String domain() {
        return "Hello Index Html From Mydomain";
    }

    @Request("/config")
    public String config() {
        return config.getBasePackage();
    }

    @Request.Read("/read")
    public Param read(Param info) {
        return info;
    }

    /**
     * 配置请求熔断，该resource_flow必须在配置文件在配置对应的熔断规则
     */
    @GuardCmd("flow_qps")
    @Request("/guard")
    public String guard(@Variable("name") String name) {
        return "Guard In Info:" + name;
    }

    @Request("/exp")
    public void exception() {
        throw new IllegalArgumentException("require parameter");
    }

    @WriteCmd
    @Request("/write")
    public String writeProcess() {
        return "Content Write Html";
    }

    @Request("/srt")
    public RxIo<String> srt() {
        return RxIo.emitter(new IoEmitter<String>() {
            @Override
            public String call() throws Exception {
                return "Hello StandardResult";
            }
        });
    }

    /**
     * 异步调用
     */
    @Async
    @Request("/spider")
    public void spider(HttpRequest request, final HttpResponse response) throws Exception {
        String url = "https://www.baidu.com";
        RxIo.http(loop, url).subscribe(new IoAction<HttpAnswer>() {
            @Override
            public void call(HttpAnswer answer) throws Exception {
                WebUtil.response(response, MediaType.TEXT_HTML, answer.getContent(), true);
            }
        }).start();
    }

    @Request("/spider2/{url}")
    public RxIo<String> spider2(@Variable("url") String url) throws Exception {
        return RxIo.http(loop, url).map(new IoFunction<HttpAnswer, String>() {
            @Override
            public String call(HttpAnswer httpAnswer) throws Exception {
                return httpAnswer.getContent();
            }
        });
    }

    @Request("/spider3")
    public RxIo<String> spider3() throws Exception {
        return okHttp.execute("https://www.fkw.com").map(new IoFunction<HttpAnswer, String>() {
            @Override
            public String call(HttpAnswer httpAnswer) throws Exception {
                return httpAnswer.getContent();
            }
        });
    }

    @Request("/proxy1")
    public RxIo<String> proxy1() throws Exception {
        IORequest request = IORequest.builder()
                .proxyMode(IORequest.ProxyMode.SOCKS)
                .serviceId("socks_proxy")
                .url("http://myip.ipip.net/");
        return okHttp.execute(request).map(new IoFunction<HttpAnswer, String>() {
            @Override
            public String call(HttpAnswer httpAnswer) throws Exception {
                return httpAnswer.getContent();
            }
        });
    }

    /**
     * curl -i -X POST -H "Content-type:application/json" \
     * -d '{"field1":"value1","field5":[{"name":"MySubName1","id":1},{"name":"MySubName2","id":2}],"field4":{"sub2":"MyName1","sub3":{"name":"MySubName2","id":2},"sub1":"MyTitle1"},"field3":10,"field2":"value2"}'\
     * http://localhost:8880/body
     * 将请求json转换为对象模型
     */
    @Request("/body")
    public RxIo<String> abody(@Model MyBean bean) {
        return RxIo.emitter(new IoEmitter<String>() {
            @Override
            public String call() throws Exception {
                HttpRequest request = bean.getRequest();
                Map<String, String> headers = request.getHeaders();
                StringBuilder response = new StringBuilder();
                response.append("Hello，Reactor-" + bean + ";flow:" + bean.getFlow() + "\r\n");
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    response.append(key + ":" + value + "\r\n");
                }
                return response.toString();
            }
        });
    }

    /**
     * http://127.0.0.1:8880/apr/xiaomitv
     * 基于RxIo的响应式异步输出，底层判断如果返回值是RxIo直接异步执行网络响应
     */
    @Request("/apr/{name}")
    public RxIo<String> aproduct(@Variable("name") String name) {
        return RxIo.from("Hello，Reactor-" + name);
    }

    @Request("/apr2/{name}")
    public RxIo<String> aproduct2(@Variable("name") String name) {
        return RxIo.from("Hello，Reactor-" + name)
            .map(new IoFunction<String, String>() {
                @Override
                public String call(String s) throws Exception {
                    return s + " From Map";
                }
            });
    }

    /**
     * 模拟服务器端的文件图片展现
     */
    @Request("/io/buffer1")
    public RxIo<FileStream> iobuffer() {
        return RxIo.emitter(new IoEmitter<FileStream>() {
            @Override
            public FileStream call() throws Exception {
                return FileStream.create(MediaType.IMAGE_PNG, "C://12.jpg");
            }
        });
    }

    /**
     * 模拟客户端请求服务器端的文件下载，采用数据零拷贝进行数据传输，对JVM内存没有压力，
     * HTTP协议实现中HEADER返回contetype-type为stream，BODY为文件二进制流
     */
    @Request("/io/buffer2")
    public RxIo<FileStream> iobuffer2() {
        return RxIo.emitter(new IoEmitter<FileStream>() {
            @Override
            public FileStream call() throws Exception {
                File downloadFile = new File("F:\\Downloads\\CentOS-6.6-x86_64-bin-DVD1.iso");
                return FileStream.create(downloadFile)
                        .putHeader("content-disposition", "attachment;filename=" + downloadFile.getName());
            }
        });
    }

    /**
     * 模拟服务器端的先请求外部文件，再供客户端下载
     */
    @Request("/io/buffer3")
    public RxIo<FileStream> iobuffer3() throws Exception {
        String url = "https://1.s131i.faiusr.com/2/AIMBCAAQAhgAIKHk5-oFKJbbjuMHMNIGOK4D.jpg";
        return okHttp.execute(url).map(new IoFunction<HttpAnswer, FileStream>() {
            @Override
            public FileStream call(HttpAnswer httpAnswer) throws Exception {
                return FileStream.create(MediaType.IMAGE_PNG, httpAnswer.getBuffer());
            }
        });
    }

    /**
     * 客户端文件上传并存储到服务端
     * curl -F "userid=1" -F "filecomment=NormalFile" -F "yum.txt=@/tmp/yum_save_tx.2020-09-20.13-54.hLsN2N.yumtx" 192.168.1.8:8880/upload/file1
     */
    @Request.Post("/upload/file1")
    public String fileUpload(HttpRequest request) throws Exception {
        Map<String, HttpFormFile> files = request.getFiles();
        for (Map.Entry<String, HttpFormFile> entry : files.entrySet()) {
            String filename = entry.getKey();
            HttpFormFile fileItem = entry.getValue();
            fileItem.rename(new File("C://upload/" + filename));
        }
        return "upload ok";
    }
}
