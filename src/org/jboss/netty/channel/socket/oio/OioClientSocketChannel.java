package org.jboss.netty.channel.socket.oio;

import static org.jboss.netty.channel.Channels.*;

import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;

import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelSink;

//注意访问权限是package
class OioClientSocketChannel extends OioSocketChannel {

    volatile PushbackInputStream in;
    volatile OutputStream out;

    OioClientSocketChannel(
            ChannelFactory factory,
            ChannelPipeline pipeline,
            ChannelSink sink) {

        super(null, factory, pipeline, sink, new Socket());

        //发送"channelOpen"事件给该通道的流水线中第一个ChannelUpstreamHandler
    	//如果这个Channel有parent，那么一个"childChannelOpen"事件也会发射
        fireChannelOpen(this);//-->Channels
    }

    @Override
    PushbackInputStream getInputStream() {
        return in;
    }

    @Override
    OutputStream getOutputStream() {
        return out;
    }
}
