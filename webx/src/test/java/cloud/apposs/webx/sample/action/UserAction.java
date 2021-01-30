package cloud.apposs.webx.sample.action;

import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.netkit.EventLoopGroup;
import cloud.apposs.netkit.filterchain.http.client.HttpAnswer;
import cloud.apposs.netkit.rxio.IoAction;
import cloud.apposs.netkit.rxio.IoEmitter;
import cloud.apposs.netkit.rxio.IoFunction;
import cloud.apposs.netkit.rxio.RxIo;
import cloud.apposs.okhttp.OkHttp;
import cloud.apposs.rest.FileStream;
import cloud.apposs.rest.annotation.Action;
import cloud.apposs.rest.annotation.Async;
import cloud.apposs.rest.annotation.Model;
import cloud.apposs.rest.annotation.Request;
import cloud.apposs.rest.annotation.Variable;
import cloud.apposs.rest.interceptor.auth.Auth;
import cloud.apposs.util.MediaType;
import cloud.apposs.util.Param;
import cloud.apposs.webx.WebUtil;
import cloud.apposs.webx.sample.bean.MyBean;
import cloud.apposs.webx.upload.MultiFormRequest;
import org.apache.commons.fileupload.FileItem;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

@Action
public class UserAction {
    private EventLoopGroup loop;

    private OkHttp okHttp;

    public UserAction() throws IOException {
        loop = new EventLoopGroup(2);
        loop.start(true);
    }

    @Autowired
    public void setOkHttp(OkHttp okHttp) {
        this.okHttp = okHttp;
    }

    @Request("/")
    public String root() {
        return index();
    }

    @Request(value = "/index", method = Request.Method.GET)
    public String index() {
        return "forward:index";
    }

    @Request("/login")
    public String login(HttpServletRequest request) {
        return "Login Html";
    }

    /**
     * http://127.0.0.1:8090/product/110/mengniu
     */
    @Request("/product/{id}/{name}")
    public String product(@Variable("id") int id, @Variable("name") String name) {
        return "Product Info:" + id + "-" + name;
    }

    @Request("/myform")
    public String myform() {
        return "forward:myform";
    }

    @Request(value = "/xssfile", method = Request.Method.POST)
    public String xssfile(HttpServletRequest request) {
        return "forward:xssfile";
    }

    /**
     * 异步调用
     */
    @Async
    @Request("/spider")
    public void spider(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String url = "http://www.baidu.com";
        RxIo.http(loop, url).subscribe(new IoAction<HttpAnswer>() {
            @Override
            public void call(HttpAnswer answer) throws Exception {
                WebUtil.response(request, response, MediaType.TEXT_HTML, answer.getContent(), true);
            }
        }).start();
    }

    /**
     * http://127.0.0.1:8880/apr/xiaomitv
     * 基于RxIo的响应式异步输出，底层判断如果返回值是RxIo直接异步执行网络响应
     */
    @Request("/apr/{name}")
    public RxIo<String> aproduct(@Variable("name") String name) {
        return RxIo.from("Hello，Reactor-" + name);
    }

    @Request("/capt")
    public Param captcha(Param info) {
        return info;
    }

    @Auth("ADMIN")
    @Request("/userinfo")
    public Param userinfo(Param info) {
        return info;
    }

    /**
     * curl -i -X POST -H "Content-type:application/json"
     * -d '{"field1":"value1","field5":[{"name":"MySubName1","id":1},{"name":"MySubName2","id":2}],"field4":{"sub2":"MyName1","sub3":{"name":"MySubName2","id":2},"sub1":"MyTitle1"},"field3":10,"field2":"value2"}'
     * http://localhost:8090/body
     * 将请求json转换为对象模型
     */
    @Request("/body")
    public RxIo<String> abody(@Model MyBean bean) {
        return RxIo.emitter(new IoEmitter<String>() {
            @Override
            public String call() throws Exception {
                HttpServletRequest request = bean.getRequest();
                Enumeration<String> headers = request.getHeaderNames();
                StringBuilder response = new StringBuilder();
                response.append("Hello，Reactor-" + bean + ";flow:" + bean.getFlow() + "\r\n");
                while (headers.hasMoreElements()) {
                    String key = headers.nextElement();
                    String value = request.getHeader(key);
                    response.append(key + ":" + value + "\r\n");
                }
                return response.toString();
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
                String filename = new String(downloadFile.getName().getBytes("utf-8"), "ISO8859-1");
                return FileStream.create(downloadFile)
                        .putHeader("content-disposition", "attachment;filename=" + filename);
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
     * curl -v -F "app=1" -F "filecomment=TXT FILE" -F "yum.txt=@/tmp/article.txt" 127.0.0.1:8090/upload/file1
     */
    @Request.Post("/upload/file1")
    public String fileUpload(MultiFormRequest request) throws Exception {
        Map<String, FileItem> files = request.getFiles();
        for (Map.Entry<String, FileItem> entry : files.entrySet()) {
            String filename = entry.getKey();
            FileItem fileItem = entry.getValue();
            fileItem.write(new File("C://upload/" + filename));
        }
        return "upload ok";
    }
}
