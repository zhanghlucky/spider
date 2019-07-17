package com.hui.zhang.spider.register.zookeeper;

import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.hui.zhang.spider.common.serialize.HessianSerialize;
import com.hui.zhang.spider.exception.SpiderException;
import com.hui.zhang.spider.register.data.RpcProvider;
import io.netty.util.internal.ConcurrentSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

/**
 * zookeeper 注册中心
 * @author zhanghui
 *
 */
@Deprecated
public class ZkRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ZkRegistry.class);
    public final static ZkRegistry INSTANCE=new ZkRegistry();

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

    public ZkRegistry() {
        //处理注册信息
        registerExecutor.execute(() -> {
            RpcProvider rpcProvider = null;
            try {
                while (true) {
                    rpcProvider = providerQueue.take();
                    providerSet.add(rpcProvider);
                    register0(rpcProvider);
                }
            } catch (Throwable e) {
                e.printStackTrace();
                logger.error("ZkRegistry register exception:{},try again", e.getMessage());
                //每秒执行一次 重新注册失败的注册信息
                final RpcProvider finalRpcProvider = rpcProvider;
                registerScheduledExecutor.schedule(() -> {
                    providerQueue.add(finalRpcProvider);
                }, 1, TimeUnit.SECONDS);
            }
        });

        localRegisterWatchExecutor.execute(() -> {
            while (true) {
                try {
                    //每三秒检查一次
                    Thread.sleep(3000);
                    //遍历需要发布的服务， 如果断开 则重新注册
                    Iterator<RpcProvider> iterator = providerSet.iterator();
                    while (iterator.hasNext()) {
                        RpcProvider rpcProvider = iterator.next();
                        String path= SpiderZk.INSTANCE.getServiceNodePath(rpcProvider.getInterfaceName(),rpcProvider.getVersion(), rpcProvider.getAddress());
                        //需要重新注冊
                        if (SpiderZk.INSTANCE.client().checkExists().forPath(path) == null) {
                            register(rpcProvider);
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    logger.error("ZkRegistry check register status exception:{}", e.getMessage());
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
    public void register(RpcProvider rpcProvider){
        providerQueue.add(rpcProvider);
    }

    /**
     * 注册服务
     */
    private void register0(RpcProvider rpcProvider) {

        try {
            //创建服务临时节点
            String ephemeralNodePath= SpiderZk.INSTANCE.getServiceNodePath(rpcProvider.getInterfaceName(),rpcProvider.getVersion(), rpcProvider.getAddress());
            logger.info("create server ephemeral node path:{}",ephemeralNodePath);

            SpiderZk.INSTANCE.createNode(ephemeralNodePath, true);
            SpiderZk.INSTANCE
                    .client()
                    .setData().forPath(ephemeralNodePath, HessianSerialize.serialize(rpcProvider));

            //创建服务在线节点
            String nodePath= SpiderZk.INSTANCE.getServiceInfoPath(rpcProvider.getInterfaceName(),rpcProvider.getVersion());
            logger.info("create server node path:{}",nodePath);

            SpiderZk.INSTANCE.createNode(nodePath, true);
            SpiderZk.INSTANCE
                    .client()
                    .setData().forPath(nodePath, HessianSerialize.serialize(rpcProvider));

        } catch (Exception e) {
            e.printStackTrace();
            throw new SpiderException("spider register fail:{}", e);
        }


    }

    /**
     * 下线服务
     */
    public void offline(RpcProvider rpcProvider) {

        try {

            String ephemeralNodePath= SpiderZk.INSTANCE.getServiceNodePath(rpcProvider.getInterfaceName(),rpcProvider.getVersion(), rpcProvider.getAddress());
            logger.info("offline ephemeral node path:{}",ephemeralNodePath);
            SpiderZk.INSTANCE.client().delete().forPath(ephemeralNodePath);

            String nodePath= SpiderZk.INSTANCE.getServiceInfoPath(rpcProvider.getInterfaceName(),rpcProvider.getVersion());
            logger.info("offline node path:{}",nodePath);
            SpiderZk.INSTANCE.client().delete().forPath(nodePath);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SpiderException("spider offline fail:{}", e);
        }
    }

}