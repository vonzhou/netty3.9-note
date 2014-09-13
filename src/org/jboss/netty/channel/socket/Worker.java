package org.jboss.netty.channel.socket;

/**
 * 既然继承Runnable为何不叫Task；
 * 说明这个线程用来专门分发IO操作，而不是自己执行。
 * A Worker is responsible to dispatch IO operations
 */
public interface Worker extends Runnable {

    /**
     * 在ＩＯ线程中执行给定的Runnable;
     * 
     * Execute the given Runnable in the IO-Thread. This may be now or
     * later once the IO-Thread do some other work.
     */
    void executeInIoThread(Runnable task);

}
