package com.hui.zhang.spider.register.zk;

import com.hui.zhang.spider.common.model.Address;
import com.hui.zhang.spider.common.model.ServiceMeta;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by zhanghui on 2018-12-21.
 */
public class RpcPathTool {
    /**
     * 获得服务节点目录
     * @param service
     * @param version
     * @return
     */
    public static   String getServiceNodeDirPath(String service,String version) {
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
    public static  String getServiceNodePath(String service,String version, String address) {
        if (StringUtils.isNotEmpty(version)){
            return String.format("/service/%s-%s/providers/%s", service,version , address);
        }else {
            return String.format("/service/%s/providers/%s", service , address);
        }
    }

   /* *//**
     * 服务信息节点目录
     * @return
     *//*
    public static String getServiceInfoDirPath() {
        return String.format("/info");
    }
    *//**
     * 获得服务信息节点路径
     * @param service
     * @param version
     * @return
     *//*
    public static   String getServiceInfoPath(String service,String version) {
        if (StringUtils.isNotEmpty(version)){
            return String.format("/info/%s-%s",service,version);
        }else {
            return String.format("/info/%s",service);
        }
    }
*/


    /**
     * 获得下线节点目录
     * @param service
     * @param version
     * @return
     */
    public static String getServiceOffDirPath(String service,String version) {
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
    public static String getServiceOffPath(String service,String version, String address) {
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
    public  static String getServiceProxyNodePath(String service,String proxyEnvId) {
        return String.format("/proxy-node/%s/%s", proxyEnvId,service);
    }

    public  static String getProxyListNodePath(String proxyEnvGroup,String proxyEnvId) {
        return String.format("/proxy-list/%s/%s", proxyEnvGroup,proxyEnvId);
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
