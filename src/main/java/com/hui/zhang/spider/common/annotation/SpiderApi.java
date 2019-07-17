package com.hui.zhang.spider.common.annotation;

/**
 * Created by zhanghui on 2017/11/14.
 */
public @interface SpiderApi {
    String version() default "";
    String address() default "";
    String envGroup() default "";

}
