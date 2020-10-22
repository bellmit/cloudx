package cloud.apposs.netkit;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public interface EventChannel {
	long transferFrom(FileChannel fc, long position, long count) throws IOException;
	
	long transferTo(FileChannel fc, long position, long count) throws IOException;
	
	SelectionKey register(Selector sel, int ops) throws IOException;

	boolean connect(InetSocketAddress addr) throws IOException;
	
	boolean finishConnect() throws IOException;
	
	boolean isConnected();
	
	boolean isOpen();
	
	SocketAddress getLocalSocketAddress();
	
	SocketAddress getRemoteSocketAddress();
	
	int recv(ByteBuffer dst) throws IOException;
	
	int send(ByteBuffer src) throws IOException;
	
	void close();
}
