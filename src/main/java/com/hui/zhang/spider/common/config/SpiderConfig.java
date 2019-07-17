package com.hui.zhang.spider.common.config;

import com.edb01.common.util.PropertyUtil;
import com.edb01.common.util.docker.RegistHostPortUtil;
import com.edb01.common.util.etc.AppConfigUtil;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public final class SpiderConfig {
	public static final SpiderConfig INSTANCE=new SpiderConfig();
	private static final Map<String, Object> configMap = new HashMap<>();
	
	static{
		//zookeeper://127.0.0.1:2181?backup=127.0.0.1:2182,127.0.0.1:2183
		String zkHost= AppConfigUtil.getCfgEnvironmentPO().getZookeeperAddress();//zookeeper://192.168.3.53:2181
		zkHost=zkHost.replace("zookeeper://","");
		zkHost=zkHost.replace("?backup=",",");
		//String zkHost="192.168.3.53:2181";
		configMap.put("zk.host",zkHost);
		Integer zkTimeout=5000;//
		//TEST
		configMap.put("zk.timeout",zkTimeout);
		try {

			String host = RegistHostPortUtil.getRegistHost();
			int port= RegistHostPortUtil.getRegistProtocolPort();
			String addr = host + ":" + port;
			String addrDubbo = host + ":" + (port+10000);
			configMap.put("server.address",addr);
			configMap.put("server.dubbo.address",addrDubbo);
		}catch (Exception e){
			e.printStackTrace();
		}
	}


	public String getZkHost() {
		return configMap.get("zk.host").toString();
	}

	public int getZkTimeout() {
		return Integer.valueOf(configMap.get("zk.timeout").toString());
	}

	public String getServerAddress() {
		return configMap.get("server.address").toString();
	}

	public String getServerAddressForDubbo() {
		return configMap.get("server.dubbo.address").toString();
	}

}
