package com.hechuangwu.glidebitmappool;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        GlideBitmapPool.initialize( 10*1024*1024 );

        new Thread( new Runnable() {
            @Override
            public void run() {
                //Bitmap.create(width, height, config);替换为
                Bitmap bitmap1 = GlideBitmapFactory.decodeResource(getResources(), R.drawable.test1);
                //bitmap.recycle();替换为
                GlideBitmapPool.putBitmap(bitmap1);

                Bitmap bitmap2 = GlideBitmapFactory.decodeResource(getResources(), R.drawable.test2);
                GlideBitmapPool.putBitmap(bitmap2);
            }
        } ).start();
    }
}
