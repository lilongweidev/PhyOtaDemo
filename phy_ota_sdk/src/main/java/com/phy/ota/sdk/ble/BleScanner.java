package com.phy.ota.sdk.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;


/**
 * Ble扫描
 *
 * @author llw
 */
public class BleScanner {

    private Context mContext;
    private OTAUtilsCallback mOtaUtilsCallback;
    private MyLeScanCallback leScanCallback;
    private MyScanCallback scanCallback;


    public BleScanner(Context context) {
        this.mContext = context;
    }

    public BleScanner(Context context, OTAUtilsCallback otaUtilsCallback) {
        this.mContext = context;
        this.mOtaUtilsCallback = otaUtilsCallback;
    }


    private no.nordicsemi.android.support.v18.scanner.ScanCallback scanCallbackCompat = new no.nordicsemi.android.support.v18.scanner.ScanCallback() {
        /**
         * 扫描结果
         * @param callbackType 回调类型
         * @param result 结果
         */
        @Override
        public void onScanResult(int callbackType, @NonNull @org.jetbrains.annotations.NotNull no.nordicsemi.android.support.v18.scanner.ScanResult result) {
            if(BandUtil.bleCallBack != null){
                BandUtil.bleCallBack.onScanDevice(result.getDevice(),result.getRssi(),result.getScanRecord().getBytes());
            }else {
                throw  new RuntimeException("bleCallBack is null");
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            throw  new RuntimeException("Scan error");
        }
    };

    public void startScanDeviceBasic(){

        BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        scanner.startScan(scanCallbackCompat);
    }

    public void stopScanDeviceBasic(){
        BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        scanner.stopScan(scanCallbackCompat);
    }

    /**
     * 扫描设备
     */
    public void scanDevice() {
        BluetoothManager manager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //Android 5.0以下
            leScanCallback = new MyLeScanCallback();
            adapter.startLeScan(leScanCallback);
        } else {
            scanCallback = new MyScanCallback();
            adapter.getBluetoothLeScanner().startScan(scanCallback);
        }
    }

    /**
     * 停止扫描设备
     */
    public void stopScanDevice(){
        BluetoothManager mBluetoothManager = (BluetoothManager) mContext.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = mBluetoothManager.getAdapter();
        if(adapter != null && adapter.isEnabled()){

            if(leScanCallback != null || scanCallback != null){
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                    adapter.stopLeScan(leScanCallback);
                }else {
                    adapter.getBluetoothLeScanner().stopScan(scanCallback);
                }
            }
        }
    }


    /**
     * LeScanCallback
     */
    class MyLeScanCallback implements BluetoothAdapter.LeScanCallback {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (mOtaUtilsCallback != null) {
                mOtaUtilsCallback.onDeviceSearch(device, rssi, scanRecord);
            } else {
                throw new RuntimeException("PHYBleCallBack is null");
            }
        }
    }

    /**
     * ScanCallback
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    class MyScanCallback extends ScanCallback {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (mOtaUtilsCallback != null) {
                mOtaUtilsCallback.onDeviceSearch(result.getDevice(),result.getRssi(),result.getScanRecord().getBytes());
            } else {
                throw new RuntimeException("PHYBleCallBack is null");
            }
        }
    }
}
