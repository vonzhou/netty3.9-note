package org.jboss.netty.channel.socket;

import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;

/**
 * 创建客户端SocketChannel的ChannelFactory
 */
public interface ClientSocketChannelFactory extends ChannelFactory {
	//具体化了返回类型
    SocketChannel newChannel(ChannelPipeline pipeline);
}
