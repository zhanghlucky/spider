package com.hui.zhang.spider.future;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by zuti on 2018/2/27.
 * email zuti@centaur.cn
 */
public class InvokeFuture extends AbstractFuture {

	public static final ConcurrentMap<String, InvokeFuture> futures = new ConcurrentHashMap<>();

}
