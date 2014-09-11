package org.jboss.netty.channel;

import org.jboss.netty.util.ExternalResourceReleasable;


/**
 * 他是和传输层的主要接口，创建一个和具体通信实体（如网络套接字）关联的Channel。
 * 比如说NioServerSocketChannelFactory创建的通道会持有基于NIO的server socket及底层的通信实体。
 * 一旦一个新的Channel创建了，那么通过参数指定的那个ChannelPipeline 就会开始处理ChannelEvents。
 */
public interface ChannelFactory extends ExternalResourceReleasable {

    /**
     *创建并打开一个新的Channel，并关联一个pipeline
     *如果失败会抛出ChannelException
     */
    Channel newChannel(ChannelPipeline pipeline);

    /**
     * 关闭ChannelFactory及其内部创建的资源
     */
    void shutdown();

    /**
     * 释放这个factory工作依赖的外部资源（并不是由这个factory自己创建的）
     * 比如说在ChannelFactory构造方法时指定的Executor对象，就需要有这个方法来完成释放工作
     * 但是如果有一个打开的Channel，却把外部资源释放了，就会发生意外的结果
     * 同样需要先调用shutdown()
     */
    void releaseExternalResources();
}
