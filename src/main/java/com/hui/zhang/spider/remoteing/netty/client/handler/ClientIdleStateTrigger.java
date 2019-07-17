package com.hui.zhang.spider.remoteing.netty.client.handler;

import com.hui.zhang.spider.remoteing.netty.data.RpcRequest;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Created by zuti on 2018/3/1.
 * email zuti@centaur.cn
 */
@ChannelHandler.Sharable
public class ClientIdleStateTrigger extends ChannelInboundHandlerAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientIdleStateTrigger.class);

	private static RpcRequest HEARTBEAT;

	static {
		HEARTBEAT = new RpcRequest();
		HEARTBEAT.setRequestId(UUID.randomUUID().toString());
		HEARTBEAT.setClassName("heartbeat");
		HEARTBEAT.setVersion(null);
		HEARTBEAT.setMethodName(null);
		HEARTBEAT.setParameterTypes(null);
		HEARTBEAT.setParameters(null);
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state() == IdleState.WRITER_IDLE) {
				LOGGER.debug("[spider] channel idle, send heartbeat message, channel id is{}", ctx.channel().id());
				ctx.channel().writeAndFlush(HEARTBEAT);
			}
		} else {
			super.userEventTriggered(ctx, evt);
		}
	}

}
