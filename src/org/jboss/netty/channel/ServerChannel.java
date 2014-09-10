package org.jboss.netty.channel;


//可以视为监听套接字通道
//A Channel that accepts an incoming connection attempt and creates 
//its child Channels by accepting them. ServerSocketChannel is a good example.
public interface ServerChannel extends Channel {
    // This is a tag interface.
}
