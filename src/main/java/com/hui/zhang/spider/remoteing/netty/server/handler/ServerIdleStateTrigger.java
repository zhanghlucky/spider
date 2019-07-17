package com.hui.zhang.spider.remoteing.netty.server.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by zuti on 2018/3/1.
 * email zuti@centaur.cn
 */

@ChannelHandler.Sharable
public class ServerIdleStateTrigger extends ChannelInboundHandlerAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerIdleStateTrigger.class);

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if(evt instanceof IdleStateEvent){
			IdleStateEvent event = (IdleStateEvent)evt;
			//读超时
			if(event.state() == IdleState.READER_IDLE){
				LOGGER.warn("[spider] server read timeout, close channel {}", ctx.channel().id());
				ctx.channel().close();//关闭channel
			}
		} else {
			super.userEventTriggered(ctx, evt);
		}
	}

}
