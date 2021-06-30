package com.phy.ota.sdk.ble;

/**
 * 更新固件回调
 * @author llw
 */
public interface UpdateFirmwareCallback {

    /**
     * OTA更新进度
     * @param process 进度
     */
    void onProcess(float process);

    /**
     * OTA更新完成
     */
    void onUpdateComplete();

    /**
     * 发生错误
     * @param code 错误码
     */
    void onError(int code);
}
