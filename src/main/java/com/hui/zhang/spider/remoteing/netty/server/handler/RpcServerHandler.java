package com.hui.zhang.spider.remoteing.netty.server.handler;

import com.edb01.common.logger.EdbLogger;
import com.edb01.common.logger.EdbLoggerFactory;
import com.edb01.common.spring.SpringBeanUtil;
import com.edb01.common.trace.TraceTool;
import com.edb01.common.util.HessianEncoder;
import com.edb01.common.util.JsonEncoder;
import com.edb01.common.util.PropertyUtil;
import com.edb01.core.result.RpcResult;
import com.hui.zhang.spider.common.annotation.Sparam;
import com.hui.zhang.spider.common.beanutil.JavaBeanDescriptor;
import com.hui.zhang.spider.common.beanutil.JavaBeanParam;
import com.hui.zhang.spider.common.beanutil.JavaBeanSerializeUtil;
import com.hui.zhang.spider.common.http.HttpClientUtil;
import com.hui.zhang.spider.register.zk.RpcPathTool;
import com.hui.zhang.spider.register.zk.RpcZk;
import com.hui.zhang.spider.register.zk.SpiderRpc;
import com.hui.zhang.spider.remoteing.netty.data.RpcRequest;
import com.hui.zhang.spider.remoteing.netty.data.RpcResponse;
import com.esotericsoftware.reflectasm.MethodAccess;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RPC服务端消息处理
 *
 * @author zhanghui
 */
public class RpcServerHandler  extends SimpleChannelInboundHandler<RpcRequest>   {

	private static final EdbLogger logger = EdbLoggerFactory.getLogger(RpcServerHandler.class);

	private static final String HEARTBEAT_CLASSNAME = "heartbeat";

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {

		if (!isHeartbeatMsg(request)) {
            logger.debug("[spider] handle request.interface:{},method:{}",request.getClassName(),request.getMethodName());
            RpcResponse response=null;
            //代理模式
            if(null!= PropertyUtil.getProperty("proxy")&&
                    Boolean.valueOf(PropertyUtil.getProperty("proxy"))&&
                    !request.getClassName().equals("com.hui.zhang.spider.monitor.AppMetaService")){
                response= proxyHandler(request);
            //本地模式
            }else{
                response=localHandler(request);
            }
			//保持长连接
			ChannelFuture channelFuture = ctx.writeAndFlush(response);
		}
		//心跳消息
		else {
            logger.debug("[spider] heartbeat message channel: {}.", ctx.channel());
			ReferenceCountUtil.release(request);
		}
	}

	//本地请求
	private RpcResponse localHandler(RpcRequest request){
        RpcResponse response= new RpcResponse();
        response.setRequestId(request.getRequestId());
        try {

            Class<?> service=Class.forName(request.getClassName());
            Method [] method =  service.getMethods();

            Annotation[][] an=new Annotation[][]{};
            for(int k = 0;k < method.length;k++){
                if (method[k].getName().equals( request.getMethodName())){
                    an = method[k].getParameterAnnotations();
                    break;
                }
            }
            Object serverBean= SpringBeanUtil.getBeanByType(service);
            Method[] beanMethods = serverBean.getClass().getMethods();
            // Get annotations on the method in the impl.
       /*     for (Method beanMethod:
                 beanMethods) {
                if (beanMethod.getName().equals(request.getMethodName())) {
                    beanType = beanMethod.getAnnotation(SpiderMethod.class).beanType();
                    break;
                }
            }*/
            //LOGGER.info("service-class:{},serverBean：{}",service,serverBean);
            MethodAccess access = MethodAccess.get(serverBean.getClass());
            JavaBeanParam[] parameterTypes=request.getParameterTypes();

            RpcResult checkResult = new RpcResult(true,0,"success",null);
            String[] parameters=request.getParameters();
            Object[] params=null;
            if(null!=parameters){
                params=new Object[parameters.length];
                for (int i = 0; i <parameters.length ; i++) {
                    JavaBeanParam parameterType=parameterTypes[i];
                    String parameter=parameters[i];

                    if (parameterType.getTypeDescriptorList().size()<=0){
                        Class paramTypeCls=(Class)JavaBeanSerializeUtil.deserialize(parameterType.getMainDescriptor());
                        if (paramTypeCls.getName().equals(String.class.getName())){
                            params[i]=parameter;
                        }else{
                            params[i]=JsonEncoder.DEFAULT.decode(parameter,paramTypeCls);
                        }

                    }else{

                        Class paramTypeCls=(Class)JavaBeanSerializeUtil.deserialize(parameterType.getMainDescriptor());
                        List<JavaBeanDescriptor> list=parameterType.getTypeDescriptorList();
                        Class[] clsList=new Class[list.size()];
                        for (int j = 0; j <list.size() ; j++) {
                            JavaBeanDescriptor javaBeanDescriptor=list.get(j);
                            clsList[j]=(Class)JavaBeanSerializeUtil.deserialize(javaBeanDescriptor);
                        }
                        Object obj=JsonEncoder.DEFAULT.decode(parameter,paramTypeCls,clsList);
                        params[i]= obj;
                    }
                }
                checkResult=paramCheck(params,an, request.isBeanType());
            }
            if (checkResult.success){
                //logger.setTraceId(request.getRequestId());//设置调用链ID
                TraceTool.getInstance().setTraceId(request.getRequestId());

                Object resultObject=access.invoke(serverBean, request.getMethodName(), params);//代理出实例
                String result=JsonEncoder.DEFAULT.encode(resultObject);
                response.setResult(result);
            }else{
                String result=JsonEncoder.DEFAULT.encode(checkResult);
                response.setResult(result);
            }

        } catch (Exception e) {
            logger.error("[spider] server node error, request:{}.error: {}", request.getClassName(), e.getMessage());
            e.printStackTrace();
            response.setResult(null);
            response.setError(e);
        }
        //LOGGER.info("[spider] local service:{} ",request.getClassName()+":"+request.getVersion());
        return response;
    }

    //代理请求
    private RpcResponse proxyHandler(RpcRequest request) throws  Exception{
        long start=System.currentTimeMillis();
        //String serviceProxyNodePath=null;
        String className= request.getClassName();
        String proxyEnvId=null;
        //兼容性
        if(null==request.getProxyEnvId()){
            logger.warn("代理请求未指定代理环境，随机取环境！");

            List<String> proxyEnvList= SpiderRpc.instance().zkClient().getChildren().forPath("/proxy-node");
            if (proxyEnvList.size()>0){
                int random=(int)(Math.random()*proxyEnvList.size());
                proxyEnvId=proxyEnvList.get(random);
                logger.info("随机使用代理环境：{}",proxyEnvId);
            }else {
                throw new Exception("代理请求-无代理环境ID节点！");
            }
        }else{
            proxyEnvId=request.getProxyEnvId();
        }
        //--兼容性

        String serviceProxyNodePath= RpcPathTool.getServiceProxyNodePath(className,proxyEnvId);
        try {

            byte[] data=RpcZk.instance().connect().getData().forPath(serviceProxyNodePath);

            String url= HessianEncoder.decodeByte(data,String.class);
            url=url+"/rpc/proxy-service";

            Map<String,String> map=new HashedMap();
            map.put("data", HessianEncoder.encoderStr(request));
            RpcResponse rpcResponse= HttpClientUtil.rpcPost(url,map);
            long end=System.currentTimeMillis();

            logger.info("[spider] proxy service:{}-{}-{} . use time:{}ms",request.getClassName(),request.getVersion(),request.getMethodName(),(end-start));
            return  rpcResponse;
        }catch (Exception e){
            e.printStackTrace();
        }

        return  null;
    }



    /**
     * tian.luan 方法参数校验
     *
     * @param params
     * @param an
     * @return
     * @throws IllegalAccessException
     */
    @SuppressWarnings("unchecked")
    private RpcResult paramCheck(Object[] params, Annotation[][] an, Boolean beanType) throws IllegalAccessException {
        if (!beanType) {
            for (int i = 0; i < params.length; i++) {
                //先校验方法体内是否有注解
                if (an.length > 0) {
                    Annotation[] annotation = an[i];
                    if (annotation.length > 0) {
                        Sparam sp = Sparam.class.cast(annotation[0]);
                        if (sp.required()) {
                            if (null == params[i] || "".equals(params[i])) {
                                return new RpcResult(Long.valueOf(sp.code()), sp.msg());
                            }
                        }
                        if (0 != sp.min()) {
                            int parameter = Integer.parseInt((params[i]).toString());
                            if (parameter < sp.min()) {
                                return new RpcResult(Long.valueOf(sp.code()), sp.msg());
                            }
                        }
                        if (0 != sp.max()) {
                            int parameter = Integer.parseInt((params[i]).toString());
                            if (parameter > sp.max()) {
                                return new RpcResult(Long.valueOf(sp.code()), sp.msg());

                            }
                        }
                        if (!StringUtils.isEmpty(sp.regexp())) {
                            String parameter = String.valueOf(params[i]);
                            if (!StringUtils.isEmpty(parameter)) {
                                Pattern pattern = Pattern.compile(sp.regexp());
                                Matcher matcher = pattern.matcher(parameter);
                                if (!matcher.matches()) {
                                    return new RpcResult(Long.valueOf(sp.code()), sp.msg());
                                }
                            }
                        }
                        if (0 != sp.length()) {
                            if (params[i] != null) {
                                String parameter = String.valueOf(params[i]);
                                if (sp.length() < parameter.length()) {
                                    return new RpcResult(Long.valueOf(sp.code()), sp.msg());

                                }
                            }
                        }
                    }
                }
            }
        } else {
            Field[] fields = params[0].getClass().getDeclaredFields();
            for (Field field : fields) {
                Annotation[] allFAnnos = field.getAnnotations();
                if (allFAnnos.length > 0) {
                    for (Annotation allFAnno : allFAnnos) {
                        if (allFAnno instanceof Sparam) {
                            field.setAccessible(true);
                            Sparam sp = Sparam.class.cast(allFAnnos[0]);
                            if (sp.required()) {
                                Object parameter = field.get(params[0]);
                                if (null == parameter || "".equals(parameter)) {
                                    return new RpcResult(Long.valueOf(sp.code()), sp.msg());
                                }
                            }
                            // TODO Long?
                            if (0 != sp.min() && (field.getType().getName().equals("java.lang.Integer") || field.getType().getName().equals("int"))) {
                                int parameter = Integer.parseInt(field.get(params[0]).toString());
                                if (parameter < sp.min()) {
                                    return new RpcResult(Long.valueOf(sp.code()), sp.msg());
                                }
                            }
                            if (0 != sp.max() && (field.getType().getName().equals("java.lang.Integer") || field.getType().getName().equals("int"))) {
                                int parameter = Integer.parseInt(field.get(params[0]).toString());
                                if (parameter > sp.max()) {
                                    return new RpcResult(Long.valueOf(sp.code()), sp.msg());

                                }
                            }
                            if (!StringUtils.isEmpty(sp.regexp()) && (field.getType().getName().equals("java.lang.String"))) {
                                String parameter = (String) field.get(params[0]);
                                if (null != parameter){
                                    Pattern pattern = Pattern.compile(sp.regexp());
                                    Matcher matcher = pattern.matcher(parameter);
                                    if (!matcher.matches()) {
                                        return new RpcResult(Long.valueOf(sp.code()), sp.msg());
                                    }
                                }
                            }
                            if (0 != sp.length() && field.getType().getName().equals("java.lang.String")) {
                                String parameter = (String) field.get(params[0]);
                                if (!StringUtils.isEmpty(parameter)) {
                                    if (sp.length() < parameter.length()) {
                                        return new RpcResult(Long.valueOf(sp.code()), sp.msg());
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
        logger.debug("[spider] parameter verification passed");
        return new RpcResult(null);
    }

	private boolean isHeartbeatMsg(RpcRequest request) {
		return request.getClassName().equals(HEARTBEAT_CLASSNAME);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("[spider] server exception:{}", cause);
		ctx.close();
	}
}
