package org.jboss.netty.channel;

public interface ChannelHandlerContext {

	// 得到这个pipeline所属的Channel，等价于getPipeline().getChannel()
	Channel getChannel();

	// Handler所属的pipeline
	ChannelPipeline getPipeline();

	// Handler都有对应的名字
	String getName();

	// 返回这个Context维护的Handler
	ChannelHandler getHandler();

	// 对应的Handler类型，看是否是ChannelUpstreamHandler，ChannelDownstreamHandler实例
	boolean canHandleUpstream();

	boolean canHandleDownstream();

	// 传递事件给最近的Handler
	void sendUpstream(ChannelEvent e);

	void sendDownstream(ChannelEvent e);

	Object getAttachment();

	void setAttachment(Object attachment);
}