package com.hui.zhang.spider.register.zk;

import com.edb01.common.util.MD5Util;
import com.edb01.common.util.etc.AppConfigUtil;
import com.mongodb.MongoClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhanghui on 2018-12-21.
 */

public class SpiderRpc {
    private static final Logger logger = LoggerFactory.getLogger(SpiderRpc.class);
    //private static volatile SpiderRpc instance;
    private String zkAddress;
    private static final Map<String, SpiderRpc> SPIDER_RPC_MAP = new ConcurrentHashMap<>();


    public SpiderRpc(){
    }

    public SpiderRpc(String zkAddress){
        this.zkAddress=zkAddress;
    }

    public synchronized static SpiderRpc instance(String zkAddress) {
        if (StringUtils.isEmpty(zkAddress)){
            zkAddress=AppConfigUtil.getCfgEnvironmentPO().getZookeeperAddress().replace("zookeeper://", "").replace("?backup=", ",");
        }
        String key= MD5Util.MD5(zkAddress);
        SpiderRpc spiderRpc=SPIDER_RPC_MAP.get(key);
        if (null==spiderRpc){
            spiderRpc= new SpiderRpc(zkAddress);
            SPIDER_RPC_MAP.put(key,spiderRpc);
        }
        return spiderRpc;
    }

    public synchronized static SpiderRpc instance() {
        String zkAddress = AppConfigUtil.getCfgEnvironmentPO().getZookeeperAddress().replace("zookeeper://", "").replace("?backup=", ",");
        return  instance(zkAddress);
    }

    /**
     * 获取zkClient
     * @return
     */
    public synchronized CuratorFramework zkClient(){
       return RpcZk.instance().connect(zkAddress);

    }

    /**
     * 获得发现类
     * @return
     */
    public synchronized RpcDiscovery rpcDiscovery(){

        return RpcDiscovery.instance(zkAddress);
    }

    /**
     * 获得注册类
     * @return
     */
    public synchronized RpcRegistry rpcRegistry(){
        return RpcRegistry.instance(zkAddress);
    }


}
