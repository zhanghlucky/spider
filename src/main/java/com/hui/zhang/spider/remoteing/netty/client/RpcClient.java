package com.hui.zhang.spider.remoteing.netty.client;

import com.edb01.common.util.JsonEncoder;
import com.edb01.common.util.PropertyUtil;
import com.hui.zhang.spider.common.model.Address;
import com.hui.zhang.spider.future.InvokeFuture;
import com.hui.zhang.spider.register.RpcMonitor;
import com.hui.zhang.spider.remoteing.netty.data.RpcRequest;
import com.hui.zhang.spider.remoteing.netty.data.RpcResponse;
import com.esotericsoftware.reflectasm.MethodAccess;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * RPC服务客户端
 * @author zhanghui
 *
 */
public class RpcClient{
	private static final Logger LOGGER = LoggerFactory.getLogger(RpcClient.class);

	//private static final long DEFAULT_TIMEOUT = 20*10000; //毫秒

    private String host;
    private int port;
    //private long timeout = DEFAULT_TIMEOUT;
    private Address address;

    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.address = Address.of(host, port);
    }

    public RpcClient(String host, int port, long timeout) {
        this(host, port);
        //this.timeout = timeout;
    }

    public RpcResponse send(RpcRequest request) throws Throwable {
        //LOGGER.info("[spider] send request.interface:{},method:{}",request.getClassName(),request.getMethodName());
    	//http代理请求
        /*if(null!=PropertyUtil.getProperty("proxy")&&Boolean.valueOf(PropertyUtil.getProperty("proxy"))){
            RpcResponse rpcResponse= proxySend(request);
            return  rpcResponse;
        }else{
            //获取连接
            Channel channel = RpcMonitor.getChannel(address);
            InvokeFuture invokeFuture = write(request, channel);
            return (RpcResponse) invokeFuture.get(request.getTimeout(), TimeUnit.MILLISECONDS);
        }*/
        Channel channel = RpcMonitor.getChannel(address);
        InvokeFuture invokeFuture = write(request, channel);

        return (RpcResponse) invokeFuture.get(request.getTimeout(), TimeUnit.MILLISECONDS);
    }

    /**
     * 发送消息
     * @param request
     * @param channel
     */
    private InvokeFuture write(RpcRequest request, Channel channel) {
        InvokeFuture invokeFuture = new InvokeFuture();
        channel.writeAndFlush(request);
        InvokeFuture.futures.putIfAbsent(request.getRequestId(), invokeFuture);
        return invokeFuture;
    }


/*    private RpcResponse proxySend(RpcRequest request) throws  Exception{
        Class  cls=Class.forName("com.hui.zhang.spider.proxy.handler.ProxyHandler");
        Object obj = cls.newInstance();
        MethodAccess access = MethodAccess.get(obj.getClass());
        Object[] params={request};
        RpcResponse rpcResponse=(RpcResponse)access.invoke(obj, "send", params);//代理出实例
        return  rpcResponse;
    }*/
}

