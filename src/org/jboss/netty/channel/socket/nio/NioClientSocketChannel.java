package org.jboss.netty.channel.socket.nio;

import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelSink;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.util.Timeout;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import static org.jboss.netty.channel.Channels.*;

final class NioClientSocketChannel extends NioSocketChannel {

    private static final InternalLogger logger =
        InternalLoggerFactory.getInstance(NioClientSocketChannel.class);

    private static SocketChannel newSocket() {
        SocketChannel socket;
        try {
            socket = SocketChannel.open();
        } catch (IOException e) {
            throw new ChannelException("Failed to open a socket.", e);
        }

        boolean success = false;
        try {
            socket.configureBlocking(false);
            success = true;
        } catch (IOException e) {
            throw new ChannelException("Failed to enter non-blocking mode.", e);
        } finally {
            if (!success) {
                try {
                    socket.close();
                } catch (IOException e) {
                    if (logger.isWarnEnabled()) {
                        logger.warn(
                                "Failed to close a partially initialized socket.",
                                e);
                    }
                }
            }
        }

        return socket;
    }

    volatile ChannelFuture connectFuture;
    volatile boolean boundManually;

    // Does not need to be volatile as it's accessed by only one thread.
    long connectDeadlineNanos;
    volatile SocketAddress requestedRemoteAddress;

    volatile Timeout timoutTimer;

    NioClientSocketChannel(
            ChannelFactory factory, ChannelPipeline pipeline,
            ChannelSink sink, NioWorker worker) {

        super(null, factory, pipeline, sink, newSocket(), worker);
        fireChannelOpen(this);
    }
}
