package com.phy.demo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.phy.demo.adapter.DeviceAdapter;
import com.phy.demo.adapter.FileAdapter;
import com.phy.ota.sdk.utils.OTASDKUtils;
import com.phy.ota.sdk.ble.BandUtil;
import com.phy.ota.sdk.ble.ScanDeviceCallback;
import com.phy.ota.sdk.ble.Device;
import com.phy.ota.sdk.ble.FirmWareFile;
import com.phy.ota.sdk.ble.UpdateFirmwareCallback;
import com.phy.ota.sdk.constant.BaseConstant;
import com.phy.ota.sdk.constant.ErrorCode;
import com.phy.demo.dialog.AlertDialog;
import com.phy.demo.dialog.CustomDialog;
import com.phy.ota.sdk.utils.BleHelper;
import com.phy.ota.sdk.utils.KLog;
import com.phy.ota.sdk.utils.SPUtils;
import com.phy.ota.sdk.utils.SizeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.phy.ota.sdk.constant.BaseConstant.*;

/**
 * 功能弹窗帮助类
 *
 * @author llw
 */
public class OTAHelper {

    public static final String TAG = OTAHelper.class.getSimpleName();

    private static Context mContext;

    //秘钥弹窗
    private static AlertDialog aesDialog;
    //设置秘钥弹窗
    private static AlertDialog settingKeyDialog;
    //查看秘钥弹窗
    private static AlertDialog lookKeyDialog;
    //搜索设备弹窗
    private static AlertDialog searchDeviceDialog;
    //加载弹窗
    private static CustomDialog loadingDialog;
    //升级固件结果弹窗
    private static AlertDialog updateResultDialog;
    //秘钥输入框
    private static EditText etKey;
    //清空输入框
    private static ImageView ivClear;
    //设备列表视图
    private static RecyclerView rvDevice;
    //文件路径
    public static String filePath;
    //文件名称
    public static String fileName;
    private static String path = Environment.getExternalStorageDirectory().getPath();
    //设备Mac地址
    public static String macAddress;
    //设备
    private static Device device;
    //设备列表
    private static List<Device> deviceList = new ArrayList<>();
    //设备列表适配器
    private static DeviceAdapter deviceAdapter;

    private static OTASDKUtils otasdkUtils;

    private static boolean flag = false;

    public static void init(Context context) {
        mContext = context;
        loadingDialog = new CustomDialog(mContext);
        //设置升级固件回调
        otasdkUtils = new OTASDKUtils(mContext, updateFirmwareCallback);
        //设置扫描广播
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(receiver, filter);
    }

    /**
     * 扫描连接回调
     */
    private static ScanDeviceCallback callBack = new ScanDeviceCallback() {
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

    /**
     * 固件升级回调
     */
    private static UpdateFirmwareCallback updateFirmwareCallback = new UpdateFirmwareCallback() {
        @Override
        public void onProcess(float process) {
            Log.d(TAG, "onProcess:" + process);
            flag = true;
            int progress = Math.round(process);
            //设置当前进度
        }

        @Override
        public void onUpdateComplete() {
            hideLoading();
            Looper.prepare();
            flag = false;
            showUpdateResultDialog(true, 0);
            Looper.loop();
            Log.d(TAG, "onUpdateComplete");
        }

        @Override
        public void onError(int code) {
            hideLoading();
            Looper.prepare();
            flag = false;
            showUpdateResultDialog(false, code);
            Looper.loop();
            Log.d(TAG, "onError:" + code);
        }
    };

    /**
     * 添加到设备列表
     *
     * @param device
     */
    private static void addDeviceList(Device device) {
        if (device.getDevice().getName() == null) {
            return;
        }
        if (!deviceList.contains(device)) {
            deviceList.add(device);
            //刷新列表适配器
            deviceAdapter.notifyDataSetChanged();
        }
    }

    public static void readyToUpgrade(String macAddress, String fileName, String filePath) {
        OTAHelper.macAddress = macAddress;
        OTAHelper.fileName = fileName;
        OTAHelper.filePath = filePath;
    }

    /**
     * 显示秘钥弹窗
     */
    public static void showAESKeyDialog() {
        String keyStr = SPUtils.getString(BaseConstant.AES_KEY, "");
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .addDefaultAnimation()
                .setContentView(R.layout.dialog_aes)
                .setCancelable(true)
                .setWidthAndHeight(SizeUtils.dp2px(mContext, 280), LinearLayout.LayoutParams.WRAP_CONTENT)
                .setOnClickListener(R.id.tv_setting_key, v -> {
                    showSettingKeyDialog();
                    aesDialog.dismiss();
                }).setOnClickListener(R.id.tv_show_key, v -> {
                    showKeyDialog(keyStr);
                    aesDialog.dismiss();
                }).setOnClickListener(R.id.tv_start_upgrade, v -> {
                    showLoading();
                    //固件升级
                    otasdkUtils.updateFirmware(macAddress, filePath, true);
                    aesDialog.dismiss();
                });
        aesDialog = builder.create();
        LinearLayout layHasKey = aesDialog.getView(R.id.lay_has_key);
        layHasKey.setVisibility(keyStr.isEmpty() ? View.GONE : View.VISIBLE);
        aesDialog.show();
    }

    /**
     * 显示设置秘钥弹窗
     */
    private static void showSettingKeyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .addDefaultAnimation()
                .setContentView(R.layout.dialog_setting_key)
                .setCancelable(true)
                .setWidthAndHeight(SizeUtils.dp2px(mContext, 280), LinearLayout.LayoutParams.WRAP_CONTENT)
                .setOnClickListener(R.id.iv_clear, v -> {
                    etKey.setText("");
                    ivClear.setVisibility(View.GONE);
                })
                .setOnClickListener(R.id.tv_cancel, v -> settingKeyDialog.dismiss())
                .setOnClickListener(R.id.tv_submit, v -> {
                    //提交
                    String key = etKey.getText().toString();
                    if (key.length() != 32) {
                        showMsg("数据长度不足");
                        return;
                    }
                    SPUtils.putString(BaseConstant.AES_KEY, key);
                    copyKey(key);
                    showMsg("设置成功且已复制");
                    settingKeyDialog.dismiss();
                });

        settingKeyDialog = builder.create();
        etKey = settingKeyDialog.getView(R.id.et_key);
        TextView tvNum = settingKeyDialog.getView(R.id.tv_num);
        ivClear = settingKeyDialog.getView(R.id.iv_clear);
        //输入框监听
        String keyStr = SPUtils.getString(BaseConstant.AES_KEY, "");
        ivClear.setVisibility(keyStr.isEmpty() ? View.GONE : View.VISIBLE);
        tvNum.setText(keyStr.length() + "/32");
        etKey.setText(keyStr.isEmpty() ? "" : keyStr);
        etKey.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    ivClear.setVisibility(View.VISIBLE);
                } else {
                    ivClear.setVisibility(View.GONE);
                }
                tvNum.setText(s.length() + "/32");
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        settingKeyDialog.show();
    }


    /**
     * 显示秘钥内容弹窗
     */
    private static void showKeyDialog(String key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .addDefaultAnimation()
                .setContentView(R.layout.dialog_look_key)
                .setCancelable(true)
                .setWidthAndHeight(SizeUtils.dp2px(mContext, 300), LinearLayout.LayoutParams.WRAP_CONTENT)
                .setText(R.id.tv_key, key)
                .setOnClickListener(R.id.iv_copy, v -> {
                    copyKey(key);
                    showMsg("秘钥已复制");
                    lookKeyDialog.dismiss();
                });
        lookKeyDialog = builder.create();
        lookKeyDialog.show();
    }

    /**
     * 显示搜索设备弹窗
     */
    public static void showSearchDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .addDefaultAnimation()
                .setContentView(R.layout.dialog_search_device)
                .setCancelable(false)
                .setWidthAndHeight(SizeUtils.dp2px(mContext, 340), LinearLayout.LayoutParams.WRAP_CONTENT)
                .setOnClickListener(R.id.tv_stop_scan, v -> {//停止扫描
                    stopSearchDevice();
                    searchDeviceDialog.dismiss();
                });
        searchDeviceDialog = builder.create();
        rvDevice = searchDeviceDialog.getView(R.id.rv_device);

        deviceAdapter = new DeviceAdapter(R.layout.item_device_rv, deviceList);
        rvDevice.setLayoutManager(new LinearLayoutManager(mContext));
        //列表item点击事件
        deviceAdapter.setOnItemClickListener((adapter, view, position) -> {
            macAddress = deviceList.get(position).getDevice().getAddress();
            //固件升级
            showLoading();
            stopSearchDevice();
            updateFirmware();
            searchDeviceDialog.dismiss();
        });
        deviceAdapter.setAnimationEnable(true);
        deviceAdapter.setAnimationWithDefault(BaseQuickAdapter.AnimationType.SlideInRight);
        rvDevice.setAdapter(deviceAdapter);
        searchDeviceDialog.show();
        //扫描蓝牙
        startSearchDevice();
    }

    /**
     * 升级结果弹窗
     */
    public static void showUpdateResultDialog(boolean isSuccess, int code) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .addDefaultAnimation()
                .setContentView(R.layout.dialog_update_result)
                .setCancelable(true)
                .setIcon(R.id.iv_update_result, isSuccess ? R.mipmap.ic_success : R.mipmap.ic_failure)
                .setText(R.id.tv_update_result, isSuccess ? "升级成功" : "失败：" + code)
                .setWidthAndHeight(SizeUtils.dp2px(mContext, 120), SizeUtils.dp2px(mContext, 120));
        updateResultDialog = builder.create();
        updateResultDialog.show();
    }

    /**
     * 固件升级
     */
    public static void updateFirmware() {
        if (fileName.endsWith(FILE_HEX) || fileName.endsWith(FILE_HEX16)) {
            showLoading();
            otasdkUtils.updateFirmware(macAddress, filePath, false);
        } else if (fileName.endsWith(FILE_HEXE16)) {
            showAESKeyDialog();
        } else {
            showLoading();
            otasdkUtils.updateResource(macAddress, filePath);
        }
    }

    /**
     * 固件升级
     */
    public static void updateFirmware(String macAddress, String fileName, String filePath) {
        if (fileName.endsWith(FILE_HEX) || OTAHelper.fileName.endsWith(FILE_HEX16)) {
            otasdkUtils.updateFirmware(macAddress, filePath, false);
        } else if (fileName.endsWith(FILE_HEXE16)) {
            otasdkUtils.updateFirmware(macAddress, filePath, true);
        } else {
            otasdkUtils.updateResource(macAddress, filePath);
        }
    }

    /**
     * 解析升级文件
     *
     * @return
     */
    public static boolean parseFile(String fileName) {
        showLoading();
        OTAHelper.fileName = fileName;
        filePath = path + "/" + OTAHelper.fileName;
        FirmWareFile firmWareFile = new FirmWareFile(filePath);
        if (firmWareFile.getCode() == BaseConstant.FILE_PARSE_SUCCESS) {
            hideLoading();
            return true;
        } else {
            hideLoading();
            Log.d(TAG, "错误码：" + ErrorCode.FILE_PARSE_ERROR);
            return false;
        }

    }

    /**
     * 搜索文件
     */
    public static void searchFile(List<String> fileList, FileAdapter fileAdapter) {
        File file = new File(path);
        if (file.exists()) {
            File[] listFiles = file.listFiles();
            for (File f : listFiles) {
                if (f.getName().endsWith(FILE_HEX16)
                        || f.getName().endsWith(FILE_HEX)
                        || f.getName().endsWith(FILE_HEXE)
                        || f.getName().endsWith(FILE_RES)
                        || f.getName().endsWith(FILE_HEXE16)) {
                    fileList.add(f.getName());
                    fileAdapter.notifyDataSetChanged();
                }
            }
        } else {
            showMsg("sdcard not found");
        }
    }

    /**
     * 复制文本
     *
     * @param key
     */
    private static void copyKey(String key) {
        //获取剪贴板管理器：
        ClipboardManager cm = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        // 创建普通字符型ClipData
        ClipData mClipData = ClipData.newPlainText("Label", key);
        // 将ClipData内容放到系统剪贴板里。
        cm.setPrimaryClip(mClipData);
    }

    /**
     * 广播接收器
     */
    private static BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        BandUtil.getBandUtil(mContext).stopScanDevice();
                        enableBluetooth(true);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        //to check if BluetoothAdapter is enable by your code
                        startSearchDevice();
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
    private static boolean enableBluetooth(boolean enable) {
        BluetoothManager manager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
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
    public static void startSearchDevice() {
        deviceList.clear();
        deviceAdapter.notifyDataSetChanged();
        //设置扫描回调
        BandUtil.setBleCallBack(callBack);
        BandUtil.getBandUtil(mContext).scanDevice();
    }

    /**
     * 停止搜索设备
     */
    private static void stopSearchDevice() {
        BandUtil.getBandUtil(mContext).stopScanDevice();
    }


    /**
     * 显示加载弹窗
     */
    public static void showLoading() {
        flag = true;

        //监听页面·返回键
        loadingDialog.setOnKeyListener((dialog, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                // you code here;
                if(flag){
                    showMsg("正在升级中，请勿退出。");
                    return true;
                }
            }
            return false;
        });
        loadingDialog.show();
    }


    /**
     * 隐藏加载弹窗
     */
    public static void hideLoading() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }


    public static void showMsg(String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }


}
