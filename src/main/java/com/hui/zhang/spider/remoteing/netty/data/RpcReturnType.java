package com.hui.zhang.spider.remoteing.netty.data;

import com.hui.zhang.spider.common.beanutil.JavaBeanDescriptor;

import java.io.Serializable;
import java.util.List;

/**
 * Created by zhanghui on 2018-11-23.
 */
public class RpcReturnType implements Serializable {
    private static final long serialVersionUID = 188554L;
    private JavaBeanDescriptor returnType;
    private List<RpcReturnType> childRpcReturnTypeList;

    public RpcReturnType(){}

    public JavaBeanDescriptor getReturnType() {
        return returnType;
    }

    public void setReturnType(JavaBeanDescriptor returnType) {
        this.returnType = returnType;
    }

    public List<RpcReturnType> getChildRpcReturnTypeList() {
        return childRpcReturnTypeList;
    }

    public void setChildRpcReturnTypeList(List<RpcReturnType> childRpcReturnTypeList) {
        this.childRpcReturnTypeList = childRpcReturnTypeList;
    }
}
