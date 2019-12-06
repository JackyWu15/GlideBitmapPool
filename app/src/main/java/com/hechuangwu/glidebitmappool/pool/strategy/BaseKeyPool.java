package com.hechuangwu.glidebitmappool.pool.strategy;

import com.hechuangwu.glidebitmappool.pool.Util;
import com.hechuangwu.glidebitmappool.pool.inter.Poolable;

import java.util.Queue;

/**
 * Created by cwh on 2019/12/5 0005.
 * 功能: 4.4以上的Key缓存池
 */
abstract class BaseKeyPool<T extends Poolable> {
    private static final int MAX_SIZE = 20;//默认缓存20个Key
    private final Queue<T> keyPool = Util.createQueue(MAX_SIZE);//用一个数组队列维护

    /**
     * 获取
     */
    protected T get(){
        //返回第一个，并删除
        T poll = keyPool.poll();
        if(poll==null){
            poll = create();
        }
        return poll;
    }

    /**
     * 添加
     */
    public void offer(T key) {
        if (keyPool.size() < MAX_SIZE) {
            keyPool.offer(key);
        }
    }

    protected abstract T create();



}
