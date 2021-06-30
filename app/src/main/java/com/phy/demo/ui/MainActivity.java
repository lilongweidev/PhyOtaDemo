package com.phy.demo.ui;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.phy.demo.adapter.DeviceAdapter;
import com.phy.demo.MyApplication;
import com.phy.demo.R;
import com.phy.ota.sdk.ble.BandUtil;
import com.phy.ota.sdk.ble.ScanDeviceCallback;
import com.phy.ota.sdk.ble.Device;
import com.phy.ota.sdk.utils.BleHelper;
import com.phy.ota.sdk.utils.KLog;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    private static int REQUEST_ENABLE_BLUETOOTH = 1;//请求码

    private Device device;
    private List<Device> deviceList = new ArrayList<>();//设备列表
    private DeviceAdapter deviceAdapter;//设备列表适配器
    private RecyclerView rvDevice;

    private ScanDeviceCallback callBack = new ScanDeviceCallback() {
        @Override
        public void onScanDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
            //通过扫描记录解析返回设备真实名称
            String realName = BleHelper.parseDeviceName(scanRecord);
            //将扫描到的设备添加到列表中
            addDeviceList(new Device(device, rssi, 1, realName));
        }

        @Override
        public void onConnectDevice(boolean connect) {
            KLog.i(TAG, "connect change");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化列表
        initList();

    }

    @Override
    protected void onResume() {
        super.onResume();
        deviceList.clear();
        //初始化蓝牙
        initBluetooth();
    }

    /**
     * 添加到设备列表
     *
     * @param device
     */
    private void addDeviceList(Device device) {
        if (device.getDevice().getName() == null) {
            return;
        }
        if (!deviceList.contains(device)) {
            deviceList.add(device);
            //刷新列表适配器
            deviceAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 初始化列表
     */
    private void initList() {
        rvDevice = findViewById(R.id.rv_device);
        deviceAdapter = new DeviceAdapter(R.layout.item_device_rv, deviceList);
        rvDevice.setLayoutManager(new LinearLayoutManager(this));
        //列表item点击事件
        deviceAdapter.setOnItemClickListener((adapter, view, position) -> {
            //自动升级
            autoUpdate(position);
        });
        deviceAdapter.setAnimationEnable(true);
        deviceAdapter.setAnimationWithDefault(BaseQuickAdapter.AnimationType.SlideInRight);
        rvDevice.setAdapter(deviceAdapter);
        //设置扫描回调
        BandUtil.setBleCallBack(callBack);

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, filter);
    }

    /**
     * 自动更新
     *
     * @param position
     */
    private void autoUpdate(int position) {
        BandUtil.getBandUtil(getApplicationContext()).stopScanDevice();
        device = deviceAdapter.getItem(position);
        MyApplication.getApplication().setDevice(device);
        Intent intent = new Intent(MainActivity.this, AutoActivity.class);
        startActivity(intent);
        BandUtil.getBandUtil(getApplicationContext()).setMac(device.getDevice().getAddress());
    }

    /**
     * 初始化蓝牙
     */
    private void initBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {//是否支持蓝牙
            if (bluetoothAdapter.isEnabled()) {//打开
                //开始扫描周围的蓝牙BLE设备
                searchDevice();
            } else {//未打开
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
            }
        } else {
            Toast.makeText(this, "您的设备不支持蓝牙", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 结果返回
     *
     * @param requestCode 请求码
     * @param resultCode  结果码
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                KLog.e(TAG, "蓝牙打开成功");
            } else {
                KLog.e(TAG, "蓝牙打开失败");
            }
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        BandUtil.getBandUtil(getApplicationContext()).stopScanDevice();
                        enableBluetooth(true);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        //to check if BluetoothAdapter is enable by your code
                        searchDevice();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                    default:
                        break;
                }
            }
        }
    };

    /**
     * 启用蓝牙
     */
    public boolean enableBluetooth(boolean enable) {
        BluetoothManager manager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = manager.getAdapter();
        if (bluetoothAdapter == null) {
            return false;
        }

        if (enable) {
            if (!bluetoothAdapter.isEnabled()) {
                return bluetoothAdapter.enable();
            }
            return true;
        } else {
            if (bluetoothAdapter.isEnabled()) {
                return bluetoothAdapter.disable();
            }
            return false;
        }
    }

    /**
     * 搜索设备
     */
    private void searchDevice() {
        deviceList.clear();
        deviceAdapter.notifyDataSetChanged();

        if (BandUtil.isBleOpen(getApplicationContext())) {
            BandUtil.getBandUtil(getApplicationContext()).scanDevice();
        } else {
            Toast.makeText(this, "请打开蓝牙", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
        }
    }


    private long timeMillis;//几点时间
    /**
     * 增加一个退出应用的提示
     *
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - timeMillis) > 2000) {
                Toast.makeText(this, "再按一次退出OTA App",Toast.LENGTH_SHORT).show();
                timeMillis = System.currentTimeMillis();
            } else {
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}