package org.jboss.netty.channel.socket.nio;

import org.jboss.netty.util.ThreadNameDeterminer;

import java.util.concurrent.Executor;


/**
 * Holds {@link NioServerBoss} instances to use
 */
public class NioServerBossPool extends AbstractNioBossPool<NioServerBoss> {
    private final ThreadNameDeterminer determiner;

    /**
     * Create a new instance
     *
     * @param bossExecutor  the {@link Executor} to use for server the {@link NioServerBoss}
     * @param bossCount     the number of {@link NioServerBoss} instances this {@link NioServerBossPool} will hold
     * @param determiner    the {@link ThreadNameDeterminer} to use for name the threads. Use {@code null}
     *                      if you not want to set one explicit.
     */
    public NioServerBossPool(Executor bossExecutor, int bossCount, ThreadNameDeterminer determiner) {
        super(bossExecutor, bossCount, false);
        this.determiner = determiner;
        init();
    }

    /**
     * Create a new instance using no {@link ThreadNameDeterminer}
     *
     * @param bossExecutor  the {@link Executor} to use for server the {@link NioServerBoss}
     * @param bossCount     the number of {@link NioServerBoss} instances this {@link NioServerBossPool} will hold
     */
    public NioServerBossPool(Executor bossExecutor, int bossCount) {
        this(bossExecutor, bossCount, null);
    }

    @Override
    protected NioServerBoss newBoss(Executor executor) {
        return new NioServerBoss(executor, determiner);
    }
}
