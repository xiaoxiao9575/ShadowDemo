package com.example.shadowdemo.host;

import android.app.Application;

import com.example.shadowdemo.introduce_shadow_lib.InitApplication;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        InitApplication.onApplicationCreate(this);
    }
}
