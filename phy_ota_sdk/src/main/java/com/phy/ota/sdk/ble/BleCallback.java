package com.phy.ota.sdk.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.util.Log;

import com.phy.ota.sdk.OTAUtils;
import com.phy.ota.sdk.constant.BleConstant;
import com.phy.ota.sdk.constant.ErrorCode;
import com.phy.ota.sdk.utils.AESTool;
import com.phy.ota.sdk.utils.BleHelper;
import com.phy.ota.sdk.utils.HexString;
import com.phy.ota.sdk.utils.KLog;

import java.util.List;

import static com.phy.ota.sdk.ble.OTAType.RESOURCE;
import static com.phy.ota.sdk.constant.BaseConstant.*;
import static com.phy.ota.sdk.constant.BleConstant.*;

/**
 * Ble 回调
 *
 * @author llw
 */
public class BleCallback extends BluetoothGattCallback {
    private static final String TAG = BleCallback.class.getSimpleName();

    private OTAUtilsCallback mOtaUtilsCallback;
    private FirmWareFile firmWareFile;
    private OTAType otaType;

    private int partitionIndex = 0;
    private int blockIndex = 0;
    private int cmdIndex = 0;
    private long mFlashAddress = 0;
    private List<String> cmdList;

    private boolean isResponse;
    private String response;

    private float totalSize;
    private float finishSize;

    private int retryTimes = 3;
    private int cmdErrorTimes;
    private int blockErrorTimes;

    private boolean isCancel;

    private int errorPartitionId = -1;
    private int errorBlockId = -1;
    private boolean isRetrying;

    //加密流程中需要用到的参数
    private String appCiphertext;//App端密文
    private String firmwareCiphertext;//固件端密文
    public String pwd;


    public void setOtaUtilsCallBack(OTAUtilsCallback otaUtilsCallback) {
        this.mOtaUtilsCallback = otaUtilsCallback;
    }

    /**
     * 设置固件文件
     *
     * @param firmWareFile 固件文件
     * @param otaType      OTA类型
     */
    public void setFirmWareFile(FirmWareFile firmWareFile, OTAType otaType) {
        this.firmWareFile = firmWareFile;
        this.otaType = otaType;
        totalSize = firmWareFile.getLength();
        //初始化数据
        initData();
    }

    /**
     * 初始化数据（设置固件文件时进行）
     */
    private void initData() {
        partitionIndex = 0;//分区索引
        blockIndex = 0;//区块索引
        cmdIndex = 0;//cmd索引
        mFlashAddress = 0;//闪存地址
        cmdList = null;
        finishSize = 0;
        isCancel = false;
    }

    public void setCancel() {
        isCancel = true;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    /**
     * 连接状态改变
     *
     * @param gatt     普通属性协议
     * @param status   连接状态
     * @param newState 新状态
     */
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        switch (newState) {
            case BluetoothProfile.STATE_CONNECTED://连接成功
                KLog.i(TAG, "onConnectionStateChange:连接成功 ");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    gatt.requestMtu(512);//增加MTU大小，最大是512
                } else {
                    gatt.discoverServices();//发现服务
                }
                break;
            case BluetoothProfile.STATE_DISCONNECTED://断开连接
                KLog.i(TAG, "onConnectionStateChange:断开连接 ");
                if (gatt != null) {
                    gatt.close();
                }
                //设置回调中连接断开
                mOtaUtilsCallback.onDeviceConnectChange(false);
                break;
            default:
                break;
        }
    }

    /**
     * 发现服务
     *
     * @param gatt   普通属性协议
     * @param status 状态
     */
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            KLog.i(TAG, "onServicesDiscovered: Success");
            //开启通知，返回false，断开连接
            boolean notifyOpen = BleHelper.enableIndicateNotification(gatt);

            if (BleHelper.checkIsOTA(gatt)) {
                KLog.i(TAG, "onServicesDiscovered: 连接OTA模式");
                mOtaUtilsCallback.onDeviceConnectChange(true);
            } else {
                //如果不是OTA状态，直接返回
                KLog.i(TAG, "onServicesDiscovered: 连接应用模式");
                if (notifyOpen) {
                    mOtaUtilsCallback.onDeviceConnectChange(true);
                } else {
                    Log.e(TAG, "开启通知属性异常");
                }
            }

            if (!notifyOpen) {
                gatt.disconnect();
            }
        } else {
            //断开连接
            gatt.disconnect();
        }
    }

    /**
     * 特性写入
     *
     * @param gatt           普通属性协议
     * @param characteristic 特性
     * @param status         状态
     */
    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.e(TAG, "onCharacteristicWrite: 数据发送成功：" + response);
        String uuidStr = characteristic.getUuid().toString();
        switch (uuidStr) {
            case OTA_CHARACTERISTIC_WRITE_UUID:
                //OTA特征写入 UUID
                onOTACharacteristicWrite(gatt, characteristic);
                break;
            case OTA_DATA_CHARACTERISTIC_WRITE_UUID:
                //OTA数据 特征写入 UUID
                onOTADataCharacteristicWrite(gatt, characteristic, status);
                break;
            default:
                break;
        }
    }

    /**
     * 特征改变
     *
     * @param gatt           普通属性协议
     * @param characteristic 特性
     */
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        String uuidStr = characteristic.getUuid().toString();
        response = HexString.parseStringHex(characteristic.getValue());
        if (uuidStr.equals(OTA_CHARACTERISTIC_INDICATE_UUID)) {
            Log.e(TAG, "收到特征值:" + HexString.parseStringHex(characteristic.getValue()));
            isResponse = true;
            Log.e(TAG,response);
            //0087  一组16*20 ota数据发送成功，开始下一组
            if (ONCE_OTA_SEND_SUCCESS.equals(response)) {
                KLog.e(TAG, "一组16*20 ota数据发送成功，开始下一组");
                otaSendSuccess(gatt);
                //0085  一个partition 数据发送成功，发送下一个partition命令
            } else if (ONCE_PARTITION_SEND_SUCCESS.equals(response)) {
                KLog.e(TAG, "一个partition 数据发送成功，开始下一个");
                partitionSendSuccess(gatt);
                //0083 所有ota数据发送成功
            } else if (ALL_OTA_SEND_SUCCESS.equals(response)) {
                KLog.e(TAG, "所有ota数据发送成功");
                allOTASendSuccess();
                //高速模式下 发送reboot
            } else if (HIGH_SPEED_SEND_REBOOT.equals(response.toLowerCase())) {
                mOtaUtilsCallback.onRebootSuccess();
                //高速模式下 进入ota
            } else if (("00").equals(response)) {
                KLog.e(TAG, "========111111");
                gatt.disconnect();
            } else if (("6887".equals(response))) {
                if (!isCancel) {
                    retry(gatt, ErrorCode.OTA_DATA_WRITE_ERROR);
                }
            } else if (response.length() == 34 && response.startsWith("71")) {
                firmwareCiphertext = response.substring(2);
                //这里加休眠，时间不准确，Thread.sleep(100);
                //在onCharacteristicWrite回调中去处理
            } else {
                if (!"0081".equals(response) && !("0084").equals(response) && !"0089".equals(response)) {
                    mOtaUtilsCallback.onError(ErrorCode.OTA_RESPONSE_ERROR);
                    KLog.e(TAG, "error:" + response);
                }
            }
        }
    }


    /**
     * 描述写入
     *
     * @param gatt   普通属性协议
     *               characteristic 特性
     * @param status 状态
     */
    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (BleConstant.DESCRIPTOR_UUID.equals(descriptor.getUuid().toString().toLowerCase())) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //连接设备，在discoverService和设置notification之后返回
                mOtaUtilsCallback.onDeviceConnectChange(true);
            } else {
                gatt.disconnect();
            }
        }
    }

    /**
     * 更新
     */
    @Override
    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        mOtaUtilsCallback.onPhyUpdate(gatt, txPhy, rxPhy, status);
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            OTAUtils.MTU_SIZE = mtu;
        } else {
            OTAUtils.MTU_SIZE = 23;
        }
        gatt.discoverServices();
    }

    /**
     * 所有ota数据发送成功
     */
    private void allOTASendSuccess() {
        if (otaType == OTAType.OTA || otaType == OTAType.Security) {
            mOtaUtilsCallback.onOTASendFinish();
        } else if (otaType == OTAType.RESOURCE) {
            mOtaUtilsCallback.onResourceSendFinish();
        }
    }

    /**
     * 一组16*20 ota数据发送成功
     */
    private void otaSendSuccess(BluetoothGatt gatt) {
        if (blockErrorTimes > 0) {
            blockErrorTimes = 0;
        }
        blockIndex++;
        cmdIndex = 0;
        if (blockIndex < firmWareFile.getList().get(partitionIndex).getBlocks().size()) {
            cmdList = firmWareFile.getList().get(partitionIndex).getBlocks().get(blockIndex);
            sendOTAData(gatt, cmdList.get(cmdIndex));
        }
    }

    /**
     * 一个partition 数据发送成功
     */
    private void partitionSendSuccess(BluetoothGatt gatt) {
        partitionIndex++;
        blockIndex = 0;
        if (partitionIndex < firmWareFile.getList().size()) {
            if (otaType == OTAType.OTA || otaType == OTAType.Security) {
                //后面地址由前一个长度决定
                Partition prePartition = firmWareFile.getList().get(partitionIndex - 1);
                //run addr 在11000000 ~ 1107ffff， flash addr=run addr，其余的，flash addr从0开始递增
                if ((0x11000000 > Long.parseLong(prePartition.getAddress(), 16)) || (Long.parseLong(prePartition.getAddress(), 16) > 0x1107ffff)) {
                    if (firmWareFile.getPath().endsWith(HEXE16)) {
                        mFlashAddress = mFlashAddress + prePartition.getPartitionLength() + 4;
                    } else {
                        mFlashAddress = mFlashAddress + prePartition.getPartitionLength() + 8;
                    }
                }
            }

            sendPartition(gatt, firmWareFile, partitionIndex, mFlashAddress);
        }
    }

    /**
     * OTA特征写入 UUID
     */
    private void onOTACharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (!isResponse) {
            return;
        }
        switch (response) {
            case READY_RECEIVE_SUCCESS://0081
                if (otaType == RESOURCE) {
                    sendResource(gatt, firmWareFile);
                } else {// otaType : OTA or Security
                    sendPartition(gatt, firmWareFile, partitionIndex, mFlashAddress);
                    blockIndex = 0;
                }
                break;
            case FIRMWARE_RECEIVE_SUCCESS://0084
                cmdIndex = 0;
                cmdList = firmWareFile.getList().get(partitionIndex).getBlocks().get(blockIndex);
                sendOTAData(gatt, cmdList.get(cmdIndex));
                break;
            case ADDRESS_89://0089
                sendPartition(gatt, firmWareFile, partitionIndex, mFlashAddress);
                blockIndex = 0;
                break;
            default:
                break;
        }

        if (response == null) {
            return;
        }
        if (response.length() == 34) {
            if (response.startsWith("71")) {
                //固件端密文
                firmwareCiphertext = response.substring(2);
                BleHelper.sendOTACommand(gatt, "06" + appCiphertext, true);
            } else if (response.startsWith("72")) {
                //加密验证
                encryptionVerification(gatt, "07");
            } else if (response.startsWith("73")) {
                BleHelper.sendOTACommand(gatt, "0102", true);
                response = "0102";
            } else if (response.startsWith("8B")) {
                firmwareCiphertext = response.substring(2);
                BleHelper.sendOTACommand(gatt, "07" + appCiphertext, true);
            } else if (response.startsWith("8C")) {
                //加密验证
                encryptionVerification(gatt, "08");
            } else if (response.startsWith("8D")) {
                //启动安全数据
                mOtaUtilsCallback.onStartSecurityData();
            }
        }

        if ("0102".equals(response)) {
            gatt.disconnect();
        }

        if ("0102".equals(HexString.parseStringHex(characteristic.getValue()))
                || "0103".equals(HexString.parseStringHex(characteristic.getValue()))) {
            KLog.i(TAG, "start ota or resource");
        }

        isResponse = false;
    }

    /**
     * 加密验证
     */
    private void encryptionVerification(BluetoothGatt gatt, String command) {
        String firmwareStr = AESTool.decrypt(firmwareCiphertext, pwd);
        KLog.e(TAG, "onCharacteristicWrite: " + firmwareStr + " :" + response.substring(2));
        if (response.substring(2).equals(firmwareStr)) {
            String encString = AESTool.encrypt(firmwareStr, appCiphertext);
            String secondEncStr = AESTool.encrypt(encString, pwd);
            BleHelper.sendOTACommand(gatt, command + secondEncStr, true);
        } else {
            Log.e("OTAUtils", "responseSecurity: AES加密验证失败");
        }
    }

    /**
     * OTA数据 特征写入 UUID
     */
    private void onOTADataCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        //一条ota数据发送成功
        if (status == OTA_DATA_SEND_SUCCESS) {
            if (cmdErrorTimes > 0) {
                cmdErrorTimes = 0;
            }

            if (errorPartitionId != partitionIndex || errorBlockId != blockIndex) {
                if (isRetrying) {
                    isRetrying = false;
                    errorPartitionId = -1;
                    errorBlockId = -1;
                }
                finishSize += characteristic.getValue().length;
                //OTA 进度
                mOtaUtilsCallback.onProcess(finishSize * 100 / totalSize);
            }
            cmdIndex++;
            if (cmdIndex < cmdList.size()) {
                sendOTAData(gatt, cmdList.get(cmdIndex));
            }

        } else {
            retryCmd(gatt, ErrorCode.OTA_DATA_WRITE_ERROR);
            //mOtaUtilsCallBack.onError(ErrorCode.OTA_DATA_WRITE_ERROR);
        }
        Log.e(TAG, "save ota data status: " + status);
    }

    /**
     * 发送资源
     */
    private void sendResource(BluetoothGatt gatt, FirmWareFile firmWareFile) {
        if (isCancel) {
            return;
        }
        boolean success = BleHelper.sendResource(gatt, firmWareFile);
        if (!success) {
            mOtaUtilsCallback.onError(ErrorCode.OTA_COMMAND_SEND_SERVICE_NOT_FOUND);
        }
    }

    /**
     * 发送分区
     */
    private void sendPartition(BluetoothGatt gatt, FirmWareFile firmWareFile, int partitionIndex, long flashAddress) {
        if (isCancel) {
            return;
        }
        //run addr 在11000000 ~ 1107ffff， flash addr=run addr，其余的，flash addr从0开始递增
        Partition partition = firmWareFile.getList().get(partitionIndex);
        if ((0x11000000 <= Long.parseLong(partition.getAddress(), 16)) && (Long.parseLong(partition.getAddress(), 16) <= 0x1107ffff)) {
            flashAddress = Long.parseLong(partition.getAddress(), 16);
        }

        if (otaType == OTAType.RESOURCE) {
            mFlashAddress = 0;
            flashAddress = 0;
        }

        boolean success = BleHelper.sendPartition(gatt, firmWareFile, partitionIndex, flashAddress);
        if (!success) {
            mOtaUtilsCallback.onError(ErrorCode.OTA_COMMAND_SEND_SERVICE_NOT_FOUND);
        }
    }

    /**
     * 发送OTA数据
     */
    private void sendOTAData(BluetoothGatt gatt, String data) {
        if (isCancel) {
            return;
        }
        boolean success = BleHelper.sendOTAData(gatt, data);
        Log.e(TAG,"OTAData: "+data);
        if (!success) {
            mOtaUtilsCallback.onError(ErrorCode.OTA_DATA_SEND_SERVICE_NOT_FOUND);
        }
    }

    /**
     * 重试
     */
    private void retry(BluetoothGatt gatt, int errorCode) {
        Log.e(TAG, "retry block:");
        if (blockErrorTimes < retryTimes) {
            errorBlockId = blockIndex;
            errorPartitionId = partitionIndex;
            isRetrying = true;

            cmdIndex = 0;
            cmdList = firmWareFile.getList().get(partitionIndex).getBlocks().get(blockIndex);
            sendOTAData(gatt, cmdList.get(cmdIndex));

            blockErrorTimes++;
        } else {
            mOtaUtilsCallback.onError(errorCode);
        }
    }

    /**
     * cmd重试
     */
    private void retryCmd(BluetoothGatt gatt, int errorCode) {
        KLog.i(TAG, "retryCmd: ");
        if (cmdErrorTimes < retryTimes) {
            sendOTAData(gatt, cmdList.get(cmdIndex));
            cmdErrorTimes++;
        } else {
            mOtaUtilsCallback.onError(errorCode);
        }
    }

    public void startSecurity(BluetoothGatt gatt) {
        //APP模式下先进行一次AES校验
        appCiphertext = BleHelper.getRandomStr();
        String encString = AESTool.encrypt(appCiphertext, pwd);
        if (BleHelper.checkIsOTA(gatt)) {
            BleHelper.sendOTACommand(gatt, "06" + encString, true);
        } else {
            BleHelper.sendOTACommand(gatt, "05" + encString, true);
        }
    }
}
