package com.example.shadowdemo.host;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.tencent.shadow.dynamic.host.EnterCallback;
import com.tencent.shadow.dynamic.host.PluginManager;
import com.example.shadowdemo.introduce_shadow_lib.InitApplication;

import static com.example.shadowdemo.introduce_shadow_lib.InitApplication.getPluginManager;

public class MainActivity extends AppCompatActivity {

    public static final int FROM_ID_START_ACTIVITY = 1001;
    public static final int FROM_ID_CALL_SERVICE = 1002;
    private Button btn_startPlugActivity;
    private Button btn_startPlugService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        btn_startPlugActivity = (Button) findViewById(R.id.btn_startPlugActivity);
        btn_startPlugService = (Button) findViewById(R.id.btn_startPlugService);
        btn_startPlugService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PluginManager pluginManager = InitApplication.getPluginManager();
                pluginManager.enter(MainActivity.this, FROM_ID_CALL_SERVICE, null, null);
            }
        });

        btn_startPlugActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setEnabled(false);//防止点击重入

                PluginManager pluginManager = InitApplication.getPluginManager();
                pluginManager.enter(MainActivity.this, FROM_ID_START_ACTIVITY, new Bundle(), new EnterCallback() {
                    @Override
                    public void onShowLoadingView(View view) {
//                        MainActivity.this.setContentView(view);//显示Manager传来的Loading页面
                    }

                    @Override
                    public void onCloseLoadingView() {
//                        MainActivity.this.setContentView(R.layout.activity_main);
                    }

                    @Override
                    public void onEnterComplete() {
                        v.setEnabled(true);
                    }
                });
            }
        });
    }
}