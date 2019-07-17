package com.hui.zhang.spider.common.annotation;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

/**
 * Created by zhanghui on 2017/12/29.
 * Spider Method
 */
@Target({ METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface SpiderMethod {
        boolean beanType() default false;//声明方法参数是否为实体类，约定一个实体类参数
        boolean isGeneric() default  false; //生命方法体参数实体是否包含泛型
        int sort() default 100;//方法排序
        boolean tokenCheck() default true;      //是否token校验
        boolean interfaceCheck() default true;  //是否接口权限校验
        long timeout() default 30*1000L;  //接口超时时间 单位 毫秒 30s
        boolean isSpiderWebService() default  true; //  是否生成spider 接口
        boolean isSpiderLock() default  false;// 是否添加分布式锁
}
