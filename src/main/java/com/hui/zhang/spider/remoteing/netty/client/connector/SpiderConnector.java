package com.hui.zhang.spider.remoteing.netty.client.connector;

import com.hui.zhang.spider.exception.SpiderException;
import com.hui.zhang.spider.remoteing.netty.client.handler.ClientIdleStateTrigger;
import com.hui.zhang.spider.remoteing.netty.client.handler.ConnectionWatcher;
import com.hui.zhang.spider.remoteing.netty.client.handler.RpcClientHandler;
import com.hui.zhang.spider.remoteing.netty.coder.RpcDecoder;
import com.hui.zhang.spider.remoteing.netty.coder.RpcEncoder;
import com.hui.zhang.spider.remoteing.netty.data.RpcRequest;
import com.hui.zhang.spider.remoteing.netty.data.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class SpiderConnector extends AbstractConnector {

    private static final Logger logger = LoggerFactory.getLogger(SpiderConnector.class);

    private static final int WRITE_IDLE_TIME = 10;

    protected final HashedWheelTimer timer = new HashedWheelTimer();

    private Bootstrap boot = new Bootstrap();

    private static EventLoopGroup group =
            new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), new DefaultThreadFactory("spider-connector", Thread.MAX_PRIORITY));
    
    @Override
    public Channel connect(String host, int port) {
        
//        EventLoopGroup group = new NioEventLoopGroup();

        boot.group(group).channel(NioSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO));
            
        final ConnectionWatcher watchdog = new ConnectionWatcher(boot, timer, port,host, true) {
                @Override
                public ChannelHandler[] handlers() {
                    return new ChannelHandler[] {
                            this,
                            new IdleStateHandler(0, WRITE_IDLE_TIME, 0, TimeUnit.SECONDS),
                            new ClientIdleStateTrigger(),
                            new RpcEncoder(RpcRequest.class),
                            new RpcDecoder(RpcResponse.class),
                            new RpcClientHandler()
                    };
                }
            };
            
            ChannelFuture future;
            //进行连接
            try {
                synchronized (boot) {
                    boot.handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(watchdog.handlers());
                        }
                    });

                    future = boot.connect(host,port);
                }
                future.sync();
            } catch (Throwable e) {
                //e.printStackTrace();
                throw new SpiderException("connects to " + host + ":" + port + " fail e:"+e.getMessage()+"",e);
            }

        Channel channel = future.channel();
        return channel;
    }

}
