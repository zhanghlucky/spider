package com.hui.zhang.spider.common.http;

import com.edb01.common.util.HessianEncoder;
import com.edb01.common.util.JsonEncoder;
import com.edb01.common.util.etc.AppParamsUtil;
import com.hui.zhang.spider.remoteing.netty.data.RpcResponse;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class HttpClientUtil {
	private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
	private final static  RequestConfig requestConfig;

	private final static int CONNECT_TIMEOUT=10*1000;//链接超时时间10s
	private final static int SOCKET_TIMEOUT=30*1000;//socket超时时间30s
	private final static int CONNECT_REQUEST_TIMEOUT=30*1000;//等待响应超时时间30s

	private final static Map<String,String> HTTP_PROXY_MAP;

	static{
		// http.proxy	{"proxy":"false","host":"192.168.3.101","port":"802"}
		String httpProxyStr= AppParamsUtil.getParamValue("http.proxy");
		RequestConfig.Builder builder=RequestConfig.custom();
		builder.setSocketTimeout(SOCKET_TIMEOUT)
				.setConnectTimeout(CONNECT_TIMEOUT)
				.setConnectionRequestTimeout(CONNECT_REQUEST_TIMEOUT);

		if (StringUtils.isNotEmpty(httpProxyStr)){
			HTTP_PROXY_MAP=JsonEncoder.DEFAULT.decode(httpProxyStr,Map.class);
			boolean proxy=Boolean.valueOf(HTTP_PROXY_MAP.get("proxy"));
			if (proxy){
				HttpHost proxyHttpHost = new HttpHost(HTTP_PROXY_MAP.get("host"), Integer.valueOf(HTTP_PROXY_MAP.get("port")));
				requestConfig =builder.setProxy(proxyHttpHost).build();
				logger.info("使用HTTP代理 Host：{}，Port：{}",HTTP_PROXY_MAP.get("host"),HTTP_PROXY_MAP.get("port"));
			}else {
				requestConfig= builder.build();
			}
		}else {
			HTTP_PROXY_MAP=new HashedMap();
			requestConfig= builder.build();
		}
	}
	

	/**
	 * 发起http post请求
	 * @param url
	 * @param map
	 * @param cls
	 * @return
	 * @throws Exception
	 */
	public static <T> T doPost(String url,Map<String,String> map,Class<T> cls){
		CloseableHttpClient httpClient= Client.SSLClient(url);
		HttpPost  httpPost = new HttpPost(url);
		httpPost.setConfig(requestConfig);

        //设置参数
        List<NameValuePair> list = new ArrayList<NameValuePair>();  
        for (Map.Entry<String, String> entry : map.entrySet()) {  
        	 list.add(new BasicNameValuePair(entry.getKey(),entry.getValue()));  
        }
        if(list.size() > 0){  
            UrlEncodedFormEntity entity=null;
			try {
				entity = new UrlEncodedFormEntity(list,"utf-8");
			} catch (UnsupportedEncodingException e) {
				logger.error("不支持的编码异常：{}",e);
				return  null;
			}
            httpPost.setEntity(entity);  
        }
		ResponseHandler<T> responseHandler=getPostResponseHandler(url,map,cls);
		try {
			T t=httpClient.execute(httpPost,responseHandler);
			return  t;
		} catch (IOException e) {
			logger.error("RPC-PROXY请求返回IO异常：{}",e);
			return null;
		}
    }

	/**
	 * rpc post 请求
	 * @param url
	 * @param map
	 * @return
	 */
	public static RpcResponse rpcPost(String url, Map<String,String> map){
		CloseableHttpClient httpClient = Client.SSLClient(url);
		HttpPost  httpPost = new HttpPost(url);
		httpPost.setConfig(requestConfig);
		//设置参数
		List<NameValuePair> list = new ArrayList<NameValuePair>();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			list.add(new BasicNameValuePair(entry.getKey(),entry.getValue()));
		}
		if(list.size() > 0){
			UrlEncodedFormEntity entity=null;
			try {
				entity = new UrlEncodedFormEntity(list,"utf-8");
			} catch (UnsupportedEncodingException e) {
				logger.error("不支持的编码异常：{}",e);
				return RpcResponse.failed(new Exception("不支持的编码异常"));
			}
			httpPost.setEntity(entity);
		}
		ResponseHandler<RpcResponse> responseHandler = getRpcResponseHandler(url,map);
		try {
			return httpClient.execute(httpPost,responseHandler);
		} catch (IOException e) {
			logger.error("RPC-PROXY请求返回IO异常：{}",e);
			return RpcResponse.failed(new Exception("RPC-PROXY请求返回IO异常"));
		}
	}


	private  static <T>   ResponseHandler<T> getPostResponseHandler(String url,Map<String,String> map,Class<T> cls){
		ResponseHandler<T> responseHandler = new ResponseHandler<T>() {
			@Override
			public T handleResponse(final HttpResponse response){
				if(response != null){
					HttpEntity resEntity = response.getEntity();
					if(resEntity != null){
						String result=null;
						try {
							result = EntityUtils.toString(resEntity,"utf-8");
						} catch (ParseException e) {
							logger.error("解析响应结果异常：{}",e);
							return null;
						} catch (IOException e) {
							logger.error("解析结果IO异常：{}",e);
							return null;
						}
						if(result.contains("COW Proxy")){//代理异常，重试请求

						}else {
							try {
								//T responseObj = (T) JsonEncoder.DEFAULT.decode(result, cls);
								T responseObj = (T) HessianEncoder.decodeStr(result,cls);//hession 协议
								return  responseObj;
							} catch (Exception e) {
								logger.error("序列化对象：{}到：{}类型，异常：{}",result,cls.getName(),e.getMessage());
								return  null;
							}
						}
					}
				}
				logger.error("返回结果为空");
				return  null;
			}
		};
		return  responseHandler;
	}


	private  static  ResponseHandler<RpcResponse> getRpcResponseHandler(String url, Map<String,String> map){
		ResponseHandler<RpcResponse> responseHandler = new ResponseHandler<RpcResponse>() {
			@Override
			public RpcResponse handleResponse(final HttpResponse response){
				if( null!=response){
					HttpEntity resEntity = response.getEntity();
					if(resEntity != null){
						String result=null;
						try {
							result = EntityUtils.toString(resEntity,"utf-8");
						} catch (ParseException e) {
							logger.error("解析响应结果异常：{}",e);
							return RpcResponse.failed(e);
						} catch (IOException e) {
							logger.error("解析响应IO异常：{}",e);
							return RpcResponse.failed(e);
						}
						try {
							return HessianEncoder.decodeStr(result,RpcResponse.class);
						} catch (Exception e) {
							try{
								e=JsonEncoder.DEFAULT.decode(result, Exception.class);
								logger.error("<-HTTP-WEB代理执行异常，异常描述：{},传入参数：{}",e,map.toString());
								return RpcResponse.failed(e);
							}catch (Exception ee){
								logger.error("<-请求HTTP-WEB代理服务器{}超时，错误描述：{},返回文本：{},传入参数：{}",HTTP_PROXY_MAP.get("host"),ee,result,map.toString());
								return RpcResponse.failed(ee);
							}
						}
					}
				}
				return RpcResponse.failed(new Exception("代理请求异常,服务端响应为空"));
			}
		};
		return  responseHandler;
	}



}
