package org.jboss.netty.channel.socket.oio;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.DefaultFileRegion;
import org.jboss.netty.channel.FileRegion;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.SocketException;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.util.regex.Pattern;

import static org.jboss.netty.channel.Channels.*;

class OioWorker extends AbstractOioWorker<OioSocketChannel> {

    private static final Pattern SOCKET_CLOSED_MESSAGE = Pattern.compile(
            "^.*(?:Socket.*closed).*$", Pattern.CASE_INSENSITIVE);

    OioWorker(OioSocketChannel channel) {
        super(channel);
    }

    @Override
    public void run() {
        boolean fireConnected = channel instanceof OioAcceptedSocketChannel;
        if (fireConnected && channel.isOpen()) {
            // Fire the channelConnected event for OioAcceptedSocketChannel.
            // See #287
            fireChannelConnected(channel, channel.getRemoteAddress());
        }
        super.run();
    }

    /**
     * 关键实现。
     * 处理到达的消息；
     */
    @Override
    boolean process() throws IOException {
        byte[] buf;
        int readBytes;
        PushbackInputStream in = channel.getInputStream();
        int bytesToRead = in.available();
        if (bytesToRead > 0) {
            buf = new byte[bytesToRead];
            readBytes = in.read(buf);
        } else {
        	//注意read方法的返回值：
        	//the next byte of data, or -1 if the end of the stream has been reached. 
            int b = in.read();
            if (b < 0) {
                return false;
            }
            
            in.unread(b);
            return true;
        }
        /// 读取到消息后，通知上面的handler；
        fireMessageReceived(channel, channel.getConfig().getBufferFactory().getBuffer(buf, 0, readBytes));

        return true;
    }

    static void write(
            OioSocketChannel channel, ChannelFuture future,
            Object message) {

        boolean iothread = isIoThread(channel);
        OutputStream out = channel.getOutputStream();
        if (out == null) {
            Exception e = new ClosedChannelException();
            future.setFailure(e);
            if (iothread) {
                fireExceptionCaught(channel, e);
            } else {
                fireExceptionCaughtLater(channel, e);
            }
            return;
        }

        try {
            int length = 0;

            // Add support to write a FileRegion. This in fact will not give any performance gain
            // but at least it not fail and we did the best to emulate it
            if (message instanceof FileRegion) {
                FileRegion fr = (FileRegion) message;
                try {
                    synchronized (out) {
                        WritableByteChannel  bchannel = Channels.newChannel(out);

                        long i;
                        while ((i = fr.transferTo(bchannel, length)) > 0) {
                            length += i;
                            if (length >= fr.getCount()) {
                                break;
                            }
                        }
                    }
                } finally {
                    if (fr instanceof DefaultFileRegion) {
                        DefaultFileRegion dfr = (DefaultFileRegion) fr;
                        if (dfr.releaseAfterTransfer()) {
                            fr.releaseExternalResources();
                        }
                    }
                }
            } else {
                ChannelBuffer a = (ChannelBuffer) message;
                length = a.readableBytes();
                synchronized (out) {
                    a.getBytes(a.readerIndex(), out, length);
                }
            }

            future.setSuccess();
            if (iothread) {
                fireWriteComplete(channel, length);
            } else {
                fireWriteCompleteLater(channel, length);
            }

        } catch (Throwable t) {
            // Convert 'SocketException: Socket closed' to
            // ClosedChannelException.
            if (t instanceof SocketException &&
                    SOCKET_CLOSED_MESSAGE.matcher(
                            String.valueOf(t.getMessage())).matches()) {
                t = new ClosedChannelException();
            }
            future.setFailure(t);
            if (iothread) {
                fireExceptionCaught(channel, t);
            } else {
                fireExceptionCaughtLater(channel, t);
            }
        }
    }
}
