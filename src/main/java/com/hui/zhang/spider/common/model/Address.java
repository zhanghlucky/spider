package com.hui.zhang.spider.common.model;

/**
 * Created by zuti on 2018/3/3.
 * email zuti@centaur.cn
 */
public class Address {

	public static final Address EMPTY = null;
	private String host;
	private int port;

	public Address(String host, int port) {
		this.host = host;
		this.port = port;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Address address = (Address) o;

		if (port != address.port) return false;
		return host != null ? host.equals(address.host) : address.host == null;
	}

	@Override
	public int hashCode() {
		int result = host != null ? host.hashCode() : 0;
		result = 31 * result + port;
		return result;
	}

	public static Address of(String host, int port) {
		return new Address(host, port);
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}
}
