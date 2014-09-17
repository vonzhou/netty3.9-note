package org.jboss.netty.channel.socket.nio;

import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.util.ExternalResourceReleasable;
import org.jboss.netty.util.internal.ExecutorUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractNioBossPool<E extends Boss>
        implements BossPool<E>, ExternalResourceReleasable {

    /**
     * The boss pool raises an exception unless all boss threads start and run within this timeout (in seconds.)
     */
    private static final int INITIALIZATION_TIMEOUT = 10;

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractNioBossPool.class);

    private final Boss[] bosses;
    private final AtomicInteger bossIndex = new AtomicInteger();
    private final Executor bossExecutor;
    private volatile boolean initialized; // 状态控制变量

    /**
     * 构造器（执行器，boss的个数，是否自动初始化）
     */
    AbstractNioBossPool(Executor bossExecutor, int bossCount) {
        this(bossExecutor, bossCount, true);
    }

    AbstractNioBossPool(Executor bossExecutor, int bossCount, boolean autoInit) {
        if (bossExecutor == null) {
            throw new NullPointerException("bossExecutor");
        }
        if (bossCount <= 0) {
            throw new IllegalArgumentException(
                    "bossCount (" + bossCount + ") " +
                            "must be a positive integer.");
        }
        // 创建Boss，分配内存。
        bosses = new Boss[bossCount];
        this.bossExecutor = bossExecutor;
        if (autoInit) {
            init();
        }
    }

    protected void init() {
        if (initialized) {
            throw new IllegalStateException("initialized already");
        }
        initialized = true;

        for (int i = 0; i < bosses.length; i++) {
        	// 创建这些Boss，有具体类实现，实际上就是 NioServerBoss 对象。
        	// 创建NioServerBoss 对象后会调用openSelector打开对应的Selector，所以要有超时控制。
            bosses[i] = newBoss(bossExecutor);
        }

        waitForBossThreads();
    }

    private void waitForBossThreads() {
    	// Java并发编程实践， 
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(INITIALIZATION_TIMEOUT);
        boolean warn = false;
        for (Boss boss: bosses) { //NioServerBoss extends AbstractNioSelector
        	// 会出现这种情况吗？
            if (!(boss instanceof AbstractNioSelector)) {
                continue;
            }

            AbstractNioSelector selector = (AbstractNioSelector) boss;
            long waitTime = deadline - System.nanoTime();
            try {
                if (waitTime <= 0) { // 初始化阶段超时
                    if (selector.thread == null) {
                        warn = true;
                        break;
                    }
                } else if (!selector.startupLatch.await(waitTime, TimeUnit.NANOSECONDS)) {
                	// 等待所有的线程都达到这里，返回 false if the waiting time elapsed before the count reached zero
                    warn = true;
                    break;
                }
            } catch (InterruptedException ignore) {
                // Stop waiting for the boss threads and let someone else take care of the interruption.
            	// 恢复中断，让上层代码就行处理。
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (warn) {
            logger.warn(
                    "Failed to get all boss threads ready within " + INITIALIZATION_TIMEOUT + " second(s). " +
                    "Make sure to specify the executor which has more threads than the requested bossCount. " +
                    "If unsure, use Executors.newCachedThreadPool().");
        }
    }

    /**
     * 创建一个新的Boss，利用Executor来执行IO操作。
     */
    protected abstract E newBoss(Executor executor);

    @SuppressWarnings("unchecked")
    public E nextBoss() {
        return (E) bosses[Math.abs(bossIndex.getAndIncrement() % bosses.length)];
    }

    public void rebuildSelectors() {
        for (Boss boss: bosses) {
            boss.rebuildSelector();
        }
    }

    public void releaseExternalResources() {
        shutdown();
        ExecutorUtil.shutdownNow(bossExecutor);
    }

    public void shutdown() {
        for (Boss boss: bosses) {
            boss.shutdown();
        }
    }
}
