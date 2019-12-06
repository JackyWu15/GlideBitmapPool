package com.hechuangwu.glidebitmappool.pool;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

/**
 * Created by cwh on 2019/12/5 0005.
 * 功能:
 */
public class Util {

    /**
     * 根据位图获得其大小
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static int getBitmapByteSize(Bitmap bitmap) {
        if (bitmap.isRecycled()) {
            throw new IllegalStateException("Cannot obtain size for recycled Bitmap: " + bitmap
                    + "[" + bitmap.getWidth() + "x" + bitmap.getHeight() + "] " + bitmap.getConfig());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                return bitmap.getAllocationByteCount();
            } catch (NullPointerException e) {
            }
        }
        return bitmap.getHeight() * bitmap.getRowBytes();
    }

    /**
     * 位图总大小
     */
    public static int getBitmapByteSize(int width, int height, Bitmap.Config config) {
        return width * height * getBytesPerPixel(config);
    }

    /**
     * 每个像素点的大小，字节为单位
     */
    public static int getBytesPerPixel(Bitmap.Config config) {
        //在解析gif时，可能会为null
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }
        int bytesPerPixel;
        switch (config) {
            case ALPHA_8:
                bytesPerPixel = 1;
                break;
            case RGB_565:
            case ARGB_4444:
                bytesPerPixel = 2;
                break;
            case ARGB_8888:
            default:
                bytesPerPixel = 4;
                break;
        }
        return bytesPerPixel;
    }

    /**
     *  复用以4.4的sdk版本作为分界处理
     * 1，大于等于4.4：只要新的位图大小小于或等于复用的位图就可以复用
     * 2，小于4.4：新的位图和复用的位图宽高必须一致，而且inSampleSize必须为1
     * @param candidate 复用的位图
     * @param targetOptions 新申请的位图参数
     * @return
     */
    public static boolean canUseForInBitmap(Bitmap candidate, BitmapFactory.Options targetOptions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int width = targetOptions.outWidth / targetOptions.inSampleSize;
            int height = targetOptions.outHeight / targetOptions.inSampleSize;
            //新申请的位图大小
            int byteCount = width * height * getBytesPerPixel( candidate.getConfig() );
            try {
                //4.4以后通过getAllocationByteCount获取复用位图的大小
                return byteCount <= candidate.getAllocationByteCount();
            } catch (NullPointerException e) {
                //如果失败，重新计算
                return byteCount <= candidate.getHeight() * candidate.getRowBytes();
            }

        }

        //4.4之前的版本，宽高必须一致，而且inSampleSize为1，才能复用
        return candidate.getWidth() == targetOptions.outWidth
                && candidate.getHeight() == targetOptions.outHeight
                && targetOptions.inSampleSize == 1;

    }

    /**
     * 缩放位图
     * @param options 原始参数
     * @param reqWidth 需求的宽
     * @param reqHeight 需求的高
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * 创建一个队列
     */
    public static <T> Queue<T> createQueue(int size) {
        return new ArrayDeque<>(size);
    }

    /**
     * 是否相等
     */
    public static boolean bothNullOrEqual(Object a, Object b) {
        return Objects.equals( a, b );
    }
}
