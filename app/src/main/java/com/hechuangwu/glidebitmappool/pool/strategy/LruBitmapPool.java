package com.hechuangwu.glidebitmappool.pool.strategy;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import com.hechuangwu.glidebitmappool.pool.inter.BitmapPool;
import com.hechuangwu.glidebitmappool.pool.inter.LruPoolStrategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by cwh on 2019/12/5 0005.
 * 功能:
 */
public class LruBitmapPool implements BitmapPool {
    private static final String TAG = LruBitmapPool.class.getSimpleName();
    private final LruPoolStrategy strategy;
    private static final Bitmap.Config DEFAULT_CONFIG = Bitmap.Config.ARGB_8888;//默认像素格式
    private final int initialMaxSize;
    private final Set<Bitmap.Config> allowedConfigs;
    private final BitmapTracker tracker;//位图跟踪
    private int misses;
    private int maxSize;//缓存最大容量
    private int currentSize;//已使用容量
    private int hits;
    private int puts;//缓存的个数
    private int evictions;


    private LruBitmapPool(int maxSize, LruPoolStrategy strategy, Set<Bitmap.Config> allowedConfigs) {
        this.initialMaxSize = maxSize;
        this.maxSize = maxSize;
        this.strategy = strategy;
        this.allowedConfigs = allowedConfigs;
        this.tracker = new NullBitmapTracker();
    }
    public LruBitmapPool(int maxSize, Set<Bitmap.Config> allowedConfigs) {
        this(maxSize, getDefaultStrategy(), allowedConfigs);
    }

    public LruBitmapPool(int maxSize) {
        this(maxSize, getDefaultStrategy(), getDefaultAllowedConfigs());
    }

    /**
     * 看canUseForInBitmap的注释，以4.4分界做兼容
     */
    private static LruPoolStrategy getDefaultStrategy() {
        final LruPoolStrategy strategy;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            strategy = new SizeConfigStrategy();
        } else {
            strategy = new AttributeStrategy();
        }
        return strategy;
    }

    /**
     * 保存允许复用的像素格式
     */
    private static Set<Bitmap.Config> getDefaultAllowedConfigs() {
        Set<Bitmap.Config> configs = new HashSet<>();
        configs.addAll( Arrays.asList(Bitmap.Config.values()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            configs.add(null);
        }
        //Bitmap.Config.HARDWARE是8.0之后才有的，不允许复用
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            configs.remove(Bitmap.Config.HARDWARE);
        }
        return Collections.unmodifiableSet(configs);
    }

    @Override
    public int getMaxSize() {
        return maxSize;
    }

    //重置
    @Override
    public void setSizeMultiplier(float sizeMultiplier) {
        maxSize = Math.round(initialMaxSize * sizeMultiplier);
        evict();
    }


    /**
     * 获取干净的缓存位图
     */
    @Override
    public Bitmap get(int width, int height, Bitmap.Config config) {
        Bitmap result = getDirtyOrNull(width, height, config);
        if(result!=null){
            //将位图的图案擦除
            result.eraseColor( Color.TRANSPARENT );
        }else {
            result = Bitmap.createBitmap(width, height, config);
        }

        return result;
    }

    /**
     * 获取没有擦除数据的缓存位图
     */
    @Override
    public Bitmap getDirty(int width, int height, Bitmap.Config config) {
        Bitmap result = getDirtyOrNull(width, height, config);
        if (result == null) {
            result = Bitmap.createBitmap(width, height, config);
        }
        return result;
    }

    /**
     * 根据策略获取
     */
    private synchronized Bitmap getDirtyOrNull(int width, int height, Bitmap.Config config) {
        //获取缓存的位图
        final Bitmap result = strategy.get(width, height, config != null ? config : DEFAULT_CONFIG);
        //没有获取到缓存的位图
        if(result==null){
            Log.d(TAG, "Missing bitmap=" + strategy.logBitmap(width, height, config));
            misses++;
        }else {

        }
        Log.i(TAG, "Get bitmap=" + strategy.logBitmap(width, height, config));
        dump();

        return result;
    }


    /**
     * 回收当前位图
     */
    @Override
    public void put(Bitmap bitmap) {
        if (bitmap == null) {
            throw new NullPointerException("Bitmap must not be null");
        }
        if (bitmap.isRecycled()) {
            throw new IllegalStateException("Cannot pool recycled bitmap");
        }

        //不满足回收条件
        if (!bitmap.isMutable() || strategy.getSize(bitmap) > maxSize
                || !allowedConfigs.contains(bitmap.getConfig())) {
            Log.i(TAG, "Reject bitmap from pool"
                    + ", bitmap: " + strategy.logBitmap(bitmap)
                    + ", is mutable: " + bitmap.isMutable()
                    + ", is allowed config: " + allowedConfigs.contains(bitmap.getConfig()));
            bitmap.recycle();
            return;
        }

        int size = strategy.getSize( bitmap );
        strategy.put(bitmap);
        tracker.add(bitmap);
        puts++;//计数加1
        currentSize += size;//容量增加
        Log.i(TAG, "Put bitmap in pool=" + strategy.logBitmap(bitmap));
        dump();
        evict();
    }




    /**
     * 超过最大缓存容量进行移除回收
     */
    private void evict() {
        trimToSize(maxSize);
    }



    /**
     * 清理缓存
     */
    @Override
    public void clearMemory() {
        trimToSize(0);
    }

    /**
     * 根据当前内存情况的级别清理，否则系统会重启我们的程序
     */
    @Override
    public void trimMemory(int level) {
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            clearMemory();
        } else if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            trimToSize(maxSize / 2);
        }
    }



    /**
     * 是否已超过指定的容量
     */
    private synchronized void trimToSize(int size) {
        while (currentSize>size){
            Bitmap removed = strategy.removeLast();
            if (removed == null) {
                dump();
                currentSize = 0;
                return;
            }
            tracker.remove(removed);
            //去掉移除位图的占用容量
            currentSize -= strategy.getSize( removed );
            evictions++;
            dump();
            removed.recycle();
        }
    }



    private void dump() {
        Log.i(TAG, "Hits=" + hits + ", misses=" + misses + ", puts=" + puts + ", evictions=" + evictions
                + ", currentSize=" + currentSize + ", maxSize=" + maxSize + "\nStrategy=" + strategy);
    }
    private interface BitmapTracker {
        void add(Bitmap bitmap);
        void remove(Bitmap bitmap);
    }

    private static class NullBitmapTracker implements BitmapTracker {
        @Override
        public void add(Bitmap bitmap) {
            // Do nothing.
        }

        @Override
        public void remove(Bitmap bitmap) {
            // Do nothing.
        }
    }
}
