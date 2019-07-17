package com.hui.zhang.spider.remoteing.netty.data;

import com.edb01.common.util.UUIDGenerator;

import java.io.Serializable;

public class RpcResponse implements Serializable  {
	public static RpcResponse INSTRANGE = new RpcResponse();
	private static final long serialVersionUID = 1L;
	private String requestId;
    private Throwable error;
    private String result;

	public String getRequestId() {
		return requestId;
	}
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
	public Throwable getError() {
		return error;
	}
	public void setError(Throwable error) {
		this.error = error;
	}
	private String validateBean;

	public String getValidateBean() {
		return validateBean;
	}

	public void setValidateBean(String validateBean) {
		this.validateBean = validateBean;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}
	public RpcResponse (){

	}
	public RpcResponse (String validateBean){
		this.validateBean = validateBean;
	}
	public  RpcResponse build(String validateBean){
		return new RpcResponse(validateBean);
	}

	public static RpcResponse failed(Exception error){
		RpcResponse rpcResponse=new RpcResponse();
		rpcResponse.setError(error);
		rpcResponse.setResult(null);
		rpcResponse.setValidateBean(null);
		rpcResponse.setRequestId(UUIDGenerator.random32UUID());
		return rpcResponse;
	}
}