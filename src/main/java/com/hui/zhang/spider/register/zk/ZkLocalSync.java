package com.hui.zhang.spider.register.zk;

import com.edb01.common.util.HessianEncoder;
import com.edb01.common.util.JsonEncoder;
import com.edb01.common.util.MD5Util;
import com.edb01.common.util.etc.AppConfigUtil;
import com.hui.zhang.spider.common.serialize.HessianSerialize;
import com.hui.zhang.spider.register.data.ProviderNode;
import com.hui.zhang.spider.register.data.RpcProvider;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Provider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by zhanghui on 2019-05-30.
 * 本地同步类
 */
public class ZkLocalSync {
    private static final Logger logger = LoggerFactory.getLogger(ZkLocalSync.class);
    //服务提供者
    private static volatile  Map<String,Map<String,List<ProviderNode>>> PROVIDER_MAP=new HashedMap();


    private static volatile String baseZkAddress = AppConfigUtil.getCfgEnvironmentPO().getZookeeperAddress().
            replace("zookeeper://", "").replace("?backup=", ",");

    /**
     * 获得服务列表
     * @param service
     * @param version
     * @param address
     * @return
     */
    public static List<ProviderNode> getProviderList(String service,String version,String address){
        return getProviderList(service,version,address,null);
    }

    /**
     * 获得服务列表
     * @param service
     * @param version
     * @param address
     * @param zkAddress
     * @return
     */
    public static List<ProviderNode> getProviderList(String service,String version,String address,String zkAddress){
        if (StringUtils.isEmpty(zkAddress)){
            zkAddress=baseZkAddress;
        }
        //RpcZk.instance().connect(zkAddress);//设置监听

        Map<String,List<ProviderNode>> rpcProviderMap=PROVIDER_MAP.get(MD5Util.MD5(zkAddress));
        if (null!=rpcProviderMap){
            List<ProviderNode> checkedList=new ArrayList<>();

            String serviceVersion= service;
            if (StringUtils.isNotEmpty(version)){
                serviceVersion= service+"-"+version;
            }

            List<ProviderNode> providerList= rpcProviderMap.get(serviceVersion);
            if(StringUtils.isNotEmpty(address)){
                for (ProviderNode providerNode:providerList) {
                    if (providerNode.getAddress().equals(address)){
                        //providerList=new ArrayList<>();
                        checkedList.add(providerNode);
                        return checkedList;
                    }
                }
            }
            return  providerList;
        }
        logger.warn("no provider support!method:{},service:{},version:{},address:{},zkAddress:{}","getProviderList",service,version,address,zkAddress);
        return new ArrayList<>();
    }

    /**
     * 获得所有的服务列表
     * @return
     */
    public static List<ProviderNode> getAllProviderList(){
       return  getAllProviderList(null);
    }

    /**
     * 获得所有的服务列表
     * @param zkAddress
     * @return
     */
    public static List<ProviderNode> getAllProviderList(String zkAddress){
        if (StringUtils.isEmpty(zkAddress)){
            zkAddress=baseZkAddress;
        }
        RpcZk.instance().connect(zkAddress);//设置监听

        Map<String,List<ProviderNode>> rpcProviderMap=PROVIDER_MAP.get(MD5Util.MD5(zkAddress));
        if (null!=rpcProviderMap){
            List<ProviderNode> list=new ArrayList<>();
            Iterator<Map.Entry<String,List<ProviderNode>>> entries = rpcProviderMap.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String,List<ProviderNode>> entry = entries.next();
                list.addAll(entry.getValue());
            }
            return list;
        }
        logger.warn("no provider support! method:{},zkAddress:{}","getAllProviderList",zkAddress);
        return null;
    }

    public static Map<String,Map<String,List<ProviderNode>>>  getProviderMap(){
        return PROVIDER_MAP;
    }

    /**
     * 更新服务列表
     * @param data
     * @param zkAddress
     */
    //nodePath 格式
    // /service/com.edb01.monitor.service.MonitorService-1.0.0/providers/192.168.3.195:37756
    // /service/com.edb01.erp.wms.service.api.WmStockWaveGoodsItemService-1.0.0/providers/192.168.3.194:33200
    public static synchronized void updateProvider(ChildData data,String zkAddress){
        try {
            String nodePath=data.getPath();
            String array[]=nodePath.split("/");

            if (array.length==5&&array[1].equals("service")){
                String serviceVersion=array[2];
                String address=array[4];
                String [] svArray=serviceVersion.split("-");

                String service=svArray[0];
                String version=null;
                if (svArray.length>1){
                    version=svArray[1];
                }

                ProviderNode providerNode=new ProviderNode();
                providerNode.setInterfaceName(service);
                providerNode.setVersion(version);
                providerNode.setAddress(address);

                Map<String,List<ProviderNode>> rpcProviderMap=PROVIDER_MAP.get(MD5Util.MD5(zkAddress));
                if(null==rpcProviderMap){
                    List<ProviderNode> providerList=new ArrayList<>();
                    providerList.add(providerNode);
                    rpcProviderMap=new HashedMap();
                    rpcProviderMap.put(serviceVersion,providerList);

                }else{
                    List<ProviderNode> providerList= rpcProviderMap.get(serviceVersion);
                    if (null==providerList){
                        providerList=new ArrayList<>();
                        providerList.add(providerNode);
                        rpcProviderMap.put(serviceVersion,providerList);
                    }else{
                        boolean existFlag=false;
                        for (ProviderNode mip:providerList) {
                            if (mip.getAddress().equals(address)){
                                existFlag=true;
                                break;
                            }
                        }
                        if (!existFlag){
                            providerList.add(providerNode);
                            rpcProviderMap.put(serviceVersion,providerList);
                        }
                    }
                }
                PROVIDER_MAP.put(MD5Util.MD5(zkAddress),rpcProviderMap);
            }
        }catch (Exception e){
            logger.error("updateProvider error:{}",e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 删除服务列表
     * @param data
     * @param zkAddress
     */

    public static synchronized void removeProvider(ChildData data,String zkAddress){
        if (StringUtils.isEmpty(zkAddress)){
            zkAddress=baseZkAddress;
        }
        try {
            String nodePath=data.getPath();
            String array[]=nodePath.split("/");

            if (array.length==5&&array[1].equals("service")){
                String serviceVersion=array[2];
                String address=array[4];
                String [] svArray=serviceVersion.split("-");
                String service=svArray[0];
                String version=svArray[1];

                ProviderNode providerNode=new ProviderNode();
                providerNode.setInterfaceName(service);
                providerNode.setVersion(version);
                providerNode.setAddress(address);

                Map<String,List<ProviderNode>> rpcProviderMap=PROVIDER_MAP.get(MD5Util.MD5(zkAddress));
                if(null!=rpcProviderMap){
                    List<ProviderNode> providerList= rpcProviderMap.get(serviceVersion);
                    if (null!=providerList){
                        boolean existFlag=false;
                        for (int i = 0; i <providerList.size() ; i++) {
                            if (providerList.get(i).getAddress().equals(address)){
                                providerList.remove(i);
                                break;
                            }
                        }
                    }
                    rpcProviderMap.put(serviceVersion,providerList);
                    PROVIDER_MAP.put(MD5Util.MD5(zkAddress),rpcProviderMap);
                }
            }
        }catch (Exception e){
            logger.error("removeProvider error:{}",e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获得代理服务URL
     * @param service
     * @param proxyEnvId
     * @return
     */
    public static String getProxyNodeUrl(String service,String proxyEnvId){
        return getProxyNodeUrl(service,proxyEnvId,null);
    }

    /**
     * 获得代理服务URL
     * @param service
     * @param proxyEnvId
     * @param zkAddress
     * @return
     */
    public static String getProxyNodeUrl(String service,String proxyEnvId,String zkAddress){
        if (StringUtils.isEmpty(zkAddress)){
            zkAddress=baseZkAddress;
        }

        Stat stat = null;
        try {
            String path=RpcPathTool.getServiceProxyNodePath(service,proxyEnvId);
            stat = RpcZk.instance().connect(zkAddress).checkExists().forPath(path);
            if(stat != null){
                byte[] bytes = RpcZk.instance().connect(zkAddress).getData().forPath(path);
                String url=HessianEncoder.decodeByte(bytes,String.class);
                return  url;
            }else{
                logger.error("no url path:{}",path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.error("no proxy node url support");

        return null;

    }

    /**
     * 更新代理服务节点
     * @param data
     * @param zkAddress
     */
    /*public static synchronized void updateProxyNodeUrl(ChildData data,String zkAddress){
        try {
            String nodePath=data.getPath();
            int size=nodePath.split("/").length;
            if (size==4){
                String key=data.getPath();
                //String url=HessianEncoder.decodeByte(data.getData(),String.class);
                if (StringUtils.isEmpty(zkAddress)){
                    zkAddress=baseZkAddress;
                }
                Map<String,String>  proxyNodeUrlMap=PROXY_NODE_URL_MAP.get(MD5Util.MD5(zkAddress));
                if (null==proxyNodeUrlMap){
                    proxyNodeUrlMap=new HashedMap();
                    proxyNodeUrlMap.put(key,url);//不管有没有，都更新url
                }else{
                    proxyNodeUrlMap.put(key,url);//不管有没有，都更新url
                }
                PROXY_NODE_URL_MAP.put(MD5Util.MD5(zkAddress),proxyNodeUrlMap);
            }
        }catch (Exception e){
            logger.error("updateProxyNodeUrl error:{}",e.getMessage());
            e.printStackTrace();
        }
    }*/

    /**
     * 删除代理服务节点
     * @param data
     * @param zkAddress
     */
    /*public static synchronized void removeProxyNodeUrl(ChildData data,String zkAddress){
        try {
            String nodePath=data.getPath();
            int size=nodePath.split("/").length;
            if (size==4){
                String key=data.getPath();
                //String url=HessianEncoder.decodeByte(data.getData(),String.class);
                if (StringUtils.isEmpty(zkAddress)){
                    zkAddress=baseZkAddress;
                }
                Map<String,String>  proxyNodeUrlMap=PROXY_NODE_URL_MAP.get(MD5Util.MD5(zkAddress));
                if (null==proxyNodeUrlMap){
                    //NOTHING TO DO
                }else{
                    proxyNodeUrlMap.remove(key);
                }
                PROXY_NODE_URL_MAP.put(MD5Util.MD5(zkAddress),proxyNodeUrlMap);
            }
        }catch (Exception e){
            logger.error("removeProxyNodeUrl error:{}",e.getMessage());
            e.printStackTrace();
        }
    }*/

    /**
     * 获得代理服务列表
     * @param proxyEnvGroup
     * @param proxyEnvId
     * @return
     */
    public static List<ProviderNode> getProxyRpcProviderList(String proxyEnvGroup,String proxyEnvId){
        return  getProxyRpcProviderList(proxyEnvGroup,proxyEnvId,null);
    }

    /**
     * 获得代理服务列表
     * @param proxyEnvGroup
     * @param proxyEnvId
     * @param zkAddress
     * @return
     */
    public static List<ProviderNode> getProxyRpcProviderList(String proxyEnvGroup,String proxyEnvId,String zkAddress){
        String proxyPath=RpcPathTool.getProxyListNodePath(proxyEnvGroup,proxyEnvId);
        if (StringUtils.isEmpty(zkAddress)){
            zkAddress=baseZkAddress;
        }

        Stat stat = null;
        try {
            //String path=RpcPathTool.getProxyListNodePath(service,proxyEnvId);
            stat = RpcZk.instance().connect(zkAddress).checkExists().forPath(proxyPath);
            if(stat != null){
                byte[] bytes = RpcZk.instance().connect(zkAddress).getData().forPath(proxyPath);
                List<ProviderNode> list=HessianEncoder.decodeByte(bytes,List.class);
                return  list;
            }else{
                logger.error("no list path:{}",proxyPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.error("no proxy node list support");

        return null;

        //RpcZk.instance().connect(zkAddress);//设置监听

        /*Map<String,List<ProviderNode>>  proxyProviderMap=PROXY_PROVIDER_MAP.get(MD5Util.MD5(zkAddress));
        if (null!=proxyProviderMap){
            List<ProviderNode> ls=proxyProviderMap.get(proxyPath);
            return  ls;
        }
        return null;*/
    }

    /**
     * 更新代理服务列表
     * @param data
     * @param zkAddress
     */
    /*public static synchronized void updateProxyProvider(ChildData data,String zkAddress){
        try {
            String nodePath=data.getPath();
            int size=nodePath.split("/").length;
            if (size==4){
                if (StringUtils.isEmpty(zkAddress)){
                    zkAddress=baseZkAddress;
                }
                Map<String,List<ProviderNode>>  proxyProviderMap=PROXY_PROVIDER_MAP.get(MD5Util.MD5(zkAddress));
                if (null==proxyProviderMap){
                    proxyProviderMap=new HashedMap();
                }
                String key=data.getPath();
                List<ProviderNode> providerList = (List) HessianSerialize.deserialize(data.getData());
                proxyProviderMap.put(key,providerList);

                PROXY_PROVIDER_MAP.put(MD5Util.MD5(zkAddress),proxyProviderMap);
            }
        }catch (Exception e){
            logger.error("updateProxyProvider error:{}",e.getMessage());
            e.printStackTrace();
        }
    }*/

    /**
     * 删除代理服务节点
     * @param
     * @param zkAddress
     */
    /*public static synchronized void removeProxyProvider(ChildData data,String zkAddress){
        try {
            String nodePath=data.getPath();
            int size=nodePath.split("/").length;
            if (size==4){
                String key=data.getPath();
                if (StringUtils.isEmpty(zkAddress)){
                    zkAddress=baseZkAddress;
                }
                Map<String,List<ProviderNode>>  proxyProviderMap=PROXY_PROVIDER_MAP.get(MD5Util.MD5(zkAddress));
                if (null!=proxyProviderMap){
                    proxyProviderMap.remove(key);
                }
                PROXY_PROVIDER_MAP.put(MD5Util.MD5(zkAddress),proxyProviderMap);


            }
        }catch (Exception e){
            logger.error("removeProxyProvider error:{}",e.getMessage());
            e.printStackTrace();
        }
    }*/

    public static  RpcProvider getRpcProvider(ProviderNode providerNode,String zkAddress){
        String providerPath=RpcPathTool.getServiceNodePath(providerNode.getInterfaceName(),providerNode.getVersion(),providerNode.getAddress());
        Stat stat = null;
        try {
            stat = RpcZk.instance().connect(zkAddress).checkExists().forPath(providerPath);
            if(stat != null){
                byte[] bytes = RpcZk.instance().connect(zkAddress).getData().forPath(providerPath);
                RpcProvider rpcProvider = (RpcProvider) HessianSerialize.deserialize(bytes);
                return  rpcProvider;
            }else{
                logger.error("no provider node find providerPath:{}",providerPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static  RpcProvider getRpcProvider(ProviderNode providerNode){
        String providerPath=RpcPathTool.getServiceNodePath(providerNode.getInterfaceName(),providerNode.getVersion(),providerNode.getAddress());
        Stat stat = null;
        try {
            stat = RpcZk.instance().connect(baseZkAddress).checkExists().forPath(providerPath);
            if(stat != null){
                byte[] bytes = RpcZk.instance().connect(baseZkAddress).getData().forPath(providerPath);
                RpcProvider rpcProvider = (RpcProvider) HessianSerialize.deserialize(bytes);
                return  rpcProvider;
            }else{
                logger.error("no provider node find providerPath:{}",providerPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



}
