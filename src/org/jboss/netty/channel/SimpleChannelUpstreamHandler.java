package org.jboss.netty.channel;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;

import java.util.List;


/**
 * 对于不同的事件类型都提供了方法。他会进行向下转型这些接收到的 upstream event，
 * 得到更加有意义的子类型，而后调用适当的处理方法。这些方法的名字和在ChannelEvent中对应的事件名字是一样的。
 */
public class SimpleChannelUpstreamHandler implements ChannelUpstreamHandler {

    private static final InternalLogger logger =
        InternalLoggerFactory.getInstance(SimpleChannelUpstreamHandler.class.getName());

    /**
     * 向下转型，具体执行。
     * Down-casts the received upstream event into more meaningful sub-type 
     * event and calls an appropriate handler method with the down-casted event.
     */
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {

        if (e instanceof MessageEvent) {
            messageReceived(ctx, (MessageEvent) e);
        } else if (e instanceof WriteCompletionEvent) {
            WriteCompletionEvent evt = (WriteCompletionEvent) e;
            writeComplete(ctx, evt);
        } else if (e instanceof ChildChannelStateEvent) {
            ChildChannelStateEvent evt = (ChildChannelStateEvent) e;
            if (evt.getChildChannel().isOpen()) {
                childChannelOpen(ctx, evt);
            } else {
                childChannelClosed(ctx, evt);
            }
        } else if (e instanceof ChannelStateEvent) {
            ChannelStateEvent evt = (ChannelStateEvent) e;
            switch (evt.getState()) {
            case OPEN:
                if (Boolean.TRUE.equals(evt.getValue())) {
                    channelOpen(ctx, evt);
                } else {
                    channelClosed(ctx, evt);
                }
                break;
            case BOUND:
                if (evt.getValue() != null) {
                    channelBound(ctx, evt);
                } else {
                    channelUnbound(ctx, evt);
                }
                break;
            case CONNECTED:
                if (evt.getValue() != null) {
                    channelConnected(ctx, evt);
                } else {
                    channelDisconnected(ctx, evt);
                }
                break;
            case INTEREST_OPS:
                channelInterestChanged(ctx, evt);
                break;
            default:
                ctx.sendUpstream(e);
            }
        } else if (e instanceof ExceptionEvent) {
            exceptionCaught(ctx, (ExceptionEvent) e);
        } else {
            ctx.sendUpstream(e);
        }
    }

    /**
     * 当接从远端收到一个消息实体（如ChannelBuffer）的时候执行。
     */
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        ctx.sendUpstream(e);
    }

    /**
     * 当IO线程或ChannelHandler发生异常时。
     */
    public void exceptionCaught(
            ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        ChannelPipeline pipeline = ctx.getPipeline();

        ChannelHandler last = pipeline.getLast();
        if (!(last instanceof ChannelUpstreamHandler) && ctx instanceof DefaultChannelPipeline) {
            // The names comes in the order of which they are insert when using DefaultChannelPipeline
            List<String> names = ctx.getPipeline().getNames();
            for (int i = names.size() - 1; i >= 0; i--) {
                ChannelHandler handler = ctx.getPipeline().get(names.get(i));
                if (handler instanceof ChannelUpstreamHandler) {
                    // find the last handler
                    last = handler;
                    break;
                }
            }
        }
        if (this == last) {
            logger.warn(
                    "EXCEPTION, please implement " + getClass().getName() +
                    ".exceptionCaught() for proper handling.", e.getCause());
        }
        ctx.sendUpstream(e);
    }

    /**
     * 通道打开时，没有绑定或连接。
     * 注意的是：这个事件是IO线程内部触发的（下面的Binder），我们不能再这里执行复杂操作（heavy operation）
     * 否则会阻塞向其他worker分发。
     */
    public void channelOpen( ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        ctx.sendUpstream(e);
    }

    /**
     * 打开绑定后执行。
     * 注意同上。
     */
    public void channelBound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        ctx.sendUpstream(e);
    }

    /**
     * 打开，绑定，连接之后执行。
     * 注意同上。
     */
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        ctx.sendUpstream(e);
    }

    /**
     * 当通道的interestOps改变后执行。
     */
    public void channelInterestChanged(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        ctx.sendUpstream(e);
    }

    /**
     * 断开与远端的连接之时。
     */
    public void channelDisconnected(  ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        ctx.sendUpstream(e);
    }

    /**
     * Invoked when a Channel was unbound from the current local address.
     */
    public void channelUnbound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        ctx.sendUpstream(e);
    }

    /**
     * Invoked when a Channel was closed and all its related resources
     * were released.
     */
    public void channelClosed(
            ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        ctx.sendUpstream(e);
    }

    /**
     * Invoked when something was written into a Channel.
     */
    public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e) throws Exception {
        ctx.sendUpstream(e);
    }

    /**
     * 当子通道打开时，比如说a server channel accepted a connection
     */
    public void childChannelOpen(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {
        ctx.sendUpstream(e);
    }

    /**
     * 子通道关闭时，比如连接套接字关闭。
     */
    public void childChannelClosed(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {
        ctx.sendUpstream(e);
    }
}
