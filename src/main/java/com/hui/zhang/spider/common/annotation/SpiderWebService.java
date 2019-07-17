package com.hui.zhang.spider.common.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Created by zuti on 2017/12/28.
 * email zuti@centaur.cn
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component // 表明可被 Spring 扫描
@Inherited
public @interface SpiderWebService {
}
