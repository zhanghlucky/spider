package com.hui.zhang.spider.remoteing.netty.server.thread;

import com.hui.zhang.spider.common.config.SpiderConfig;
import com.hui.zhang.spider.remoteing.netty.coder.RpcDecoder;
import com.hui.zhang.spider.remoteing.netty.coder.RpcEncoder;
import com.hui.zhang.spider.remoteing.netty.data.RpcRequest;
import com.hui.zhang.spider.remoteing.netty.data.RpcResponse;
import com.hui.zhang.spider.remoteing.netty.server.handler.RpcServerHandler;
import com.hui.zhang.spider.remoteing.netty.server.handler.ServerIdleStateTrigger;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * netty服务线程
 * @author zhanghui
 *
 */
public class NettyServerThread extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(NettyServerThread.class);

    private Map<String, Channel> channels;
    private Channel channel;
    private String host;
    private int port;

    public NettyServerThread(){
        String[] array = SpiderConfig.INSTANCE.getServerAddress().split(":");
        this.host = array[0];
        this.port = Integer.parseInt(array[1]);
    }
    public NettyServerThread(boolean dubboFlag){
        if (dubboFlag){
            String[] array = SpiderConfig.INSTANCE.getServerAddressForDubbo().split(":");
            this.host = array[0];
            this.port = Integer.parseInt(array[1]);
        }else{
            String[] array = SpiderConfig.INSTANCE.getServerAddress().split(":");
            this.host = array[0];
            this.port = Integer.parseInt(array[1]);
        }
    }
	@Override
	public void run() {
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
                            .addLast("handler",new IdleStateHandler(15, 0, 0, TimeUnit.SECONDS))
                            .addLast(new ServerIdleStateTrigger())
                            .addLast(new RpcDecoder(RpcRequest.class)) // 将 RPC 请求进行解码（为了处理请求） ChannelInboundHandlerAdapter  1
                            .addLast(new RpcEncoder(RpcResponse.class)) // 将 RPC 响应进行编码（为了返回响应）ChannelOutboundHandlerAdapter 2
                            .addLast(new RpcServerHandler()); // 处理 RPC 请求 SimpleChannelInboundHandler 3
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.bind(getBindAddress()).sync();
            channel=future.channel();
            channel.closeFuture().sync();

        } catch (Exception e) {
			//e.printStackTrace();
			LOGGER.error("[spider] server node error:{}",e);
		} finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
	}

	private SocketAddress getBindAddress(){
        InetSocketAddress socketAddress= new InetSocketAddress(host,port);
        LOGGER.info("[spider] netty bind. ip:{},port:{}",host,port);
        return  socketAddress;
    }
	
}