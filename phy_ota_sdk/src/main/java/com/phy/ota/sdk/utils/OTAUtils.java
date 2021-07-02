package com.phy.ota.sdk.utils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.phy.ota.sdk.ble.BleCallback;
import com.phy.ota.sdk.ble.BleScanner;
import com.phy.ota.sdk.ble.FirmWareFile;
import com.phy.ota.sdk.ble.OTACallback;
import com.phy.ota.sdk.ble.OTAType;
import com.phy.ota.sdk.ble.OTAUtilsCallback;
import com.phy.ota.sdk.constant.BaseConstant;
import com.phy.ota.sdk.constant.ErrorCode;

/**
 * OTA
 *
 * @author llw
 */
public class OTAUtils {

    public static final String TAG = OTAUtils.class.getSimpleName();

    /**
     * 包大小
     */
    public static int MTU_SIZE = 23;
    /**
     * 上下文参数
     */
    private Context mContext;
    /**
     * ble扫描
     */
    private BleScanner mBleScanner;
    /**
     * ble回调
     */
    private BleCallback mBleCallBack;
    /**
     * 蓝牙Gatt
     */
    private BluetoothGatt mBluetoothGatt;
    /**
     * 是否连接
     */
    private boolean isConnected;
    /**
     * OTA回调
     */
    private OTACallback callBack;

    /**
     * 创建OTAUtils实例
     *
     * @param context  上下文
     * @param callBack OTA回调
     */
    public OTAUtils(Context context, OTACallback callBack) {
        this.mContext = context;
        this.callBack = callBack;
        //初始化
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        OTAUtilsCallback otaUtilsCallback = new OTAUtilsCallbackImpl();
        //ble扫描 设置ota 回调
        mBleScanner = new BleScanner(mContext, otaUtilsCallback);
        mBleCallBack = new BleCallback();
        //ble回调 设置ota 回调
        mBleCallBack.setOtaUtilsCallBack(otaUtilsCallback);
        //初始化缓存
        SPUtils.init(mContext);
        mBleCallBack.pwd = SPUtils.getString(BaseConstant.AES_KEY, "");
        Log.d("pwd",mBleCallBack.pwd);
    }

    /**
     * 连接设备
     *
     * @param address 设备地址
     */
    public void connectDevice(@NonNull String address) {
        if (isConnected) {
            KLog.e(TAG, "connectDevice: 已经连上");
            callBack.onConnected(isConnected);
            return;
        }

        BluetoothManager manager = (BluetoothManager) mContext.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        //获取远程设备
        BluetoothDevice remoteDevice = manager.getAdapter().getRemoteDevice(address);
        mBluetoothGatt = remoteDevice.connectGatt(mContext.getApplicationContext(), false, mBleCallBack);
    }

    /**
     * 断开设备连接
     */
    public void disConnectDevice() {
        if (isConnected && mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
    }

    /**
     * 开始扫描设备
     */
    public void startScanDevice() {
        mBleScanner.scanDevice();
    }

    /**
     * 停止扫描设备
     */
    public void stopScanDevice() {
        mBleScanner.stopScanDevice();
    }

    /**
     * OTA Hex格式
     */
    public void startOTA() {
        if (!isConnected) {
            callBack.onError(ErrorCode.DEVICE_NOT_CONNECT);
            return;
        }

        if (BleHelper.checkIsOTA(mBluetoothGatt)) {
            callBack.onOTA(true);
        } else {
            //升级⽂件为hex⽂件时 发送: 0x0102
            String command = "0102";
            boolean success = sendCommand(mBluetoothGatt, command);
            if (success) {
                callBack.onOTA(false);
            }
        }
    }

    /**
     * OTA  Res格式
     */
    public void startResource() {
        if (!isConnected) {
            callBack.onError(ErrorCode.DEVICE_NOT_CONNECT);
            return;
        }

        if (BleHelper.checkIsOTA(mBluetoothGatt)) {
            callBack.onResource(true);
        } else {
            //升级⽂件为res⽂件时，发送： 0x0103
            String command = "0103";
            boolean success = sendCommand(mBluetoothGatt, command);
            if (success) {
                callBack.onResource(false);
            }
        }
    }

    /**
     * 固件设备重启
     */
    public void reBoot() {
        if (!isConnected) {
            callBack.onError(ErrorCode.DEVICE_NOT_CONNECT);
            return;
        }
        if (!BleHelper.checkIsOTA(mBluetoothGatt)) {
            callBack.onError(ErrorCode.DEVICE_NOT_IN_OTA);
            return;
        }
        String command = "04";
        boolean success = sendCommand(mBluetoothGatt, command);
        if (success) {
            callBack.onReboot();
        }
    }

    /**
     * 升级固件
     *
     * @param filePath 文件路径
     * @param otaType
     */
    public void updateFirmware(@NonNull String filePath, OTAType otaType) {
        //检查设备是否已连接
        if (!isConnected) {
            callBack.onError(ErrorCode.DEVICE_NOT_CONNECT);
            return;
        }
        //检查设备是否已经在OTA状态
        if (!BleHelper.checkIsOTA(mBluetoothGatt)) {
            callBack.onError(ErrorCode.DEVICE_NOT_IN_OTA);
            return;
        }

        FirmWareFile firmWareFile = new FirmWareFile(filePath);
        if (firmWareFile.getCode() != 200) {
            callBack.onError(ErrorCode.FILE_PARSE_ERROR);
            return;
        }
        mBleCallBack.setFirmWareFile(firmWareFile, otaType);

        String command = "01" + HexString.int2ByteString(firmWareFile.getList().size());
        command = command + "00";
        sendCommand(mBluetoothGatt, command);
    }

    public void updateFirmware(@NonNull String filePath) {
        updateFirmware(filePath, OTAType.OTA);
    }

    /**
     * 升级Resource
     */
    public void updateResource(@NonNull String filePath) {
        //检查设备是否已连接
        if (!isConnected) {
            callBack.onError(ErrorCode.DEVICE_NOT_CONNECT);
            return;
        }
        //检查设备是否已经在Resource状态
        if (!BleHelper.checkIsOTA(mBluetoothGatt)) {
            callBack.onError(ErrorCode.DEVICE_NOT_IN_OTA);
            return;
        }
        FirmWareFile firmWareFile = new FirmWareFile(filePath);
        if (firmWareFile.getCode() != 200) {
            callBack.onError(ErrorCode.FILE_PARSE_ERROR);
            return;
        }
        mBleCallBack.setFirmWareFile(firmWareFile, OTAType.RESOURCE);
        String command = "01" + HexString.int2ByteString(firmWareFile.getList().size());
        command = command + "00";
        //发送命令
        sendCommand(mBluetoothGatt, command);
    }

    /**
     * 取消OTA升级后，会和设备自动断开连接
     */
    public void cancelOTA() {
        if (checkOTA()) {
            mBleCallBack.setCancel();
            mBluetoothGatt.disconnect();
        }
    }

    public boolean checkOTA() {
        return isConnected && BleHelper.checkIsOTA(mBluetoothGatt);
    }

    public void close(){
        if(mBluetoothGatt != null){
            mBluetoothGatt.close();
        }
    }

    /**
     * 设置OTA数据发送失败重试次数
     */
    public void setRetryTimes(int times) {
        mBleCallBack.setRetryTimes(times);
    }

    /**
     * 设置PHY
     */
    public boolean setPHY() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mBluetoothGatt != null) {
                mBluetoothGatt.setPreferredPhy(BluetoothDevice.PHY_LE_2M, BluetoothDevice.PHY_LE_2M, BluetoothDevice.PHY_OPTION_NO_PREFERRED);
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * 发送命令
     */
    private boolean sendCommand(BluetoothGatt bluetoothGatt, String command) {
        //发送OTA命令
        boolean success = BleHelper.sendOTACommand(bluetoothGatt, command, true);
        if (!success) {
            callBack.onError(ErrorCode.OTA_COMMAND_SEND_SERVICE_NOT_FOUND);
        }
        return success;
    }

    /**
     * 启动安全传输（数据加密）
     */
    public void startSecurity() {
        mBleCallBack.startSecurity(mBluetoothGatt);
    }

    /**
     * OTAUtilsCallBack 实现接口方法
     */
    private class OTAUtilsCallbackImpl implements OTAUtilsCallback {

        @Override
        public void onDeviceSearch(BluetoothDevice device, int rssi, byte[] scanRecord) {
            //OTA 回调 设备搜索
            callBack.onDeviceSearch(device, rssi, scanRecord);
        }

        @Override
        public void onDeviceConnectChange(boolean connect) {
            isConnected = connect;
            //OTA 回调 设备连接
            callBack.onConnected(connect);
        }

        @Override
        public void onProcess(float process) {
            //OTA 回调 进度
            callBack.onProcess(process);
        }

        @Override
        public void onError(int code) {
            KLog.e("OTA Error", "onError: =========" + code);
            //OTA 回调 异常错误
            callBack.onError(code);
        }

        @Override
        public void onOTASendFinish() {
            //OTA 回调 OTA文件发送完成
            callBack.onOTASendFinish();
        }

        @Override
        public void onResourceSendFinish() {
            //OTA 回调 资源文件发送完成
            callBack.onResourceSendFinish();
        }

        @Override
        public void onReboot() {
            //OTA 回调 重启设备
            callBack.onReboot();
        }

        @Override
        public void onRebootSuccess() {
            //OTA 回调 重启设备成功
            callBack.onRebootSuccess();
        }

        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            //OTA 回调 固件更新
            callBack.onPhyUpdate();
        }

        @Override
        public void onStartSecurityData() {
            //OTA 回调 开始数据加密
            callBack.onStartSecurityData();
        }
    }
}
