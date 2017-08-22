package com.zhy.ioc;

/**
 * Created by zhy on 16/4/22.
 */
public interface ViewInject<T> {
    // 第一个参数为宿主对象，第二个参数为实际调用findViewById的对象
    void inject(T t, Object source);
}
