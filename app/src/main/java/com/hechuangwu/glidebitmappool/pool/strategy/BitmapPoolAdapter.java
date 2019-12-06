package com.hechuangwu.glidebitmappool.pool.strategy;

import android.graphics.Bitmap;

import com.hechuangwu.glidebitmappool.pool.inter.BitmapPool;

/**
 * Created by cwh on 2019/12/5 0005.
 * 功能: 3.0以下适配
 */
public class BitmapPoolAdapter implements BitmapPool {
    @Override
    public int getMaxSize() {
        return 0;
    }

    @Override
    public void setSizeMultiplier(float sizeMultiplier) {

    }

    @Override
    public void put(Bitmap bitmap) {

    }

    @Override
    public Bitmap get(int width, int height, Bitmap.Config config) {
        return Bitmap.createBitmap(width, height, config);
    }

    @Override
    public Bitmap getDirty(int width, int height, Bitmap.Config config) {
        return get(width, height, config);
    }

    @Override
    public void clearMemory() {

    }

    @Override
    public void trimMemory(int level) {

    }
}
