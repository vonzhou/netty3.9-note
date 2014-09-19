package org.jboss.netty.bootstrap;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineException;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * 客户端Channel辅助类
 */
public class ClientBootstrap extends Bootstrap {

    /**
     * 没有设置ChannelFactory，在IO请求之前要setFactory(ChannelFactory)
     */
    public ClientBootstrap() {
    }

    /**
     * 用指定的 ChannelFactory来创建实例.
     */
    public ClientBootstrap(ChannelFactory channelFactory) {
        super(channelFactory);
    }

    /**
     * 通过和"localAddress"的配置来connect，
     * 如果"localAddress"没有 设置，则会自动分配
     * 如果"remoteAddress"没有的话就会抛出IllegalStateException
     * 如果创建ChannelPipeline失败，则ChannelPipelineException
     */
    public ChannelFuture connect() {
        SocketAddress remoteAddress = (SocketAddress) getOption("remoteAddress");
        if (remoteAddress == null) {
            throw new IllegalStateException("remoteAddress option is not set.");
        }
        return connect(remoteAddress);
    }

    /**
     * 如果从选项中得到的localAddress不是SocketAddress类型或者null就会抛出ClassCastException
     * 其他的同上
     */
    public ChannelFuture connect(SocketAddress remoteAddress) {
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }
        SocketAddress localAddress = (SocketAddress) getOption("localAddress");
        return connect(remoteAddress, localAddress);
    }

    /**
     * 尝试和对端连接，如果 localAddress=null，本机套接字地址就自动分配；
     * 
     * 当连接尝试成功或失败后 ChannelFuture 会获得通知；
     * 
     *  如果创建ChannelPipeline失败，则ChannelPipelineException
     */
    public ChannelFuture connect(final SocketAddress remoteAddress, final SocketAddress localAddress) {

        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }

        ChannelPipeline pipeline;
        try {
        	//先由pipeline Factory得到一个流水线
            pipeline = getPipelineFactory().getPipeline();
        } catch (Exception e) {
            throw new ChannelPipelineException("Failed to initialize a pipeline.", e);
        }

        //Set the options.
        //ChannelFactory根据指定的pipeline创建一个通道，创建的时候就会打开，比如看 OioClientSocketChannel
        Channel ch = getFactory().newChannel(pipeline);
        boolean success = false;
        try {
        	// 配置通道
            ch.getConfig().setOptions(getOptions());
            success = true;
        } finally {
            if (!success) {
                ch.close();
            }
        }

        //这里的绑定，连接都是有AbstractChannel实现的-通过Channels中的静态方法；
        // Bind.
        if (localAddress != null) {
            ch.bind(localAddress);
        }

        // Connect.这里最终会调用Channels.connect辅助方法
        return ch.connect(remoteAddress);
    }

    /**
     * 绑定操作，在绑定和连接需要分开的时候用到。
     * 比如在尝试连接之前，可以通过Channel.setAttachment(Object)为这个通道设置一个attachment
     * 这样一旦连接建议，就可以访问这个attachment
     */
    public ChannelFuture bind(final SocketAddress localAddress) {

        if (localAddress == null) {
            throw new NullPointerException("localAddress");
        }

        ChannelPipeline pipeline;
        try {
            pipeline = getPipelineFactory().getPipeline();
        } catch (Exception e) {
            throw new ChannelPipelineException("Failed to initialize a pipeline.", e);
        }

        // Set the options.
        Channel ch = getFactory().newChannel(pipeline);
        boolean success = false;
        try {
            ch.getConfig().setOptions(getOptions());
            success = true;
        } finally {
            if (!success) {
                ch.close();
            }
        }
        // Bind.
        return ch.bind(localAddress);
    }
}
