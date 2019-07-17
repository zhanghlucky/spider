package com.hui.zhang.spider.register.data;

import com.hui.zhang.spider.common.beanutil.JavaBeanDescriptor;
import com.hui.zhang.spider.common.beanutil.JavaBeanParam;

import java.io.Serializable;

/**
 * Created by zhanghui on 2017/11/13.
 */
public class RpcParam  implements Serializable {
    public JavaBeanParam paramType;
    public String paramName;
    public boolean isNative;

    public boolean isNative() {
        return isNative;
    }

    public void setNative(boolean aNative) {
        isNative = aNative;
    }

    public JavaBeanParam getParamType() {
        return paramType;
    }

    public void setParamType(JavaBeanParam paramType) {
        this.paramType = paramType;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }
}
