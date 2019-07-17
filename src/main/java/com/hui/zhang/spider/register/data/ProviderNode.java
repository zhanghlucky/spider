package com.hui.zhang.spider.register.data;

import java.io.Serializable;

/**
 * Created by zhanghui on 2019-06-20.
 */
public class ProviderNode  implements Serializable {
    private String interfaceName;
    private String version;
    private String address;



    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
