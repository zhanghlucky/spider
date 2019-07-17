package com.test;

import com.hui.zhang.spider.remoteing.netty.client.connector.SpiderConnector;
import com.hui.zhang.spider.remoteing.netty.client.handler.RpcClientHandler;
import com.hui.zhang.spider.remoteing.netty.coder.RpcDecoder;
import com.hui.zhang.spider.remoteing.netty.coder.RpcEncoder;
import com.hui.zhang.spider.remoteing.netty.data.RpcRequest;
import com.hui.zhang.spider.remoteing.netty.data.RpcResponse;
import com.hui.zhang.spider.remoteing.netty.server.handler.RpcServerHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Created by zuti on 2018/2/26.
 * email zuti@centaur.cn
 */
public class TestHeartbeat {

//	@Test
	public void testClient() throws Exception {
		EventLoopGroup group = new NioEventLoopGroup();
		RpcClientHandler rpcClientHandler = new RpcClientHandler();
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(group).channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel channel) throws Exception {
						channel.pipeline()
								.addLast("handler", new IdleStateHandler(0, 3, 0, TimeUnit.SECONDS))
								.addLast(new RpcEncoder(RpcRequest.class)) // 将 RPC 请求进行编码（为了发送请求）
								.addLast(new RpcDecoder(RpcResponse.class)) // 将 RPC 响应进行解码（为了处理响应）
								.addLast(rpcClientHandler); // 使用 RpcClient 发送 RPC 请求
					}
				})
				.option(ChannelOption.SO_KEEPALIVE, true);

		ChannelFuture future = bootstrap.connect("192.168.50.128", 38802).sync();
		future.channel().closeFuture().sync().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				System.out.println("channel 关闭。。。。。。。。。。。。。。。。。。。。。。。。。。");
			}
		});

		Thread.sleep(Integer.MAX_VALUE);

	}

//	@Test
	public void testClientLong() throws Exception {
		String host = "192.168.50.128";
		int port = 38809;
		Channel channel = new SpiderConnector().connect(host, port);

		Thread.sleep(Integer.MAX_VALUE);
	}

//	@Test
	public void testServer() {
		//是用来处理I/O操作的线程池
		EventLoopGroup bossGroup = new NioEventLoopGroup();//用来accept客户端连接
		EventLoopGroup workerGroup = new NioEventLoopGroup();//处理客户端数据的读写操作
		try {
			ServerBootstrap bootstrap =  new ServerBootstrap();
			bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel channel) throws Exception {
							((Channel) channel).pipeline()
									.addLast("handler",new IdleStateHandler(5, 0, 0, TimeUnit.SECONDS))
									.addLast(new RpcDecoder(RpcRequest.class)) // 将 RPC 请求进行解码（为了处理请求） ChannelInboundHandlerAdapter  1
									.addLast(new RpcEncoder(RpcResponse.class)) // 将 RPC 响应进行编码（为了返回响应）ChannelOutboundHandlerAdapter 2
									.addLast(new RpcServerHandler()); // 处理 RPC 请求 SimpleChannelInboundHandler 3
						}
					})
					.option(ChannelOption.SO_BACKLOG, 128)
					.childOption(ChannelOption.SO_KEEPALIVE, true);

			ChannelFuture future = bootstrap.bind(getBindAddress()).sync();
			future.channel().closeFuture().sync();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	private SocketAddress getBindAddress(){

		InetSocketAddress socketAddress= new InetSocketAddress("192.168.50.128",38809);
		return  socketAddress;
	}

}
