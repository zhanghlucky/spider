package com.hui.zhang.spider.common.annotation;

import java.lang.annotation.*;

/**
 * Created by zhanghui on 2017/12/26.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER,ElementType.FIELD})
@Inherited
public @interface Sparam {
    boolean required() default false;
    int  length() default 0;
    String code() default "100000";
    String msg() default  "参数异常";
    int min() default 0;
    int max() default 0;
    String regexp() default "";
}
