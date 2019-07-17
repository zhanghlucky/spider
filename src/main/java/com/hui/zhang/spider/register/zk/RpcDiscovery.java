package com.hui.zhang.spider.register.zk;

import com.edb01.common.util.MD5Util;
import com.hui.zhang.spider.common.exceptions.NoServerNodeException;
import com.hui.zhang.spider.common.serialize.HessianSerialize;
import com.hui.zhang.spider.register.data.ProviderNode;
import com.hui.zhang.spider.register.data.RpcProvider;
import io.netty.util.internal.ThreadLocalRandom;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhanghui on 2018-12-21.
 */
public class RpcDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(RpcDiscovery.class);

    //private static volatile RpcDiscovery instance;
    //private volatile CuratorFramework zkClient;
    private String zkAddress;
    private static final Map<String, RpcDiscovery> DISCOVERY_MAP = new ConcurrentHashMap<>();


    public RpcDiscovery(){

    }

    public RpcDiscovery(String zkAddress){
       this.zkAddress=zkAddress;
    }

    public synchronized static RpcDiscovery instance(String zkAddress) {
        String key= MD5Util.MD5(zkAddress);
        RpcDiscovery rpcDiscovery=DISCOVERY_MAP.get(key);
        if (null==rpcDiscovery){
            rpcDiscovery=new RpcDiscovery(zkAddress);
            DISCOVERY_MAP.put(key,rpcDiscovery);
        }
        return rpcDiscovery;
    }

    /**
     * 发现服务
     * @param service
     * @param version
     * @param address
     * @return
     * @throws NoServerNodeException
     */
    public ProviderNode discover(String service, String version, String address) throws NoServerNodeException {
        ProviderNode providerNode=null;
        List<ProviderNode> rpcProviderList= this.getRpcProviderList(service,version,address);
        int size=rpcProviderList.size();
        if(size>0){
            if(size==1){
                providerNode=rpcProviderList.get(0);
            }else{
                providerNode=rpcProviderList.get(ThreadLocalRandom.current().nextInt(size));
            }
        }else{
            logger.error("no server node support service:{},version:{},address:{}",service,version,address);
            //throw new NoServerNodeException("no server node support");
        }
        return providerNode;
    }

    /**
     * 发现服务
     * @param service
     * @param version
     * @return
     * @throws NoServerNodeException
     */
    public ProviderNode discover(String service,String version) throws NoServerNodeException {
        return discover(service,version,null);
    }

    /**
     * 发现代理服务
     * @param proxyEnvGroup
     * @return
     * @throws NoServerNodeException
     */
    public ProviderNode discoverProxy(String proxyEnvGroup,String proxyEnvId,String serviceName) throws NoServerNodeException {
        ProviderNode rpcProvider=null;
        List<ProviderNode> proxyRpcProviderList= this.getProxyRpcProviderList(proxyEnvGroup,proxyEnvId);
        for (ProviderNode node: proxyRpcProviderList) {
            if (node.getInterfaceName().equals(serviceName)){
                rpcProvider=node;//随机取第一个
                break;
            }
        }
        if (null!=rpcProvider){
            return rpcProvider;
        }
        logger.error("no proxy server node support.proxyEnvGroup:{} ,proxyEnvId:{},serviceName:{}",proxyEnvGroup,proxyEnvId,serviceName);
        return null;
    }

    /**
     * 获得服务节点集合
     * @param service
     * @param version
     * @param address
     * @return
     */
    public List<String> getServerNodeList(String service,String version,String address){
        List<String> ls=new ArrayList<>();
        List<ProviderNode> rpcProviderList= this.getRpcProviderList(service,version,address);
        for (ProviderNode rpc:rpcProviderList) {
            ls.add(rpc.getAddress());
        }
        return  ls;
    }

/*    public List<String> getServerNodeList(String service,String version,String address){

      try {
            String dirPath= RpcPathTool.getServiceNodeDirPath(service,version);
            List<String> addrList=null;
            Stat stat = RpcZk.instance().connect(zkAddress).checkExists().forPath(dirPath);
            if(stat != null){
                addrList= RpcZk.instance().connect(zkAddress).getChildren().forPath(dirPath);
            }else{
                logger.error("can not find server node service:{},version:{},address:{}",service,version,address);
                return  null;
            }

            List<String> nodeList=new ArrayList<>();
            if (StringUtils.isNotEmpty(address)){
                String addrArray[]=address.split(",");
                for (String addr:addrList) {
                    for (String paddr:addrArray) {
                        if (addr.equals(paddr)){
                            nodeList.add(addr);
                            break;
                        }
                    }
                }
            }else{
                for (String addr:addrList) {
                    nodeList.add(addr);
                }
            }
            String offDirPath= RpcPathTool.getServiceOffDirPath(service,version);
            List<String> offNodeList=new ArrayList<>();
            Stat offstat = RpcZk.instance().connect(zkAddress).checkExists().forPath(offDirPath);
            if(offstat != null){
                offNodeList= RpcZk.instance().connect(zkAddress).getChildren().forPath(offDirPath);
            }
            List<String> list=new ArrayList<>();
            for (String node:nodeList) {
                boolean flag=true;
                for (String offNode: offNodeList) {
                    if (node.equals(offNode)){
                        flag=false;
                        break;
                    }
                }
                if (flag){
                    list.add(node);
                }
            }
            return list;
        } catch (Exception e) {
            logger.error("find server node error:{},service:{},version:{},address:{}",e.getMessage(),service,version,address);
        }
        return null;

    }*/

    /**
     * 获取rpc提供者对象列表
     * @param service
     * @param version
     * @param address
     * @return
     */
    private List<ProviderNode>  getRpcProviderList(String service, String version, String address){
        List<ProviderNode> ls= ZkLocalSync.getProviderList(service,version,address,zkAddress);
        return ls;

        /*String dirPath= RpcPathTool.getServiceNodeDirPath(service,version);
        List<String> nodeList=getServerNodeList(service,version,address);
        try {
            List<RpcProvider> rpcProviderList = new ArrayList<RpcProvider>();
            for (String node : nodeList) {
                String providerPath=dirPath + "/" + node;
                Stat stat = RpcZk.instance().connect(zkAddress).checkExists().forPath(providerPath);
                if(stat != null){
                    byte[] bytes = RpcZk.instance().connect(zkAddress).getData().forPath(dirPath + "/" + node);
                    RpcProvider rpcProvider = (RpcProvider) HessianSerialize.deserialize(bytes);
                    rpcProviderList.add(rpcProvider);
                }else{
                    logger.error("no provider node find providerPath:{}",providerPath);
                }
            }
            return rpcProviderList;
        } catch (Exception e) {
            logger.error("find provider node error:{}",e.getMessage());
        }
        return null;*/
    }

    /**
     * 获得rpc代理提供者对象列表
     * @param proxyEnvGroup
     * @return
     */
    private List<ProviderNode> getProxyRpcProviderList(String proxyEnvGroup,String proxyEnvId){
        List<ProviderNode> list=ZkLocalSync.getProxyRpcProviderList(proxyEnvGroup,proxyEnvId,zkAddress);
        return list;
        //String proxysPath="/proxys/"+envGroup;
        /*String proxysPath=RpcPathTool.getProxyListNodePath(proxyEnvGroup,proxyEnvId);
        try {
            Stat stat = RpcZk.instance().connect(zkAddress).checkExists().forPath(proxysPath);
            if(stat != null){
                byte[] bytes = RpcZk.instance().connect(zkAddress).getData().forPath(proxysPath);
                List<RpcProvider> rpcProviderList = (List) HessianSerialize.deserialize(bytes);
                return rpcProviderList;
            }else{
                logger.error("no proxy server find proxysPath:{}",proxysPath);
            }
        } catch (Exception e) {
            logger.error("find proxy node error:{}",e.getMessage());
        }*/

        //return new ArrayList<>();
    }



}
