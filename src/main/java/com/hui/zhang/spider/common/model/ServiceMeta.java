package com.hui.zhang.spider.common.model;

/**
 * 服务源数据
 */
public class ServiceMeta {
	private String service;
	private String version;
	public ServiceMeta(String service, String version) {
		this.service = service;
		this.version = version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ServiceMeta that = (ServiceMeta) o;

		if (service != null ? !service.equals(that.service) : that.service != null) return false;
		return version != null ? version.equals(that.version) : that.version == null;
	}

	@Override
	public int hashCode() {
		int result = service != null ? service.hashCode() : 0;
		result = 31 * result + (version != null ? version.hashCode() : 0);
		return result;
	}
}
