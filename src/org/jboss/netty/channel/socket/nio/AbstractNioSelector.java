package org.jboss.netty.channel.socket.nio;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.ThreadRenamingRunnable;
import org.jboss.netty.util.internal.DeadLockProofWorker;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ConcurrentModificationException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

abstract class AbstractNioSelector implements NioSelector {

    private static final AtomicInteger nextId = new AtomicInteger();

    //原子操作
    private final int id = nextId.incrementAndGet();

    /**
     * Internal Netty logger.
     */
    protected static final InternalLogger logger = InternalLoggerFactory
            .getInstance(AbstractNioSelector.class);

    private static final int CLEANUP_INTERVAL = 256; 
    // XXX Hard-coded value, but won't need customization.

    /**
     * 真正的任务执行器，来执行任务Runnables，比如通道注册任务。
     */
    private final Executor executor;

    /**
     * 如果worker启动，那么该变量将指向真正的Thread，因为有些方法会根据该变量进行行动。
     * If this worker has been started thread will be a reference to the thread
     * used when starting. i.e. the current thread when the run method is executed.
     */
    protected volatile Thread thread;

    /**
     * 闭锁，直到IO线程启动，而且将变量thread设为有效值。
     * Count down to 0 when the I/O thread starts and {@link #thread} is set to non-null.
     */
    final CountDownLatch startupLatch = new CountDownLatch(1);

    /**
     * NIO库中的选择器Selector
     */
    protected volatile Selector selector;

    /**
     * 控制 Selector.select 方法从阻塞状态醒来的原子变量。
     * Boolean that controls determines if a blocked Selector.select should
     * break out of its selection process. In our case we use a timeone for
     * the select method and the select method will block for that time unless
     * waken up.
     */
    protected final AtomicBoolean wakenUp = new AtomicBoolean();

    /**
     * 我们的工作队列。
     */
    private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<Runnable>();

    private volatile int cancelledKeys; 
    // should use AtomicInteger but we just need approximation

    
    /**
     * 闭锁，等到线程关闭。
     */
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private volatile boolean shutdown;

    AbstractNioSelector(Executor executor) {
        this(executor, null);
    }

    AbstractNioSelector(Executor executor, ThreadNameDeterminer determiner) {
        this.executor = executor;
        // 启动这个 AbstractNioSelector；
        openSelector(determiner);
    }

    public void register(Channel channel, ChannelFuture future) {
    	//创建一个注册任务（具体由上层实现），而后加入工作队列。
        Runnable task = createRegisterTask(channel, future);
        registerTask(task);
    }

    protected final void registerTask(Runnable task) {
        taskQueue.add(task);

        Selector selector = this.selector;

        if (selector != null) {
        	//Causes the first selection operation that has not yet returned to return immediately
            if (wakenUp.compareAndSet(false, true)) {
                selector.wakeup();
            }
        } else {
            if (taskQueue.remove(task)) {
                // the selector was null this means the Worker has already been shutdown.
                throw new RejectedExecutionException("Worker has already been shutdown");
            }
        }
    }

    protected final boolean isIoThread() {
        return Thread.currentThread() == thread;
    }

    public void rebuildSelector() {
    	// 如果是在其他线程中调用rebuildSelector，那么就将其视为一个任务加入到工作队列。
        if (!isIoThread()) {
            taskQueue.add(new Runnable() {
                public void run() {
                    rebuildSelector();
                }
            });
            return;
        }

        final Selector oldSelector = selector;
        final Selector newSelector;

        if (oldSelector == null) {
            return;
        }

        try {
        	// 无非再次打开一个Selector；
            newSelector = SelectorUtil.open();
        } catch (Exception e) {
            logger.warn("Failed to create a new Selector.", e);
            return;
        }

        //然后把所有的Channel迁移到这个新的Selector上。
        // Register all channels to the new Selector.
        int nChannels = 0;
        for (;;) {
            try {
                for (SelectionKey key: oldSelector.keys()) {
                    try {
                        if (key.channel().keyFor(newSelector) != null) {
                            continue;
                        }

                        int interestOps = key.interestOps();
                        key.cancel();
                        key.channel().register(newSelector, interestOps, key.attachment());
                        nChannels ++;
                    } catch (Exception e) {
                        logger.warn("Failed to re-register a Channel to the new Selector,", e);
                        close(key);
                    }
                }
            } catch (ConcurrentModificationException e) {
                // Probably due to concurrent modification of the key set.
                continue;
            }

            break;
        }

        // 更新成功。
        selector = newSelector;

        try {
            // time to close the old selector as everything else is registered to the new one
            oldSelector.close();
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failed to close the old Selector.", t);
            }
        }

        logger.info("Migrated " + nChannels + " channel(s) to the new Selector,");
    }

    public void run() {
        thread = Thread.currentThread();
        // 打开闭锁；
        startupLatch.countDown();

        int selectReturnsImmediately = 0;
        Selector selector = this.selector;

        if (selector == null) {
            return;
        }
        // use 80% of the timeout for measure
        final long minSelectTimeout = SelectorUtil.SELECT_TIMEOUT_NANOS * 80 / 100;
        boolean wakenupFromLoop = false;
        for (;;) {
            wakenUp.set(false);

            try {
                long beforeSelect = System.nanoTime();
                // 返回有多少个Channel准备好了
                int selected = select(selector);
                if (SelectorUtil.EPOLL_BUG_WORKAROUND && selected == 0 && !wakenupFromLoop && !wakenUp.get()) {
                	//上述select阻塞的时间；
                    long timeBlocked = System.nanoTime() - beforeSelect;

                    //如果小于最小超时时间限制；
                    if (timeBlocked < minSelectTimeout) {
                        boolean notConnected = false;
                        // loop over all keys as the selector may was unblocked because of a closed channel
                        for (SelectionKey key: selector.keys()) {
                            SelectableChannel ch = key.channel();
                            try {
                                if (ch instanceof DatagramChannel && !ch.isOpen() ||
                                        ch instanceof SocketChannel && !((SocketChannel) ch).isConnected()) {
                                    notConnected = true;
                                    // cancel the key just to be on the safe side
                                    key.cancel();
                                }
                            } catch (CancelledKeyException e) {
                                // ignore
                            }
                        }
                        if (notConnected) {
                            selectReturnsImmediately = 0;
                        } else {
                        	//在超时限制之前就返回，并且返回的结果是0，这或许是导致jdk epoll bug的原因，累积。
                            // returned before the minSelectTimeout elapsed with nothing select.
                            // this may be the cause of the jdk epoll(..) bug, so increment the counter
                            // which we use later to see if its really the jdk bug.
                            selectReturnsImmediately ++;
                        }
                    } else {
                    	// 是超时。
                        selectReturnsImmediately = 0;
                    }
                    
                    // 这是jdk epoll bug，所以需要替换掉这个Selector！！！
                    //然后重新下一轮的select处理。
                    if (selectReturnsImmediately == 1024) {
                        // The selector returned immediately for 10 times in a row,
                        // so recreate one selector as it seems like we hit the
                        // famous epoll(..) jdk bug.
                        rebuildSelector();
                        selector = this.selector;
                        selectReturnsImmediately = 0;
                        wakenupFromLoop = false;
                        // try to select again
                        continue;
                    }
                } else {
                    // reset counter
                    selectReturnsImmediately = 0;
                }

                /**
                 * 在调用selector.wakeup()之前总是先执行wakenUp.compareAndSet(false, true)，
                 * 来减小wake-up的开销，因为Selector.wakeup()执行的代价很大。
                 * 然后这种方法存在一种竟态条件，发生在如果把 wakenUp 设置为true太早的时候：
                 * 1）Selecttor在'wakenUp.set(false)'和'selector.select(...)'之间醒来（BAD）；
                 * 2）在'selector.select(...)'和'if (wakenUp.get()) { ... }'醒来时OK的。
                 * 在第一种情况下，'wakenUp'被置为了true，但是没有对那个select生效，所以他会让接下来的那个
                 * 'selector.select(...)'立即醒来。直到在下一轮循环当中'wakenUp' 被再次置为FALSE的时候，
                 * 那么 'wakenUp.compareAndSet(false, true)'就会失败，任何想惊醒Selector的尝试都会失败，
                 * 导致接下来的'selector.select(...)'方法无谓的阻塞。
                 * 
                 * 为了解决这个问题，就在selector.select(...)之后，判断wakenUp是true的时候，立即调用一次
                 * selector.wakeup()。
                 * 对这两种情况来说，惊醒selector的操作都是低效的。
                 */
                // It is inefficient in that it wakes up the selector for both
                // the first case (BAD - wake-up required) and the second case
                // (OK - no wake-up required).

                if (wakenUp.get()) {
                    wakenupFromLoop = true;
                    selector.wakeup();
                } else {
                    wakenupFromLoop = false;
                }

                cancelledKeys = 0;
                processTaskQueue();// 处理任务
                selector = this.selector; // processTaskQueue() can call rebuildSelector()

                if (shutdown) {
                    this.selector = null;

                    // process one time again
                    processTaskQueue();

                    for (SelectionKey k: selector.keys()) {
                        close(k);
                    }

                    try {
                    	// 要关闭Selector；
                        selector.close();
                    } catch (IOException e) {
                        logger.warn(
                                "Failed to close a selector.", e);
                    }
                    // 打开这个闭锁；
                    shutdownLatch.countDown();
                    break;
                } else {
                    process(selector);
                }
            } catch (Throwable t) {
                logger.warn(
                        "Unexpected exception in the selector loop.", t);

                // Prevent possible consecutive immediate failures that lead to
                // excessive CPU consumption.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ignore.
                }
            }
        }
    }

    /**
     * 启动这个AbstractNioWorker，返回相应的Selector，它可以用来注册AbstractNioChannel。
     */
    private void openSelector(ThreadNameDeterminer determiner) {
        try {
        	// 创建一个Selector；
            selector = SelectorUtil.open();
        } catch (Throwable t) {
            throw new ChannelException("Failed to create a selector.", t);
        }

        // Start the worker thread with the new Selector.
        boolean success = false;
        try {
        	//执行任务，任务从何而来由具体的类实现。
            DeadLockProofWorker.start(executor, newThreadRenamingRunnable(id, determiner));
            success = true;
        } finally {
            if (!success) {
                // Release the Selector if the execution fails.
                try {
                	// 如果失败的话就关闭Selector；
                    selector.close();
                } catch (Throwable t) {
                    logger.warn("Failed to close a selector.", t);
                }
                selector = null;
                // The method will return to the caller at this point.
            }
        }
        assert selector != null && selector.isOpen();
    }

    private void processTaskQueue() {
        for (;;) {
            final Runnable task = taskQueue.poll();
            if (task == null) {
                break;
            }
            task.run();
            try {
                cleanUpCancelledKeys();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    protected final void increaseCancelledKeys() {
        cancelledKeys ++;
    }

    protected final boolean cleanUpCancelledKeys() throws IOException {
        if (cancelledKeys >= CLEANUP_INTERVAL) {
            cancelledKeys = 0;
            // 非阻塞 Select。
            selector.selectNow();
            return true;
        }
        return false;
    }

    public void shutdown() {
    	// 不能再IO线程中调用 这个shutdown函数。
        if (isIoThread()) {
            throw new IllegalStateException("Must not be called from a I/O-Thread to prevent deadlocks!");
        }

        Selector selector = this.selector;
        shutdown = true;
        if (selector != null) {
            selector.wakeup();
        }
        try {
        	// 等待这个闭锁打开。
            shutdownLatch.await();
        } catch (InterruptedException e) {
            logger.error("Interrupted while wait for resources to be released #" + id);
            Thread.currentThread().interrupt();
        }
    }

    protected abstract void process(Selector selector) throws IOException;

    // 对selector.select()方法的封装。
    protected int select(Selector selector) throws IOException {
        return SelectorUtil.select(selector);
    }

    protected abstract void close(SelectionKey k);

    protected abstract ThreadRenamingRunnable newThreadRenamingRunnable(int id, ThreadNameDeterminer determiner);

    protected abstract Runnable createRegisterTask(Channel channel, ChannelFuture future);
}
