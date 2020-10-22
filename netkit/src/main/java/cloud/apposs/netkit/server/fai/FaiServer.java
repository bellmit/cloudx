package cloud.apposs.netkit.server.fai;

import cloud.apposs.netkit.IoProcessor;
import cloud.apposs.netkit.filterchain.cmd.CmdHandler;
import cloud.apposs.netkit.filterchain.cmd.WriteCmdFilter;
import cloud.apposs.netkit.filterchain.executor.ThreadPoolFilter;
import cloud.apposs.netkit.filterchain.fai.FaiFilter;
import cloud.apposs.netkit.filterchain.fai.FaiProtocol;
import cloud.apposs.netkit.filterchain.guard.GuardFilter;
import cloud.apposs.netkit.server.ServerConfig;
import cloud.apposs.netkit.server.TcpServer;
import cloud.apposs.util.Errno;

import java.util.ArrayList;
import java.util.List;

public class FaiServer extends TcpServer {
    public static final String THREAD_POOL_WRITE = "Thread_Pool_Write";
    public static final String THREAD_POOL_READ = "Thread_Pool_Read";
    public static final String THREAD_POOL_INTERNAL = "Thread_Pool_Internal";

    private final WriteCmdFilter<Integer> cmdFilter;

    /**
     * 当前服务需要存活的线程，主要用于HELO指令线程检测
     */
    private List<Thread> coreThread = new ArrayList<Thread>();

    public FaiServer(ServerConfig config) {
        super(config);
        this.cmdFilter = new WriteCmdFilter<Integer>(new FaiWriteCmdHandler());
        this.filterChain.addFilter(new FaiFilter());
        this.filterChain.addFilter(new GuardFilter(new GuardFilter.GuardResource() {
            @Override
            public String getKey(IoProcessor processor, Object message) {
                FaiProtocol protocol = (FaiProtocol) message;
                return String.valueOf(protocol.cmd());
            }

            @Override
            public Object[] getArgs(IoProcessor processor, Object message) {
                FaiProtocol protocol = (FaiProtocol) message;
                Object[] args = new Object[1];
                args[0] = protocol.getLimitKey();
                return args;
            }
        }));
        this.filterChain.addFilter(cmdFilter);
        if (config.isExecutorOn()) {
            this.filterChain.addFilter(new ThreadPoolFilter());
        }

        this.setHandler(new FaiHandler(this));
    }

    public void addWrittenCmd(int cmd) {
        cmdFilter.addCmd(cmd);
    }

    private class FaiWriteCmdHandler implements CmdHandler<Integer> {
        @Override
        public Integer parseCmd(Object message) {
            if (!(message instanceof FaiProtocol)) {
                return null;
            }

            FaiProtocol protocol = (FaiProtocol) message;
            return protocol.cmd();
        }

        @Override
        public boolean readonly(Integer cmd) {
            return config.isReadOnly() && ((FaiHandler) handler).isWrittenCmd(cmd);
        }

        @Override
        public boolean discard(IoProcessor processor, Object message) throws Exception {
            if (message instanceof FaiProtocol) {
                FaiProtocol protocol = (FaiProtocol) message;
                protocol.result(Errno.EREADONLY.value());
                processor.write(protocol.buffer());
                return true;
            }
            return false;
        }
    }

    public void addCoreThread(Thread thread) {
        coreThread.add(thread);
    }

    public boolean removeCoreThread(Thread thread) {
        return coreThread.remove(thread);
    }

    List<Thread> getCoreThreadList() {
        return coreThread;
    }
}
