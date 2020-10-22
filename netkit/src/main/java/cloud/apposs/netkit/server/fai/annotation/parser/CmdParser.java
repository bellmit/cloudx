package cloud.apposs.netkit.server.fai.annotation.parser;

import cloud.apposs.netkit.server.fai.CmdMethod;
import cloud.apposs.netkit.server.fai.FaiHandler;
import cloud.apposs.netkit.server.fai.annotation.Cmd;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * {@link cloud.apposs.netkit.server.fai.annotation.Cmd} 解析器
 */
public class CmdParser extends ParserAdaptor {

    @Override
    public void parseMethod(FaiHandler handler, Method method) {
        Annotation cmdAnnotation = getCmdAnno(method);
        if (cmdAnnotation != null) {
            int cmd = getCmd(cmdAnnotation);
            CmdMethod methodWrap = createCmdMethod(cmd, method);
            handler.addCmdMethod(cmd, methodWrap);
            method.setAccessible(true);

            // 添加CMD参数注解
            Annotation[][] parameters = method.getParameterAnnotations();
            for (Annotation[] parameterAnnotations : parameters) {
                methodWrap.addParameterAnnotation(parameterAnnotations);
            }
        }
    }

    /**
     * 生成 Cmd 职能的方法包装类
     */
    protected CmdMethod createCmdMethod(int cmd, Method method) {
        return new CmdMethod(cmd, method);
    }

    /**
     * 获取 Cmd 注解，可以是被组合注解解析器重写此方法，以获取不同的 Cmd 注解类
     */
    protected Annotation getCmdAnno(Method method) {
        return method.getAnnotation(Cmd.class);
    }

    /**
     * 获取 Cmd，可以是被组合注解解析器重写此方法，以获取不同的 Cmd
     */
    protected int getCmd(Annotation anno) {
        Cmd cmd = (Cmd) anno;
        return cmd.value();
    }
}
