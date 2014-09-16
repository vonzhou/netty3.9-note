package org.jboss.netty.util.internal;

import java.util.concurrent.Executor;

/**
 * 一个辅助的内部类，可以防死锁，只是一个静态的start方法，
 * 用任务执行器去执行相应的任务，注意那个ThreadLocal对象的使用。
 */
public final class DeadLockProofWorker {

    /**
     * An <em>internal use only</em> thread-local variable that tells the
     * {@link Executor} that this worker acquired a worker thread from.
     */
    public static final ThreadLocal<Executor> PARENT = new ThreadLocal<Executor>();

    public static void start(final Executor parent, final Runnable runnable) {
        if (parent == null) {
            throw new NullPointerException("parent");
        }
        if (runnable == null) {
            throw new NullPointerException("runnable");
        }

        parent.execute(new Runnable() {
            public void run() {
            	//每个线程在执行的时候，设置自己的ThreadLocal副本为它的任务执行器，
            	// 执行完成后移除，这样在具体的Runnable中就可以获得对应的Excutor。
                PARENT.set(parent);
                try {
                    runnable.run();
                } finally {
                    PARENT.remove();
                }
            }
        });
    }

    /**
     * 私有的构造器。
     */
    private DeadLockProofWorker() {
    }
}
