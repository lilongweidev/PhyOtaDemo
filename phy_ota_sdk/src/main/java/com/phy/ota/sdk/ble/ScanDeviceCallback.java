package com.phy.ota.sdk.ble;

import android.bluetooth.BluetoothDevice;

/**
 * Ble App回调
 *
 * @author llw
 */
public interface ScanDeviceCallback {

    /**
     * 扫描设备
     *
     * @param device     蓝牙设备
     * @param rssi       信号强度
     * @param scanRecord 扫描记录
     */
    void onScanDevice(BluetoothDevice device, int rssi, byte[] scanRecord);

    /**
     * 连接设备
     *
     * @param connect 连接结果 true or false
     */
    void onConnectDevice(boolean connect);
}
