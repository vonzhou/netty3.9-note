package org.jboss.netty.channel.socket.nio;

import java.nio.channels.Selector;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.socket.ServerSocketChannel;
import org.jboss.netty.channel.socket.ServerSocketChannelFactory;
import org.jboss.netty.util.ExternalResourceReleasable;

/**
 * 该类实现了ServerSocketChannelFactory 创建服务器端的基于NIO的ServerSocketChannel，
 * 可以高效的处理并发的连接。这里有两种类型的线程：Boss threads 和 Worker threads（所以对应的有两个线程池服务）
 * 。每个绑定的ServerSocketChannel 都有 自己的boss thread，比如说监听俩不同端口的server
 * 就会对应两个boss threads。Boss thread的作用是接受到达的连接请求，直到这个端口解绑定。
 * 一旦连接接受成功（就是accept成功返回），这个 boss thread 就会将这个接受的连接套接字通道
 * 传递给NioServerSocketChannelFactory 所管理的某个worker 线程来处理。一个worker thread
 * 的职责就是为一个或多个通道执行非阻塞的读写服务。
 */
public class NioServerSocketChannelFactory implements ServerSocketChannelFactory {

    private final WorkerPool<NioWorker> workerPool;
    private final NioServerSocketPipelineSink sink;
    private final BossPool<NioServerBoss> bossPool;
    private boolean releasePools;

    /**
     * 各种构造器，根据boss 和 worker线程池的类型和大小。
     */
    public NioServerSocketChannelFactory() {
        this(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        releasePools = true;
    }

    public NioServerSocketChannelFactory(
            Executor bossExecutor, Executor workerExecutor) {
        this(bossExecutor, workerExecutor, getMaxThreads(workerExecutor));
    }

  
    public NioServerSocketChannelFactory(
            Executor bossExecutor, Executor workerExecutor,
            int workerCount) {
        this(bossExecutor, 1, workerExecutor, workerCount);
    }

    public NioServerSocketChannelFactory(
            Executor bossExecutor, int bossCount, Executor workerExecutor,
            int workerCount) {
        this(bossExecutor, bossCount, new NioWorkerPool(workerExecutor, workerCount));
    }

    public NioServerSocketChannelFactory(
            Executor bossExecutor, WorkerPool<NioWorker> workerPool) {
        this(bossExecutor, 1 , workerPool);
    }

    public NioServerSocketChannelFactory(
            Executor bossExecutor, int bossCount, WorkerPool<NioWorker> workerPool) {
        this(new NioServerBossPool(bossExecutor, bossCount, null), workerPool);
    }

    /**
     * 上述的构造器，都会利用这个构造器。
     * 也就是说在Excutor框架的那些线程池的基础上又做了一层封装，使得他们真正的职责分明。
     * 不论是BossPool还是WorkerPool的实现里面肯定会由Excutor成员，那才是任务真正执行的地方。
     * 同时，线程池划分了类型，那么任务自然也要进行区分，就是这里的 NioServerBoss 和 NioWorker。
     * BossPool which will be used to obtain the NioServerBoss that execute
     *        the I/O for accept new connections
     * WorkerPool which will be used to obtain the NioWorker that execute
     *        the I/O worker threads
     */
    public NioServerSocketChannelFactory(BossPool<NioServerBoss> bossPool, WorkerPool<NioWorker> workerPool) {
        if (bossPool == null) {
            throw new NullPointerException("bossExecutor");
        }
        if (workerPool == null) {
            throw new NullPointerException("workerPool");
        }
        this.bossPool = bossPool;
        this.workerPool = workerPool;
        sink = new NioServerSocketPipelineSink();
    }
    
    /**
     * 核心方法，也是这个Factory的职责，创建一个新的NioServerSocketChannel。
     */
    public ServerSocketChannel newChannel(ChannelPipeline pipeline) {
        return new NioServerSocketChannel(this, pipeline, sink, bossPool.nextBoss(), workerPool);
    }

    public void shutdown() {
        bossPool.shutdown();
        workerPool.shutdown();
        if (releasePools) {
            releasePools();
        }
    }

    public void releaseExternalResources() {
        bossPool.shutdown();
        workerPool.shutdown();
        releasePools();
    }

    private void releasePools() {
        if (bossPool instanceof ExternalResourceReleasable) {
            ((ExternalResourceReleasable) bossPool).releaseExternalResources();
        }
        if (workerPool instanceof ExternalResourceReleasable) {
            ((ExternalResourceReleasable) workerPool).releaseExternalResources();
        }
    }

    /**
     * Returns number of max threads for the {@link NioWorkerPool} to use. If
     * the * {@link Executor} is a {@link ThreadPoolExecutor}, check its
     * maximum * pool size and return either it's maximum or
     * {@link SelectorUtil#DEFAULT_IO_THREADS}, whichever is lower. Note that
     * {@link SelectorUtil#DEFAULT_IO_THREADS} is 2 * the number of available
     * processors in the machine.  The number of available processors is
     * obtained by {@link Runtime#availableProcessors()}.
     *
     * @param executor
     *        the {@link Executor} which will execute the I/O worker threads
     * @return
     *        number of maximum threads the NioWorkerPool should use
     */
    private static int getMaxThreads(Executor executor) {
        if (executor instanceof ThreadPoolExecutor) {
            final int maxThreads = ((ThreadPoolExecutor) executor).getMaximumPoolSize();
            return Math.min(maxThreads, SelectorUtil.DEFAULT_IO_THREADS);
        }
        return SelectorUtil.DEFAULT_IO_THREADS;
    }
}
