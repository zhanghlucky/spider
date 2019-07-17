package com.hui.zhang.spider.remoteing.netty.server;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.edb01.common.util.PropertyUtil;
import com.hui.zhang.spider.common.annotation.SpiderService;
import com.hui.zhang.spider.common.beanutil.JavaBeanDescriptor;
import com.hui.zhang.spider.common.beanutil.JavaBeanParam;
import com.hui.zhang.spider.common.beanutil.JavaBeanSerializeUtil;
import com.hui.zhang.spider.common.util.ClassMapUtil;
import com.hui.zhang.spider.register.data.RpcMethod;
import com.hui.zhang.spider.register.data.RpcParam;
import com.hui.zhang.spider.register.zk.SpiderRpc;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import com.hui.zhang.spider.common.config.SpiderConfig;
import com.hui.zhang.spider.register.data.RpcProvider;
import com.hui.zhang.spider.remoteing.netty.server.thread.NettyServerThread;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

/**
 * RPC服务bean
 * @author zhanghui
 *
 */
//@Component
public class RpcServerApplicationContextAware implements ApplicationContextAware, InitializingBean{
	private static final Logger LOGGER = LoggerFactory.getLogger(RpcServerApplicationContextAware.class);
    //private Map<String, Object> handlerMap = new HashMap<String, Object>(); // 存放接口名与服务对象之间的映射关系
	private List<RpcProvider> rpcProviderList=new ArrayList<>();

	private static String appName;
	public RpcServerApplicationContextAware(){
	    if (null==appName){
            appName= PropertyUtil.getProperty("app.name");//取出appname
        }
    }

    public void setApplicationContext(ApplicationContext ctx) {
        Map<String, Object> serviceBeanMap =ctx.getBeansWithAnnotation(SpiderService.class); // 获取所有带有 RpcService 注解的 Spring Bean

        if (MapUtils.isNotEmpty(serviceBeanMap)) {
            for (Object serviceBean : serviceBeanMap.values()) {
                //只取第一个接口
                Class interfaceCls= serviceBean.getClass().getInterfaces()[0];
                String interfaceName = interfaceCls.getName();
                String version=serviceBean.getClass().getAnnotation(SpiderService.class).version();
                String desc=serviceBean.getClass().getAnnotation(SpiderService.class).desc();

                List<RpcMethod> recMethodList=new ArrayList<>();
                LocalVariableTableParameterNameDiscoverer u =
                        new LocalVariableTableParameterNameDiscoverer();
                Method[] interfaceMethods = interfaceCls.getDeclaredMethods();
                Method[] implMethods = serviceBean.getClass().getDeclaredMethods();

                for (int k=0;k<implMethods.length;k++) {
                    Method method=implMethods[k];
                    if (isMethodOK(method,interfaceMethods)){
                        Map<String,Object> paramsMap=new LinkedHashMap();

                        String[] paramNames = u.getParameterNames(method);
                        Class[]  clsTypes= method.getParameterTypes();
                        Type[]   types=method.getGenericParameterTypes();

                        List<RpcParam> rpcParamList=new ArrayList<>();
                        for (int i = 0; i < paramNames.length; i++) {
                            Class clsType=clsTypes[i];
                            String paramName=paramNames[i];
                            Type type=types[i];
                            JavaBeanDescriptor mainDescriptor= JavaBeanSerializeUtil.serialize(clsType);
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

                            Object chObj= ClassMapUtil.getChildObj(paramName,clsType,type);
                            paramsMap.put(paramName,chObj);

                            RpcParam rpcParam=new RpcParam();
                            rpcParam.setParamType(paramType);
                            rpcParam.setParamName(paramName);
                            rpcParamList.add(rpcParam);
                        }
                        Class returnTypeCls=method.getReturnType();
                        Type rType= method.getGenericReturnType();
                        //System.out.println(returnTypeCls);

                        JavaBeanDescriptor returnType=JavaBeanSerializeUtil.serialize(returnTypeCls);
                        returnType.setClassTypeName(rType.getTypeName());

                        String methodName=method.getName();
                        RpcMethod rpcMethod=new RpcMethod();
                        rpcMethod.setMethodName(methodName);
                        rpcMethod.setMethodIndex(k);
                        rpcMethod.setReturnType(returnType);
                        rpcMethod.setRpcParamList(rpcParamList);
                        rpcMethod.setParamsMap(paramsMap);
                        recMethodList.add(rpcMethod);
                    }
                }

                LOGGER.debug("[spider] add server:{}",interfaceName);
                //int port=RegistHostUtil.getRegistPort();
                try{
                    //String ip = RegistHostUtil.getRegistIp();
                    //String addr = ip+":"+port;
                    RpcProvider rpcProvider=new RpcProvider(appName,desc,interfaceName,version,recMethodList,SpiderConfig.INSTANCE.getServerAddress());
                    rpcProviderList.add(rpcProvider);
                }catch (Exception e){
                    e.printStackTrace();
                   LOGGER.error("[spider] register server error :{}",e.getMessage());
                }

            }
        }
        //启动netty服务
        new NettyServerThread().start();
        //注册服务
        SpiderRpc.instance(null).rpcRegistry().register(rpcProviderList);
        
        LOGGER.info("[spider] register server:{}",SpiderConfig.INSTANCE.getServerAddress());
    }

    private boolean isMethodOK(Method method, Method[] interfaceMethods ){
        for (Method im: interfaceMethods) {
            if (method.getName().equals(im.getName())
                    &&method.getReturnType().getName().equals(im.getReturnType().getName())
                    &&method.getParameterCount()==im.getParameterCount()){
                return true;
            }
        }
        return false;
    }

    public void afterPropertiesSet() throws Exception {
    	
    }
}