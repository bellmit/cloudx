package cloud.apposs.gateway.handler.index;

import cloud.apposs.gateway.GatewayException;
import cloud.apposs.gateway.WebUtil;
import cloud.apposs.gateway.handler.AbstractHandler;
import cloud.apposs.netkit.filterchain.http.server.HttpRequest;
import cloud.apposs.netkit.filterchain.http.server.HttpResponse;
import cloud.apposs.netkit.rxio.RxIo;
import cloud.apposs.util.FileUtil;
import cloud.apposs.util.HttpStatus;
import cloud.apposs.util.Param;

import java.io.File;

/**
 * 目录文件读取服务，对应root指令
 */
public class IndexHandler extends AbstractHandler {
    /** 文件读取目录 */
    private String directory;

    /** 当为根据目录时直接读取哪个文件 */
    private String index = "index.html";

    @Override
    public void initialize(Param options) {
        this.directory = options.getString("directory");
        if (options.containsKey("index")) {
            this.index = options.getString("index");
        }
    }

    @Override
    public RxIo<String> handle(HttpRequest request, HttpResponse response) throws Exception {
        String path = WebUtil.getRequestPath(request);
        if (path.equals("/")) {
            path = path + index;
        }
        String filePath = directory + path;
        File readFile = new File(filePath);
        if (!readFile.exists()) {
            throw new GatewayException(HttpStatus.HTTP_STATUS_404,
                    "Read \"" + readFile.getAbsolutePath() + "\" Failed (No Such File Or Directory)");
        }
        response.getHeaders().putAll(addHeaders);
        return RxIo.from(FileUtil.readString(new File(filePath)));
    }
}
