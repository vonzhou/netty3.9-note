package org.jboss.netty.channel.socket.oio;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelSink;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.DefaultSocketChannelConfig;
import org.jboss.netty.channel.socket.SocketChannel;
import org.jboss.netty.channel.socket.SocketChannelConfig;

abstract class OioSocketChannel extends AbstractOioChannel
                                implements SocketChannel {

    final Socket socket;
    //这里就到达了最原始的Java套接字编程环节了
    private final SocketChannelConfig config;

    OioSocketChannel(
            Channel parent,
            ChannelFactory factory,
            ChannelPipeline pipeline,
            ChannelSink sink,
            Socket socket) {

        super(parent, factory, pipeline, sink);

        this.socket = socket;
        try {
            socket.setSoTimeout(1000);
        } catch (SocketException e) {
            throw new ChannelException(
                    "Failed to configure the OioSocketChannel socket timeout.", e);
        }
        config = new DefaultSocketChannelConfig(socket);
    }

    public SocketChannelConfig getConfig() {
        return config;
    }

    abstract PushbackInputStream getInputStream();
    abstract OutputStream getOutputStream();

    @Override
    boolean isSocketBound() {
        return socket.isBound();
    }

    @Override
    boolean isSocketConnected() {
        return socket.isConnected();
    }

    @Override
    InetSocketAddress getLocalSocketAddress() throws Exception {
    	//获得该套接字绑定端的地址，如果没有绑定则为NULL
        return (InetSocketAddress) socket.getLocalSocketAddress();
    }

    @Override
    InetSocketAddress getRemoteSocketAddress() throws Exception {
    	//获得该套接字已连接，则返回对端的地址
        return (InetSocketAddress) socket.getRemoteSocketAddress();
    }

    @Override
    void closeSocket() throws IOException {
        socket.close();
    }

    @Override
    boolean isSocketClosed() {
        return socket.isClosed();
    }
}
