package com.hui.zhang.spider.register.data;

import com.hui.zhang.spider.common.beanutil.JavaBeanDescriptor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by zhanghui on 2017/11/13.
 */
public class RpcMethod  implements Serializable {
    private static final long serialVersionUID = 840983861899921407L;

    public JavaBeanDescriptor returnType;
    public String methodName;
    public int methodIndex;
    public List<RpcParam> rpcParamList;
    public Map<String,Object> paramsMap;
    public boolean isTokenCheck;
    public boolean isBeanMode;
    public boolean isInterfaceCheck; //是否接口校验
    public long timeout;
    boolean isGeneric;
    boolean isSpiderLock; //  是否添加分布式锁

    public boolean isSpiderLock() {
        return isSpiderLock;
    }

    public void setSpiderLock(boolean spiderLock) {
        isSpiderLock = spiderLock;
    }

    public boolean isGeneric() {
        return isGeneric;
    }

    public void setGeneric(boolean generic) {
        isGeneric = generic;
    }

    public int getMethodIndex() {
        return methodIndex;
    }

    public void setMethodIndex(int methodIndex) {
        this.methodIndex = methodIndex;
    }

    public Map<String, Object> getParamsMap() {
        return paramsMap;
    }

    public void setParamsMap(Map<String, Object> paramsMap) {
        this.paramsMap = paramsMap;
    }

    public JavaBeanDescriptor getReturnType() {
        return returnType;
    }

    public void setReturnType(JavaBeanDescriptor returnType) {
        this.returnType = returnType;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public List<RpcParam> getRpcParamList() {
        return rpcParamList;
    }

    public void setRpcParamList(List<RpcParam> rpcParamList) {
        this.rpcParamList = rpcParamList;
    }

    public boolean isTokenCheck() {
        return isTokenCheck;
    }

    public void setTokenCheck(boolean tokenCheck) {
        isTokenCheck = tokenCheck;
    }

    public boolean isBeanMode() {
        return isBeanMode;
    }

    public void setBeanMode(boolean beanMode) {
        isBeanMode = beanMode;
    }

    public boolean isInterfaceCheck() {
        return isInterfaceCheck;
    }

    public void setInterfaceCheck(boolean interfaceCheck) {
        isInterfaceCheck = interfaceCheck;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
