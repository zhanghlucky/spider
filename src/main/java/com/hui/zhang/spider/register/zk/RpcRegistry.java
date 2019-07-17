package com.hui.zhang.spider.register.zk;

import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.edb01.common.util.MD5Util;
import com.hui.zhang.spider.common.serialize.HessianSerialize;
import com.hui.zhang.spider.exception.SpiderException;
import com.hui.zhang.spider.register.data.ProviderNode;
import com.hui.zhang.spider.register.data.RpcProvider;
import io.netty.util.internal.ConcurrentSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * zookeeper 注册中心
 * @author zhanghui
 *
 */
public class RpcRegistry {
	private static final Logger logger = LoggerFactory.getLogger(RpcRegistry.class);
	//服务提供者队列
	private final LinkedBlockingQueue<RpcProvider> providerQueue = new LinkedBlockingQueue<>();
	//服务提供者线程池
	private final ExecutorService registerExecutor =
			Executors.newSingleThreadExecutor(new NamedThreadFactory("provider executor"));
	//调度器
	private final ScheduledExecutorService registerScheduledExecutor =
			Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("provider schedule executor"));
	//服务提供者集合
	private final ConcurrentSet<RpcProvider> providerSet = new ConcurrentSet<>();

	//检查注册情况线程池
	private final ExecutorService localRegisterWatchExecutor =
			Executors.newSingleThreadExecutor(new NamedThreadFactory("provider check executor"));

	//private static volatile RpcRegistry instance;
	//private volatile CuratorFramework zkClient;

	private String zkAddress;
	private static final Map<String, RpcRegistry> REGISTRY_MAP = new ConcurrentHashMap<>();


	public RpcRegistry(String zkAddress){
		/*if(StringUtils.isNotEmpty(zkAddress)){
			zkClient=RpcZk.DEFAULT.connect(zkAddress);
		}else{
			zkClient=RpcZk.DEFAULT.connect();
		}*/
		this.zkAddress=zkAddress;
		this.takeProviderQueue();
	}

	public synchronized static RpcRegistry instance(String zkAddress) {
		/*if (instance == null) {
			synchronized (RpcRegistry.class) {
				if (instance == null) {
					instance = new RpcRegistry(zkAddress);
				}
			}
		}
		return instance;*/

		String key= MD5Util.MD5(zkAddress);
		RpcRegistry rpcRegistry=REGISTRY_MAP.get(key);
		if (null==rpcRegistry){
			rpcRegistry=new RpcRegistry(zkAddress);
			REGISTRY_MAP.put(key,rpcRegistry);
		}
		return rpcRegistry;

	}

	public RpcRegistry() {

	}

	private  void takeProviderQueue() {
		//处理注册信息
		registerExecutor.execute(() -> {
			RpcProvider rpcProvider = null;
			try {
				while (true) {
					rpcProvider = providerQueue.take();
					providerSet.add(rpcProvider);
					registerToZk(rpcProvider);
				}
			} catch (Throwable e) {
				logger.error("RpcRegistry register exception:{},try again", e.getMessage());
				//每秒执行一次 重新注册失败的注册信息
				final RpcProvider finalRpcProvider = rpcProvider;
				registerScheduledExecutor.schedule(() -> {
					this.register(finalRpcProvider);
				}, 1, TimeUnit.SECONDS);
			}
		});

		localRegisterWatchExecutor.execute(() -> {
			while (true) {
				try {
					Thread.sleep(5000);
					Iterator<RpcProvider> iterator = providerSet.iterator();
					while (iterator.hasNext()) {
						RpcProvider rpcProvider = iterator.next();
						String ephemeralNodePath=RpcPathTool.getServiceNodePath(rpcProvider.getInterfaceName(),rpcProvider.getVersion(), rpcProvider.getAddress());
						if ( RpcZk.instance().connect(zkAddress).checkExists().forPath(ephemeralNodePath) == null) {
							this.register(rpcProvider);
						}
					}
				} catch (Throwable e) {
					logger.error("RpcRegistry check register status exception:{}", e.getMessage());
				}
			}
		});
	}

	/**
	 * 注册服务
	 * @param rpcProviderList
	 */
	public void register(List<RpcProvider> rpcProviderList){
		for (RpcProvider rpcProvider : rpcProviderList) {
			register(rpcProvider);
		}
	}

	/**
	 * 注册服务
	 * @param rpcProvider
	 */
	public void register(RpcProvider rpcProvider){
		providerQueue.add(rpcProvider);
	}

	/**
	 * 注册服务
	 * @param rpcProvider
	 */
    private void registerToZk(RpcProvider rpcProvider) {
		try {
			//创建服务临时节点
			String ephemeralNodePath=RpcPathTool.getServiceNodePath(rpcProvider.getInterfaceName(),rpcProvider.getVersion(), rpcProvider.getAddress());
			logger.info("create server ephemeral node path:{}",ephemeralNodePath);

			CuratorFramework client=RpcZk.instance().connect(zkAddress);

			this.createZkNode(ephemeralNodePath, true);
			byte [] dataByte=HessianSerialize.serialize(rpcProvider);
			//logger.info("$$$$$$$${}##{}",ephemeralNodePath,dataByte);
			client.setData().forPath(ephemeralNodePath, dataByte);

			//创建服务在线节点
			//String nodePath= RpcPathTool.getServiceInfoPath(rpcProvider.getInterfaceName(),rpcProvider.getVersion());
			//logger.info("create server node path:{}",nodePath);

			//this.createZkNode(nodePath, true);
			//client.setData().forPath(nodePath, HessianSerialize.serialize(rpcProvider));


		} catch (Exception e) {
			logger.error("register error:{}",e.getMessage());
			throw new SpiderException("spider register fail:{}", e);
		}
	}

	/**
	 * 下线服务
	 * @param rpcProvider
	 */
	public void offline(ProviderNode rpcProvider) {
		try {
			String ephemeralNodePath= RpcPathTool.getServiceNodePath(rpcProvider.getInterfaceName(),rpcProvider.getVersion(), rpcProvider.getAddress());
			Stat stat = SpiderRpc.instance().zkClient().checkExists().forPath(ephemeralNodePath);
			if (null!=stat){
				RpcZk.instance().connect(zkAddress).delete().forPath(ephemeralNodePath);
			}
			//String nodePath= RpcPathTool.getServiceInfoPath(rpcProvider.getInterfaceName(),rpcProvider.getVersion());
			//RpcZk.instance().connect(zkAddress).delete().forPath(nodePath);
		} catch (Exception e) {
			logger.error("offline error:{}",e.getMessage());
			e.printStackTrace();
			//throw new SpiderException("spider offline fail:{}", e);
		}
	}

	/**
	 *  创建服务节点
	 * @param path
	 * @param ephemeral
	 * @throws Exception
	 */
	public void createZkNode(String path, boolean ephemeral) throws Exception {
		int i = path.lastIndexOf('/');
		if (i > 0) {//递归创建目录节点
			this.createZkNode(path.substring(0, i), false);
		}
		if (ephemeral) {
			Stat stat =  RpcZk.instance().connect(zkAddress).checkExists().forPath(path);
			if(stat == null){
				RpcZk.instance().connect(zkAddress).create().withMode(CreateMode.EPHEMERAL).forPath(path);
			}
		} else {
			Stat stat =  RpcZk.instance().connect(zkAddress).checkExists().forPath(path);
			if (stat == null) {
				RpcZk.instance().connect(zkAddress).create().withMode(CreateMode.PERSISTENT).forPath(path);
			}
		}
	}
}