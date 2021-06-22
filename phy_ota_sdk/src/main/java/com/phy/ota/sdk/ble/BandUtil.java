package com.phy.ota.sdk.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import com.phy.ota.sdk.utils.KLog;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by zhoululu on 2017/6/21.
 */

public class BandUtil {

    String TAG = getClass().getSimpleName();

    private String mac;

    private static BandUtil bandUtil;

    private BleScanner mBleScanner;
    private Context context;
    BluetoothGatt bluetoothGatt;

    public static ScanDeviceCallback bleCallBack;

    BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            KLog.e("app gatt=", gatt.toString());

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                if (gatt != null) {
                    gatt.close();
                }

                bleCallBack.onConnectDevice(false);
            }
        }

        /**
         * 发现服务
         * @param gatt
         * @param status
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> serviceList = gatt.getServices();
            for (BluetoothGattService service : serviceList) {
                KLog.d("service uuid", service.getUuid().toString());
                List<BluetoothGattCharacteristic> characteristicList = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristicList) {
                    KLog.d("charac uuid", characteristic.getUuid().toString());
                    List<BluetoothGattDescriptor> descriptorList = characteristic.getDescriptors();
                    for (BluetoothGattDescriptor descriptor : descriptorList) {
                        KLog.d("descriptor uuid", descriptor.getUuid().toString());
                    }
                }
            }

            bleCallBack.onConnectDevice(true);
        }

        /**
         * 特性读取
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        /**
         * 特性写入
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        /**
         * 特性改变
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        /**
         * 描述读取
         */
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        /**
         * 描述写入
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }
    };

    private BandUtil(Context context) {
        this.context = context;
        mBleScanner = new BleScanner(context);
    }

    public static BandUtil getBandUtil(Context context) {
        if (bandUtil == null) {
            synchronized (BandUtil.class) {
                if (bandUtil == null) {
                    bandUtil = new BandUtil(context);
                }
            }
        }
        return bandUtil;
    }

    /**
     * 扫描设备
     */
    public void scanDevice() {
        if (mBleScanner != null) {
            mBleScanner.startScanDeviceBasic();
        }
        KLog.e(TAG, "scanDevice");
    }

    /**
     * 停止扫描设备
     */
    public void stopScanDevice() {
        if (mBleScanner != null) {
            mBleScanner.stopScanDeviceBasic();
        }
        KLog.e(TAG, "stopScanDevice ");
    }

    /**
     * 连接设备
     * @param macAddress mac地址
     */
    public void connectDevice(String macAddress) {
        BluetoothManager mBluetoothManager = (BluetoothManager) context.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothDevice device = mBluetoothManager.getAdapter().getRemoteDevice(macAddress);

        bluetoothGatt = device.connectGatt(context.getApplicationContext(), false, callback);

        KLog.e("gatt=", bluetoothGatt.toString());
    }

    public void syncTime(final Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        byte[] dateArray = new byte[6];
        dateArray[0] = (byte) (calendar.get(Calendar.YEAR) % 100);
        dateArray[1] = (byte) (calendar.get(Calendar.MONTH) + 1);
        dateArray[2] = (byte) calendar.get(Calendar.DAY_OF_MONTH);
        dateArray[3] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
        dateArray[4] = (byte) calendar.get(Calendar.MINUTE);
        dateArray[5] = (byte) calendar.get(Calendar.SECOND);

        //mBleCore.sendCommand((byte) 0x02, dateArray);
    }

    public void disConnectDevice() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }

    public static void setBleCallBack(ScanDeviceCallback bleCallback) {
        BandUtil.bleCallBack = bleCallback;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public static boolean isBleOpen(Context context) {
        BluetoothManager mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            return false;
        }
        BluetoothAdapter adapter = mBluetoothManager.getAdapter();
        if (adapter == null) {
            return false;
        }

        return adapter.isEnabled();
    }

}
