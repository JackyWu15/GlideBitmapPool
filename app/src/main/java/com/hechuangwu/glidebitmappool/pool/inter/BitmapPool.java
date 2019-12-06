
package com.hechuangwu.glidebitmappool.pool.inter;

import android.graphics.Bitmap;

/**
 * Created by cwh on 2019/12/5 0005.
 * 功能: 策略接口
 */
public interface BitmapPool {

    int getMaxSize();

    void setSizeMultiplier(float sizeMultiplier);

    void put(Bitmap bitmap);

    Bitmap get(int width, int height, Bitmap.Config config);

    Bitmap getDirty(int width, int height, Bitmap.Config config);

    void clearMemory();

    void trimMemory(int level);
}

