package com.hui.zhang.spider.common.encoder;

import com.alibaba.fastjson.JSON;
import com.edb01.common.util.JsonEncoder;
import com.hui.zhang.spider.common.beanutil.JavaBeanDescriptor;
import com.hui.zhang.spider.common.beanutil.JavaBeanSerializeUtil;
import com.hui.zhang.spider.remoteing.netty.data.RpcResponse;
import com.hui.zhang.spider.remoteing.netty.data.RpcReturnType;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by zhanghui on 2018-11-23.
 */
public class RpcEncoder {
    private static final Logger logger = LoggerFactory.getLogger(RpcEncoder.class);
    public static final RpcEncoder DEFAULT = new RpcEncoder();
    private static  final ObjectMapper mapper ;

    static {
        mapper = new ObjectMapper();
    }
    public RpcEncoder() {

    }

    public <T> T decode(RpcResponse rpcResponse,JavaBeanDescriptor returnType,RpcReturnType rpcReturnType){
        if(StringUtils.isNotEmpty(rpcResponse.getResult())){
            if (null==rpcReturnType){//没有泛型的时候
                try {
                    Class<T> cls= (Class) JavaBeanSerializeUtil.deserialize(returnType);
                    T t= JSON.parseObject(rpcResponse.getResult(),cls);
                    return t;
                } catch (Exception e) {
                    logger.error("解析异常：{}",e.getMessage());
                }
                return null;
            }else{//有泛型的时候 待调整为递归算法
                Map<String,JavaType> javaTypeMap=new HashedMap();
                this.initJavaType(null,1,rpcReturnType,javaTypeMap);
                JavaType javaType=javaTypeMap.get("javaType");
                //System.out.println("###>>>>"+JsonEncoder.DEFAULT.encode(javaTypeMap.get("javaType").getTypeName()));
                /*JavaType javaType=null;

                Class cls= (Class) JavaBeanSerializeUtil.deserialize(returnType);
                List<RpcReturnType> childList=rpcReturnType.getChildRpcReturnTypeList();
                if(null==childList){
                    Class constructClazzs= (Class) JavaBeanSerializeUtil.deserialize(rpcReturnType.getReturnType());
                    javaType =mapper.getTypeFactory().constructParametricType(cls, constructClazzs);
                }else{
                    if(childList.size()==1) {

                        RpcReturnType cRpcReturnType = childList.get(0);
                        Class cCls = (Class) JavaBeanSerializeUtil.deserialize(cRpcReturnType.getReturnType());

                        List<RpcReturnType> ccList = cRpcReturnType.getChildRpcReturnTypeList();
                        Class ccCls = null;
                        Class[] ccArray=null;
                        if (null == ccList) {

                        } else {
                            if(ccList.size()==1){
                                RpcReturnType ccRpcReturnType = ccList.get(0);
                                ccCls = (Class) JavaBeanSerializeUtil.deserialize(ccRpcReturnType.getReturnType());
                            }else {
                                ccArray=new Class[ccList.size()];
                                for (int i = 0; i < ccList.size(); i++) {
                                    ccArray[i]=(Class) JavaBeanSerializeUtil.deserialize(ccList.get(i).getReturnType());
                                }
                            }
                        }
                        JavaType dataType = null;
                        if (null != ccCls) {
                            dataType = mapper.getTypeFactory().constructParametricType(cCls, ccCls);
                        }
                        if (null != ccArray) {
                            dataType = mapper.getTypeFactory().constructParametricType(cCls, ccArray);
                        }

                        if (null != dataType) {
                            javaType = mapper.getTypeFactory().constructParametricType(cls, dataType);
                        }else{
                            javaType = mapper.getTypeFactory().constructParametricType(cls, cCls);
                        }
                    }else{
                        Class[] clsArray=new Class[childList.size()];
                        for (int i = 0; i < childList.size(); i++) {
                            clsArray[i]=(Class) JavaBeanSerializeUtil.deserialize(childList.get(i).getReturnType());
                        }
                        javaType = mapper.getTypeFactory().constructParametricType(cls, clsArray);
                    }
                }*/
                try {
                    T t=mapper.readValue(rpcResponse.getResult(), javaType);
                    return t;
                } catch (IOException e) {
                    logger.error("json转对象异常：{}",e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return  null;
    }


    private void initJavaType(Class pCls,int deep,RpcReturnType rpcReturnType,Map<String,JavaType> javaTypeMap){
        Class thisCls = (Class) JavaBeanSerializeUtil.deserialize(rpcReturnType.getReturnType());
        JavaType dataType=null;
        if (null!=rpcReturnType.getChildRpcReturnTypeList()){
            int childSize=rpcReturnType.getChildRpcReturnTypeList().size();
            Class ccCls = null;
            Class[] ccArray=null;
            //只有一个节点
            if(childSize==1){
                RpcReturnType ccRpcReturnType = rpcReturnType.getChildRpcReturnTypeList().get(0);
                ccCls = (Class) JavaBeanSerializeUtil.deserialize(ccRpcReturnType.getReturnType());
                //递归
                int next=deep+1;
                this.initJavaType(thisCls,next,ccRpcReturnType,javaTypeMap);//递归
            //多于一个节点
            }else{
                ccArray=new Class[rpcReturnType.getChildRpcReturnTypeList().size()];
                for (int i = 0; i < rpcReturnType.getChildRpcReturnTypeList().size(); i++) {
                    ccArray[i]=(Class) JavaBeanSerializeUtil.deserialize(rpcReturnType.getChildRpcReturnTypeList().get(i).getReturnType());
                }
            }
            if (childSize==1) {
                if (null==javaTypeMap.get("javaType")){
                    dataType = mapper.getTypeFactory().constructParametricType(thisCls, ccCls);
                }else{
                    //dataType = mapper.getTypeFactory().constructParametricType(thisCls, javaTypeMap.get("javaType"));
                }
            }else {
                if (null==javaTypeMap.get("javaType")){
                    dataType = mapper.getTypeFactory().constructParametricType(thisCls, ccArray);
                }else {
                    dataType = mapper.getTypeFactory().constructParametricType(thisCls, javaTypeMap.get("javaType"));
                }
            }
        }
        //子递归
        if (deep>1){
            if (null!=dataType){
                javaTypeMap.put("javaType",dataType);
            }else{
                if(null!=javaTypeMap.get("javaType")){
                    JavaType javaType=mapper.getTypeFactory().constructParametricType(pCls, javaTypeMap.get("javaType"));
                    javaTypeMap.put("javaType",javaType);
                }else{
                    JavaType javaType=mapper.getTypeFactory().constructParametricType(pCls, thisCls);
                    javaTypeMap.put("javaType",javaType);
                }
            }
        //第一层递归
        }else {
            if (null!=dataType){
                javaTypeMap.put("javaType",dataType);
            }else{
                if(null==javaTypeMap.get("javaType")){
                    JavaType javaType=mapper.getTypeFactory().constructParametricType(thisCls, new Class[0]);
                    javaTypeMap.put("javaType",javaType);
                }
            }
        }
    }

}
