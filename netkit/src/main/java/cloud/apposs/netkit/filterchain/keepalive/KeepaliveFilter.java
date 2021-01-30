package cloud.apposs.netkit.filterchain.keepalive;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.filterchain.IoFilterAdaptor;

/**
 * 每次数据交互都更新操作时间，保持Server端长链接（一般用于大文件传输时避免超时）
 */
public class KeepaliveFilter extends IoFilterAdaptor {
    @Override
    public void channelRead(NextFilter nextFilter, IoProcessor processor, Object message) throws Exception {
        // 更新会话时间
        processor.setActionTime(System.currentTimeMillis());
        nextFilter.channelRead(processor, message);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoProcessor processor, IoBuffer buffer) throws Exception {
        // 更新会话时间
        processor.setActionTime(System.currentTimeMillis());
        nextFilter.filterWrite(processor, buffer);
    }
}
