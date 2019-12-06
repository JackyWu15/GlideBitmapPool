package com.hechuangwu.glidebitmappool.pool.strategy;

import com.hechuangwu.glidebitmappool.pool.inter.Poolable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cwh on 2019/12/5 0005.
 * 功能:
 */
public class GroupedLinkedMap<K extends Poolable, V>  {
    //头节点
    private final LinkedEntry<K, V> head = new LinkedEntry<>();
    //节点
    private final Map<K, LinkedEntry<K, V>> keyToEntry = new HashMap<>();


    /**
     * 获取位图
     */
    public V get(K key) {
        LinkedEntry<K, V> entry = keyToEntry.get(key);
        if (entry == null) {
            entry = new LinkedEntry<>(key);
            keyToEntry.put(key, entry);
        } else {
            key.offer();
        }
        //将节点放到最前面
        makeHead(entry);
        return entry.removeLast();
    }

    /**
     * 添加位图
     */
    public void put(K key, V value) {
        LinkedEntry<K, V> entry = keyToEntry.get( key );
        if(entry==null){
            entry = new LinkedEntry<>(key);
            makeTail(entry);
            keyToEntry.put( key,entry );
        }else {
            key.offer();
        }
        //位图缓存到节点的数组中
        entry.add( value );

    }


    /**
     * 将节点放到链表的最后面，Lru
     */
    private void makeTail(LinkedEntry<K, V> entry) {
        removeEntry(entry);
        entry.prev = head.prev;
        entry.next = head;
        updateEntry(entry);
    }

    /**
     * 将节点放到链表的最前面，Lru
     */
    private void makeHead(LinkedEntry<K, V> entry) {
        removeEntry(entry);
        //新节点放头节点后面
        entry.prev = head;
        entry.next = head.next;
        updateEntry(entry);
    }

    /**
     * 将节点提出来
     */
    private static <K, V> void removeEntry(LinkedEntry<K, V> entry) {
        entry.prev.next = entry.next;
        entry.next.prev = entry.prev;
    }

    /**
     * 将上一个最前面的放新节点后面
     */
    private static <K, V> void updateEntry(LinkedEntry<K, V> entry) {
        entry.next.prev = entry;
        entry.prev.next = entry;
    }


    /**
     * 移除节点或位图
     * @return
     */
    public V removeLast() {
        //头的上一个即链表的最后一个
        LinkedEntry<K, V> last = head.prev;
        while (!last.equals( head )){
            //移除掉此节点中数组的最后一个位图
            V removed = last.removeLast();
            if(removed!=null){
                return removed;
            }else {
                //表明这个节点没有缓存，移除掉这个节点
                removeEntry( last );
                //也从map移除掉
                keyToEntry.remove( last.key );
                //这个key缓存到队列，以便下次复用
                last.key.offer();
            }
            //指向自身
            last = last.prev;
        }
        return null;
    }





    /**
     * 环形双向链表，节点包含数组，同时节点是Map的value
     */
    private static class LinkedEntry<K,V>{
        private final K key;
        private List<V> values;
        LinkedEntry<K, V> next;
        LinkedEntry<K, V> prev;

        public LinkedEntry() {
            this(null);
        }

        public LinkedEntry(K key) {
            next = prev = this;
            this.key = key;
        }

        /**
         * 添加
         */
        public void add(V value) {
            if (values == null) {
                values = new ArrayList<>();
            }
            values.add(value);
        }

        /**
         * 如果超过最大缓存，则移除最后一个
         */
        public V removeLast() {
            final int valueSize = size();
            return valueSize > 0 ? values.remove(valueSize - 1) : null;
        }


        public int size() {
            return values != null ? values.size() : 0;
        }
    }

}
