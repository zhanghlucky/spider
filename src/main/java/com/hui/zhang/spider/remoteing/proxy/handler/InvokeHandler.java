package com.hui.zhang.spider.remoteing.proxy.handler;

import com.edb01.common.logger.EdbLogger;
import com.edb01.common.logger.EdbLoggerFactory;
import com.edb01.common.trace.TraceTool;
import com.edb01.common.util.JsonEncoder;
import com.edb01.common.util.MD5Util;
import com.edb01.common.util.UUIDGenerator;
import com.hui.zhang.spider.common.annotation.Sparam;
import com.hui.zhang.spider.common.beanutil.JavaBeanDescriptor;
import com.hui.zhang.spider.common.beanutil.JavaBeanParam;
import com.hui.zhang.spider.common.beanutil.JavaBeanSerializeUtil;
import com.hui.zhang.spider.common.encoder.RpcEncoder;
import com.hui.zhang.spider.common.exceptions.RpcException;
import com.hui.zhang.spider.common.validate.ValidateBean;
import com.hui.zhang.spider.register.data.ProviderNode;
import com.hui.zhang.spider.register.data.RpcProvider;
import com.hui.zhang.spider.register.zk.SpiderRpc;
import com.hui.zhang.spider.remoteing.netty.client.RpcClient;
import com.hui.zhang.spider.remoteing.netty.data.RpcRequest;
import com.hui.zhang.spider.remoteing.netty.data.RpcResponse;
import com.hui.zhang.spider.remoteing.netty.data.RpcReturnType;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhanghui on 2017/11/24.
 */
public class InvokeHandler implements InvocationHandler {
    private static final EdbLogger logger = EdbLoggerFactory.getLogger(InvokeHandler.class);

    public Class<?>  interfaceClass;
    private String version;
    private String address;
    private String proxyEnvGroup;
    private String proxyEnvId;
    private String zkAddress;
    public InvokeHandler(Class<?>  interfaceClass,String version,String address){
        this.interfaceClass=interfaceClass;
        this.version=version;
        this.address=address;
    }

    public InvokeHandler(Class<?>  interfaceClass,String version,String address,String zkAddress){
        this.interfaceClass=interfaceClass;
        this.version=version;
        this.address=address;
        this.zkAddress=zkAddress;
    }

    public InvokeHandler(Class<?>  interfaceClass,String version,String address,String zkAddress,String proxyEnvGroup,String proxyEnvId){
        this.interfaceClass=interfaceClass;
        this.version=version;
        this.address=address;
        this.zkAddress=zkAddress;
        this.proxyEnvGroup=proxyEnvGroup;
        this.proxyEnvId=proxyEnvId;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws  Throwable {
        //TOADD 待实现method是第几个method
        RpcRequest request = new RpcRequest();
        //request.setRequestId(logger.getTraceId());
        request.setRequestId(TraceTool.getInstance().getTraceId());
        request.setClassName(method.getDeclaringClass().getName());
        request.setVersion(version);
        request.setMethodName(method.getName());
        request.setTimeout(5000);//默认超时时间
        request.setProxyEnvId(proxyEnvId);
        JavaBeanParam parameterTypes[]=new JavaBeanParam[method.getParameterTypes().length];
        Class [] ptypes=method.getParameterTypes();
        Type[]   types=method.getGenericParameterTypes();

        Annotation[][] annotation=method.getParameterAnnotations();
        for (int j = 0; j <ptypes.length ; j++) {
            Class ptype=ptypes[j];
            Type type=types[j];
            JavaBeanDescriptor mainDescriptor= JavaBeanSerializeUtil.serialize(ptype);
            mainDescriptor.setClassTypeName(type.getTypeName());

            List<JavaBeanDescriptor> typeDescriptorList =new LinkedList<>();
            if (type instanceof ParameterizedType){//泛型的参数类型
                ParameterizedType pt = (ParameterizedType) type;
                Type[] argTypes= pt.getActualTypeArguments();
                for (Type argType:argTypes) {
                    Class argClas=(Class)argType;
                    typeDescriptorList.add(JavaBeanSerializeUtil.serialize(argClas));
                }
            }
            JavaBeanParam paramType=new JavaBeanParam(mainDescriptor,typeDescriptorList);
            parameterTypes[j]=paramType;
        }
        request.setParameterTypes(parameterTypes);

        //返回数据类型
        JavaBeanDescriptor returnType=JavaBeanSerializeUtil.serialize(method.getReturnType());
        request.setReturnType(returnType);
        //泛型返回数据类型
        //List<JavaBeanDescriptor> genericReturnTypeList=new ArrayList<>();
        Type rType=  method.getGenericReturnType();
        RpcReturnType rpcReturnType=null;
        if (null!=rType){
            rpcReturnType=new RpcReturnType();
            List<RpcReturnType>  childRpcReturnTypeList=new ArrayList<>();
            checkType(rType,rpcReturnType,childRpcReturnTypeList);
        }

        String [] parameters=null;
        if(null!=args){
            parameters=new String[args.length];
            for (int i = 0; i <args.length ; i++) {
                Object arg=args[i];
                String parameter=null;
                if (arg instanceof String){//对String 直接使用，非String 强转
                    parameter=(String)arg;
                }else if(null==arg){//空对象转 null
                    parameter="";
                } else{
                    parameter=JsonEncoder.DEFAULT.encode(arg);
                }

                parameters[i]=parameter;
                Annotation[] an=  annotation[i];
                if (an.length > 0){
                    Sparam sp  = Sparam.class.cast(an[0]);
                    if (sp.required()) {
                        if (StringUtils.isEmpty(parameter)){
                           /* throw  new RpcException(sp.code(),sp.msg());*/ // 异常抛出 通过 try 处理异常
                        return ValidateBean.INSTRANGE.build(sp.code(),sp.msg());
                        }
                    }
                    if (0 != sp.min()){
                        if (sp.min() < Integer.parseInt(parameter)){
                          /*  throw  new RpcException(sp.code(),sp.msg());*/
                        return ValidateBean.INSTRANGE.build(sp.code(),sp.msg());
                        }
                    }
                    if (0 != sp.max()){
                        if (sp.max() > Integer.parseInt(parameter)){
                         /*   throw  new RpcException(sp.code(),sp.msg());*/
                        return ValidateBean.INSTRANGE.build(sp.code(),sp.msg());
                        }
                    }
                    if (null != sp.regexp()){
                        Pattern pattern = Pattern.compile(sp.regexp());
                        Matcher matcher = pattern.matcher(parameter);
                        if (!matcher.matches()){
                            /*throw  new RpcException(sp.code(),sp.msg());*/
                        return ValidateBean.INSTRANGE.build(sp.code(),sp.msg());
                        }
                    }
                    if (0 != sp.length()){
                        if (sp.length() < parameter.length()){
                           /* throw  new RpcException(sp.code(),sp.msg());*/
                           return ValidateBean.INSTRANGE.build(sp.code(),sp.msg());
                        }
                    }
                }
            }
        }
        request.setParameters(parameters);
        //发现服务
        ProviderNode rpcProvider=null;

        if (StringUtils.isNotEmpty(address)){
            rpcProvider= SpiderRpc.instance(zkAddress).rpcDiscovery().discover(request.getClassName(),request.getVersion(),address);
        }else{
            if(StringUtils.isNotEmpty(proxyEnvGroup)&&StringUtils.isNotEmpty(proxyEnvId)){
                rpcProvider= SpiderRpc.instance(zkAddress).rpcDiscovery().discoverProxy(proxyEnvGroup,proxyEnvId,request.getClassName());
            }else{
                rpcProvider= SpiderRpc.instance(zkAddress).rpcDiscovery().discover(request.getClassName(),request.getVersion());
            }
        }

        if(null!=rpcProvider){
            String[] array = rpcProvider.getAddress().split(":");
            String host = array[0];
            int port = Integer.parseInt(array[1]);
            RpcClient client = new RpcClient(host, port); // 初始化 RPC 客户端
            RpcResponse response = client.send(request); // 通过 RPC 客户端发送 RPC 请求并获取 RPC 响应
            if (response.getError() != null) {
                logger.error("[spider] return data error:{}",response.getError().getMessage());
                throw  new RpcException(response.getError());
            } else {
                Object obj=RpcEncoder.DEFAULT.decode(response,returnType,rpcReturnType);
                return obj;
            }
        }else{
            throw  new RpcException("no server node support");
        }
    }



    private static String getMethodName(String fildeName) throws Exception{
        byte[] items = fildeName.getBytes();
        items[0] = (byte) ((char) items[0] - 'a' + 'A');
        return new String(items);
    }

    private void checkType(Type pType , RpcReturnType rpcReturnType, List<RpcReturnType>  childRpcReturnTypeList){
        if (pType instanceof ParameterizedType) {//泛型的参数类型
            ParameterizedType mainPt = (ParameterizedType) pType;
            Type[] childTypes= mainPt.getActualTypeArguments();
            for (Type childType:childTypes) {
                //beanList.add(JavaBeanSerializeUtil.serialize(argType.getClass()));
                List<RpcReturnType>  childList=new ArrayList<>();
                RpcReturnType childRpcReturnType=new RpcReturnType();
                if (childType instanceof ParameterizedType) {

                    this.checkType(childType,childRpcReturnType,childList);//递归

                    ParameterizedType childPt = (ParameterizedType) childType;
                    Class childCls=(Class)childPt.getRawType();
                    JavaBeanDescriptor childJavaBeanDescriptor=JavaBeanSerializeUtil.serialize(childCls);
                    childRpcReturnType.setReturnType(childJavaBeanDescriptor);
                    childRpcReturnType.setChildRpcReturnTypeList(childList);

                    childRpcReturnTypeList.add(childRpcReturnType);

                }else{
                   Class childCls=(Class)childType;
                    JavaBeanDescriptor childJavaBeanDescriptor=JavaBeanSerializeUtil.serialize(childCls);
                    childRpcReturnType.setReturnType(childJavaBeanDescriptor);
                    childRpcReturnTypeList.add(childRpcReturnType);
                }
            }
            Class pCls=(Class)mainPt.getRawType();
            JavaBeanDescriptor pJavaBeanDescriptor=JavaBeanSerializeUtil.serialize(pCls);
            rpcReturnType.setReturnType(pJavaBeanDescriptor);
            rpcReturnType.setChildRpcReturnTypeList(childRpcReturnTypeList);
        }else{
            Class pCls=(Class)pType;
            JavaBeanDescriptor pJavaBeanDescriptor=JavaBeanSerializeUtil.serialize(pCls);
            rpcReturnType.setReturnType(pJavaBeanDescriptor);
            rpcReturnType.setChildRpcReturnTypeList(childRpcReturnTypeList);
        }
    }
}
