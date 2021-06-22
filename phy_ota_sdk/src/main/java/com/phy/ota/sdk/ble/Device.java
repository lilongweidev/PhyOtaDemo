package com.phy.ota.sdk.ble;

import android.bluetooth.BluetoothDevice;

import java.util.Objects;

/**
 * 蓝牙设备
 * @author llw
 */
public class Device {

    private BluetoothDevice device;//蓝牙设备
    private int rssi;//信号强度
    private int broadcastType;//广播类型
    private String realName;//真实名称

    /**
     * 构造Device
     * @param device 蓝牙设备
     * @param rssi 信号强度
     * @param broadcastType 广播类型
     * @param realName 真实名称
     */
    public Device(BluetoothDevice device, int rssi, int broadcastType, String realName) {
        this.device = device;
        this.rssi = rssi;
        this.broadcastType = broadcastType;
        this.realName = realName;
    }

    public BluetoothDevice getDevice(){
        return device;
    }

    public int getRssi(){
        return rssi;
    }


    public  int getBroadcastType(){
        return broadcastType;
    }

    public String getRealName(){
        return realName;
    }

    @Override
    public boolean equals(Object object) {
        if(object instanceof Device){
            final Device that =(Device) object;
            return device.getAddress().equals(that.device.getAddress());
        }
        return super.equals(object);
    }
}
