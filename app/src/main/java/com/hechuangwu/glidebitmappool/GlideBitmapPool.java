package com.hechuangwu.glidebitmappool;

import android.graphics.Bitmap;
import android.os.Build;

import com.hechuangwu.glidebitmappool.pool.inter.BitmapPool;
import com.hechuangwu.glidebitmappool.pool.strategy.BitmapPoolAdapter;
import com.hechuangwu.glidebitmappool.pool.strategy.LruBitmapPool;


/**
 * Created by cwh on 2019/12/5 0005.
 * 功能:
 */
public class GlideBitmapPool {
    private static final int DEFAULT_MAX_SIZE = 6 * 1024 * 1024;//默认分配6M
    private static GlideBitmapPool sInstance;
    private BitmapPool bitmapPool;
    private GlideBitmapPool(int maxSize) {
        //3.0及以上才能使用复用，使用Lru策略
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            bitmapPool = new LruBitmapPool(maxSize);
        } else {
            //3.0以下不能复用，所以使用原来的方式直接创建
            bitmapPool = new BitmapPoolAdapter();
        }
    }

    private static GlideBitmapPool getInstance() {
        if (sInstance == null) {
            sInstance = new GlideBitmapPool(DEFAULT_MAX_SIZE);
        }
        return sInstance;
    }

    /**
     * 初始化内存大小
     */
    public static void initialize(int maxSize) {
        sInstance = new GlideBitmapPool(maxSize);
    }


    /**
     * 获取干净的位图
     */
    public static Bitmap getBitmap(int width, int height, Bitmap.Config config) {
        return getInstance().bitmapPool.get(width, height, config);
    }

    /**
     * 获取有图案的位图
     */
    public static Bitmap getDirtyBitmap(int width, int height, Bitmap.Config config) {
        return getInstance().bitmapPool.getDirty(width, height, config);
    }

    /**
     * 回收放入缓存池
     */
    public static void putBitmap(Bitmap bitmap) {
        getInstance().bitmapPool.put(bitmap);
    }


    /**
     * 清理缓存
     */
    public static void clearMemory() {
        getInstance().bitmapPool.clearMemory();
    }

    /**
     * 根据当前内存情况的级别清理，否则系统会重启我们的程序
     */
    public static void trimMemory(int level) {
        getInstance().bitmapPool.trimMemory(level);
    }

    /**
     * 关闭掉工具
     */
    public static void shutDown() {
        if (sInstance != null) {
            sInstance.bitmapPool.clearMemory();
            sInstance = null;
        }
    }




}
