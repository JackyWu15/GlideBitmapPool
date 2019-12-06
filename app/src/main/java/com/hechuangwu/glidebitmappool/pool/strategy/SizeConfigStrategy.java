package com.hechuangwu.glidebitmappool.pool.strategy;

import android.graphics.Bitmap;

import com.hechuangwu.glidebitmappool.pool.Util;
import com.hechuangwu.glidebitmappool.pool.inter.LruPoolStrategy;
import com.hechuangwu.glidebitmappool.pool.inter.Poolable;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Created by cwh on 2019/12/5 0005.
 * 功能: 4.4以上的处理，只管大小和像素格式
 */
public class SizeConfigStrategy implements LruPoolStrategy {
    private static final String TAG = SizeConfigStrategy.class.getSimpleName();
    //允许复用的像素格式
    private static final Bitmap.Config[] ARGB_8888_IN_CONFIGS =  new Bitmap.Config[]{Bitmap.Config.ARGB_8888,null,};//null用于隐藏属性
    private static final Bitmap.Config[] RGB_565_IN_CONFIGS = new Bitmap.Config[]{Bitmap.Config.RGB_565};
    private static final Bitmap.Config[] ARGB_4444_IN_CONFIGS = new Bitmap.Config[]{Bitmap.Config.ARGB_4444};
    private static final Bitmap.Config[] ALPHA_8_IN_CONFIGS = new Bitmap.Config[]{Bitmap.Config.ALPHA_8};

    private final KeyPool keyPool = new KeyPool(); //Key池，用于对象复用
//    private static final int MAX_SIZE_MULTIPLE = 8;
    private static final int MAX_SIZE_MULTIPLE = 10;//glide源码默认为8倍，但在锥子手机上发现，系统生成的位图是通过宽高计算出来的9倍(6M多)，无法复用
    private final Map<Bitmap.Config, NavigableMap<Integer, Integer>> sortedSizes = new HashMap<>();//同一个格式，同大小的个数
    private final GroupedLinkedMap<Key, Bitmap> groupedMap = new GroupedLinkedMap<>();//位图池


    /**
     * 缓存位图
     */
    @Override
    public void put(Bitmap bitmap) {
        int size = Util.getBitmapByteSize(bitmap);
        Key key = keyPool.get( size, bitmap.getConfig() );
        //缓存起来
        groupedMap.put( key,bitmap );
        //根据系统生成的位图大小，进行缓存
        NavigableMap<Integer, Integer> sizes = getSizesForConfig(bitmap.getConfig());
        Integer current = sizes.get(key.size);
        sizes.put(key.size, current == null ? 1 : current + 1);
    }




    /**
     * 获取位图
     */
    @Override
    public Bitmap get(int width, int height, Bitmap.Config config) {
        //新的位图大小
        int size = Util.getBitmapByteSize( width, height, config );
        //获取缓存格式
        Key bestKey = findBestKey( size, config );
        //获取缓存位图
        Bitmap result = groupedMap.get( bestKey );
        //有缓存
        if(result!=null){
            decrementBitmapOfSize(bestKey.size, result);
            //重置下属性
            result.reconfigure(width, height, result.getConfig() != null ? result.getConfig() : Bitmap.Config.ARGB_8888);
        }
        return result;
    }

    /**
     * 位图的大小
     */
    @Override
    public int getSize(Bitmap bitmap) {
        return Util.getBitmapByteSize(bitmap);
    }


    /**
     * 查找缓存池是否有缓存，没有就创建并保存
     */
    private Key findBestKey(int size, Bitmap.Config config) {
        //从缓存池获取第一个Key
        Key key = keyPool.get( size, config );
        for (Bitmap.Config possibleConfig : getInConfigs(config)) {
            NavigableMap<Integer, Integer> sizesForPossibleConfig = getSizesForConfig(possibleConfig);
            //ceilingKey返回的是大于或等于size的最小值，也就是说返回的复用位图，要比当前的大才能用
            Integer possibleSize = sizesForPossibleConfig.ceilingKey( size );
            //但不能大于10倍
            if (possibleSize != null && possibleSize <= size * MAX_SIZE_MULTIPLE) {
                //如果不相等，说明这个大小的没有缓存过，那就加到缓存池中
                if (possibleSize != size|| (!Util.bothNullOrEqual( possibleConfig, config ))) {
                    keyPool.offer( key );
                    key = keyPool.get( possibleSize,possibleConfig );
                }
            }
        }
        return key;

    }

    /**
     * 以config为健，TreeMap为值保存
     */
    private NavigableMap<Integer, Integer> getSizesForConfig(Bitmap.Config config) {
        NavigableMap<Integer, Integer> sizes = sortedSizes.get(config);
        if (sizes == null) {
            sizes = new TreeMap<>();
            sortedSizes.put(config, sizes);
        }
        return sizes;
    }

    @Override
    public Bitmap removeLast() {
        Bitmap removed = groupedMap.removeLast();
        if(removed!=null){
            int removedSize = Util.getBitmapByteSize(removed);
            decrementBitmapOfSize(removedSize, removed);
        }
        return null;
    }


    /**
     * 减少这个像素格式的缓存数量
     */
    private void decrementBitmapOfSize(Integer size, Bitmap removed) {
        Bitmap.Config config = removed.getConfig();
        NavigableMap<Integer, Integer> sizes = getSizesForConfig(config);
        Integer current = sizes.get(size);
        if (current == null) {
            throw new NullPointerException("Tried to decrement empty size"
                    + ", size: " + size
                    + ", removed: " + logBitmap(removed)
                    + ", this: " + this);
        }

        if(current==1){
            sizes.remove( size );
        }else {
            sizes.put( size,current-1 );
        }

    }

    @Override
    public String logBitmap(Bitmap bitmap) {
        int size = Util.getBitmapByteSize(bitmap);
        return getBitmapString(size, bitmap.getConfig());
    }

    @Override
    public String logBitmap(int width, int height, Bitmap.Config config) {
        int size = Util.getBitmapByteSize(width, height, config);
        return getBitmapString(size, config);
    }




    /**
     * 以size和config封装为一个缓存
     */
    static final class Key implements Poolable {
        private final KeyPool pool;
        private int size;
        private Bitmap.Config config;

        public Key(KeyPool keyPool) {
            this.pool = keyPool;
        }

        Key(KeyPool pool, int size, Bitmap.Config config) {
            this(pool);
            init(size, config);
        }

        public void init(int size, Bitmap.Config config) {
            this.size = size;
            this.config = config;
        }

        @Override
        public void offer() {
            pool.offer(this);
        }

        @Override
        public String toString() {
            return getBitmapString(size, config);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Key) {
                Key other = (Key) o;
                return size == other.size&& Util.bothNullOrEqual(config, other.config);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int result = size;
            result = 31 * result + (config != null ? config.hashCode() : 0);
            return result;
        }
    }

    /**
     * 位图缓存池
     */
    static class KeyPool extends BaseKeyPool<Key>{
        /**
         * 从缓存池获取一个缓存Key，并设置参数
         */
        public Key get(int size, Bitmap.Config config) {
            Key key = get();
            key.init( size,config );
            return key;
        }

        @Override
        protected Key create() {
            return new Key(this);
        }
    }

    private static String getBitmapString(int size, Bitmap.Config config) {
        return "[" + size + "](" + config + ")";
    }

    private static Bitmap.Config[] getInConfigs(Bitmap.Config requested) {
        switch (requested) {
            case ARGB_8888:
                return ARGB_8888_IN_CONFIGS;
            case RGB_565:
                return RGB_565_IN_CONFIGS;
            case ARGB_4444:
                return ARGB_4444_IN_CONFIGS;
            case ALPHA_8:
                return ALPHA_8_IN_CONFIGS;
            default:
                return new Bitmap.Config[]{requested};
        }
    }
}
