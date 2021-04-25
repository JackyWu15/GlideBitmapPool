# GlideBitmapPool

  
## 使用

```java
// ------ 初始化内存 -------

GlideBitmapPool.initialize(10 * 1024 * 1024); // 10M

// ------ 解码 -------

Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test1);
// 替换为
Bitmap bitmap = GlideBitmapFactory.decodeResource(getResources(), R.drawable.test1);

// ------ 回收 ------- 

bitmap.recycle();
//替换为
GlideBitmapPool.putBitmap(bitmap);

// ------ 新建 -------

Bitmap bitmap = Bitmap.create(width, height, config);
//替换为
Bitmap bitmap = GlideBitmapPool.getBitmap(width, height, config);

// ------ 清缓存内存 -------

GlideBitmapPool.clearMemory();
GlideBitmapPool.trimMemory(level);
```



