package com.hui.zhang.spider.remoteing.netty.data;


import com.hui.zhang.spider.common.beanutil.JavaBeanDescriptor;
import com.hui.zhang.spider.common.beanutil.JavaBeanParam;

import java.io.Serializable;
import java.util.List;

public class RpcRequest implements Serializable {
	private static final long serialVersionUID = 1554L;
	
	private String requestId;
    private String className;
    private String version;
    private String methodName;
    private int methodIndex;
    private JavaBeanParam[] parameterTypes;
    private String[] parameters;
    private JavaBeanDescriptor returnType;
    //private List<JavaBeanDescriptor> genericReturnTypeList;
	//private RpcReturnType rpcReturnType;
	private boolean isBeanType;
	private long timeout;
	private String proxyEnvId;


	public String getProxyEnvId() {
		return proxyEnvId;
	}

	public void setProxyEnvId(String proxyEnvId) {
		this.proxyEnvId = proxyEnvId;
	}

	public boolean isBeanType() {
		return isBeanType;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public void setBeanType(boolean beanType) {
		isBeanType = beanType;
	}


	public int getMethodIndex() {
		return methodIndex;
	}

	public void setMethodIndex(int methodIndex) {
		this.methodIndex = methodIndex;
	}

	public JavaBeanDescriptor getReturnType() {
		return returnType;
	}

	public void setReturnType(JavaBeanDescriptor returnType) {
		this.returnType = returnType;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getRequestId() {
		return requestId;
	}
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public String getMethodName() {
		return methodName;
	}
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public JavaBeanParam[] getParameterTypes() {
		return parameterTypes;
	}

	public void setParameterTypes(JavaBeanParam[] parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	public String[] getParameters() {
		return parameters;
	}

	public void setParameters(String[] parameters) {
		this.parameters = parameters;
	}

	/*public RpcReturnType getRpcReturnType() {
		return rpcReturnType;
	}

	public void setRpcReturnType(RpcReturnType rpcReturnType) {
		this.rpcReturnType = rpcReturnType;
	}*/
}