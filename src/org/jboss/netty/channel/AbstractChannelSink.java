package org.jboss.netty.channel;

import static org.jboss.netty.channel.Channels.*;

/**
 * A skeletal ChannelSink implementation.
 */
public abstract class AbstractChannelSink implements ChannelSink {

    /**
     * Creates a new instance.
     */
    protected AbstractChannelSink() {
    }

    //Sends an ExceptionEvent upstream with the specified exception cause code
    public void exceptionCaught(ChannelPipeline pipeline,
            ChannelEvent event, ChannelPipelineException cause) throws Exception {
        Throwable actualCause = cause.getCause();
        if (actualCause == null) {
            actualCause = cause;
        }
        if (isFireExceptionCaughtLater(event, actualCause)) {
            fireExceptionCaughtLater(event.getChannel(), actualCause);
        } else {
        	//发送一个 "exceptionCaught"事件给该Channel流水线的第一个 ChannelUpstreamHandler
            fireExceptionCaught(event.getChannel(), actualCause);
        }
    }


    //在处理Event的过程中发生了异常，是否在一个IO线程中触发一个"exceptionCaught"事件
    protected boolean isFireExceptionCaughtLater(ChannelEvent event, Throwable actualCause) {
        return false;
    }

    /**
     * This implementation just directly call Runnable.run()
     * Sub-classes should override this if they can handle it in a better way
     */
    public ChannelFuture execute(ChannelPipeline pipeline, Runnable task) {
        try {
            task.run();
            return succeededFuture(pipeline.getChannel());
        } catch (Throwable t) {
            return failedFuture(pipeline.getChannel(), t);
        }
    }
}
