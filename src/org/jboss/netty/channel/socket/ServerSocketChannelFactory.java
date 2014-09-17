package org.jboss.netty.channel.socket;

import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ServerChannelFactory;

/**
 * 这个接口很简单，就是根据特定的ChannelPipeline创建一个新的ServerSocketChannel。
 */
public interface ServerSocketChannelFactory extends ServerChannelFactory {
    ServerSocketChannel newChannel(ChannelPipeline pipeline);
}
