package cloud.apposs.netkit.server.fai;

import java.lang.reflect.Method;

/**
 * 资源保护的 Cmd 方法包装类
 */
public class GuardCmdMethod extends CmdMethod {
    public GuardCmdMethod(int cmd, Method method) {
        super(cmd, method);
    }
}
