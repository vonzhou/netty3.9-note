package org.jboss.netty.channel.socket.nio;


import java.nio.channels.Selector;

/**
 * 存在的意义是什么？解决那个bug？
 */
public interface NioSelectorPool {

    /**
     * Replaces the current {@link Selector}s of the {@link Boss}es with new {@link Selector}s to work around the
     * infamous epoll 100% CPU bug.
     */
    void rebuildSelectors();

    /**
     * Shutdown the {@link NioSelectorPool} and all internal created resources
     */
    void shutdown();
}
