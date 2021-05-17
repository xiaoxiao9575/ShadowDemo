package com.example.shadowdemo.plugin;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/***
 *  date : 2021/5/12 13:44
 *  author : gujianpeng
 *  description : 
 */
public class MyService extends Service {

    private ScheduledExecutorService mExecutorService;

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new IMyAidlInterface.Stub() {
            @Override
            public String basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {
                return Integer.toString(anInt) + aLong + aBoolean + aFloat + aDouble + aString;
            }
        };
    }

    @Override
    public void onCreate() {
        Log.d("MyService", "onCreate");
        super.onCreate();
        mExecutorService = Executors.newScheduledThreadPool(2);
        mExecutorService.scheduleAtFixedRate(mRunnable, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void onDestroy() {
        Log.d("MyService", "onDestroy");
        super.onDestroy();
        mExecutorService.shutdown();
        mExecutorService = null;
        mRunnable = null;
    }

    private int count = 0;
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            count++;
            Log.d("MyService", "=== count:" + count);
        }
    };
}
