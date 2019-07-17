package com.hui.zhang.spider.register.zookeeper;

import com.alibaba.fastjson.JSON;
import com.hui.zhang.spider.common.exceptions.NoServerNodeException;
import com.hui.zhang.spider.common.serialize.HessianSerialize;
import com.hui.zhang.spider.register.data.RpcProvider;
import io.netty.util.internal.ThreadLocalRandom;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
/**
 * zookeeper 发现中心
 * @author zhanghui
 *
 */
@Deprecated
public class ZkDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(ZkDiscovery.class);
    public static final ZkDiscovery INSTANCE=new ZkDiscovery();

    /**
     * 根据地址发现服务
     * @param interfaceName
     * @param version
     * @param address
     * @return
     * @throws NoServerNodeException
     */
    public RpcProvider discover(String interfaceName,String version,String address) throws NoServerNodeException {
        RpcProvider rpcProvider=null;
        String dirPath= SpiderZk.INSTANCE.getServiceNodeDirPath(interfaceName,version);
        List<String> nodeList=getServerNodeList(interfaceName,version,address);

        List<RpcProvider> rpcProviderList= getRpcProviderList(dirPath, nodeList);
        int size=rpcProviderList.size();
        if(size>0){
            if(size==1){
                rpcProvider=rpcProviderList.get(0);
                //logger.debug("获得唯一服务节点: {}", rpcProvider.getAddress());
            }else{
                rpcProvider=rpcProviderList.get(ThreadLocalRandom.current().nextInt(size));
                //logger.debug("随机获得服务节点: {}", rpcProvider.getAddress());
            }
        }else{
            logger.error("no server node support service:{},address:{}",interfaceName,address);
            throw new NoServerNodeException("no server node support");
        }
        return rpcProvider;
    }

    /**
     * 发现服务
     * @return
     * @throws Exception
     */
    public RpcProvider discover(String interfaceName,String version) throws NoServerNodeException {
        RpcProvider rpcProvider=null;
        String dirPath= SpiderZk.INSTANCE.getServiceNodeDirPath(interfaceName,version);
        List<String> nodeList=getServerNodeList(interfaceName,version);
        if (null!=nodeList){
            List<RpcProvider> rpcProviderList= getRpcProviderList(dirPath, nodeList);
            int size=rpcProviderList.size();
            if(size>0){
                if(size==1){
                    rpcProvider=rpcProviderList.get(0);
                    //logger.info("获得唯一服务节点: {}", rpcProvider.getAddress());
                }else{
                    rpcProvider=rpcProviderList.get(ThreadLocalRandom.current().nextInt(size));
                    //logger.info("随机获得服务节点: {}", rpcProvider.getAddress());
                }
            }else{
                logger.error("no server node support {}",interfaceName);
                //throw new NoServerNodeException("no server node suppport");
            }
        }else{
            logger.error("no server node support {}",interfaceName);
        }

        return rpcProvider;
    }

    public RpcProvider discoverFromEnvGroup(String interfaceName,String version,String envGroup) throws NoServerNodeException {
        RpcProvider rpcProvider=null;
        String dirPath= SpiderZk.INSTANCE.getServiceNodeDirPath(interfaceName,version);
        List<String> nodeList=getServerNodeList(interfaceName,version);
        if (null!=nodeList){
            //List<RpcProvider> rpcProviderList= getRpcProviderList(dirPath, nodeList);
            List<RpcProvider> proxyRpcProviderList= this.getProxyRpcProviderList(envGroup);

            int size=proxyRpcProviderList.size();
            if(size>0){
                if(size==1){
                    rpcProvider=proxyRpcProviderList.get(0);
                    //logger.info("获得唯一服务节点: {}", rpcProvider.getAddress());
                }else{
                    rpcProvider=proxyRpcProviderList.get(ThreadLocalRandom.current().nextInt(size));
                    //logger.info("随机获得服务节点: {}", rpcProvider.getAddress());
                }
            }else{
                logger.error("no proxy server node support {}",interfaceName);
                //throw new NoServerNodeException("no server node suppport");
            }
        }else{
            logger.error("no proxy server node support {}",interfaceName);
        }

        return rpcProvider;
    }


    private List<String> getServerNodeList(String service,String version){
        try {
            String dirPath= SpiderZk.INSTANCE.getServiceNodeDirPath(service,version);
            List<String> nodeList= SpiderZk.INSTANCE.client().getChildren().forPath(dirPath);

            String offDirPath= SpiderZk.INSTANCE.getServiceOffDirPath(service,version);
            List<String> offNodeList=new ArrayList<>();
            Stat stat = SpiderZk.INSTANCE.client().checkExists().forPath(offDirPath);
            if(stat != null){
                offNodeList= SpiderZk.INSTANCE.client().getChildren().forPath(offDirPath);
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
            logger.error("find node error!{}",e.getMessage());
        }
        return null;
    }


    private List<String> getServerNodeList(String service,String version,String address){
        try {
            if (StringUtils.isNotEmpty(address)){
                String addrArray[]=address.split(",");
                String dirPath= SpiderZk.INSTANCE.getServiceNodeDirPath(service,version);
//				List<String> addrList=SpiderZk.INSTANCE.getZkClient().getChildren(dirPath);
                List<String> addrList= SpiderZk.INSTANCE.client().getChildren().forPath(dirPath);
                List<String> nodeList=new ArrayList<>();
                for (String addr:addrList) {
                    for (String paddr:addrArray) {
                        if (addr.equals(paddr)){
                            nodeList.add(addr);
                            break;
                        }
                    }
                }
                String offDirPath= SpiderZk.INSTANCE.getServiceOffDirPath(service,version);
                List<String> offNodeList=new ArrayList<>();
				/*if(SpiderZk.INSTANCE.getZkClient().exists(offDirPath)){
					offNodeList=SpiderZk.INSTANCE.getZkClient().getChildren(offDirPath);
				}*/

                Stat stat = SpiderZk.INSTANCE.client().checkExists().forPath(offDirPath);
                if (stat != null) {
                    offNodeList= SpiderZk.INSTANCE.client().getChildren().forPath(offDirPath);
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
            }else{
                logger.error("point ip is null：{}",address);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<RpcProvider> getProxyRpcProviderList(String envGroup){
        String proxysPath="/proxys/"+envGroup;
        try {
            byte[] bytes = SpiderZk.INSTANCE.client().getData().forPath(proxysPath);
            List<RpcProvider> rpcProviderList = (List) HessianSerialize.deserialize(bytes);
            return rpcProviderList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<RpcProvider>  getRpcProviderList(String dirPath,List<String> nodeList){
        try {
            List<RpcProvider> dlist = new ArrayList<RpcProvider>();
            for (String node : nodeList) {
                //			RpcProvider rpcProvider=SpiderZk.INSTANCE.getZkClient().readData(dirPath+ "/" + node);
                byte[] bytes = SpiderZk.INSTANCE.client().getData().forPath(dirPath + "/" + node);
                RpcProvider rpcProvider = (RpcProvider) HessianSerialize.deserialize(bytes);
                dlist.add(rpcProvider);
            }
            logger.debug("server node qty is:{}", dlist.size());
            return dlist;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}