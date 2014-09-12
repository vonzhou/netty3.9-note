package org.jboss.netty.channel;

import java.util.List;
import java.util.Map;


/**
 * ChannelPipeline的作用就是组织一系列的ChannelHandlers 为某一个Channel服务，处理各种事件。
 * 实现了拦截过滤器的高级形式（an advanced form of the Intercepting Filter pattern），
 * 进而有效控制如何处理一个事件以及ChannelHandlers之间如何交互。
 */
public interface ChannelPipeline {

    /**
     * 在头部插入这个ChannelHandler；
     * 如果同名的Handler已经存在，IllegalArgumentException
     * 如果某个参数为null，NullPointerException
     */
    void addFirst(String name, ChannelHandler handler);

    /**
     * 尾部插入
     */
    void addLast(String name, ChannelHandler handler);

    /**
     * 在这个指定的baseName Handler前面插入
     * baseName指定的Handler不在，NoSuchElementException；
     * 剩下的俩个异常同上面；
     */
    void addBefore(String baseName, String name, ChannelHandler handler);

    /**
     * 在这个指定的baseName Handler后面插入
     */
    void addAfter(String baseName, String name, ChannelHandler handler);

    /**
     * Removes the specified ChannelHandler from this pipeline.
     */
    void remove(ChannelHandler handler);

    /**
     * 移除名为name的Handler，并返回
     */
    ChannelHandler remove(String name);

    /**
     * 移除某个类型的Handler，泛型方法
     */
    <T extends ChannelHandler> T remove(Class<T> handlerType);

    /**
     * Removes the first/last ChannelHandler in this pipeline.
     *如果pipeline为空的话，NoSuchElementException；
     */
    ChannelHandler removeFirst();
    ChannelHandler removeLast();

    /**
     * Handler的替换
     */
    void replace(ChannelHandler oldHandler, String newName, ChannelHandler newHandler);

    /**
     * Handler的替换；
     * 只不过这里旧的Handler用名字指定；
     */
    ChannelHandler replace(String oldName, String newName, ChannelHandler newHandler);

    /**
     * 替换某个类型的Handler；
     */
    <T extends ChannelHandler> T replace(Class<T> oldHandlerType, String newName, ChannelHandler newHandler);

    /**
     * 各种获Handler的方法，第一个
     */
    ChannelHandler getFirst();

    /**
     * 最后一个
     */
    ChannelHandler getLast();

    /**
     * 以Handler之名
     */
    ChannelHandler get(String name);

    /**
     * 以HandlerClass类型名
     */
    <T extends ChannelHandler> T get(Class<T> handlerType);

    /**
     * 得到这个Handler的context
     */
    ChannelHandlerContext getContext(ChannelHandler handler);

    /**
     * 以名得到这个Handler的context
     */
    ChannelHandlerContext getContext(String name);

    /**
     * 根据类型得到这个Handler的context
     */
    ChannelHandlerContext getContext(Class<? extends ChannelHandler> handlerType);

    /**
     * 发送一个事件给该流水线第一个ChannelUpstreamHandler；
     * 因为这个pipeline发送的
     */
    void sendUpstream(ChannelEvent e);

    /**
     *  发送一个事件给该流水线第一个ChannelDownstreamHandler；
     * 因为这个pipeline发送的
     */
    void sendDownstream(ChannelEvent e);

    /**
     * 调度一个任务（Runnable对象）给这个pipeline的IO线程去执行；
     */
    ChannelFuture execute(Runnable task);

    /**
     * 所属的Channel；
     */
    Channel getChannel();

    /**
     * 关联的ChannelSink；
     */
    ChannelSink getSink();

    /**
     * 将该Pipeline添置到特定的通道和ChannelSink；
     * 一旦添置，就是永久的。
     * Attaches this pipeline to the specified Channel and
     * ChannelSink.  Once a pipeline is attached, it can't be detached
     * nor attached again.
     *
     * 抛出IllegalStateException 如果这个pipeline已经添加到某个channelSink；
     */
    void attach(Channel channel, ChannelSink sink);

    /**
     * 判断这个Pipeline是否已加入某个Channel
     */
    boolean isAttached();

    /**
     * 获得所有Handler的名字
     */
    List<String> getNames();

    /**
     * 获得所有Handler的信息，名字，Handler 映射表；是有序的TreeMap;
     */
    Map<String, ChannelHandler> toMap();
}
