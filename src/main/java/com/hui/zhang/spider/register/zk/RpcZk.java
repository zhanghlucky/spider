package com.hui.zhang.spider.register.zk;

import com.edb01.common.util.MD5Util;
import com.edb01.common.util.etc.AppConfigUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by zhanghui on 2018-12-21.
 */
public class RpcZk {
    private static final Logger logger = LoggerFactory.getLogger(RpcZk.class);
    //public static final RpcZk DEFAULT = new RpcZk();
    private static volatile RpcZk instance;
    private static final Map<String,CuratorFramework> CLIENT_MAP=new ConcurrentHashMap<>();
    private final int  SESSION_TIMEOUT= 3 * 1000;
    private final int  CONNECTION_TIMEOUT = 30 * 1000;


    public RpcZk() {

    }

    public static RpcZk instance() {
        if (instance == null) {
            synchronized (RpcZk.class) {
                if (instance == null) {
                    instance = new RpcZk();
                }
            }
        }
        return instance;
    }


    public CuratorFramework connect(){
        String zkAddress = AppConfigUtil.getCfgEnvironmentPO().getZookeeperAddress().replace("zookeeper://", "").replace("?backup=", ",");
        return connect(zkAddress);
    }

    public CuratorFramework connect(String zkAddress) {
        String key= MD5Util.MD5(zkAddress);
        CuratorFramework client=CLIENT_MAP.get(key);
        if (null!=client){
            return client;
        }

        client = CuratorFrameworkFactory.newClient(
                zkAddress, SESSION_TIMEOUT, CONNECTION_TIMEOUT, new ExponentialBackoffRetry(1000, 20));

        client.getConnectionStateListenable().addListener((clt, newState) -> {
            logger.info("zookeeper connection state changed to {}", newState);
            if (newState == ConnectionState.LOST) {
                //链接丢失
                logger.info("lost session with zookeeper");
            } else if (newState == ConnectionState.CONNECTED) {
                //连接新建
                logger.info("connected with zookeeper");
            } else if (newState == ConnectionState.RECONNECTED) {
                //重连成功
                logger.info("reconnected with zookeeper");
            }
        });
        client.start();

        try{
            listenerProvider(client,zkAddress);
            //listenerProxyNodeUrl(client,zkAddress);
            //listenerProxyProvider(client,zkAddress);
        }catch (Exception e){
            e.printStackTrace();
        }

        CLIENT_MAP.put(key,client);
        return client;
    }


    //监听服务提供者
    private  void listenerProvider(CuratorFramework client,String zkAddress) throws Exception{
        ExecutorService pool = Executors.newCachedThreadPool();
        //设置节点的cache
        TreeCache treeCache = new TreeCache(client, "/service");
        //设置监听器和处理过程
        treeCache.getListenable().addListener(new TreeCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
                ChildData data = event.getData();
                if(data !=null){
                    switch (event.getType()) {
                        case NODE_ADDED:
                            //System.out.println("NODE_ADDED : "+ data.getPath() );
                            ZkLocalSync.updateProvider(data,zkAddress);
                            break;
                        case NODE_REMOVED:
                            //System.out.println("NODE_REMOVED : "+ data.getPath() );
                            //System.out.println("CHILD_REMOVED : "+ data.getPath() +"  数据:"+ data.getData());
                            ZkLocalSync.removeProvider(data,zkAddress);
                            break;
                        case NODE_UPDATED:
                            //System.out.println("NODE_UPDATED : "+ data.getPath() );
                            ZkLocalSync.updateProvider(data,zkAddress);
                            break;
                        default:
                            break;
                    }
                }else{
                    //System.out.println( "data is null : "+ event.getType());
                }
            }
        },pool);


        //开始监听
        treeCache.start();
    }

    //监听代理服务URL
    /*private  void listenerProxyNodeUrl(CuratorFramework client,String zkAddress) throws Exception{
        ExecutorService pool = Executors.newCachedThreadPool();
        //设置节点的cache
        TreeCache treeCache = new TreeCache(client, "/proxy-node");
        //设置监听器和处理过程
        treeCache.getListenable().addListener(new TreeCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
                ChildData data = event.getData();
                if(data !=null){
                    switch (event.getType()) {
                        case NODE_ADDED:
                            //System.out.println("NODE_ADDED : "+ data.getPath() );
                            ZkLocalSync.updateProxyNodeUrl(data,zkAddress);
                            break;
                        case NODE_REMOVED:
                            //System.out.println("NODE_REMOVED : "+ data.getPath() );
                            //System.out.println("CHILD_REMOVED : "+ data.getPath() +"  数据:"+ data.getData());
                            ZkLocalSync.removeProxyNodeUrl(data,zkAddress);
                            break;
                        case NODE_UPDATED:
                            //System.out.println("NODE_UPDATED : "+ data.getPath() );
                            ZkLocalSync.updateProxyNodeUrl(data,zkAddress);
                            break;
                        default:
                            break;
                    }
                }else{
                    //System.out.println( "data is null : "+ event.getType());
                }
            }
        },pool);
        //开始监听
        treeCache.start();
    }*/

    //监听代理服务提供者
    /*private  void listenerProxyProvider(CuratorFramework client,String zkAddress) throws Exception{
        ExecutorService pool = Executors.newCachedThreadPool();
        //设置节点的cache
        TreeCache treeCache = new TreeCache(client, "/proxy-list");
        //设置监听器和处理过程
        treeCache.getListenable().addListener(new TreeCacheListener() {
            @Override
            public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
                ChildData data = event.getData();
                if(data !=null){
                    switch (event.getType()) {
                        case NODE_ADDED:
                            //System.out.println("NODE_ADDED : "+ data.getPath() );
                            ZkLocalSync.updateProxyProvider(data,zkAddress);
                            break;
                        case NODE_REMOVED:
                            //System.out.println("NODE_REMOVED : "+ data.getPath() );
                            //System.out.println("CHILD_REMOVED : "+ data.getPath() +"  数据:"+ data.getData());
                            ZkLocalSync.removeProxyProvider(data,zkAddress);
                            break;
                        case NODE_UPDATED:
                            //System.out.println("NODE_UPDATED : "+ data.getPath() );
                            ZkLocalSync.updateProxyProvider(data,zkAddress);
                            break;
                        default:
                            break;
                    }
                }else{
                    //System.out.println( "data is null : "+ event.getType());
                }
            }
        },pool);
        //开始监听
        treeCache.start();
    }*/

}
