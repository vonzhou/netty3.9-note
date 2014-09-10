package org.jboss.netty.channel;

import static org.jboss.netty.channel.Channels.*;

/**
 * The default upstream ChannelStateEvent implementation.
 */
public class UpstreamChannelStateEvent implements ChannelStateEvent {

    private final Channel channel;
    private final ChannelState state;
    private final Object value;


    public UpstreamChannelStateEvent(
            Channel channel, ChannelState state, Object value) {

        if (channel == null) {
            throw new NullPointerException("channel");
        }
        if (state == null) {
            throw new NullPointerException("state");
        }

        this.channel = channel;
        this.state = state;
        this.value = value;
    }

    public Channel getChannel() {
        return channel;
    }

    //因为是上行事件，所以事件发生了
    public ChannelFuture getFuture() {
        return succeededFuture(getChannel());
    }

    public ChannelState getState() {
        return state;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        String channelString = getChannel().toString();
        StringBuilder buf = new StringBuilder(channelString.length() + 64);
        buf.append(channelString);
        switch (getState()) {
        case OPEN:
            if (Boolean.TRUE.equals(getValue())) {
                buf.append(" OPEN");
            } else {
                buf.append(" CLOSED");
            }
            break;
        case BOUND:
            if (getValue() != null) {
                buf.append(" BOUND: ");
                buf.append(getValue());
            } else {
                buf.append(" UNBOUND");
            }
            break;
        case CONNECTED:
            if (getValue() != null) {
                buf.append(" CONNECTED: ");
                buf.append(getValue());
            } else {
                buf.append(" DISCONNECTED");
            }
            break;
        case INTEREST_OPS:
            buf.append(" INTEREST_CHANGED");
            break;
        default:
            buf.append(getState().name());
            buf.append(": ");
            buf.append(getValue());
        }
        return buf.toString();
    }
}
