package cloud.apposs.netkit.server.fai.annotation.parser;

import cloud.apposs.netkit.filterchain.IoFilterChainBuilder;
import cloud.apposs.netkit.filterchain.cmd.WriteCmdFilter;
import cloud.apposs.netkit.server.fai.CmdMethod;
import cloud.apposs.netkit.server.fai.FaiHandler;
import cloud.apposs.netkit.server.fai.annotation.WrittenCmd;
import java.lang.reflect.Method;

/**
 * {@link WrittenCmd}  解析器
 * 需要在 {@link CmdParser} 解析之后才开始解析
 */
public class WrittenCmdParser extends ParserAdaptor{

    @Override
    @SuppressWarnings("unchecked")
    public void parseMethod(FaiHandler handler, Method method) {
        WrittenCmd writtenAnnotation = method.getAnnotation(WrittenCmd.class);
        if (writtenAnnotation == null) {
            return;
        }

        IoFilterChainBuilder chain = handler.getServer().getFilterChain();
        WriteCmdFilter writeCmdFilter = (WriteCmdFilter<Integer>)chain.getFilter(WriteCmdFilter.class);

        /*
        WrittenCmd 注解的方法一定在已经被 Cmd 注解过，
        该解析器也应该在 Cmd 解析器之后，
        所以不用判空，
        */
        CmdMethod methodWrap = handler.getCmdMethodByMethod(method);
        int cmd = methodWrap.getCmd();

        methodWrap.setReadWriteType(CmdMethod.ReadWriteCmdEnum.WRITE);

        if (writeCmdFilter != null) {
            writeCmdFilter.addCmd(cmd);
        }

        int taskLimit = writtenAnnotation.value();
        if (taskLimit > 0) {
            FaiHandler.AidCmdTaskGroup taskGroup;
            String groupName = "Cmd:" + cmd;
            taskGroup = new FaiHandler.AidCmdTaskGroup(groupName, taskLimit);
            taskGroup.setWriteCmd(true);
            handler.addCmdTaskGroups(cmd, taskGroup);
        }

    }
}
