package cloud.apposs.netkit.server.fai;

import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.WriteRequest;
import cloud.apposs.netkit.buffer.IoAllocator;
import cloud.apposs.netkit.filterchain.IoFilterChainBuilder;
import cloud.apposs.netkit.filterchain.executor.ThreadPool;
import cloud.apposs.netkit.filterchain.executor.ThreadPoolFilter;
import cloud.apposs.netkit.filterchain.executor.ThreadPoolHandler;
import cloud.apposs.netkit.filterchain.executor.ThreadPoolType;
import cloud.apposs.netkit.filterchain.fai.FaiProtocol;
import cloud.apposs.netkit.server.IoServer;
import cloud.apposs.netkit.server.ServerConfig;
import cloud.apposs.netkit.server.ServerHandler;
import cloud.apposs.netkit.server.ServerHandlerAdaptor;
import cloud.apposs.netkit.server.ServerHandlerContext;
import cloud.apposs.netkit.server.fai.annotation.args.ArgAid;
import cloud.apposs.netkit.server.fai.annotation.args.ArgCmd;
import cloud.apposs.netkit.server.fai.annotation.args.ArgFlow;
import cloud.apposs.netkit.server.fai.annotation.args.ArgWid;
import cloud.apposs.netkit.server.fai.annotation.parser.ParserChain;
import cloud.apposs.netkit.server.fai.annotation.parser.ParserChainProvider;
import cloud.apposs.util.Errno;
import cloud.apposs.util.ReflectUtil;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FaiHandler extends ServerHandlerAdaptor {
    public static final String CONTEXT_SESSION = "FaiSession";
    public static final String GUARD_EXP = "GuardExp";

    protected final FaiServer server;
    protected final ServerConfig config;

    /**
     * 注解解析器链
     */
    private ParserChain parserChain;

    /**
     * method -> {@link CmdMethod}
     */
    private Map<Method, CmdMethod> methodWrapMap = new HashMap<Method, CmdMethod>();

    /**
     * cmd -> {@link CmdMethod}
     */
    private Map<Integer, CmdMethod> cmdMethodMap = new HashMap<Integer, CmdMethod>();

    private final List<Integer> internalCmdList = new LinkedList<Integer>();

    private final Map<Integer, AidCmdTaskGroup> aidTaskGroups = new ConcurrentHashMap<Integer, AidCmdTaskGroup>();
    private final Map<Integer, AidCmdTaskGroup> cmdTaskGroups = new ConcurrentHashMap<Integer, AidCmdTaskGroup>();

	public FaiHandler(FaiServer server) {
		this.server = server;
		this.config = server.getConfig();
        this.parserChain = ParserChainProvider.newParserChain(this);
		addEmptyWrittenCmd(NKDef.Protocol.Cmd.CLEAR_CACHE);
        addEmptyWrittenCmd(NKDef.Protocol.Cmd.CLEAR_ACCT);
		addInternalCmd(NKDef.Protocol.Cmd.STAT);
		addInternalCmd(NKDef.Protocol.Cmd.HELO);
		addInternalCmd(NKDef.Protocol.Cmd.FLUSH_CONF);
	}

	@Override
	public void channelRead(final ServerHandlerContext context,
			final Object message) throws Exception {
		FaiProtocol protocol = (FaiProtocol) message;
		int cmd = protocol.cmd();
		int flow = protocol.flow();
        FaiSession session = (FaiSession) context.getAttribute(CONTEXT_SESSION);
		int rt;
		try {
			if (cmd == NKDef.Protocol.Cmd.HELO) {
				rt = processNKHelo(session);
			} else if (cmd == NKDef.Protocol.Cmd.FLUSH_CONF) {
			    rt = processNKFlushConf(session);
            } else {
				rt = processCmd(session, cmd, flow);
			}
			// 如果业务处理返回非成功错误码则直接返回错误码给客户端
			if (rt != Errno.OK.value()) {
			    // 回调
                fallback(session, rt);
				session.write(rt);
			}
		} catch(Exception e) {
		    // 熔断机制需要统计业务异常
		    context.setAttribute(GUARD_EXP, e);
		    // 回调
			session.close(fallback(session, e));
		}
	}

    @Override
    public void channelSend(ServerHandlerContext context, WriteRequest request) {
        Object sessionAttribute = context.getAttribute(CONTEXT_SESSION);
        if (sessionAttribute != null) {
            FaiSession session = (FaiSession) sessionAttribute;
            // 是否保持长链接
            if (session.isKeepAlive()) {
                session.reset();
            } else {
                session.close(true);
            }
        } else {
            // 数据发送完毕之后主动关闭连接
            context.close(true);
        }
    }

    @Override
    public void channelError(final ServerHandlerContext context, final Throwable cause) {
        Object sessionAttribute = context.getAttribute(CONTEXT_SESSION);
        if (sessionAttribute != null) {
            FaiSession session = (FaiSession) sessionAttribute;
            boolean fallback = fallback(session, cause);
            session.close(fallback);
        } else {
            cause.printStackTrace();
            context.close(true);
        }
    }

    @Override
    public void channelClose(ServerHandlerContext context) {
        Object sessionAttribute = context.getAttribute(CONTEXT_SESSION);
        if (sessionAttribute != null) {
            FaiSession session = (FaiSession) sessionAttribute;
            session.clear();
        }
    }

    @Override
    public void parseAnnotation(final IoServer server, final Class<? extends ServerHandler> clazz) {
        paddingThreadPoolHandler(server);

        // 解析类注解
        parserChain.parse();

        // 解析方法注解
        for (Method method : clazz.getDeclaredMethods()) {
            parserChain.parseMethod(method);
        }
    }

    public void addInternalCmd(int cmd) {
        internalCmdList.add(cmd);
    }

    public List<Integer> getInternalCmdList() {
        return internalCmdList;
    }

    public boolean isWrittenCmd(Integer cmd) {
        CmdMethod cmdMethod = cmdMethodMap.get(cmd);
        return cmdMethod != null && cmdMethod.isWrittenCmd();
    }

    public void addCmdMethod(int cmd, CmdMethod methodWrap) {
        cmdMethodMap.put(cmd, methodWrap);
        methodWrapMap.put(methodWrap.getMethod(), methodWrap);
    }

    private boolean isReadenCmd(Integer cmd) {
        CmdMethod cmdMethod = cmdMethodMap.get(cmd);
        return cmdMethod != null && cmdMethod.isReadenCmd();
    }

    /**
     * 添加不在子类实现的写方法集<br>
     * eg. {@link NKDef.Protocol.Cmd#CLEAR_CACHE}
     */
    private void addEmptyWrittenCmd(int cmd) {
        CmdMethod cmdMethod = new CmdMethod(cmd, null);
        cmdMethod.setReadWriteType(CmdMethod.ReadWriteCmdEnum.WRITE);
        cmdMethodMap.put(cmd, cmdMethod);
    }

    /**
     * NKTool 处理刷新配置文件请求
     */
	private int processNKFlushConf(FaiSession session) throws IOException {
	    int rt = Errno.ERROR.value();
        flushConf();
        rt = Errno.OK.value();
        session.write(rt);
        return rt;
    }

    /**
     * 刷新配置文件
     */
    private void flushConf() throws IOException {
    }

    private int processNKHelo(FaiSession session) throws IOException {
		int rt;
		IoBuffer sendBody = IoAllocator.allocate(1);
		// 检查核心线程是否开启或者已经被kill
        List<Thread> coreThreadList = this.server.getCoreThreadList();
        for (Thread thread : coreThreadList) {
            if (!thread.isAlive()) {
                Logger.error("server core thread dead error;thread=%s", thread.getName());
                rt = Errno.ERROR.value();
                return rt;
            }
        }
		session.write(sendBody);
		Logger.info("helo ok");
		rt = Errno.OK.value();
		return rt;
	}

    /**
     * 填充 {@link  ThreadPoolHandlerProxy}
     */
	private void paddingThreadPoolHandler(IoServer server) {
        IoFilterChainBuilder chain = server.getFilterChain();
        ThreadPoolFilter threadPoolFilter = (ThreadPoolFilter)chain.getFilter(ThreadPoolFilter.class);
        if (threadPoolFilter != null) {
            threadPoolFilter.setHandler(new ThreadPoolHandlerProxy());
        }
    }

    private int processCmd(FaiSession session, int cmd, int flow) throws Exception {
		// 没注解配置反射直接调用原生方法
        CmdMethod cmdMethod = cmdMethodMap.get(cmd);
        if(cmdMethod == null) {
            return handle(session);
        }

        // 有配置 CMD 指令注解的就直接反射调用
		Method method = cmdMethod.getMethod();
		List<Annotation> parameterAnnotationList = cmdMethod.getParameterAnnotationList();

		if (parameterAnnotationList.isEmpty()) {
            // 使用继承异常堆栈的方式
            Object result = ReflectUtil.invokeMethod(this, method, session);
			return (Integer) result;
		}

		// 如果方法参数有配置注解则解析参数注解
		Object[] methodPameters = new Object[parameterAnnotationList.size() + 1];
		methodPameters[0] = session;
        int rt = parseArgs(session, methodPameters, parameterAnnotationList);
        if (rt != Errno.OK.value()) {
            return rt;
        }
		// 使用继承异常堆栈的方式
		Object result = ReflectUtil.invokeMethod(this, method, methodPameters);
		return (Integer) result;
	}

    /**
     * 解析 Session 参数
     */
    private int parseArgs(FaiSession session, Object[] methodPameters, List<Annotation>parameterAnnotationList) throws Exception {
	    int rt = Errno.OK.value();
        int index = 1;
        for (Annotation parameterAnnotation : parameterAnnotationList) {
            Class<? extends Annotation> parameterType = parameterAnnotation.annotationType();
            if (parameterType.isAssignableFrom(ArgAid.class)) {
                methodPameters[index++] = session.getAid();
            } else if (parameterType.isAssignableFrom(ArgCmd.class)) {
                methodPameters[index++] = session.getCmd();
            } else if (parameterType.isAssignableFrom(ArgFlow.class)) {
                methodPameters[index++] = session.getFlow();
            } else if (parameterType.isAssignableFrom(ArgWid.class)) {
                methodPameters[index++] = session.getWid();
            }
        }
        return rt;
    }

    public FaiServer getServer() {
        return server;
    }

    public Map<Integer, CmdMethod> getCmdMethodMap() {
        return cmdMethodMap;
    }

    /**
     * 由 {@link CmdMethod} 实现更解耦的功能
     */
    @Deprecated
    private final static class MethodAnnotation {
        private final Method method;

		private final List<Annotation> parameterAnnotationList = new LinkedList<Annotation>();

		public MethodAnnotation(Method method) {
			this.method = method;
		}

		public Method getMethod() {
			return method;
		}

		public List<Annotation> getParameterAnnotationList() {
			return parameterAnnotationList;
		}
	}
	
	private class ThreadPoolHandlerProxy implements ThreadPoolHandler {
		@Override
		public final List<ThreadPool> createPoolGroups() {
			int workerCount = config.getWorkerCount();
			List<ThreadPool> poolList = new ArrayList<ThreadPool>(3);
            poolList.add(new ThreadPool(FaiServer.THREAD_POOL_WRITE, workerCount));
			poolList.add(new ThreadPool(FaiServer.THREAD_POOL_READ, workerCount));
			poolList.add(new ThreadPool(FaiServer.THREAD_POOL_INTERNAL, 1));
			return poolList;
		}

		@Override
		public final ThreadPoolType getThreadPoolType(Object message) {
			FaiProtocol protocol = (FaiProtocol) message;
			int aid = protocol.aid();
			int cmd = protocol.cmd();
			if (cmdTaskGroups.containsKey(cmd)) {
				AidCmdTaskGroup aidCmdTaskGroup = cmdTaskGroups.get(cmd);
				ThreadPoolType.TaskGroup taskGroup = new ThreadPoolType.TaskGroup(aidCmdTaskGroup.groupName, aidCmdTaskGroup.groupLimit);
				if (aidCmdTaskGroup.isWriteCmd) {
					return new ThreadPoolType(FaiServer.THREAD_POOL_WRITE, taskGroup);
				} else if (aidCmdTaskGroup.isReadCmd) {
					return new ThreadPoolType(FaiServer.THREAD_POOL_READ, taskGroup);
				}  else if (aidCmdTaskGroup.isInternalCmd) {
					return new ThreadPoolType(FaiServer.THREAD_POOL_INTERNAL, taskGroup);
				}
			} else if (aidTaskGroups.containsKey(aid)) {
				AidCmdTaskGroup aidCmdTaskGroup = aidTaskGroups.get(aid);
				ThreadPoolType.TaskGroup taskGroup = new ThreadPoolType.TaskGroup(aidCmdTaskGroup.groupName, aidCmdTaskGroup.groupLimit);
				if (aidCmdTaskGroup.isWriteCmd) {
					return new ThreadPoolType(FaiServer.THREAD_POOL_WRITE, taskGroup);
				} else if (aidCmdTaskGroup.isReadCmd) {
					return new ThreadPoolType(FaiServer.THREAD_POOL_READ, taskGroup);
				}  else if (aidCmdTaskGroup.isInternalCmd) {
					return new ThreadPoolType(FaiServer.THREAD_POOL_INTERNAL, taskGroup);
				}
			} else if (isWrittenCmd(cmd)) {
				return new ThreadPoolType(FaiServer.THREAD_POOL_WRITE, null);
			} else if (isReadenCmd(cmd)) {
				return new ThreadPoolType(FaiServer.THREAD_POOL_READ, null);
			} else if (internalCmdList.contains(cmd)) {
				return new ThreadPoolType(FaiServer.THREAD_POOL_INTERNAL, null);
			}
			return new ThreadPoolType(FaiServer.THREAD_POOL_READ, null);
		}
	}

	public void addCmdTaskGroups(int cmd, AidCmdTaskGroup group) {
        cmdTaskGroups.put(cmd, group);
    }
	
	public final static class AidCmdTaskGroup {
		private boolean isWriteCmd = false;
		
		private boolean isReadCmd = true;
		
		private boolean isInternalCmd = false;
		
		private final String groupName;
		
		private final int groupLimit;

		public AidCmdTaskGroup(String groupName, int groupLimit) {
			this.groupName = groupName;
			this.groupLimit = groupLimit;
		}

        public void setWriteCmd(boolean writeCmd) {
            isWriteCmd = writeCmd;
        }
    }

	/**
	 * 业务逻辑处理
	 */
	public int handle(final FaiSession session) throws Exception {
		return Errno.OK.value();
	}

    public boolean fallback(final FaiSession session, final Throwable cause) {
        return fallback(session.getFlow(), session, cause);
    }

    /**
     * rt != OK 的回调函数
     * 默认空实现,业务方可以覆盖该方法做统一日志输出等
     */
    public void fallback(int rt, int flow, final FaiSession session) { }


    private void fallback(final FaiSession session, int rt) {
        fallback(rt, session.getFlow(), session);
    }


    /**
     * 所有会话异常时的回调，主要服务于降级
     *
     * @return true 则直接告诉底层把包扔掉，false 则自己处理这些包并发送给客户端，默认返回-1错误码给客户端
     */
	public boolean fallback(int flow, final FaiSession session, final Throwable cause) {
		try {
		    // 业务方可以覆盖 fallback 方法来定制异常回调，不输出日志
            cause.printStackTrace();
			session.write(Errno.ERROR.value());
		} catch (IOException e) {
			// 远程客户端发送失败了，直接关闭连接了
			return true;
		} finally {
			Logger.error(cause, "FaiHandler task error;flow=%d", flow);
		}
		return false;
	}

    /**
     * 通过 {@link Method} 获取 {@link CmdMethod}
     */
	public CmdMethod getCmdMethodByMethod(Method method) {
	    return methodWrapMap.get(method);
    }
}
