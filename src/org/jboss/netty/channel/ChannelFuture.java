package org.jboss.netty.channel;

import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.handler.execution.ExecutionHandler;

public interface ChannelFuture {

    /**
     * 和这个future对应的是哪个Channel的IO事件；
     * Returns a channel where the I/O operation associated with this
     * future takes place.
     */
    Channel getChannel();

    /**
     * future是否是complete状态，不论成功，失败，取消。
     */
    boolean isDone();

    /**
     * 是否被取消；
     * 这个future被调用了cancel()方法；
     */
    boolean isCancelled();

    /**
     * IO操作成功完成；
     */
    boolean isSuccess();

    /**
     * IO操作失败的原因；
     * 如果成功或者future外完成，则返回null；
     */
    Throwable getCause();

    /**
     * 取消这个future关联的IO操作，如果取消成功就通知所有的观察者。
     */
    boolean cancel();

    /**
     * 标志成功，并通知所有的观察者。
     * Marks this future as a success and notifies all listeners.
     */
    boolean setSuccess();

    /**
     * 标志失败原因，并通知观察者。
     */
    boolean setFailure(Throwable cause);

    /**
     * Notifies the progress of the operation to the listeners that implements
     * {@link ChannelFutureProgressListener}. Please note that this method will
     * not do anything and return {@code false} if this future is complete
     * already.
     *
     * @return {@code true} if and only if notification was made.
     */
    boolean setProgress(long amount, long current, long total);

    /**
     * 为这个future增加观察者，当操作完成后通知他们。
     */
    void addListener(ChannelFutureListener listener);

    /**
     * Removes the specified listener from this future.
     * The specified listener is no longer notified when this
     * future is {@linkplain #isDone() done}.  If the specified
     * listener is not associated with this future, this method
     * does nothing and returns silently.
     */
    void removeListener(ChannelFutureListener listener);

    /**
     * @deprecated Use {@link #sync()} or {@link #syncUninterruptibly()} instead.
     */
    @Deprecated
    ChannelFuture rethrowIfFailed() throws Exception;

    /**
     * Waits for this future until it is done, and rethrows the cause of the failure if this future
     * failed.  If the cause of the failure is a checked exception, it is wrapped with a new
     * {@link ChannelException} before being thrown.
     */
    ChannelFuture sync() throws InterruptedException;

    /**
     * Waits for this future until it is done, and rethrows the cause of the failure if this future
     * failed.  If the cause of the failure is a checked exception, it is wrapped with a new
     * {@link ChannelException} before being thrown.
     */
    ChannelFuture syncUninterruptibly();

    /**
     * 阻塞等待这个future完成。
     * 抛出异常时因为当前线程可能会被打断。
     * Waits for this future to be completed.
     *
     * @throws InterruptedException
     *         if the current thread was interrupted
     */
    ChannelFuture await() throws InterruptedException;

    /**
     * 无中断的等待操作完成，会默默的丢弃异常InterruptedException。
     */
    ChannelFuture awaitUninterruptibly();

    /**
     * 下面这几个方法多了指定超时时间的功能。
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;
    boolean await(long timeoutMillis) throws InterruptedException;
    boolean awaitUninterruptibly(long timeout, TimeUnit unit);
    boolean awaitUninterruptibly(long timeoutMillis);
}
