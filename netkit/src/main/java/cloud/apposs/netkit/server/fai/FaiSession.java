package cloud.apposs.netkit.server.fai;

import cloud.apposs.logger.Logger;
import cloud.apposs.netkit.EventLoopGroup;
import cloud.apposs.netkit.IoBuffer;
import cloud.apposs.netkit.filterchain.fai.FaiProtocol;
import cloud.apposs.netkit.server.ServerHandlerContext;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class FaiSession {
	private ServerHandlerContext context;
	
	private FaiProtocol recvProtocol;
	
	private FaiProtocol sendProtocol;

	private volatile boolean closed = false;
	
	public FaiSession(ServerHandlerContext context, FaiProtocol recvProtocol) {
		context.setFlow(recvProtocol.flow());
		this.context = context;
		this.recvProtocol = recvProtocol;
		this.sendProtocol = new FaiProtocol();
		this.sendProtocol.aid(recvProtocol.aid());
		this.sendProtocol.cmd(recvProtocol.cmd());
		this.sendProtocol.flow(recvProtocol.flow());
		this.sendProtocol.setWid(recvProtocol.getWid());
	}
	
	public final int getAid() {
		return recvProtocol.aid();
	}
	
	public final int getCmd() {
		return recvProtocol.cmd();
	}
	
	public final int getFlow() {
		return recvProtocol.flow();
	}
	
	public final int getResult() {
		return recvProtocol.result();
	}

	public final short getWid() {
		return recvProtocol.getWid();
	}

	public final FaiProtocol getSendProtocol(){
	    return sendProtocol;
    }
	
	public final void write(IoBuffer sendBuffer) throws IOException {
        if (isClosed()) {
			Logger.info("session had closed");
            return;
        }
		sendProtocol.body(sendBuffer.buffer());
        IoBuffer buffer = sendProtocol.buffer();
		context.write(buffer);
    }
	
	public final void write(int result) throws IOException {
        if (isClosed()) {
            Logger.info("session had closed");
            return;
        }
		sendProtocol.result(result);
		context.write(sendProtocol.buffer());
	}
	
	public final ByteBuffer body() throws IOException {
		return recvProtocol.body();
	}

	public final EventLoopGroup loopGroup() {
		return context.getLoopGroup();
	}
	
	public ServerHandlerContext getContext() {
		return context;
	}

	public void flush() {
        if (isClosed()) {
            Logger.info("session had closed");
            return;
        }
		context.flush();
	}
	
	/**
	 * 是否保持长连接
	 */
	public boolean isKeepAlive() {
		return false;
	}
	
	/**
	 * 一次完整请求后(接收数据+逻辑处理完后发送数据)的数据重置
	 */
	public void reset() {
		if (recvProtocol != null) {
			recvProtocol.reset();
		}
		if (sendProtocol != null) {
			sendProtocol.reset();
		}
	}
	
	/**
	 * 关闭会话，后释放资源
	 */
	public void clear() {
	    setClosed(true);
		if (recvProtocol != null) {
			recvProtocol.clear();
			recvProtocol = null;
		}
		if (sendProtocol != null) {
			sendProtocol.clear();
			sendProtocol = null;
		}
		if (context != null) {
			context = null;
		}
	}

	/**
	 * 关闭会话，释放资源
	 */
	public void close(boolean immediately) {
		if (context != null) {
			context.close(immediately);
		}
	}

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }
}
