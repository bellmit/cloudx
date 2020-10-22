package cloud.apposs.netkit.buffer;

import cloud.apposs.netkit.IoBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 内存分配器，底层所有内存的分配均通过此分配器分配
 * 该类所有方法均为静态，通过在服务一开始初始化时，
 * 可以通过修改此类的分配策略{@link Allocator}}来决定底层的内存分配模型
 * 内存分配模式有：
 * 1、采用内存池的方式分配(支持{@link ByteBuf}和{@link FileBuf})两种分配
 * 2、采用新建内存的方式分配(支持{@link ByteBuf}和{@link FileBuf})两种分配
 */
public final class IoAllocator {
	public static Allocator getAllocator() {
        return s_allocator;
    }
	
	public static void setAllocator(Allocator newAllocator) {
        if (newAllocator == null) {
            throw new IllegalArgumentException("allocator");
        }

        Allocator oldAllocator = s_allocator;
        s_allocator = newAllocator;
        if (null != oldAllocator) {
            oldAllocator.dispose();
        }
    }
	
	public static void setDirect(boolean direct) {
		IoAllocator.s_direct = direct;
	}
	
	/**
	 * 分配内存缓存，注意分配内存后需要在之后调用{@link IoBuffer#free()}释放内存，
	 * 对于IoFilter，其释放的资源场合在channelRead()数据发送完毕和channelClose()会话关闭阶段
	 */
	public static IoBuffer allocate(int capacity) throws IOException {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity: " + capacity);
        }

        return s_allocator.allocate(capacity, s_direct);
    }
	
	public static IoBuffer allocate(int capacity, boolean direct) throws IOException {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity: " + capacity);
        }

        return s_allocator.allocate(capacity, direct);
    }
	
	public static IoBuffer wrap(byte[] buf) throws IOException {
		return s_allocator.wrap(buf);
	}
	
	public static IoBuffer wrap(ByteBuffer buf) throws IOException {
		return s_allocator.wrap(buf);
	}
	
	private static Allocator s_allocator = new SimpleAllocator();
	
	private static boolean s_direct = true;
}
