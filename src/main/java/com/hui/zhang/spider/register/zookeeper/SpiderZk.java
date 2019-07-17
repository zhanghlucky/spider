package com.hui.zhang.spider.register.zookeeper;

import com.edb01.common.util.MD5Util;
import com.edb01.common.util.etc.AppConfigUtil;
import com.hui.zhang.spider.common.model.Address;
import com.hui.zhang.spider.common.model.ServiceMeta;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by zhanghui on 2017/11/21.
 */
@Deprecated
public class SpiderZk {
    private static final Logger logger = LoggerFactory.getLogger(SpiderZk.class);
    //private   CountDownLatch connectedSemaphore = new CountDownLatch(1);

    public static final SpiderZk INSTANCE=new SpiderZk();
    private final int sessionTimeoutMs = 3 * 1000;
    private final int connectionTimeoutMs = 30 * 1000;
    private static final Map<String,CuratorFramework> CLIENT_MAP=new HashMap<>();
    private static CuratorFramework client;
    //替换工具类
    static{
        try {
            String zkAddress = AppConfigUtil.getCfgEnvironmentPO().getZookeeperAddress().replace("zookeeper://", "").replace("?backup=", ",");
            client=SpiderZk.INSTANCE.connect(zkAddress);
            logger.info("connect zookeeper success!");
        } catch (Exception e) {
            logger.error("connect zookeeper  fail! error:{}", e.getMessage());
        }
    }
    public SpiderZk(){

    }

    /**
     * 连接zookeeper
     * @param zkAddress
     */
    public CuratorFramework connect(String zkAddress) {

        String key= MD5Util.MD5(zkAddress);
        CuratorFramework client=CLIENT_MAP.get(key);
        if (null!=client){
            SpiderZk.client=client;
            return client;
        }

        client = CuratorFrameworkFactory.newClient(
                zkAddress, sessionTimeoutMs, connectionTimeoutMs, new ExponentialBackoffRetry(1000, 20));

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

        CLIENT_MAP.put(key,client);
        SpiderZk.client=client;
        return client;
    }

    public CuratorFramework client() {
        return client;
    }

    public CuratorFramework client(String zkAddress) {
        client=connect(zkAddress);
        return client;
    }

    /**
     *
     * 创建节点
     * @param path 路径
     * @param ephemeral 是否临时节点
     */
    public void createNode(String path, boolean ephemeral) throws Exception {
        int i = path.lastIndexOf('/');
        if (i > 0) {//递归创建目录节点
            createNode(path.substring(0, i), false);
        }
        if (ephemeral) {
            Stat stat = client.checkExists().forPath(path);
            if(stat == null){
                client.create().withMode(CreateMode.EPHEMERAL).forPath(path);
            }
        } else {
            Stat stat = client.checkExists().forPath(path);
            if (stat == null) {
                client.create().withMode(CreateMode.PERSISTENT).forPath(path);
            }
        }
    }


    /**
     * 获得服务节点目录
     * @param service
     * @param version
     * @return
     */
    public   String getServiceNodeDirPath(String service,String version) {
        if (StringUtils.isNotEmpty(version)){
            return String.format("/service/%s-%s/providers", service,version);
        }else {
            return String.format("/service/%s/providers", service );
        }
    }

    /**
     * 获得服务节点路径
     * @param service
     * @param version
     * @param address
     * @return
     */
    public  String getServiceNodePath(String service,String version, String address) {
        if (StringUtils.isNotEmpty(version)){
            return String.format("/service/%s-%s/providers/%s", service,version , address);
        }else {
            return String.format("/service/%s/providers/%s", service , address);
        }
    }

    /**
     * 服务信息节点目录
     * @return
     */
    public  String getServiceInfoDirPath() {
        return String.format("/info");
    }
    /**
     * 获得服务信息节点路径
     * @param service
     * @param version
     * @return
     */
    public  String getServiceInfoPath(String service,String version) {
        if (StringUtils.isNotEmpty(version)){
            return String.format("/info/%s-%s",service,version);
        }else {
            return String.format("/info/%s",service);
        }
    }

    /**
     * 获得下线节点目录
     * @param service
     * @param version
     * @return
     */
    public String getServiceOffDirPath(String service,String version) {
        if (StringUtils.isNotEmpty(version)){
            return String.format("/offline/%s-%s/providers", service,version);
        }else {
            return String.format("/offline/%s/providers", service);
        }
    }

    /**
     * 获得下线节点路径
     * @param service
     * @param version
     * @param address
     * @return
     */
    public String getServiceOffPath(String service,String version, String address) {
        if (StringUtils.isNotEmpty(version)){
            return String.format("/offline/%s-%s/providers/%s", service,version , address);
        }else {
            return String.format("/offline/%s/providers/%s", service, address);
        }
    }

    /**
     * 获得服务代理节点路径
     * @param service
     * @return
     */
    public  String getServiceProxyNodePath(String service) {
        return String.format("/proxy/%s", service);
    }

    /**
     * 抽取提供者
     * @param path
     * @return
     */
    public static Address parseProvider(String path) {
        String address = null;
        if(StringUtils.isNotEmpty(path)){
            String[] directorys = path.split("/");
            if (directorys.length == 5) {
                address = directorys[4];
            }
            if (StringUtils.isNotBlank(address)) {
                String host = address.split(":")[0];
                String port = address.split(":")[1];
                return Address.of(host, Integer.valueOf(port));
            } else {
                return Address.EMPTY;
            }
        }
        return null;
    }

    /**
     * 抽取服务
     * @param path
     * @return
     */
    public static ServiceMeta parseService(String path) {
        ServiceMeta meta;
        String[] directorys = path.split("/");
        String metaDire = directorys[2];
        String[] service_version = metaDire.split("-");
        if (service_version.length > 1) {
            meta = new ServiceMeta(service_version[0], service_version[1]);
        } else {
            meta = new ServiceMeta(service_version[0], StringUtils.EMPTY);
        }
        return meta;
    }
}
