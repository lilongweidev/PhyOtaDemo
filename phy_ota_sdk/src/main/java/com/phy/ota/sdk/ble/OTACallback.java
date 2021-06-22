package com.phy.ota.sdk.ble;

import android.bluetooth.BluetoothDevice;

/**
 * OTA 回调接口
 * @author llw
 */
public interface OTACallback {

    /**
     * 设备连接状态变化，
     * @param isConnected true：连接成功，false：断开连接
     */
    void onConnected(boolean isConnected);

    /**
     * 设置设备OTA状态。
     * @param isConnected true：设备进入OTA状态，并连接成功。false：设备进入OTA状态，但未连接
     */
    void onOTA(boolean isConnected);

    /**
     * 设置设备Resource状态。
     * @param isConnected true：设备进入Resource状态，并连接成功。false：设备进入Resource状态，但未连接
     */
    void onResource(boolean isConnected);

    /**
     * 搜索设备
     * @param device 蓝牙设备
     * @param rssi 设备信号强度
     * @param scanRecord 扫描记录
     */
    void onDeviceSearch(BluetoothDevice device, int rssi, byte[] scanRecord);

    /**
     * OTA 进度%
     * @param process 进度
     */
    void onProcess(float process);

    /**
     * 异常信息
     * @param code 错误码
     */
    void onError(int code);

    /**
     * OTA文件发送完成
     */
    void onOTASendFinish();

    /**
     * Resource文件发送完成
     */
    void onResourceSendFinish();

    /**
     * 重启设备
     */
    void onReboot();

    /**
     * 重启设备成功
     */
    void onRebootSuccess();

    /**
     * 更新
     */
    void onPhyUpdate();

    /**
     * 开始数据加密
     */
    void onStartSecurityData();
}
