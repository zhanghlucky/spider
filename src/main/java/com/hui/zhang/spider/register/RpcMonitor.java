package com.hui.zhang.spider.register;

import com.hui.zhang.spider.common.model.Address;
import com.hui.zhang.spider.common.model.ServiceMeta;
import com.hui.zhang.spider.exception.SpiderException;
import com.hui.zhang.spider.remoteing.netty.client.RpcClient;
import com.hui.zhang.spider.remoteing.netty.client.connector.SpiderConnector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.internal.ConcurrentSet;
import io.netty.util.internal.ThreadLocalRandom;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RPC监听器
 */
public class RpcMonitor {

	private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

	//通道map
	public static ConcurrentMap<Address, CopyOnWriteArrayList<Channel>>  PROVIDER_CHANNEL_MAP= new ConcurrentHashMap<>();

	//服务源数据map
	public static final ConcurrentMap<Address, ConcurrentSet<ServiceMeta>>  PROVIDER_META_MAP= new ConcurrentHashMap<>();

	//服务提供者当前连接数
	public static final ConcurrentMap<Address, AtomicInteger>  PROVIDER_CONN_QTY_MAP= new ConcurrentHashMap<>();

	//总服务列表
	public static final ConcurrentSet<ServiceMeta>   PROVIDER_META_TOTAL_SET= new ConcurrentSet<>();

	//获取channel重试次数
	private static final int CHANNEL_RETRY_TIMES = 3;

	//最大链接数
	private static final int MAX_CONN_QTY = 5;

	/**
	 * 保存channel
	 * @param address
	 * @param channel
	 */
	public static void addChannel(Address address, Channel channel) {
		CopyOnWriteArrayList<Channel> channels = PROVIDER_CHANNEL_MAP.get(address);
		if (channels == null) {
			CopyOnWriteArrayList<Channel> newChannels = new CopyOnWriteArrayList<>();
			channels = PROVIDER_CHANNEL_MAP.putIfAbsent(address, newChannels);
			if (channels == null) {
				channels = newChannels;
			}
		}
		channels.add(channel);
		logger.debug("add channel:{} server->host:{} port:{}",channel.id(),address.getHost(),address.getPort());
		channel.closeFuture().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				removeChannel(address, channel);
			}
		});
	}

	/**
	 * 获取channel
	 * @param address
	 * @return
	 */
	public static Channel getChannel(Address address) {
		Channel channel = null;
		CopyOnWriteArrayList<Channel> channels = PROVIDER_CHANNEL_MAP.get(address);
		if (channels != null && CollectionUtils.isNotEmpty(channels)) {
			int retryTime = 1;
			while (retryTime <= CHANNEL_RETRY_TIMES) {
				try {
					int size = channels.size();
					if(size==1){
						channel = channels.get(0);
					}else{
  						channel = channels.get(ThreadLocalRandom.current().nextInt(size));
					}
					//logger.info("get channel:{}",channel.id());
					return channel;
				} catch (Exception e) {
					e.printStackTrace();
					logger.error("get channel error:{} try times:{}",e.getMessage(), retryTime);
				}
				retryTime++;
			}
		}
		//递归调用
		if (channel == null) {
			logger.error("channel is null ,create new channel address:{}",address.getHost()+":"+address.getPort());
			initProviderChannel(address);
			channel=getChannel(address);
		}
		return channel;
	}

	/**
	 * 删除channel
	 * @param address
	 * @param channel
	 */
	public static void removeChannel(Address address, Channel channel) {
		CopyOnWriteArrayList<Channel> channels = PROVIDER_CHANNEL_MAP.get(address);
		if (CollectionUtils.isNotEmpty(channels)) {
			Iterator<Channel> iterator = channels.iterator();
			while (iterator.hasNext()) {
				Channel iteChannel = iterator.next();
				if (iteChannel.equals(channel)) {
					channels.remove(iteChannel);
					logger.debug("remove channel:{} server->host:{} port:{}",iteChannel.id(),address.getHost(),address.getPort());
					break;
				}
			}
		}
		PROVIDER_CONN_QTY_MAP.get(address).decrementAndGet();
	}

	/**
	 * 删除channel
	 * @param address
	 */
	public static void removeChannel(Address address) {
		CopyOnWriteArrayList<Channel> channels = PROVIDER_CHANNEL_MAP.get(address);
		if (CollectionUtils.isNotEmpty(channels)) {
			Iterator<Channel> iterator = channels.iterator();
			while (iterator.hasNext()) {
				Channel iteChannel = iterator.next();
				channels.remove(iteChannel);
				logger.info("remove channel:{} server->host:{} port:{}",iteChannel.id(),address.getHost(),address.getPort());
			}
		}
		PROVIDER_CONN_QTY_MAP.put(address, new AtomicInteger(0));
	}


	/**
	 * 添加provider
	 */
	public synchronized static void addProvider(Address address, ServiceMeta serviceMeta) {
		ConcurrentSet<ServiceMeta> serviceMetaSet = PROVIDER_META_MAP.get(address);
		if (!CollectionUtils.isNotEmpty(serviceMetaSet)) {
			ConcurrentSet<ServiceMeta> newServiceMetaSet = new ConcurrentSet<>();
			serviceMetaSet = PROVIDER_META_MAP.putIfAbsent(address, newServiceMetaSet);
			if (serviceMetaSet == null) {
				serviceMetaSet = newServiceMetaSet;
			}
			initProviderChannel(address);
		}
		serviceMetaSet.add(serviceMeta);
	}

	/**
	 * 初始化channel
	 * @param address
	 */
	private synchronized static  void initProviderChannel(Address address){
		AtomicInteger connCount = PROVIDER_CONN_QTY_MAP.get(address);
		if (connCount == null) {
			AtomicInteger newConnCount = new AtomicInteger(0);
			connCount = PROVIDER_CONN_QTY_MAP.putIfAbsent(address, newConnCount);
			if (connCount == null) {
				connCount = newConnCount;
			}
		}
		int count = MAX_CONN_QTY - connCount.get();
		for (int i = 0; i < count; i++) {
			Channel channle = new SpiderConnector().connect(address.getHost(), address.getPort());
			addChannel(address, channle);
			connCount.getAndIncrement();
		}
	}

	/**
	 *  删除提供者
	 * @param address
	 * @param serviceMeta
	 */
	public synchronized static void removeProvider(Address address, ServiceMeta serviceMeta) {
		ConcurrentSet<ServiceMeta> serviceMetaSet = PROVIDER_META_MAP.get(address);
		boolean flag = false;
		if (serviceMetaSet == null) {
			flag = true;
		} else {
			serviceMetaSet.remove(serviceMeta);
			if (serviceMetaSet.size() == 0) {
				flag = true;
			}
		}
		if (flag) {
			removeChannel(address);
		}
	}





	/**
	 * 添加服务
	 * @param serviceMeta
	 */
	public static void addService(ServiceMeta serviceMeta) {
		PROVIDER_META_TOTAL_SET.add(serviceMeta);
	}

	/**
	 * 删除服务
	 * @param serviceMeta
	 */
	public static void remove(ServiceMeta serviceMeta) {
		PROVIDER_META_TOTAL_SET.remove(serviceMeta);
	}


}
