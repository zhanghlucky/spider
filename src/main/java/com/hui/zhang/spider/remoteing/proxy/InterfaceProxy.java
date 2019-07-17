package com.hui.zhang.spider.remoteing.proxy;

public interface InterfaceProxy {

    public <T> T getProxyBean(Class<?> proxyInterface);

    public <T> T getProxyBean(Class<?> proxyInterface,String address);
    
    public <T> T getProxyBean(Class<?> proxyInterface,String version, String address);

    public <T> T getHttpProxyBean(Class<?> proxyInterface,String version,String zkAddress, String proxyEvnGroup,String proxyEnvId);

}

