package cloud.apposs.netkit.server.fai.annotation.parser;

import cloud.apposs.netkit.server.fai.CmdMethod;
import cloud.apposs.netkit.server.fai.FaiHandler;
import cloud.apposs.netkit.server.fai.annotation.ReadenCmd;

import java.lang.reflect.Method;

/**
 * {@link cloud.apposs.netkit.server.fai.annotation.ReadenCmd} 解析器
 */
public class ReadenCmdParser extends ParserAdaptor {

    @Override
    public void parseMethod(FaiHandler handler, Method method) {
        ReadenCmd readenAnnotation = method.getAnnotation(ReadenCmd.class);
        if (readenAnnotation == null) {
            return;
        }

        int taskLimit = readenAnnotation.value();
        if (taskLimit > 0) {
            CmdMethod cmdMethod = handler.getCmdMethodByMethod(method);
            int cmd = cmdMethod.getCmd();
            String groupName = "Cmd:" + cmd;
            FaiHandler.AidCmdTaskGroup taskGroup = new FaiHandler.AidCmdTaskGroup(groupName, taskLimit);
            handler.addCmdTaskGroups(cmd, taskGroup);
        }
    }
}
