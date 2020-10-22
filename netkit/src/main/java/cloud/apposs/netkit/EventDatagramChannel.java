package cloud.apposs.netkit;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public class EventDatagramChannel implements EventChannel {
	private final DatagramChannel channel;
	
	private SocketAddress remote;
	
	public EventDatagramChannel(DatagramChannel channel) {
		this.channel = channel;
	}
	
	@Override
	public SelectionKey register(Selector sel, int ops) throws IOException {
		return channel.register(sel, ops);
	}
	
	@Override
	public boolean connect(InetSocketAddress addr) throws IOException {
		channel.connect(addr);
		return channel.isConnected();
	}

	@Override
	public boolean finishConnect() throws IOException {
		return true;
	}

	@Override
	public boolean isConnected() {
		return channel.isConnected();
	}

	@Override
	public boolean isOpen() {
		return channel.isOpen();
	}
	
	@Override
	public SocketAddress getLocalSocketAddress() {
		return channel.socket().getLocalSocketAddress();
	}
	
	@Override
	public SocketAddress getRemoteSocketAddress() {
		return channel.socket().getRemoteSocketAddress();
	}

	@Override
	public int recv(ByteBuffer dst) throws IOException {
		int oldPos = dst.position();
		SocketAddress remote = channel.receive(dst);
		if (remote != null) {
			this.remote = remote;
		}
		return dst.position() - oldPos;
	}

	@Override
	public int send(ByteBuffer src) throws IOException {
	    if (remote == null && channel.isConnected()) {
            return channel.write(src);
        }
		return channel.send(src, remote);
	}

	@Override
	public long transferFrom(FileChannel fc, long position, long count)
			throws IOException {
		return fc.transferTo(position, count, channel);
	}

	@Override
	public long transferTo(FileChannel fc, long position, long count)
			throws IOException {
		return fc.transferFrom(channel, position, count);
	}

	@Override
	public void close() {
		if (channel != null) {
			try {
				channel.close();
			} catch (IOException e) {
			}
		}
	}
}
