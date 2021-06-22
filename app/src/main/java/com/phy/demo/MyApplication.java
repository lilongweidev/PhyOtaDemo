package com.phy.demo;

import android.app.Application;

import com.phy.ota.sdk.ble.Device;
import com.phy.ota.sdk.utils.SPUtils;

public class MyApplication extends Application {

    private static MyApplication application;
    private Device device;

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        //初始化缓存
        SPUtils.init(this);
    }

    public static MyApplication getApplication() {
        return application;
    }


}
