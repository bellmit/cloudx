package cloud.apposs.netkit.filterchain.dns;

import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.filterchain.IoFilter;
import cloud.apposs.netkit.filterchain.IoFilterAdaptor;

import java.io.IOException;

public class DnsFilter extends IoFilterAdaptor {
    private static final String FILETER_CONTEXT = "DnsFilterContext";

    public DnsFilter(IoProcessor processor, int id) {
        Context context = new Context(id);
        processor.setAttribute(FILETER_CONTEXT, context);
    }

    @Override
    public void channelRead(IoFilter.NextFilter nextFilter, IoProcessor processor, Object message) throws Exception {
        if (!(message instanceof IoBuffer)) {
            nextFilter.channelRead(processor, message);
            return;
        }

        Context context = (Context) processor.getAttribute(FILETER_CONTEXT);
        DnsMessage ret = toMessage(context, (IoBuffer)message);
        if (ret != null) {
            nextFilter.channelRead(processor, ret);
        }
    }

    private DnsMessage toMessage(Context context, IoBuffer buffer) throws IOException {
        long oldIdx = buffer.readIdx();
        IoBufferAccessor accessor = new IoBufferAccessor(buffer);
        Header header = new Header(accessor);
        if (context.getId() == header.getId()) {
            return DnsMessage.fromWire(header, accessor);
        }
        buffer.readIdx(oldIdx);
        return null;
    }

    private class Context{
        private int id;

        public Context(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }
}
