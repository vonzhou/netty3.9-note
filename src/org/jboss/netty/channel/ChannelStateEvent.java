package org.jboss.netty.channel;


public interface ChannelStateEvent extends ChannelEvent {

    //Returns the changed property of the Channel
    ChannelState getState();

     // Returns the value of the changed property of the Channel
    //这些值就是true，false，一次来区分一个状态的两面，比如绑定，解绑定
    //或者是对端的套接字地址
    Object getValue();
}
