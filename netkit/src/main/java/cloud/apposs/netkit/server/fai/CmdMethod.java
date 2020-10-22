package cloud.apposs.netkit.server.fai;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link FaiHandler} 中具有 Cmd 功能的方法包装类
 */
public class CmdMethod {

    private int cmd;

    /**
     * 被代理的方法
     */
    private Method method;

    /**
     * 读写类型
     */
    private ReadWriteCmdEnum readWriteType;

    /**
     * 按序参数注解
     */
    private List<Annotation> parameterAnnotationList = new LinkedList<Annotation>();

    public CmdMethod(int cmd, Method method) {
        this.cmd = cmd;
        this.method = method;
        this.readWriteType = ReadWriteCmdEnum.READ;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public boolean isReadenCmd() {
        return this.readWriteType == ReadWriteCmdEnum.READ;
    }

    public boolean isWrittenCmd() {
        return this.readWriteType == ReadWriteCmdEnum.WRITE;
    }

    public void setReadWriteType(ReadWriteCmdEnum readWriteType) {
        this.readWriteType = readWriteType;
    }

    /**
     * 添加一个参数的所有注解
     */
    public void addParameterAnnotation(Annotation[] annotations) {
        Collections.addAll(parameterAnnotationList, annotations);
    }

    public List<Annotation> getParameterAnnotationList() {
        return parameterAnnotationList;
    }

    public int getCmd() {
        return cmd;
    }

    public ReadWriteCmdEnum getReadWriteType() {
        return readWriteType;
    }

    /**
     * 标识读写 CMD
     */
    public enum ReadWriteCmdEnum {
        READ, WRITE
    }

}
