package com.hui.zhang.spider.remoteing.netty.client.connector;

import io.netty.channel.Channel;


public abstract class AbstractConnector {

	/**
	 * 连接服务器
	 * @return
	 */
	public abstract Channel connect(String host, int port) throws Exception;

}
