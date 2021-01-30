package cloud.apposs.cachex.memory.jvm;

import cloud.apposs.protobuf.ProtoBuf;
import cloud.apposs.util.ReflectUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 二级缓存节点，存储结构为Key->Key->Value
 */
public class Element1 extends AbstractElement {
    /**
     * 内存Element的字节大小，主要服务于字节统计
     */
    private static final int ELEMENT_SIZE;

    static {
        Element1 element = new Element1("Key", new ConcurrentHashMap<String, byte[]>());
        ELEMENT_SIZE = (int) (ReflectUtil.deepSizeOf(element) + ReflectUtil.deepSizeOf(new ProtoBuf()));
    }

    /**
     * ProtoBuf的字节大小，只能是个大概值，主要服务于字节统计
     */
    public static final int PROTOBUF_SIZE;

    static {
        PROTOBUF_SIZE = (int) ReflectUtil.deepSizeOf(new ProtoBuf());
    }

    private static final long serialVersionUID = -8067453840446587213L;

    /**
     * 缓存数据
     */
    protected final Map<String, byte[]> value;

    public Element1(String key, Map<String, byte[]> value) {
        super(key, value);
        this.value = value;
    }

    @Override
    public Map<String, ProtoBuf> getValue() {
        return getValue(true);
    }

    @Override
    public Map<String, ProtoBuf> getValue(boolean update) {
        if (update) {
            doUpdateStatus();
        }
        Map<String, ProtoBuf> buffers = new HashMap<String, ProtoBuf>();
        for (Map.Entry<String, byte[]> entry : value.entrySet()) {
            buffers.put(entry.getKey(), ProtoBuf.wrap(entry.getValue()));
        }
        return buffers;
    }

    protected boolean doPut(String field, ProtoBuf value, int byteSize) {
        this.byteSize += byteSize;
        return this.value.put(field, value.array()) != null;
    }

    @Override
    public int doCalculateByteSize() {
        return ELEMENT_SIZE;
    }

    public ProtoBuf remove(String field) {
        byte[] bytes = value.remove(field);
        if (bytes == null) {
            return null;
        }
        return ProtoBuf.wrap(bytes);
    }
}
