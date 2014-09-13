package org.jboss.netty.channel;

import java.util.concurrent.Executor;

import org.jboss.netty.handler.execution.ExecutionHandler;


/**
 * ChannelUpstreamHandler处理上行的通道事件，并且在流水线中传送事件。
 * 这个接口最常用的场景是拦截IO工作现场产生的事件，传输消息或者执行相关的业务逻辑。
 * 在大部分情况下，我们是使用SimpleChannelUpstreamHandler 来实现一个
 * 具体的upstream handler，因为它为每个事件类型提供了单个的处理方法。
 * 大多数情况下ChannelUpstreamHandler 是向上游发送事件，虽然发送下行事件也是允许的（如带外数据）。
 */
public interface ChannelUpstreamHandler extends ChannelHandler {

    /**
     * 处理这个通道事件。
     */
    void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception;
}
