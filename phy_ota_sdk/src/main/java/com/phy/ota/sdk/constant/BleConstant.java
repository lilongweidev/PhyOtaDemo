package com.phy.ota.sdk.constant;

/**
 * Ble(低功耗蓝牙)常量
 * en：Ble(Bluetooth Low Energy) Constant
 * @author llw
 */
public class BleConstant {

    /**
     * 服务 UUID (en：Service UUID)
     */
    public static final String SERVICE_UUID = "0000ff01-0000-1000-8000-00805f9b34fb";
    /**
     * 特性写入 UUID (en：Characteristic Write UUID)
     */
    public static final String CHARACTERISTIC_WRITE_UUID = "0000ff02-0000-1000-8000-00805f9b34fb";
    /**
     * 特性读取 UUID (en：Characteristic Read UUID)
     */
    public static final String CHARACTERISTIC_READ_UUID = "0000ff10-0000-1000-8000-00805f9b34fb";
    /**
     * 描述 UUID (en：Descriptor UUID)
     */
    public static final String DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    /**
     * 电池服务 UUID (en：Battery Service UUID)
     */
    public static final String BATTERY_SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb";
    /**
     * 电池特征（特性）读取 UUID (en：Battery Characteristic Read UUID)
     */
    public static final String BATTERY_CHARACTERISTIC_READ_UUID = "00002a19-0000-1000-8000-00805f9b34fb";
    /**
     * OTA服务 UUID (en：OTA Service UUID)
     */
    public static final String OTA_SERVICE_UUID = "5833ff01-9b8b-5191-6142-22a4536ef123";
    /**
     * OTA特征（特性）写入 UUID (en：OTA Characteristic Write UUID)
     */
    public static final String OTA_CHARACTERISTIC_WRITE_UUID = "5833ff02-9b8b-5191-6142-22a4536ef123";
    /**
     * OTA特征（特性）表示 UUID (en：OTA Characteristic Indicate UUID)
     */
    public static final String OTA_CHARACTERISTIC_INDICATE_UUID = "5833ff03-9b8b-5191-6142-22a4536ef123";
    /**
     * OTA数据特征（特性）写入 UUID (en：OTA Data Characteristic Write UUID)
     */
    public static final String OTA_DATA_CHARACTERISTIC_WRITE_UUID = "5833ff04-9b8b-5191-6142-22a4536ef123";
}
