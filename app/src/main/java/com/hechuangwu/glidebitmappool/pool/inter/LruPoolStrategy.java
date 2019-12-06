
package com.hechuangwu.glidebitmappool.pool.inter;

import android.graphics.Bitmap;

/**
 * Created by cwh on 2019/12/5 0005.
 * 功能: Lru的策略接口
 */
public interface LruPoolStrategy {
    void put(Bitmap bitmap);

    Bitmap get(int width, int height, Bitmap.Config config);

    Bitmap removeLast();

    String logBitmap(Bitmap bitmap);

    String logBitmap(int width, int height, Bitmap.Config config);

    int getSize(Bitmap bitmap);
}