package com.hui.zhang.spider.remoteing.netty.client.handler;

import com.hui.zhang.spider.future.InvokeFuture;
import com.hui.zhang.spider.remoteing.netty.data.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
	private static final Logger LOGGER = LoggerFactory.getLogger(RpcClientHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
		//LOGGER.info("接收数据->{}", response);
		LOGGER.debug("[spider] receive data:{}",response.getResult());
		InvokeFuture invokeFuture = InvokeFuture.futures.remove(response.getRequestId());
		if (null!=invokeFuture){
			invokeFuture.set(response);
		}

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		LOGGER.debug("[spider] channel exception, close channel {}, {}", ctx.channel(), cause);
		ctx.close();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		LOGGER.debug("[spider] channel inactive {}}", ctx.channel());
		ctx.close();
	}
}
