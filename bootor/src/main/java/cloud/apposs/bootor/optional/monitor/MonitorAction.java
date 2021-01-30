package cloud.apposs.bootor.optional.monitor;

import cloud.apposs.netkit.rxio.IoEmitter;
import cloud.apposs.netkit.rxio.RxIo;
import cloud.apposs.rest.annotation.Request;
import cloud.apposs.util.StandardResult;

public class MonitorAction {
    @Request.Read(value = "/monitor/hello")
    public RxIo<StandardResult> hello() {
        return RxIo.emitter(new IoEmitter<StandardResult>() {
            @Override
            public StandardResult call() throws Exception {
                return StandardResult.success("ok");
            }
        });
    }
}
