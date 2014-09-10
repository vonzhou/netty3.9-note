package org.jboss.netty.channel;


// 详细解释看这里 http://blog.csdn.net/vonzhoufz/article/details/39181917
public interface ChannelEvent {

	//返回这个事件的关联的Channel
    Channel getChannel();

    //如果是upstream event，该方法总是返回SucceededChannelFuture，因为已经发生了
    //如果downstream event（比如IO请求），那么返回的Future就会在IO请求实际完成后得到通知
    ChannelFuture getFuture();
}
