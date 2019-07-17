package com.hui.zhang.spider.register.data;

import java.io.Serializable;
import java.util.List;

public class RpcProvider  implements Serializable  {
	private String appName;
	private String desc;
	private String interfaceName;
	private String version;
	private String address;
	private List<RpcMethod> rpcMethodList;

	public RpcProvider(){
		
	}
	
	public RpcProvider(String appName,String desc,String interfaceName,String version,List<RpcMethod> rpcMethodList,String address){
		this.appName=appName;
		this.desc=desc;
		this.interfaceName=interfaceName;
		this.version=version;
		this.address=address;
		this.rpcMethodList=rpcMethodList;
	}

	public List<RpcMethod> getRpcMethodList() {
		return rpcMethodList;
	}

	public void setRpcMethodList(List<RpcMethod> rpcMethodList) {
		this.rpcMethodList = rpcMethodList;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getInterfaceName() {
		return interfaceName;
	}
	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		RpcProvider that = (RpcProvider) o;

		if (appName != null ? !appName.equals(that.appName) : that.appName != null) return false;
		if (desc != null ? !desc.equals(that.desc) : that.desc != null) return false;
		if (interfaceName != null ? !interfaceName.equals(that.interfaceName) : that.interfaceName != null)
			return false;
		if (version != null ? !version.equals(that.version) : that.version != null) return false;
		if (address != null ? !address.equals(that.address) : that.address != null) return false;
		return rpcMethodList != null ? rpcMethodList.equals(that.rpcMethodList) : that.rpcMethodList == null;
	}

	@Override
	public int hashCode() {
		int result = appName != null ? appName.hashCode() : 0;
		result = 31 * result + (desc != null ? desc.hashCode() : 0);
		result = 31 * result + (interfaceName != null ? interfaceName.hashCode() : 0);
		result = 31 * result + (version != null ? version.hashCode() : 0);
		result = 31 * result + (address != null ? address.hashCode() : 0);
		result = 31 * result + (rpcMethodList != null ? rpcMethodList.hashCode() : 0);
		return result;
	}
}
