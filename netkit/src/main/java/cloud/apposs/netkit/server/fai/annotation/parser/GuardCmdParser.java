package cloud.apposs.netkit.server.fai.annotation.parser;

import cloud.apposs.netkit.filterchain.guard.GuardFilter;
import cloud.apposs.netkit.server.fai.CmdMethod;
import cloud.apposs.netkit.server.fai.FaiHandler;
import cloud.apposs.netkit.server.fai.GuardCmdMethod;
import cloud.apposs.netkit.server.fai.annotation.GuardCmd;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * {@link cloud.apposs.netkit.server.fai.annotation.GuardCmd} 解析器
 */
public class GuardCmdParser extends CmdParser {
    @Override
    public void parseMethod(FaiHandler handler, Method method) {
        super.parseMethod(handler, method);
        GuardFilter filter = (GuardFilter) handler.getServer().getFilterChain().getFilter(GuardFilter.class);
        if (filter == null) {
            return;
        }
        GuardCmd cmdAnnotation = (GuardCmd) getCmdAnno(method);
        if (cmdAnnotation != null) {
            String resource = cmdAnnotation.resource();
            int cmd = cmdAnnotation.value();
            // 往 GuardFilter 添加被保护的 cmd 方法
            filter.addGuardCmd(cmd, resource);
        }
    }

    @Override
    protected CmdMethod createCmdMethod(int cmd, Method method) {
        return new GuardCmdMethod(cmd, method);
    }

    @Override
    protected Annotation getCmdAnno(Method method) {
        return method.getAnnotation(GuardCmd.class);
    }

    @Override
    protected int getCmd(Annotation anno) {
        GuardCmd cmd = (GuardCmd) anno;
        return cmd.value();
    }
}
