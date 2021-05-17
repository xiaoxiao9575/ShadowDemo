# ShadowDemo
* 一、clone 仓库
* 二、测试编译
* 三、将Shadow库发布到本地仓库
* 四、宿主接入
* 1 添加依赖
* 2 添加代理 Activity 主题
* 3 清单文件注册代理Activity
* 4 在宿主中创建 PluginManager 管理工具
* a 创建 PluginManager文件升级器
* b 创建插件进程服务
* c 实现Log工具
* d 在 Application 创建 PluginManager
* 5 宿主启动插件Activity
* 6 宿主混淆
* 五、PluginManager 工程
* 1 添加依赖
* 2 创建插件管理类
* 3 使用上面创建的插件管理类
* 4 混淆
* 5 编译
* 六、插件接入
* 1 在根gradle中加入依赖
* 2 在你原项目基础上创建应用级模块 my-runtime
* a 添加依赖
* b 创建壳子
* c 混淆
* 3 在你原项目基础上创建应用级模块 my-loader
* a 添加依赖
* b 实现插件组件管理类
* c 创建插件加载器
* d 使用插件加载器
* e 混淆
* 4 在你的业务应用工程gradle中配置插件
* a 添加依赖
* b 在gradle最底下配置插件
* 5 编译
* 七、使用帮助
* 1 检测当前是否处于插件状态下
* 2 插件调用宿主类
* 3 将本地仓库发布放到 gitee 或 github


### 一、clone 仓库
https://github.com/Tencent/Shadow

### 二、测试编译
在 Shadow 目录下使用命令, 确保在 Java8 环境：

./gradlew build
### 三、将Shadow库发布到本地仓库
可以先在 buildScripts/gradle/common.gradle 路径下，修改发布版本或其他信息：

 ext.ARTIFACT_VERSION = ext.VERSION_NAME +ext.VERSION_SUFFIX
 
./gradlew publish

成功后，在下面目录可找到文件：
/Users/victor/.m2/repository/com/tencent/shadow/dynamic
/Users/victor/.m2/repository/com/tencent/shadow/core
在对应文件里可以找到相应的版本信息，至此，就可以在 Android Studio 中使用Shadow SDK了。

对应的依赖写法举例：

implementation "com.tencent.shadow.dynamic:host:2.0.12-c7497f58-SNAPSHOT"
可以在根目录配置 shadow_version 版本：

ext.shadow_version = '2.0.12-c7497f58-SNAPSHOT'
添加本地maven仓库即可使用：

 mavenLocal()
### 四、宿主接入
1 添加依赖
```
implementation "com.tencent.shadow.dynamic:host:$shadow_version"
```
2 添加代理 Activity 主题
这里要求背景透明即可， 可根据自己需求修改,建议仅竖屏:
```
   <style name="PluginContainerActivity" parent="@android:style/Theme.NoTitleBar.Fullscreen">
        <item name="android:windowBackground">@android:color/transparent</item>
        <item name="android:windowContentOverlay">@null</item>
        <item name="android:windowNoTitle">true</item>
        <item name="android:windowIsTranslucent">true</item>
    </style>
```
3 清单文件注册代理Activity
```
 <!--container 注册
          注意configChanges需要全注册
          theme需要注册成透明

          这些类不打包在host中，打包在runtime中，以便减少宿主方法数增量
          Activity 路径需要和插件中的匹配，后面会说到
          -->
        <activity
            android:name="com.tencent.shadow.sample.runtime.PluginDefaultProxyActivity"
            android:launchMode="standard"
            android:screenOrientation="portrait"
            android:configChanges="mcc|mnc|locale|touchscreen|keyboard|keyboardHidden|navigation|screenLayout|fontScale|uiMode|orientation|screenSize|smallestScreenSize|layoutDirection"
            android:hardwareAccelerated="true"
            android:theme="@style/PluginContainerActivity"
            android:process=":plugin" />

        <activity
            android:name="com.tencent.shadow.sample.runtime.PluginSingleInstance1ProxyActivity"
            android:launchMode="singleInstance"
            android:screenOrientation="portrait"
            android:configChanges="mcc|mnc|locale|touchscreen|keyboard|keyboardHidden|navigation|screenLayout|fontScale|uiMode|orientation|screenSize|smallestScreenSize|layoutDirection"
            android:hardwareAccelerated="true"
            android:theme="@style/PluginContainerActivity"
            android:process=":plugin" />

        <activity
            android:name="com.tencent.shadow.sample.runtime.PluginSingleTask1ProxyActivity"
            android:launchMode="singleTask"
            android:screenOrientation="portrait"
            android:configChanges="mcc|mnc|locale|touchscreen|keyboard|keyboardHidden|navigation|screenLayout|fontScale|uiMode|orientation|screenSize|smallestScreenSize|layoutDirection"
            android:hardwareAccelerated="true"
            android:theme="@style/PluginContainerActivity"
            android:process=":plugin" />

        <provider
            android:authorities="com.tencent.shadow.contentprovider.authority.dynamic"
            android:name="com.tencent.shadow.core.runtime.container.PluginContainerContentProvider" />
        <!--container 注册 end -->
```
4 在宿主中创建 PluginManager 管理工具
PluginManager 用来装载插件，是通过加载一个单独的apk来创建的。
下面会说怎么生成这个apk，先知道在宿主中怎么用即可。

a 创建 PluginManager文件升级器
里面的内容根据自己的需求自行实现：
```
import com.tencent.shadow.dynamic.host.PluginManagerUpdater;

import java.io.File;
import java.util.concurrent.Future;

public class FixedPathPmUpdater implements PluginManagerUpdater {

    final private File apk;

    FixedPathPmUpdater(File apk) {
        this.apk = apk;
    }

    /**
     * @return <code>true</code>表示之前更新过程中意外中断了
     */
    @Override
    public boolean wasUpdating() {
        return false;
    }
    /**
     * 更新
     *
     * @return 当前最新的PluginManager，可能是之前已经返回过的文件，但它是最新的了。
     */
    @Override
    public Future<File> update() {
        return null;
    }
    /**
     * 获取本地最新可用的
     *
     * @return <code>null</code>表示本地没有可用的
     */
    @Override
    public File getLatest() {
        return apk;
    }
    /**
     * 查询是否可用
     *
     * @param file PluginManagerUpdater返回的file
     * @return <code>true</code>表示可用，<code>false</code>表示不可用
     */
    @Override
    public Future<Boolean> isAvailable(final File file) {
        return null;
    }
}
```
b 创建插件进程服务
```
import com.tencent.shadow.dynamic.host.PluginProcessService;
/**
 * 一个PluginProcessService（简称PPS）代表一个插件进程。插件进程由PPS启动触发启动。
 * 新建PPS子类允许一个宿主中有多个互不影响的插件进程。
 */
public class MainPluginProcessService extends PluginProcessService {
}
```
在清单文件注册：
```
        <service
            android:name=".MainPluginProcessService"
            android:process=":plugin" />
```
c 实现Log工具
import com.tencent.shadow.core.common.ILoggerFactory;
public class AndroidLoggerFactory implements ILoggerFactory{...}
示例： https://github.com/Tencent/Shadow/blob/master/projects/sample/source/sample-host/src/main/java/com/tencent/shadow/sample/host/AndroidLogLoggerFactory.java

d 在 Application 创建 PluginManager
```
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        onApplicationCreate(this);
    }
    
    /**
     * 这个PluginManager对象在Manager升级前后是不变的。它内部持有具体实现，升级时更换具体实现。
     */
    private static PluginManager sPluginManager;

    public static PluginManager getPluginManager() {
        return sPluginManager;
    }

    public static void onApplicationCreate(Application application) {
        //Log接口Manager也需要使用，所以主进程也初始化。
        LoggerFactory.setILoggerFactory(new AndroidLoggerFactory());

        if (isProcess(application, ":plugin")) {
            //在全动态架构中，Activity组件没有打包在宿主而是位于被动态加载的runtime，
            //为了防止插件crash后，系统自动恢复crash前的Activity组件，此时由于没有加载runtime而发生classNotFound异常，导致二次crash
            //因此这里恢复加载上一次的runtime
            DynamicRuntime.recoveryRuntime(application);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                WebView.setDataDirectorySuffix("plugin")
            }
        }

        FixedPathPmUpdater fixedPathPmUpdater
                = new FixedPathPmUpdater(new File("生成的PluginManager.apk文件路径"));
        boolean needWaitingUpdate
                = fixedPathPmUpdater.wasUpdating()//之前正在更新中，暗示更新出错了，应该放弃之前的缓存
                || fixedPathPmUpdater.getLatest() == null;//没有本地缓存
        Future<File> update = fixedPathPmUpdater.update();
        if (needWaitingUpdate) {
            try {
                update.get();//这里是阻塞的，需要业务自行保证更新Manager足够快。
            } catch (Exception e) {
                throw new RuntimeException("Sample程序不容错", e);
            }
        }
        sPluginManager = new DynamicPluginManager(fixedPathPmUpdater);
    }

    private static boolean isProcess(Context context, String processName) {
        String currentProcName = "";
        ActivityManager manager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.pid == myPid()) {
                currentProcName = processInfo.processName;
                break;
            }
        }

        return currentProcName.endsWith(processName);
    }
}
```
5 宿主启动插件Activity
```
                PluginManager pluginManager = Application.getPluginManager();
                  /**
                     * @param context context
                     * @param formId  标识本次请求的来源位置，用于区分入口
                     * @param bundle  参数列表, 建议在参数列表加入自己的验证
                     * @param callback 用于从PluginManager实现中返回View
                     */
                Bundle bundle = new Bundle()
                // 插件 zip，这几个参数也都可以不传，直接在 PluginManager 中硬编码 
                bundle.putString("plugin_path", "/data/local/tmp/plugin-debug.zip");
                // partKey 每个插件都有自己的 partKey 用来区分多个插件，如何配置在下面讲到
                bundle.putString("part_key", "my-plugin");
                // 路径举例：com.google.samples.apps.sunflower.GardenActivity
                bundle.putString("activity_class_name", "启动的插件Activity路径");
                // 要传入到插件里的参数
                bundle.putBundle("extra_to_plugin_bundle", new Bundle())
      
                pluginManager.enter(MainActivity.this, formId, new Bundle(), new EnterCallback() {
                    @Override
                    public void onShowLoadingView(View view) {
                    }

                    @Override
                    public void onCloseLoadingView() {

                    }

                    @Override
                    public void onEnterComplete() {
                        // 启动成功
                    }
                });
```
6 宿主混淆
```
-keep class com.tencent.shadow.core.common.**{*;}
-keep class com.tencent.shadow.core.runtime.**{*;}
-keep class com.tencent.shadow.dynamic.host.**{*;}
```
### 五、PluginManager 工程
创建一个新项目，用来生成插件管理apk包。

1 添加依赖
```
    implementation "com.tencent.shadow.dynamic:manager:$shadow_version"
    compileOnly "com.tencent.shadow.core:common:$shadow_version"
    compileOnly "com.tencent.shadow.dynamic:host:$shadow_version"
```
2 创建插件管理类
下面行数有点多，只要修改 TODO 相关方法即可：
```
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

import com.tencent.shadow.core.manager.installplugin.InstalledPlugin;
import com.tencent.shadow.core.manager.installplugin.InstalledType;
import com.tencent.shadow.core.manager.installplugin.PluginConfig;
import com.tencent.shadow.dynamic.host.EnterCallback;
import com.tencent.shadow.dynamic.host.FailedException;
import com.tencent.shadow.dynamic.loader.PluginServiceConnection;
import com.tencent.shadow.dynamic.manager.PluginManagerThatUseDynamicLoader;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Afra55
 * @date 2020/5/19
 * A smile is the best business card.
 */
public class MyPluginManager extends PluginManagerThatUseDynamicLoader {

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ExecutorService mFixedPool = Executors.newFixedThreadPool(4);

    protected MyPluginManager(Context context) {
        super(context);
    }

    /**
     * TODO 下面内容需要自己实现
     * @return PluginManager实现的别名，用于区分不同PluginManager实现的数据存储路径
     */
    @Override
    protected String getName() {
        return "sample-manager";
    }

    /**
     * TODO 下面内容需要自己实现
     * @return demo插件so的abi
     */
    @Override
    public String getAbi() {
        return "";
    }

    /**
     * TODO 下面内容需要自己实现
     * @return 宿主中注册的PluginProcessService实现的类名
     */
    protected String getPluginProcessServiceName() {
        return "com.tencent.shadow.sample.introduce_shadow_lib.MainPluginProcessService";
    }

    /**
     * TODO 下面内容需要自己实现
     * @param context context
     * @param fromId  标识本次请求的来源位置，用于区分入口
     * @param bundle  参数列表
     * @param callback 用于从PluginManager实现中返回View
     */
    @Override
    public void enter(final Context context, long fromId, Bundle bundle, final EnterCallback callback) {
        // 插件 zip 包地址，可以直接写在这里，也用Bundle可以传进来
        final String pluginZipPath = bundle.getString("plugin_path");
        final String partKey = bundle.getString("part_key");
        final String className = bundle.getString("activity_class_name");
        if (className == null) {
            throw new NullPointerException("className == null");
        }
        if (fromId == 1011) { // 打开 Activity 示例
            final Bundle extras = bundle.getBundle("extra_to_plugin_bundle");
            if (callback != null) {
                // 开始加载插件了，实现加载布局
                callback.onShowLoadingView(null);
            }
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        InstalledPlugin installedPlugin
                                = installPlugin(pluginZipPath, null, true);//这个调用是阻塞的
                        Intent pluginIntent = new Intent();
                        pluginIntent.setClassName(
                                context.getPackageName(),
                                className
                        );
                        if (extras != null) {
                            pluginIntent.replaceExtras(extras);
                        }

                        startPluginActivity(context, installedPlugin, partKey, pluginIntent);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    if (callback != null) {
                        Handler uiHandler = new Handler(Looper.getMainLooper());
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                // 到这里插件就启动完成了
                                callback.onCloseLoadingView();
                                callback.onEnterComplete();
                            }
                        });
                    }
                }
            });

        } else if (fromId == 1012) { // 打开Server示例
            Intent pluginIntent = new Intent();
            pluginIntent.setClassName(context.getPackageName(), className);

            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        InstalledPlugin installedPlugin
                                = installPlugin(pluginZipPath, null, true);//这个调用是阻塞的

                        loadPlugin(installedPlugin.UUID, partKey);

                        Intent pluginIntent = new Intent();
                        pluginIntent.setClassName(context.getPackageName(), className);

                        boolean callSuccess = mPluginLoader.bindPluginService(pluginIntent, new PluginServiceConnection() {
                            @Override
                            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                               // 在这里实现AIDL进行通信操作
                            }

                            @Override
                            public void onServiceDisconnected(ComponentName componentName) {
                                throw new RuntimeException("onServiceDisconnected");
                            }
                        }, Service.BIND_AUTO_CREATE);

                        if (!callSuccess) {
                            throw new RuntimeException("bind service失败 className==" + className);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } else {
            throw new IllegalArgumentException("不认识的fromId==" + fromId);
        }
    }



    public InstalledPlugin installPlugin(String zip, String hash , boolean odex) throws IOException, JSONException, InterruptedException, ExecutionException {
        final PluginConfig pluginConfig = installPluginFromZip(new File(zip), hash);
        final String uuid = pluginConfig.UUID;
        List<Future> futures = new LinkedList<>();
        if (pluginConfig.runTime != null && pluginConfig.pluginLoader != null) {
            Future odexRuntime = mFixedPool.submit(new Callable() {
                @Override
                public Object call() throws Exception {
                    oDexPluginLoaderOrRunTime(uuid, InstalledType.TYPE_PLUGIN_RUNTIME,
                            pluginConfig.runTime.file);
                    return null;
                }
            });
            futures.add(odexRuntime);
            Future odexLoader = mFixedPool.submit(new Callable() {
                @Override
                public Object call() throws Exception {
                    oDexPluginLoaderOrRunTime(uuid, InstalledType.TYPE_PLUGIN_LOADER,
                            pluginConfig.pluginLoader.file);
                    return null;
                }
            });
            futures.add(odexLoader);
        }
        for (Map.Entry<String, PluginConfig.PluginFileInfo> plugin : pluginConfig.plugins.entrySet()) {
            final String partKey = plugin.getKey();
            final File apkFile = plugin.getValue().file;
            Future extractSo = mFixedPool.submit(new Callable() {
                @Override
                public Object call() throws Exception {
                    extractSo(uuid, partKey, apkFile);
                    return null;
                }
            });
            futures.add(extractSo);
            if (odex) {
                Future odexPlugin = mFixedPool.submit(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        oDexPlugin(uuid, partKey, apkFile);
                        return null;
                    }
                });
                futures.add(odexPlugin);
            }
        }

        for (Future future : futures) {
            future.get();
        }
        onInstallCompleted(pluginConfig);

        return getInstalledPlugins(1).get(0);
    }


    public void startPluginActivity(Context context, InstalledPlugin installedPlugin, String partKey, Intent pluginIntent) throws RemoteException, TimeoutException, FailedException {
        Intent intent = convertActivityIntent(installedPlugin, partKey, pluginIntent);
        if (!(context instanceof Activity)) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    public Intent convertActivityIntent(InstalledPlugin installedPlugin, String partKey, Intent pluginIntent) throws RemoteException, TimeoutException, FailedException {
        loadPlugin(installedPlugin.UUID, partKey);
        return mPluginLoader.convertActivityIntent(pluginIntent);
    }

    private void loadPluginLoaderAndRuntime(String uuid) throws RemoteException, TimeoutException, FailedException {
        if (mPpsController == null) {
            bindPluginProcessService(getPluginProcessServiceName());
            waitServiceConnected(10, TimeUnit.SECONDS);
        }
        loadRunTime(uuid);
        loadPluginLoader(uuid);
    }

    protected void loadPlugin(String uuid, String partKey) throws RemoteException, TimeoutException, FailedException {
        loadPluginLoaderAndRuntime(uuid);
        Map map = mPluginLoader.getLoadedPlugin();
        if (!map.containsKey(partKey)) {
            mPluginLoader.loadPlugin(partKey);
        }
        Boolean isCall = (Boolean) map.get(partKey);
        if (isCall == null || !isCall) {
            mPluginLoader.callApplicationOnCreate(partKey);
        }
    }
}
```
3 使用上面创建的插件管理类
创建路径：com.tencent.shadow.dynamic.impl, 在该路径下创建两个文件：
```
package com.tencent.shadow.dynamic.impl;

import android.content.Context;

import com.tencent.shadow.dynamic.host.ManagerFactory;
import com.tencent.shadow.dynamic.host.PluginManagerImpl;
import com.tencent.shadow.sample.manager.MyPluginManager;

/**
 * 此类包名及类名固定
 */
public final class ManagerFactoryImpl implements ManagerFactory {
    @Override
    public PluginManagerImpl buildManager(Context context) {
        return new MyPluginManager(context);
    }
}

/**
 * 此类包名及类名固定
 * classLoader的白名单
 * PluginManager可以加载宿主中位于白名单内的类
 */
public interface WhiteList {
    String[] sWhiteList = new String[]
            {
            };
}
```
4 混淆
```
-keep class com.tencent.shadow.dynamic.impl.**{*;}
-keep class com.tencent.shadow.dynamic.loader.**{*;}
-keep class com.tencent.shadow.dynamic.impl.ManagerFactoryImpl {*;}
-keep class com.tencent.shadow.dynamic.impl.WhiteList {*;}
```
5 编译
使用命令，或者在Gradle 工具窗口点击 assembleDebug：
./gradlew assembleDebug
生成apk路径：
sample-manager/build/outputs/apk/debug/sample-manager-debug.apk

至此，插件管理工程完成。

### 六、插件接入
插件的包名和宿主的包名要保持一致。

1 在根gradle中加入依赖
classpath "com.tencent.shadow.core:gradle-plugin:$shadow_version"
2 在你原项目基础上创建应用级模块 my-runtime
这个应用模块主要放在宿主中注册的壳子，路径名和类名要一致。
这个模块的 applicationId 可以随意。

a 添加依赖
这个应用模块只需要下面的依赖，其他依赖都能删掉，清单文件也不需要任何配置，生成项目时自动创建的Activity 都可以删掉。

  implementation "com.tencent.shadow.core:activity-container:$shadow_version"
b 创建壳子
壳子路径包名和宿主中注册的要保持一致：
```
package com.tencent.shadow.sample.runtime;
import com.tencent.shadow.core.runtime.container.PluginContainerActivity;
public class PluginDefaultProxyActivity extends PluginContainerActivity {
}
package com.tencent.shadow.sample.runtime;
import com.tencent.shadow.core.runtime.container.PluginContainerActivity;
public class PluginSingleInstance1ProxyActivity extends PluginContainerActivity {
}
package com.tencent.shadow.sample.runtime;
import com.tencent.shadow.core.runtime.container.PluginContainerActivity;
public class PluginSingleTask1ProxyActivity extends PluginContainerActivity {
}
```
c 混淆
```
-keep class org.slf4j.**{*;}
-dontwarn org.slf4j.impl.**

-keep class com.tencent.shadow.core.runtime.**{*;}
#需要keep在宿主AndroidManifest.xml注册的壳子activity
-keep class com.tencent.shadow.sample.runtime.**{*;}
```
3 在你原项目基础上创建应用级模块 my-loader
这个应用模块主要定义插件组件和壳子代理组件的配对关系，manager在加载"插件"时，首先需要先加载"插件"中的runtime和loader， 再通过loader的Binder（插件应该处于独立进程中避免native库冲突）操作loader进而加载业务App。

这个模块的 applicationId 可以随意。

a 添加依赖
这个应用模块只需要下面的依赖，其他依赖都能删掉，清单文件也不需要任何配置，生成项目时自动创建的Activity 都可以删掉。
```
    implementation "com.tencent.shadow.dynamic:loader-impl:$shadow_version"

    compileOnly "com.tencent.shadow.core:activity-container:$shadow_version"
    compileOnly "com.tencent.shadow.core:common:$shadow_version"
    //下面这行依赖是为了防止在proguard的时候找不到LoaderFactory接口
    compileOnly "com.tencent.shadow.dynamic:host:$shadow_version"
```
b 实现插件组件管理类
```
import android.content.ComponentName;
import android.content.Context;
import com.tencent.shadow.core.loader.infos.ContainerProviderInfo;
import com.tencent.shadow.core.loader.managers.ComponentManager;

import java.util.ArrayList;
import java.util.List;

public class SampleComponentManager extends ComponentManager {

    /**
     * runtime 模块中定义的壳子Activity, 路径类名保持一致，需要在宿主AndroidManifest.xml注册
     */
    private static final String DEFAULT_ACTIVITY = "com.tencent.shadow.sample.runtime.PluginDefaultProxyActivity";
    private static final String SINGLE_INSTANCE_ACTIVITY = "com.tencent.shadow.sample.runtime.PluginSingleInstance1ProxyActivity";
    private static final String SINGLE_TASK_ACTIVITY = "com.tencent.shadow.sample.runtime.PluginSingleTask1ProxyActivity";

    private Context context;

    public SampleComponentManager(Context context) {
        this.context = context;
    }


    /**
     * 配置插件Activity 到 壳子Activity的对应关系
     *
     * @param pluginActivity 插件Activity
     * @return 壳子Activity
     */
    @Override
    public ComponentName onBindContainerActivity(ComponentName pluginActivity) {
        switch (pluginActivity.getClassName()) {
            /**
             * 这里配置对应的对应关系, 启动不同启动模式的Acitvity
             */
        }
        return new ComponentName(context, DEFAULT_ACTIVITY);
    }

    /**
     * 配置对应宿主中预注册的壳子contentProvider的信息
     */
    @Override
    public ContainerProviderInfo onBindContainerContentProvider(ComponentName pluginContentProvider) {
        return new ContainerProviderInfo(
                "com.tencent.shadow.runtime.container.PluginContainerContentProvider",
                "com.tencent.shadow.contentprovider.authority.dynamic");
    }

    @Override
    public List<BroadcastInfo> getBroadcastInfoList(String partKey) {
        List<ComponentManager.BroadcastInfo> broadcastInfos = new ArrayList<>();

        //如果有静态广播需要像下面代码这样注册
//        if (partKey.equals(Constant.PART_KEY_PLUGIN_MAIN_APP)) {
//            broadcastInfos.add(
//                    new ComponentManager.BroadcastInfo(
//                            "com.tencent.shadow.demo.usecases.receiver.MyReceiver",
//                            new String[]{"com.tencent.test.action"}
//                    )
//            );
//        }
        return broadcastInfos;
    }

}
```
c 创建插件加载器
```
import android.content.Context;

import com.tencent.shadow.core.loader.ShadowPluginLoader;
import com.tencent.shadow.core.loader.managers.ComponentManager;
public class SamplePluginLoader extends ShadowPluginLoader {

    private final static String TAG = "shadow";

    private ComponentManager componentManager;

    public SamplePluginLoader(Context hostAppContext) {
        super(hostAppContext);
        componentManager = new SampleComponentManager(hostAppContext);
    }

    @Override
    public ComponentManager getComponentManager() {
        return componentManager;
    }
}
```
d 使用插件加载器
需要使用下面代码中的路径和类名：
```
package com.tencent.shadow.dynamic.loader.impl;

import android.content.Context;
import com.tencent.shadow.core.loader.ShadowPluginLoader;
import com.tencent.shadow.sample.loader.SamplePluginLoader;
import org.jetbrains.annotations.NotNull;

/**
 * 这个类的包名类名是固定的。
 * <p>
 * 见com.tencent.shadow.dynamic.loader.impl.DynamicPluginLoader#CORE_LOADER_FACTORY_IMPL_NAME
 */
public class CoreLoaderFactoryImpl implements CoreLoaderFactory {

    @NotNull
    @Override
    public ShadowPluginLoader build(@NotNull Context context) {
        return new SamplePluginLoader(context);
    }
}
```
注意如果要使用宿主的类，需要创建固定路径下的 WhiteList 进行注册：
```
package com.tencent.shadow.dynamic.impl;

/**
 * 此类包名及类名固定
 * classLoader的白名单
 * PluginLoader可以加载宿主中位于白名单内的类
 */
public interface WhiteList {
    String[] sWhiteList = new String[]
            {
                    "com.a.b",
            };
}
```

e 混淆
```
#kotlin一般性配置 START
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
#kotlin一般性配置 END

#kotlin优化性能 START
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}
#kotlin优化性能 END

-keep class org.slf4j.**{*;}
-dontwarn org.slf4j.impl.**

-keep class com.tencent.shadow.dynamic.host.**{*;}
-keep class com.tencent.shadow.dynamic.impl.**{*;}
-keep class com.tencent.shadow.dynamic.loader.**{*;}
-keep class com.tencent.shadow.core.common.**{*;}
-keep class com.tencent.shadow.core.loader.**{*;}
-keep class com.tencent.shadow.core.runtime.**{*;}

-dontwarn  com.tencent.shadow.dynamic.host.**
-dontwarn  com.tencent.shadow.dynamic.impl.**
-dontwarn  com.tencent.shadow.dynamic.loader.**
-dontwarn  com.tencent.shadow.core.common.**
-dontwarn  com.tencent.shadow.core.loader.**
```
4 在你的业务应用工程gradle中配置插件
a 添加依赖
```
    //Shadow Transform后业务代码会有一部分实际引用runtime中的类
    //如果不以compileOnly方式依赖，会导致其他Transform或者Proguard找不到这些类
    compileOnly "com.tencent.shadow.core:runtime:$shadow_version"
```
b 在gradle最底下配置插件
my-load 和 my-runtime 模块生成的 apk 路径及名字要对应，可以先build 生成一个 apk 查看路径和名字, 插件apk路径名字也一样，可以先build生成，再配置路径。

```
apply plugin: 'com.tencent.shadow.plugin'

shadow {
    packagePlugin {
        pluginTypes {
            debug {
                loaderApkConfig = new Tuple2('my-loader-debug.apk', ':my-loader:assembleDebug')
                runtimeApkConfig = new Tuple2('my-runtime-debug.apk', ':my-runtime:assembleDebug')
                pluginApks {
                    pluginApk1 {
                        businessName = 'my-plugin'//businessName相同的插件，context获取的Dir是相同的。businessName留空，表示和宿主相同业务，直接使用宿主的Dir。
                        partKey = 'my-plugin'
                        buildTask = 'assembleDebug'
                        apkName = 'app-debug.apk'
                        apkPath = 'app/build/outputs/apk/debug/app-debug.apk'
                         // hostWhiteList = ["com.tencent.shadow.sample.host.lib"] // 配置允许插件访问宿主的类
                    }
                }
            }

            release {
                loaderApkConfig = new Tuple2('my-loader-release-unsigned.apk', ':my-loader:assembleRelease')
                runtimeApkConfig = new Tuple2('my-runtime-release-unsigned.apk', ':my-runtime:assembleRelease')
                pluginApks {
                    pluginApk1 {
                        businessName = 'demo'
                        partKey = 'my-plugin'
                        buildTask = 'assembleRelease'
                        apkName = 'app-release-unsigned.apk'
                        apkPath = 'app/build/outputs/apk/release/app-release-unsigned.apk'
                         // hostWhiteList = ["com.tencent.shadow.sample.host.lib"] // 配置允许插件访问宿主的类
                    }
                }
            }
        }

        loaderApkProjectPath = 'my-loader'

        runtimeApkProjectPath = 'my-runtime'

        version = 4
        compactVersion = [1, 2, 3]
        uuidNickName = "1.1.5"
    }
}
```
5 编译
注意要keep住你从宿主打开插件的 Activity
使用命令，或者在Gradle 工具窗口点击 packageDebugPlugin：
```
./gradlew packageDebugPlugin
```
生成zip路径：
```
build/plugin-debug.zip
```
至此，插件工程完成。

将 manager.apk 和 plugin-debug.zip 放到指定位置即可享受。

### 七、使用帮助
1 检测当前是否处于插件状态下
```
public class PluginChecker {

    private static Boolean sPluginMode;

    /**
     * 检测当前是否处于插件状态下
     * 这里先简单通过访问一个插件框架中的类是否成功来判断
     * @return true 是插件模式
     */
    public static boolean isPluginMode() {
        if (sPluginMode == null) {
            try {
                PluginChecker.class.getClassLoader().loadClass("com.tencent.shadow.core.runtime.ShadowApplication");
                sPluginMode = true;
            } catch (ClassNotFoundException e) {
                sPluginMode = false;
            }
        }
        return sPluginMode;
    }

}
```
2 插件调用宿主类
宿主在打包时keep住插件要使用到的类名和方法。

a 生成宿主 jar 包
在你的宿主依赖库模块 gradle 中配置命令：

```
//下面的jarPackage和afterEvaluate负责让这个aar再生成一个输出jar包的任务
def jarPackage(buildType) {
    return tasks.create("jar${buildType.capitalize()}Package", Copy) {
        def aarFile = file(project.buildDir.path + "/outputs/aar/${project.name}-${buildType}.aar")
        def outputDir = file(project.buildDir.path + "/outputs/jar")

        from zipTree(aarFile)
        into outputDir
        include 'classes.jar'
        rename 'classes.jar', "${project.name}-${buildType}.jar"
        group = 'build'
        description = '生成jar包'
    }.dependsOn(project.getTasksByName("assemble${buildType.capitalize()}", false).first())
}

afterEvaluate {
    android {
        buildTypes.findAll().each { buildType ->
            def buildTypeName = buildType.getName()
            jarPackage(buildTypeName)
        }
    }
}
```
会生成一个 jar包。

b 在插件中引用 jar
在应用级 gradle 中添加依赖：

  //注意sample-host-lib要用compileOnly编译而不打包在插件中。在packagePlugin任务中配置hostWhiteList允许插件访问宿主的类。
    compileOnly files("jar 包路径")
c 在插件中使用宿主的类
跟引入了依赖库一样，可以直接使用，这个类一定要在插件配置中配置好路径并且在my-loader 模块中 WhiteList 中注册，并且keep主类名和方法名。

3 将本地仓库发布放到 gitee 或 github
示例：https://gitee.com/afra55/my-shadow
在 github 或 gitee 上创建一个空项目，找到本地仓库直接推送到 github 或 gitee，比如shadow 直接将 com 文件夹及内部相关文件提交即可：
image.png
使用
在应用级 build.gralde 配置：
```
    allprojects {
        repositories {
            maven { url "https://gitee.com/afra55/my-shadow/raw/master" }
        }
    }
```
接下来就可以像在本地仓库一样使用依赖库了。
