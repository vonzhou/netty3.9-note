/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
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