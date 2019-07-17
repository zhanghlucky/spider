/**
 * Copyright (C) 2017 Newland Group Holding Limited
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hui.zhang.spider.remoteing.proxy;
import com.hui.zhang.spider.common.annotation.SpiderApi;
import com.hui.zhang.spider.register.zk.SpiderRpc;
import com.hui.zhang.spider.remoteing.proxy.handler.InvokeHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;


public class ProxyProvider extends AbstractProxyProvider {
    private static final Logger logger = LoggerFactory.getLogger(ProxyProvider.class);
    public static final ProxyProvider INSTANCE=new ProxyProvider();



    public ProxyProvider(){

    }

    /**
     * 获得代理BEAN
     * @param proxyInterface
     * @param version
     * @param address
     * @param <T>
     * @return
     */
    public <T> T getProxyBean(Class<?> proxyInterface,String version,String address){
        return getProxyBean(proxyInterface,version,address,null);
    }



    /**
     * 通过IP获得代理bean
     * @param proxyInterface
     * @param version
     * @param ip
     * @param <T>
     * @return
     */
    public <T> T getProxyBeanByIp(Class<?> proxyInterface,String version,String ip){
        String address=getAddressByIp(proxyInterface.getName(),version,ip,null);
        if(StringUtils.isNotEmpty(address)){
            return  this.getProxyBean(proxyInterface,version,address,null);
        }else{
            logger.error("[error] no address find.ip:{}",ip);
            return null;
        }
    }

    /**
     * 获得代理bean
     * @param proxyInterface
     * @param version
     * @param address
     * @param zkAddress
     * @param <T>
     * @return
     */
    public <T> T getProxyBean(Class<?> proxyInterface,String version,String address,String zkAddress){
        if (StringUtils.isEmpty(version)){
            version="1.0.0";
        }
        if (null!=proxyInterface.getAnnotation(SpiderApi.class)){
            version=proxyInterface.getAnnotation(SpiderApi.class).version();
            if(StringUtils.isEmpty(address)){
                address=proxyInterface.getAnnotation(SpiderApi.class).address();
            }
        }
        InvocationHandler invokeHandler=new InvokeHandler(proxyInterface,version,address,zkAddress);
        Class[] interfaces=new  Class[1];
        interfaces[0]=proxyInterface;
        T t= (T) Proxy.newProxyInstance(proxyInterface.getClassLoader(),interfaces,invokeHandler);
        return  t;
    }

    /**
     * 通过IP获得代理bean
     * @param proxyInterface
     * @param version
     * @param ip
     * @param zkAddress
     * @param <T>
     * @return
     */
    public <T> T getProxyBeanByIp(Class<?> proxyInterface,String version,String ip,String zkAddress){
        String address=getAddressByIp(proxyInterface.getName(),version,ip,zkAddress);
        if(StringUtils.isNotEmpty(address)){
            return  this.getProxyBean(proxyInterface,version,address,zkAddress);
        }else{
            logger.error("[error] no address find.ip:{}",ip);
            return null;
        }
    }

    /**
     * 获得http代理
     * @param proxyInterface
     * @param version
     * @param proxyEvnGroup
     * @param <T>
     * @return
     */
    public <T> T getHttpProxyBean(Class<?> proxyInterface,String version, String zkAddress,String proxyEvnGroup,String proxyEnvId){
        if (StringUtils.isEmpty(version)){
            version="1.0.0";
        }
        String address=null;
        if (null!=proxyInterface.getAnnotation(SpiderApi.class)){
            version=proxyInterface.getAnnotation(SpiderApi.class).version();
            if(StringUtils.isEmpty(address)){
                address=proxyInterface.getAnnotation(SpiderApi.class).address();
            }
        }

       //TO ADD
        InvocationHandler invokeHandler=new InvokeHandler(proxyInterface,version,address,zkAddress,proxyEvnGroup,proxyEnvId);
        Class[] interfaces=new  Class[1];
        interfaces[0]=proxyInterface;
        T t= (T) Proxy.newProxyInstance(proxyInterface.getClassLoader(),interfaces,invokeHandler);
        return  t;
    }


    private  String getAddressByIp(String service,String version,String ip,String zkAddress){
        List<String> addressList= null;
        if(StringUtils.isNotEmpty(zkAddress)){
            addressList= SpiderRpc.instance(zkAddress).rpcDiscovery().getServerNodeList(service,version,null);
        }else{
            addressList= SpiderRpc.instance().rpcDiscovery().getServerNodeList(service,version,null);
        }
        for (String address:addressList) {
            if(address.split(":")[0].equals(ip)){
                return address;
            }
        }
        return null;
    }
}


