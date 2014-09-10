package org.jboss.netty.channel;

/*
 * ChannelSink接口代表的是整个pipeline和NIO socket关联的地方，接收和处理最后的downstream事件。
 * 这个框架（transport provider）提供了这个内部组件，所以我们在上面写程序的时候，不会关注这些。
 */
public interface ChannelSink {

    /**
     * Invoked by ChannelPipeline when a downstream ChannelEvent
     * has reached its terminal (the head of the pipeline).
     * 当这个Downstream Channel Event到达最下面的时候就会执行
     */
    void eventSunk(ChannelPipeline pipeline, ChannelEvent e) throws Exception;

    /**
     * Invoked by ChannelPipeline when an exception was raised while
     * one of its ChannelHandlers process a ChannelEvent.
     */
    void exceptionCaught(ChannelPipeline pipeline, ChannelEvent e, ChannelPipelineException cause) throws Exception;

    /**
     * Execute the given Runnable later in the io-thread.
     * Some implementation may not support this and just execute it directly.
     */
    ChannelFuture execute(ChannelPipeline pipeline, Runnable task);
}
