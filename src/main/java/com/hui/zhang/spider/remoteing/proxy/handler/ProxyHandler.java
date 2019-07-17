package com.hui.zhang.spider.remoteing.proxy.handler;

import com.edb01.common.util.HessianEncoder;
import com.hui.zhang.spider.common.http.HttpClientUtil;
import com.hui.zhang.spider.register.zk.ZkLocalSync;
import com.hui.zhang.spider.remoteing.netty.data.RpcRequest;
import com.hui.zhang.spider.remoteing.netty.data.RpcResponse;
import org.apache.commons.collections.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by zhanghui on 2018/2/8.
 */
public class ProxyHandler {
    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);

    public RpcResponse send(RpcRequest request) throws Exception{
        long start=System.currentTimeMillis();

        //String className= request.getClassName();
        //String proxyPath= RpcPathTool.getServiceProxyNodePath(className,request.getProxyEnvId());
        String url= ZkLocalSync.getProxyNodeUrl(request.getClassName(),request.getProxyEnvId());  //HessianEncoder.decodeByte( SpiderRpc.instance().zkClient().getData().forPath(proxyPath),String.class);
        url=url+"/rpc/proxy-service";

        Map<String,String> map=new HashedMap();
        map.put("data", HessianEncoder.encoderStr(request));
        RpcResponse rpcResponse= HttpClientUtil.rpcPost(url,map);
        long end=System.currentTimeMillis();
        //logger.info("proxy service:{} . use time:{}ms",request.getClassName()+":"+request.getVersion(),(end-start));
        return  rpcResponse;
    }

    /*public  String getProxyNodePath(String service) {
        return String.format("/proxy/%s", service);
    }*/
}
