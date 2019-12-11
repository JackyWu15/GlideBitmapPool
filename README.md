# GlideBitmapPool

## android不同版本的sdk，对Bitmap的处理有所区别：

- 2.3之前：像素数据存储在native层的内存中，因此，虚拟机无法自动进行垃圾回收，开发者必须主动调用recycle进行回收，容易造成内存泄漏；

- 3.0以后：像素数据存储在Java堆中，虚拟机能够进行自动回收，并引入了BitmapFactory.Options.inBitmap字段，推荐对Bitmap进行复用，只是复用条件比较苛刻；

- 4.4以后：传输方式发生变化，大数据会通过ashmem（匿名共享内存）来传递（不占用Java内存），小数据通过直接拷贝的方式（在内存中操作），放宽了图片大小的限制。

## 为什么要使用复用

    位图的创建是一项比较消耗内存的操作，当一个应用程序需要频繁使用到位图，如果不断地进行分配和释放，则会使得GC频繁工作，不断回收内存，造成内存碎片化，甚
至出现内存抖动等情况，因此，当某个位图不再使用了，要先判断其是否可以复用，如果能够复用，应该将其保留下来，而不是recycle，以便下次进行复用，如此可避免内存的
频繁开辟和回收。

## 复用条件

- sdk<3.0(HONEYCOMB):
  无法进行复用，只能及时调用recycle进行回收。
  
- 3.0(HONEYCOMB)<=sdk<4.4(KITKAT)：
  格式为jpg、png，并且被复用的Bitmap和申请的Bitmap，需同等宽高，inSampleSize为1，才能进行复用。
  
- sdk>=4.4(KITKAT)：
  被复用的Bitmap的内存必须大于需要申请内存的Bitmap的内存，就能进行复用。
  
## 原理
  
      这个项目是根据Glide开源框架进行实现的，位图池的本质是GroupedLinkedMap这个类的实现，成员变量HashMap，存储了双向环形队列的每个节点，而每个节点包含
  一个ArrayList来缓存Bitmap。散列和链表的数据结构，这种设计可以做到对节点的快速查找，以及位置的快速移动，是Lru算法一种很好的实现。
  
## 使用

```java
// ------ 解码 -------

Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test1);
// 替换为
Bitmap bitmap = GlideBitmapFactory.decodeResource(getResources(), R.drawable.test1);

// ------ 回收 ------- 

bitmap.recycle();
//替换为
GlideBitmapPool.putBitmap(bitmap);

//  ------ 新建 -------

Bitmap bitmap = Bitmap.create(width, height, config);
//替换为
Bitmap bitmap = GlideBitmapPool.getBitmap(width, height, config);
```
  

