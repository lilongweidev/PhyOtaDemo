package com.phy.ota.sdk.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

/**
 * OTA 应用回调
 * @author llw
 */

public interface OTAUtilsCallback {

    /**
     * 搜索设备
     * @param device 蓝牙设备
     * @param rssi 设备信号强度
     * @param scanRecord 扫描记录
     */
    void onDeviceSearch(BluetoothDevice device, int rssi, byte[] scanRecord);
    /**
     * 设备连接改变
     * @param isConnected true：连接设备 ， false：断开连接
     */
    void onDeviceConnectChange(boolean isConnected);

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
     * 资源文件发送完成
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
    void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status);

    /**
     * 开始数据加密
     */
    void onStartSecurityData();
}
