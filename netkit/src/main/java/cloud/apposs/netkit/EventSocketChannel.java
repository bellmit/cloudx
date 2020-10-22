package cloud.apposs.netkit;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class EventSocketChannel implements EventChannel {
	private final SocketChannel channel;
	
	public EventSocketChannel(SocketChannel channel) {
		this.channel = channel;
	}
	
	@Override
	public boolean connect(InetSocketAddress addr) throws IOException {
		return channel.connect(addr);
	}
	
	@Override
	public boolean finishConnect() throws IOException {
		return channel.finishConnect();
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
	public int recv(ByteBuffer dst) throws IOException {
		return channel.read(dst);
	}

	@Override
	public int send(ByteBuffer src) throws IOException {
		return channel.write(src);
	}

	@Override
	public SelectionKey register(Selector sel, int ops) throws IOException {
		return channel.register(sel, ops);
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
	public long transferFrom(FileChannel fc, long position, long count) throws IOException {
		return fc.transferTo(position, count, channel);
	}

	@Override
	public long transferTo(FileChannel fc, long position, long count) throws IOException {
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
