package cloud.apposs.netkit.server.fai.annotation.parser;

import cloud.apposs.netkit.server.fai.CmdMethod;
import cloud.apposs.netkit.server.fai.FaiHandler;
import cloud.apposs.netkit.server.fai.annotation.CmdTaskGroup;

import java.lang.reflect.Method;

/**
 * {@link cloud.apposs.netkit.server.fai.annotation.CmdTaskGroup} 解析器
 */
public class CmdTaskGroupParser extends ParserAdaptor {

    @Override
    public void parseMethod(FaiHandler handler, Method method) {
        CmdTaskGroup cmdTaskGroupAnnotation = method.getAnnotation(CmdTaskGroup.class);
        if (cmdTaskGroupAnnotation != null) {
            String groupName = cmdTaskGroupAnnotation.group();
            int groupLimit = cmdTaskGroupAnnotation.limit();
            FaiHandler.AidCmdTaskGroup taskGroup = new FaiHandler.AidCmdTaskGroup(groupName, groupLimit);
            CmdMethod cmdMethod = handler.getCmdMethodByMethod(method);
            if (cmdMethod.isWrittenCmd()) {
                taskGroup.setWriteCmd(true);
            }
            handler.addCmdTaskGroups(cmdMethod.getCmd(), taskGroup);
        }
    }
}
