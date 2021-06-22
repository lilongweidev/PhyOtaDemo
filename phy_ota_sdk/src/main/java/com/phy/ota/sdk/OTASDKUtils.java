package com.phy.ota.sdk;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;


import androidx.annotation.NonNull;

import com.phy.ota.sdk.ble.OTACallback;
import com.phy.ota.sdk.ble.OTAType;
import com.phy.ota.sdk.ble.UpdateFirmwareCallback;
import com.phy.ota.sdk.constant.ErrorCode;
import com.phy.ota.sdk.utils.BleHelper;
import com.phy.ota.sdk.utils.KLog;

public class OTASDKUtils {

    private int STATUS = 0;
    private int START_OTA = 1;
    private int OTA_CONNECTING = 2;
    private int OTA_ING = 3;
    private int REBOOT = 4;
    private int START_RES = 5;
    private int RES_ING = 6;
    private int APP2OTA = 7;

    private OTACallback otaCallback;
    private OTAUtils otaUtils;
    private UpdateFirmwareCallback firmwareCallback;
    private String address;
    private String filePath;
    private OTAType otaType;

    /**
     * 创建OTASDKUtils方法
     */
    public OTASDKUtils(Context context, UpdateFirmwareCallback firmwareCallback) {
        this.firmwareCallback = firmwareCallback;

        otaCallback = new OTACallbackImpl();
        otaUtils = new OTAUtils(context, otaCallback);
    }

    /**
     * 更新固件
     * @param address 地址
     * @param filePath 升级文件路径
     * @param isSecurity 是否加密
     */
    public void updateFirmware(@NonNull String address, @NonNull String filePath, boolean isSecurity) {
        this.address = address;
        this.filePath = filePath;
        this.otaType = isSecurity ? OTAType.Security : OTAType.OTA;
        initStatus();
        otaUtils.connectDevice(address);
    }

    /**
     * 更新资源
     * @param address 地址
     * @param filePath 升级文件路径
     */
    public void updateResource(@NonNull String address, @NonNull String filePath){
        this.address = address;
        this.filePath = filePath;
        this.otaType = OTAType.RESOURCE;
        initStatus();
        otaUtils.connectDevice(address);
    }

    /**
     * 开始OTA 升级
     */
    private void startOTA() {
        switch (otaType) {
            case OTA:
                otaUtils.updateFirmware(filePath);
                STATUS = OTA_ING;
                break;
            case RESOURCE:
                otaUtils.updateResource(filePath);
                STATUS = RES_ING;
                break;
            case Security:
                otaUtils.startSecurity();
                STATUS = RES_ING;
                break;
            default:
                break;
        }
    }

    /**
     * 取消OTA 升级
     */
    public void cancelOTA() {
        otaUtils.cancelOTA();
        initStatus();
    }

    public void setRetryTimes(int times) {
        if (otaUtils != null) {
            otaUtils.setRetryTimes(times);
        }
    }

    private void initStatus() {
        STATUS = 0;
    }

    private void error(int code) {
        initStatus();
        firmwareCallback.onError(code);
        otaUtils.close();
    }

    private void success() {
        initStatus();
        firmwareCallback.onUpdateComplete();
        otaUtils.close();
    }

    /**
     * 实现OTA 接口回调
     */
    private class OTACallbackImpl implements OTACallback {
        @Override
        public void onConnected(boolean isConnected) {
            if (isConnected) {
                KLog.e("TAG", "onConnected: STATUS:" + STATUS + ",otaType:" + otaType);
                if (STATUS == 0) {
                    //建立连接之后走这里
                    if (otaType == OTAType.OTA) {
                        STATUS = START_OTA;
                    } else if (otaType == OTAType.RESOURCE) {
                        STATUS = START_RES;
                    } else if (otaType == OTAType.Security) {
                        STATUS = RES_ING;
                    }
                } else if (otaType == OTAType.OTA && STATUS == START_OTA) {//设置MTU和enable之后会再一次回调走这里
                    otaUtils.startOTA();
                } else if (otaType == OTAType.Security && STATUS == RES_ING) {
                    otaUtils.startSecurity();
                } else if (otaType == OTAType.RESOURCE && STATUS == START_RES) {
                    otaUtils.startResource();
                } else if (STATUS == OTA_CONNECTING) {//切换OTA模式断开重连走这里
                    STATUS = APP2OTA;
                } else if (STATUS == APP2OTA) {
                    KLog.e("TAG", "onConnected: 222222222222222");
                    startOTA();
                } else {
                    //错误的情况
                    KLog.d("STATUS", "error:" + STATUS);
                }
            } else {
                if (STATUS == START_OTA || STATUS == START_RES || STATUS == RES_ING) {
                    KLog.e("TAG", "onConnected: 断开连接，重新扫描");
                    //从APP模式切换到OTA模式的断开情况
                    otaUtils.startScanDevice();
                } else if (STATUS == OTA_CONNECTING || STATUS == OTA_ING) {
                    error(ErrorCode.OTA_CONNECT_ERROR);
                } else if (STATUS == REBOOT) {
                    success();
                } else {
                    error(ErrorCode.CONNECT_ERROR);
                }
            }
        }

        @Override
        public void onOTA(boolean isConnected) {
            if (isConnected) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    otaUtils.setPHY();
                } else {
                    startOTA();
                }
            }
        }

        @Override
        public void onDeviceSearch(BluetoothDevice device, int rssi, byte[] scanRecord) {
            boolean isEquals = device.getAddress().equals(BleHelper.getOTAMac(address));
            if ((STATUS == START_OTA || STATUS == START_RES || STATUS == RES_ING) && isEquals) {
                otaUtils.stopScanDevice();
                otaUtils.connectDevice(device.getAddress());

                STATUS = OTA_CONNECTING;
            }
        }

        @Override
        public void onProcess(float process) {
            firmwareCallback.onProcess(process);
        }

        @Override
        public void onError(int code) {
            KLog.d("onError", "error:" + code);
            error(code);
        }

        @Override
        public void onOTASendFinish() {
            STATUS = REBOOT;
            otaUtils.reBoot();
        }

        @Override
        public void onResource(boolean isConnected) {
            if (isConnected) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    otaUtils.setPHY();
                } else {
                    startOTA();
                }
            }
        }

        @Override
        public void onResourceSendFinish() {
            STATUS = REBOOT;
            otaUtils.reBoot();
        }

        @Override
        public void onReboot() {

        }

        @Override
        public void onRebootSuccess() {
            otaUtils.disConnectDevice();
        }

        @Override
        public void onPhyUpdate() {
            startOTA();
        }

        @Override
        public void onStartSecurityData() {
            STATUS = OTA_ING;
            otaUtils.updateFirmware(filePath, OTAType.Security);
        }
    }
}
