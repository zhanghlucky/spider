package com.hui.zhang.spider.monitor;

import com.edb01.core.result.RpcResult;
import com.hui.zhang.spider.monitor.model.AppMeta;

/**
 * Created by zhanghui on 2018-12-17.
 */
public interface AppMetaService {
    /**
     * 获得程序描述
     * @return
     */
    public RpcResult<AppMeta> getAppMeta();
}
