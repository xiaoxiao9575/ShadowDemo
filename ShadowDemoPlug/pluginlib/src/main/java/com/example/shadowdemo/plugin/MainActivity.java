package com.example.shadowdemo.plugin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.shadowdemo.shadowdemo.openlibrary.HostOpenActivity;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_main);
        Log.e("111111111", "onCreate: =====================" );
    }

    public void toOpenActivity(View v){
        Log.e("111111111", "toOpenActivity: =============" );
        startActivity(new Intent(this, PluginTest1Activity.class));
    }

}