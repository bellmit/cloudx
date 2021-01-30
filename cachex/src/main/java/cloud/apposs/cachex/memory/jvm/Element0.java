package cloud.apposs.cachex.memory.jvm;

import cloud.apposs.protobuf.ProtoBuf;
import cloud.apposs.util.ReflectUtil;

/**
 * 一级缓存节点，存储结构为Key->Value
 */
public class Element0 extends AbstractElement {
    /**
     * 内存Element的字节大小，主要服务于字节统计
     */
    private static final int ELEMENT_SIZE;

    static {
        Element element = new Element0("Key", new ProtoBuf());
        ELEMENT_SIZE = (int) (ReflectUtil.deepSizeOf(element) + ReflectUtil.deepSizeOf(new ProtoBuf()));
    }

    private static final long serialVersionUID = 4602129800999523427L;

    public Element0(String key, ProtoBuf value) {
        super(key, value.array());
    }

    @Override
    public ProtoBuf getValue() {
        byte[] value = (byte[]) getValue(true);
        // 此次不直接存储ProtoBuf的原因在于当多线程进行缓存读取时，
        // 如果直接返回ProtoBuf，多线程下会有脏数据读，ProtoBuf内部维护的readIndex/writeIndex状态会混乱
        // 所以直接返回新对象保证在多线程下对象是多实例的
        return ProtoBuf.wrap(value);
    }

    @Override
    public int doCalculateByteSize() {
        byte[] value = (byte[]) getValue(true);
        return ELEMENT_SIZE + key.getBytes().length + value.length;
    }
}
