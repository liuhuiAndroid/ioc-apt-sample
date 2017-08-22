package com.zhy.ioc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by zhy on 16/4/22.
 *
 * 注解模块的实现
 *
 * SOURCE：标记一些信息
 * RUNTIME：运行时动态处理
 * CLASS：编译时动态处理
 */

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface Bind {
    int value();
}

