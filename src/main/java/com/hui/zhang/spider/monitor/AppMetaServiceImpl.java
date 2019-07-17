package com.hui.zhang.spider.monitor;

import com.edb01.common.util.docker.RegistHostPortUtil;
import com.edb01.common.util.etc.AppConfigUtil;
import com.edb01.core.result.RpcResult;
import com.hui.zhang.spider.common.annotation.SpiderMethod;
import com.hui.zhang.spider.common.annotation.SpiderWebService;
import com.hui.zhang.spider.monitor.model.AppMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by zhanghui on 2018-12-17.
 */
@SpiderWebService
@Service("appMetaService")
@com.alibaba.dubbo.config.annotation.Service(version = "1.0.0",timeout = 5000)
public class AppMetaServiceImpl implements  AppMetaService {
    private static final Logger logger = LoggerFactory.getLogger(AppMetaServiceImpl.class);

    @Override
    @SpiderMethod(tokenCheck = false, interfaceCheck = false, timeout = 5000)
    public RpcResult<AppMeta> getAppMeta() {
        RpcResult<AppMeta> rpcResult=null;
        try{
            String appName=AppConfigUtil.getCfgAppPO().getAppName();
            String appTitle=AppConfigUtil.getCfgAppPO().getAppTitle();
            String webName=AppConfigUtil.getCfgAppPO().getWebName();
            Integer webPort=AppConfigUtil.getCfgAppPO().getWebPort();
            String envId=AppConfigUtil.getCfgEnvironmentPO().getEnvId();
            String ip= RegistHostPortUtil.getRegistHost();

            AppMeta appMeta=new AppMeta();
            appMeta.setAppName(appName);
            appMeta.setAppTitle(appTitle);
            appMeta.setWebName(webName);
            appMeta.setWebPort(webPort);
            appMeta.setEnvId(envId);
            appMeta.setIp(ip);
            rpcResult=new RpcResult<>(true,200,"success",appMeta);
        }catch (Exception e){
            logger.error("get app meta error:{}",e.getMessage());
            return new RpcResult<>(false,400L,"get app meta error",null);
        }
        return rpcResult;
    }
}
