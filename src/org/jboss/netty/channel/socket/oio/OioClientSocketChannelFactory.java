package org.jboss.netty.channel.socket.oio;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.SocketChannel;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.internal.ExecutorUtil;

/**是一个ClientSocketChannelFactory 的实现，基于SocketChannel的阻塞方式的客户端通道。
 * 使用的是传统的阻塞IO API，特点是能得到好的吞吐量和低延迟，当需要服务的连接数少的时候。（联系NIO和OIO的区别）
 * 
 * 在OioClientSocketChannelFactory中只有一种线程类型，worker threads。每个连接的通道有一个专用的worker，
 * 就是传统的IO模型。
 * 
 * 当OioClientSocketChannelFactory在构造的时候传入的参数线程池对象如Executors.newCachedThreadPool()，
 * 就是worker threads的来源。所以要保证指定的Executor 能提供足够数量的线程。
 * 
 * worker thread是延迟获得的（acquired lazily）并且在没有事务要处理时就会释放,所有该thread相关的资源都会释放，
 * 所以要优雅的关闭服务（先关闭所有的Channel，调用 ChannelGroup.close()；
 * 而后调用releaseExternalResources()释放外部资源）。
 * 
 *  要注意不要在所有的通道关闭前去关闭executor ，否则会发生RejectedExecutionException ，
 *  而且有些相关资源不会正常释放。
 *  
 * 限制：通过OioClientSocketChannelFactory创建的SocketChannel 不支持异步操作，
 * 任何ＩＯ操作如连接，写等都已阻塞方式执行。
 */
public class OioClientSocketChannelFactory implements ClientSocketChannelFactory {

    private final Executor workerExecutor;
    final OioClientSocketPipelineSink sink;///？？
    private boolean shutdownExecutor;

    /**
     * 创建一个实例
     * 默认会使用Executors.newCachedThreadPool()来生成worker thread
     */
    public OioClientSocketChannelFactory() {
        this(Executors.newCachedThreadPool());
        shutdownExecutor = true;
    }

    /**
     * Creates a new instance.
     * 指定Executor来执行 I/O worker threads
     */
    public OioClientSocketChannelFactory(Executor workerExecutor) {
        this(workerExecutor, null);
    }

    /**
     * 利用ThreadNameDeterminer来设置thread 名字
     */
    public OioClientSocketChannelFactory(Executor workerExecutor,
                                         ThreadNameDeterminer determiner) {
        if (workerExecutor == null) {
            throw new NullPointerException("workerExecutor");
        }
        this.workerExecutor = workerExecutor;
        sink = new OioClientSocketPipelineSink(workerExecutor, determiner);
    }

    /**
     * 核心方法，创建具体的通道 OioClientSocketChannel
     */
    public SocketChannel newChannel(ChannelPipeline pipeline) {
        return new OioClientSocketChannel(this, pipeline, sink);
    }

    public void shutdown() {
        if (shutdownExecutor) {
            ExecutorUtil.shutdownNow(workerExecutor);
        }
    }

    /**
     * 调用ExcutorService的shutdownNow方法
     */
    public void releaseExternalResources() {
        ExecutorUtil.shutdownNow(workerExecutor);
    }
}
