package cloud.apposs.netkit;

import cloud.apposs.netkit.filterchain.IoFilterChain;
import cloud.apposs.netkit.listener.IoListenerSupport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.util.List;

/**
 * 网络IO事件处理器，负责真正的业务逻辑处理
 * <p>
 * 注意每个IoProcessor内部都必须要维护一个Channel便于将自身注册到Selector来实现异步
 */
public interface IoProcessor {
    /**
     * 初始化Channel，如ServerSocketChannel,SocketChannel,DatagramChannel
     */
    EventChannel getChannel();

    /**
     * 将自身注册到选择器中，每个处理器自身才知道本身要关心的事件是什么
     * 例如SelectionKey.OP_CONNECT、SelectionKey.OP_ACCEPT等
     */
    SelectionKey register(Selector selector) throws IOException;

    /**
     * 获取自身注册到选择器中的SelectionKey，主要为系统内部调用
     */
    SelectionKey selectionKey();

    /**
     * 网络事件封装，
     * 底层{@link EventLoop#run()}会根据该事件来判断修改SelectionKey注册事件
     */
    IoEvent getEvent();

    /**
     * 流水号，
     * 例如一个页面请求时，会生成一个flow，然后这个请求过程中，所有的数据包都带有该flow，这样再调试问题时会非常方便
     */
    long getFlow();

    void setFlow(long flow);

    /**
     * 一个请求中保存的上下文
     */
    Object getContext();

    void setContext(Object context);

    IoFilterChain getFilterChain();

    void setFilterChain(IoFilterChain chain);

    Object getAttribute(Object key);

    Object getAttribute(Object key, Object defaultVal);

    Object setAttribute(Object key, Object value);

    boolean hasAttribute(Object key);

    /**
     * 请求时间记录相关
     */
    long getCreateTime();

    long getActionTime();

    void setActionTime(long actionTime);

    /**
     * 请求时间配置相关
     */
    int getConnectTimeout();

    int getRecvTimeout();

    int getSendTimeout();

    void setConnectTimeout(int connectTimeout);

    void setSendTimeout(int sendTimeout);

    void setRecvTimeout(int recvTimeout);

    /**
     * 初始化网络接收缓存大小、决定是否采用堆外内存，可由业务方来实现决定
     * 注意初始内存的分配会影响JVM性能，如果JVM内存配置小于1G建议只配置2048以内，
     * 在小JVM内存，配置大初始内存的情况下，
     * 在高并发请求中会导致不断创建大对象进而导致JVM新生代不回收直接触发FULL GC，反而影响性能
     */
    int getBufferSize();

    boolean isBufferDirect();

    void setBufferSize(int bufferSize);

    void setBufferDirect(boolean bufferDirect);

    /**
     * 设置最大网络读取和发送字节，在尽量读取/发送网络字节时保证服务不OOM
     */
    int getMaxBufferSize();

    void setMaxBufferSize(int maxBufferSize);

    /**
     * 由系统调用，业务方调用发送网络数据调用{@link #write(String)}}方法时会经过{@link IoFilterChain#fireFilterWrite(IoBuffer)}，
     * 底层会调用该方法来把业务方发送的数据进行缓存存储<br>
     * {@link EventLoop#doSend}在发送网络数据时会调用该方法获取真正发送的缓存数据
     */
    WriteRequest getWriteRequest();

    /**
     * 写入数据到缓冲buffer中，发送网络数据，主要由业务方调用
     * 该方法会调用{@link IoFilterChain#fireFilterWrite(IoBuffer)}进行数据输出前的编码过滤
     *
     * @return 返回写入的字节数
     */
    long write(byte[] bytes) throws IOException;

    long write(String str) throws IOException;

    long write(String str, Charset charset) throws IOException;

    long write(ByteBuffer buf) throws IOException;

    long write(ByteBuffer buf, boolean flip) throws IOException;

    long write(IoBuffer buf) throws IOException;

    long write(List<IoBuffer> bufs) throws IOException;

    long write(IoBuffer... bufs) throws IOException;

    /**
     * 立刻触发EventLoop发送操作，
     * 主要服务于RxIo异步，
     * 因为RxIo的异步性，当前Server所在的EventLoop是其中一个线程，但RxIo中的EventLoop又是另外一个线程，
     * 所以不触发write写事件则当前Server的EventLoop线程是不会主动触发发送事件的
     */
    void flush();

    IoListenerSupport getListenerSupport();

    void setListenerSupport(IoListenerSupport listenerSupport);

    /**
     * 客户端请求过来建立了连接
     */
    void channelAccept(EventChannel channel) throws Exception;

    /**
     * 请求出去和服务器建立了连接
     */
    void channelConnect() throws Exception;

    /**
     * 接收到网络数据
     */
    void channelRead(Object message) throws Exception;

    /**
     * 网络数据接收完毕的回调，即远程服务已经主动关闭服务，
     * 注意不要在该逻辑下处理数据发送，因为远程服务已经关闭了连接请求，
     * 主要服务场景为客户端读取数据直到远程服务关闭连接，因为不知道如何才算读取字节完毕，
     * 例如Whois查询服务
     */
    void channelReadEof(Object message) throws Exception;

    /**
     * 网络数据发送完毕的回调，代表网络数据接收完毕后发送响应数据后的回调，
     * 触发此方法即代表单次会话响应已经完毕
     */
    void channelSend(WriteRequest request) throws Exception;

    /**
     * 关闭网络连接，释放资源
     */
    void channelClose();

    /**
     * 网络异常的回调
     */
    void channelError(Throwable cause);

    /**
     * 关闭会话
     *
     * @param immediately 是否立即关闭会话，
     *                    true即立刻关闭会话，
     *                    false则在当前会话所有{@link WriteRequest}数据发送结束后才关闭会话，
     *                    某些场景例如异常输出错误或者服务异常需要降级输出错误信息时则需要在所有会话数据发送完毕后才能关闭会话
     */
    void close(boolean immediately);

    /**
     * 当次请求完毕或者会话关闭时，清除/重置请求数据，包括清除接收和发送缓存数据，等待下次新的数据传输
     */
    void clear();
}
