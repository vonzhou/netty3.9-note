package org.jboss.netty.channel.socket;

import java.net.InetSocketAddress;

import org.jboss.netty.channel.Channel;


/**
 * 代表TCP/IP套接字通道，或者由ServerSocketChannel接收而创建的连接套接字，或者由ClientSocketChannelFactory创建
 */
public interface SocketChannel extends Channel {
    SocketChannelConfig getConfig();
    InetSocketAddress getLocalAddress();
    InetSocketAddress getRemoteAddress();
}
