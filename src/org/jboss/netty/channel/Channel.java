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

import java.net.SocketAddress;

public interface Channel extends Comparable<Channel> {

	int OP_NONE = 0;
	int OP_READ = 1;
	int OP_WRITE = 4;
	int OP_READ_WRITE = OP_READ | OP_WRITE;

	Integer getId();

	ChannelFactory getFactory();

	Channel getParent();

	ChannelConfig getConfig();

	ChannelPipeline getPipeline();

	boolean isOpen();

	boolean isBound();

	boolean isConnected();

	SocketAddress getLocalAddress();

	SocketAddress getRemoteAddress();

	ChannelFuture write(Object message);

	ChannelFuture write(Object message, SocketAddress remoteAddress);

	ChannelFuture bind(SocketAddress localAddress);

	ChannelFuture connect(SocketAddress remoteAddress);

	ChannelFuture disconnect();

	ChannelFuture unbind();

	ChannelFuture close();

	ChannelFuture getCloseFuture();

	int getInterestOps();

	boolean isReadable();

	boolean isWritable();

	ChannelFuture setInterestOps(int interestOps);

	ChannelFuture setReadable(boolean readable);

	Object getAttachment();

	void setAttachment(Object attachment);
}

