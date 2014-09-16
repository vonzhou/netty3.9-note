package org.jboss.netty.channel.socket.nio;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

import java.nio.channels.Selector;


/**
 * 这里的NIO selector就是试图解决epoll的那个bug，而增加了一个rebuildSelector方法。
 */
public interface NioSelector extends Runnable {

    void register(Channel channel, ChannelFuture future);

    /**
     * Replaces the current {@link Selector} with a new {@link Selector} to work around the infamous epoll 100% CPU
     * bug.
     */
    void rebuildSelector();

    void shutdown();
}
