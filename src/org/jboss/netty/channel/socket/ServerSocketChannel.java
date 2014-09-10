package org.jboss.netty.channel.socket;

import java.net.InetSocketAddress;

import org.jboss.netty.channel.ServerChannel;


//代表TCP/IP套接字ServerChannel，监听来自客户端的连接请求
public interface ServerSocketChannel extends ServerChannel {
    ServerSocketChannelConfig getConfig();
    InetSocketAddress getLocalAddress();
    InetSocketAddress getRemoteAddress();
}
