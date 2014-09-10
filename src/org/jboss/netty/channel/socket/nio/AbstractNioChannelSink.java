package org.jboss.netty.channel.socket.nio;

import org.jboss.netty.channel.AbstractChannelSink;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.ChannelRunnableWrapper;

public abstract class AbstractNioChannelSink extends AbstractChannelSink {

    @Override
    public ChannelFuture execute(ChannelPipeline pipeline, final Runnable task) {
        Channel ch = pipeline.getChannel();
        if (ch instanceof AbstractNioChannel<?>) {
            AbstractNioChannel<?> channel = (AbstractNioChannel<?>) ch;
            ChannelRunnableWrapper wrapper = new ChannelRunnableWrapper(pipeline.getChannel(), task);
            channel.worker.executeInIoThread(wrapper);
            return wrapper;
        }
        return super.execute(pipeline, task);
    }

    @Override
    protected boolean isFireExceptionCaughtLater(ChannelEvent event, Throwable actualCause) {
        Channel channel = event.getChannel();
        boolean fireLater = false;
        if (channel instanceof AbstractNioChannel<?>) {
            fireLater =  !AbstractNioWorker.isIoThread((AbstractNioChannel<?>) channel);
        }
        return fireLater;
    }
}
