package com.hui.zhang.spider.remoteing.netty.client.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <b>类名称：</b>ConnectionWatcher <br/>
 * <b>类描述：</b><br/>
 * <b>创建人：</b>zuti(zuti@centaur.com)<br/>
 * <b>修改人：</b><br/>
 * <b>修改时间：</b>2018/3/1 14:49<br/>
 * <b>修改备注：</b><br/>
 */
@ChannelHandler.Sharable
public abstract class ConnectionWatcher extends ChannelInboundHandlerAdapter implements /*TimerTask,*/ChannelHandlerHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionWatcher.class);

    private final Bootstrap bootstrap;
    private final Timer timer;
    private final int port;
    
    private final String host;

    private volatile boolean reconnect = false; //关闭自动重连
    private int attempts;
    private static final int DEFAULT_ATTEMPT_TIMES = 15; //重试15次重新连接
    
    
    public ConnectionWatcher(Bootstrap bootstrap, Timer timer, int port, String host, boolean reconnect) {
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.port = port;
        this.host = host;
        this.reconnect = reconnect;
    }
    
    /**
     * channel链路每次active的时候，将其连接的次数重新☞ 0
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("[spider] channel active, attempts is 0, {}", ctx.channel());
        attempts = 0;
        ctx.fireChannelActive();
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        /*if(reconnect){
            if (attempts < DEFAULT_ATTEMPT_TIMES) {
                attempts++;
                //重连的间隔时间会越来越长
                int interval = 2 << attempts;
                LOGGER.info("channel close, will reconect {}, attempts:{}, interval:{}", ctx.channel(), attempts, interval);
                timer.newTimeout(this, interval, TimeUnit.MILLISECONDS);
            }
        }*/
        ctx.fireChannelInactive();
    }
    

   /* @Override
    public void run(Timeout timeout) throws Exception {
        
        ChannelFuture future;
        synchronized (bootstrap) {
            bootstrap.handler(new ChannelInitializer<Channel>() {

                @Override
                protected void initChannel(Channel ch) throws Exception {
                    
                    ch.pipeline().addLast(handlers());
                }
            });
            future = bootstrap.connect(host,port);
        }
        //future对象
        future.addListener((ChannelFutureListener) f -> {
			boolean succeed = f.isSuccess();

			if (!succeed) {
				LOGGER.info("channel reconnect fail, {}", f.channel());
				f.channel().pipeline().fireChannelInactive();
			}else{
				LOGGER.info("channel reconnect success, {}", f.channel());
				RpcMonitor.addChannel(Address.of(host, port), f.channel());
			}
		});
        
    }*/

}
