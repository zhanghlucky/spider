package com.hui.zhang.spider.common.annotation;

import java.lang.annotation.*;

import org.springframework.stereotype.Component;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component // 表明可被 Spring 扫描
@Inherited
public @interface SpiderService {
	 //Class<?> cls();
	 String desc() default "";
	 String version() default "1.0.0";
}
