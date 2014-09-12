package org.jboss.netty.channel.socket.oio;

import org.jboss.netty.channel.AbstractChannelSink;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.ChannelRunnableWrapper;
import org.jboss.netty.channel.socket.Worker;

/**
 * OIO模型中每个通道对应的有worker thread，这里就有线程来执行这个任务；
 * 
 */
public abstract class AbstractOioChannelSink extends AbstractChannelSink {

    @Override
    public ChannelFuture execute(final ChannelPipeline pipeline, final Runnable task) {
        Channel ch = pipeline.getChannel();
        if (ch instanceof AbstractOioChannel) {
            AbstractOioChannel channel = (AbstractOioChannel) ch;
            Worker worker = channel.worker;
            if (worker != null) {
                ChannelRunnableWrapper wrapper = new ChannelRunnableWrapper(pipeline.getChannel(), task);
                channel.worker.executeInIoThread(wrapper);
                return wrapper;
            }
        }

        return super.execute(pipeline, task);
    }

    @Override
    protected boolean isFireExceptionCaughtLater(ChannelEvent event, Throwable actualCause) {
        Channel channel = event.getChannel();
        boolean fireLater = false;
        if (channel instanceof AbstractOioChannel) {
            fireLater = !AbstractOioWorker.isIoThread((AbstractOioChannel) channel);
        }
        return fireLater;
    }

}
