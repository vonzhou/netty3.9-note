package org.jboss.netty.channel;

/**
 * A {@link ChannelFactory} that creates a {@link ServerChannel}.
 *
 * @apiviz.has        org.jboss.netty.channel.ServerChannel oneway - - creates
 */
public interface ServerChannelFactory extends ChannelFactory {
    ServerChannel newChannel(ChannelPipeline pipeline);
}
