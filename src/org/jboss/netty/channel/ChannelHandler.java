package org.jboss.netty.channel;

import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.channel.group.ChannelGroup;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 每个通道关联一个Pipeline，在流水线中拦截处理各种事件的对象就是ChannelHandler，
 * 它处理ChannelEvent而后进行传递。接口ChannelHandler没有提供任何方法，有两个子接口分别用来规范处理上行和下行的通道事件。
 */
public interface ChannelHandler {

    /**
     * Indicates that the same instance of the annotated {@link ChannelHandler}
     * can be added to one or more {@link ChannelPipeline}s multiple times
     * without a race condition.
     * <p>
     * If this annotation is not specified, you have to create a new handler
     * instance every time you add it to a pipeline because it has unshared
     * state such as member variables.
     * <p>
     * This annotation is provided for documentation purpose, just like
     * <a href="http://www.javaconcurrencyinpractice.com/annotations/doc/">the JCIP annotations</a>.
     */
    @Inherited
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Sharable {
        // no value
    }
}
