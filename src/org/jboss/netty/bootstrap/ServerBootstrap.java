package org.jboss.netty.bootstrap;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.DefaultChannelFuture;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static org.jboss.netty.channel.Channels.*;

/**
 * 和前面ClientBootstrap对应的，这里是服务器端的启动器，ServerBootsrap只针对于面向连接的传输，
 * 如TCP和local transport。如果是如UDP无连接的传输应该用ConnectionlessBootstrap，
 * 因为对于UDP而言对到达的连接请求直接接收消息，而不是创建一个子通道来服务每个客户。
 * 在服务器端，一个父通道指的是监听套接字对应的通道，接收连接请求，parent channel 是通过
 * 该bootstrap的ChannelFactory 调用bind而生成的。一旦成功绑定，
 * 这个parent Channel就可以接受请求，对应的就创建子通道。
 */
public class ServerBootstrap extends Bootstrap {

    private volatile ChannelHandler parentHandler;

    /**
     * Creates a new instance with no ChannelFactory  set.
     *  setFactory(ChannelFactory)  must be called before any I/O operation is requested.
     */
    public ServerBootstrap() {
    }

    /**
     * Creates a new instance with the specified initial ChannelFactory.
     */
    public ServerBootstrap(ChannelFactory channelFactory) {
        super(channelFactory);
    }

    /**
     * 设置server端的ServerChannelFactory，用来执行IO操作。
     * 该方法只能被调用一次，如果构造函数指定了，就不能再次设定。
     */
    @Override
    public void setFactory(ChannelFactory factory) {
        if (factory == null) {
            throw new NullPointerException("factory");
        }
        if (!(factory instanceof ServerChannelFactory)) {
            throw new IllegalArgumentException(
                    "factory must be a " +
                    ServerChannelFactory.class.getSimpleName() + ": " +
                    factory.getClass());
        }
        super.setFactory(factory);
    }

    /**
     * Returns an optional {@link ChannelHandler} which intercepts an event
     * of a newly bound server-side channel which accepts incoming connections.
     *
     * @return the parent channel handler.
     *         {@code null} if no parent channel handler is set.
     */
    public ChannelHandler getParentHandler() {
        return parentHandler;
    }

    /**
     * Sets an optional {@link ChannelHandler} which intercepts an event of
     * a newly bound server-side channel which accepts incoming connections.
     *
     * @param parentHandler
     *        the parent channel handler.
     *        {@code null} to unset the current parent channel handler.
     */
    public void setParentHandler(ChannelHandler parentHandler) {
        this.parentHandler = parentHandler;
    }

    /**
     * 创建一个Channel，会绑定到选项"localAddress"指定的套接字地址；
     */
    public Channel bind() {
        SocketAddress localAddress = (SocketAddress) getOption("localAddress");
        if (localAddress == null) {
            throw new IllegalStateException("localAddress option is not set.");
        }
        return bind(localAddress);
    }

    /**
     * 同上，只是绑定的时候指定地址；
     */
    public Channel bind(final SocketAddress localAddress) {
        ChannelFuture future = bindAsync(localAddress);

        // Wait for the future.
        future.awaitUninterruptibly();// 等待ChannelFuture完成。
        if (!future.isSuccess()) {
            future.getChannel().close().awaitUninterruptibly();
            throw new ChannelException("Failed to bind to: " + localAddress, future.getCause());
        }

        return future.getChannel();
    }

    /**
     * Bind a channel asynchronous to the local address
     * specified in the current {@code "localAddress"} option.  This method is
     * similar to the following code:
     *
     * <pre>
     * {@link ServerBootstrap} b = ...;
     * b.bindAsync(b.getOption("localAddress"));
     * </pre>
     *
     *
     * @return a new {@link ChannelFuture} which will be notified once the Channel is
     * bound and accepts incoming connections
     *
     * @throws IllegalStateException
     *         if {@code "localAddress"} option was not set
     * @throws ClassCastException
     *         if {@code "localAddress"} option's value is
     *         neither a {@link SocketAddress} nor {@code null}
     * @throws ChannelException
     *         if failed to create a new channel and
     *                      bind it to the local address
     */
    public ChannelFuture bindAsync() {
        SocketAddress localAddress = (SocketAddress) getOption("localAddress");
        if (localAddress == null) {
            throw new IllegalStateException("localAddress option is not set.");
        }
        return bindAsync(localAddress);
    }

    /**
     * 异步的来执行通道绑定；
     * Bind a channel asynchronous to the specified local address.
     *
     *返回的ChannelFuture用来在通道绑定成功（可以接受连接请求）后得到通知；
     *
     */
    public ChannelFuture bindAsync(final SocketAddress localAddress) {
        if (localAddress == null) {
            throw new NullPointerException("localAddress");
        }
        Binder binder = new Binder(localAddress);
        ChannelHandler parentHandler = getParentHandler();

        ChannelPipeline bossPipeline = pipeline(); //创建一个默认的Pipeline。
        // 注意这里，这个主通道流水线增加了一个binder对象；
        bossPipeline.addLast("binder", binder);
        
        if (parentHandler != null) {
            bossPipeline.addLast("userHandler", parentHandler);
        }

        // 创建主通道（监听套接字通道），关联的pipeline是bossPipeline；
        // 比如 NioServerSocketChannel(this, pipeline, sink, bossPool.nextBoss(), workerPool);
        Channel channel = getFactory().newChannel(bossPipeline);
        // 创建一个不可取消的绑定Future
        final ChannelFuture bfuture = new DefaultChannelFuture(channel, false);
        
        // 给Binder对象中的Future增加一个观察者，如果那个Future成功的话，这里的bfuture就会成功
        // 所以还是看那边的 何时成功。
        binder.bindFuture.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    bfuture.setSuccess();
                } else {
                    // Call close on bind failure
                    bfuture.getChannel().close();
                    bfuture.setFailure(future.getCause());
                }
            }
        });
        return bfuture;
    }

    private final class Binder extends SimpleChannelUpstreamHandler {

        private final SocketAddress localAddress;
        private final Map<String, Object> childOptions =   new HashMap<String, Object>();
        private final DefaultChannelFuture bindFuture = new DefaultChannelFuture(null, false);
        
        Binder(SocketAddress localAddress) {
            this.localAddress = localAddress;
        }

        /**
         * 在 handleUpstream 方法中会调用
         */
        @Override
        public void channelOpen( ChannelHandlerContext ctx, ChannelStateEvent evt) {

            try {
            	// 内部类可以访问外部类的方法 ，如 getPipelineFactory()。
                evt.getChannel().getConfig().setPipelineFactory(getPipelineFactory());

                // Split options into two categories: parent and child.
                Map<String, Object> allOptions = getOptions();
                Map<String, Object> parentOptions = new HashMap<String, Object>();
                for (Entry<String, Object> e: allOptions.entrySet()) {
                    if (e.getKey().startsWith("child.")) {
                        childOptions.put( e.getKey().substring(6), e.getValue());
                    } else if (!"pipelineFactory".equals(e.getKey())) {
                        parentOptions.put(e.getKey(), e.getValue());
                    }
                }

                // Apply parent options.
                evt.getChannel().getConfig().setOptions(parentOptions);
            } finally {
                ctx.sendUpstream(evt);
            }

            //============ 真正的绑定套接字 =======
            evt.getChannel().bind(localAddress).addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                    	// 这里触发了 bind的成功
                        bindFuture.setSuccess();
                    } else {
                        bindFuture.setFailure(future.getCause());
                    }
                }
            });
        }

        @Override
        public void childChannelOpen(
                ChannelHandlerContext ctx,
                ChildChannelStateEvent e) throws Exception {
            // Apply child options.
            try {
                e.getChildChannel().getConfig().setOptions(childOptions);
            } catch (Throwable t) {
                fireExceptionCaught(e.getChildChannel(), t);
            }
            ctx.sendUpstream(e);
        }

        @Override
        public void exceptionCaught(
                ChannelHandlerContext ctx, ExceptionEvent e)
                throws Exception {
            bindFuture.setFailure(e.getCause());
            ctx.sendUpstream(e);
        }
    }
}
