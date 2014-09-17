package org.jboss.netty.channel.socket.nio;

import org.jboss.netty.util.ThreadNameDeterminer;

import java.util.concurrent.Executor;


/**
 * Default implementation which hands of {@link NioWorker}'s
 *
 *
 */
public class NioWorkerPool extends AbstractNioWorkerPool<NioWorker> {

    private final ThreadNameDeterminer determiner;

    public NioWorkerPool(Executor workerExecutor, int workerCount) {
        this(workerExecutor, workerCount, null);
    }

    public NioWorkerPool(Executor workerExecutor, int workerCount, ThreadNameDeterminer determiner) {
        super(workerExecutor, workerCount, false);
        this.determiner = determiner;
        init();
    }

    @Override
    @Deprecated
    protected NioWorker createWorker(Executor executor) {
        return new NioWorker(executor, determiner);
    }
}
