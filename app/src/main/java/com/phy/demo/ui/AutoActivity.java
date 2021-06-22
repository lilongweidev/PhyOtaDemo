package com.phy.demo.ui;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.king.view.circleprogressview.CircleProgressView;
import com.phy.demo.adapter.FileAdapter;
import com.phy.demo.MyApplication;
import com.phy.demo.R;
import com.phy.ota.sdk.OTASDKUtils;
import com.phy.ota.sdk.ble.Device;
import com.phy.ota.sdk.ble.UpdateFirmwareCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.phy.ota.sdk.constant.BaseConstant.*;

/**
 * 自动升级页面
 */
public class AutoActivity extends AppCompatActivity implements UpdateFirmwareCallback {

    public static final String TAG = AutoActivity.class.getSimpleName();

    private TextView tvDeviceName, tvMacAddress;
    private RecyclerView rvFile;
    private Toolbar toolbar;
    private CircleProgressView loadingProgressbar;

    private List<String> fileList = new ArrayList<>();
    private FileAdapter fileAdapter;
    private String filePath;
    private String path = Environment.getExternalStorageDirectory().getPath();
    private String macAddress;

    private OTASDKUtils otasdkUtils;
    private boolean flag = false;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto);

        init();
    }

    /**
     * 初始化页面
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void init() {
        tvDeviceName = findViewById(R.id.tv_device_name);
        tvMacAddress = findViewById(R.id.tv_mac_address);
        rvFile = findViewById(R.id.rv_file);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        loadingProgressbar = findViewById(R.id.cpv);

        Device device = MyApplication.getApplication().getDevice();
        macAddress = device.getDevice().getAddress();
        tvDeviceName.setText(device.getRealName());
        tvMacAddress.setText(macAddress);
        //列表适配器设置
        fileAdapter = new FileAdapter(R.layout.item_file_rv, fileList);
        rvFile.setLayoutManager(new LinearLayoutManager(this));
        //item点击事件
        fileAdapter.setOnItemClickListener((adapter, view, position) -> {
            updateFirmware(position);
        });
        rvFile.setAdapter(fileAdapter);

        otasdkUtils = new OTASDKUtils(getApplicationContext(), this);
        //搜索文件
        searchFile();
    }

    /**
     * 升级固件
     *
     * @param position
     */
    private void updateFirmware(int position) {
        filePath = path + "/" + fileList.get(position);
        String fileName = fileList.get(position).toLowerCase();
        Log.d(TAG, "start...");
        flag = true;
        toolbar.setEnabled(false);
        backgroundAlpha(0.7f);
        loadingProgressbar.setVisibility(View.VISIBLE);

        if (fileName.endsWith(FILE_HEX) || fileName.endsWith(FILE_HEX16)) {
            otasdkUtils.updateFirmware(macAddress, filePath, false);
        } else if (fileName.endsWith(FILE_HEXE16)) {
            otasdkUtils.updateFirmware(macAddress, filePath, true);
        } else {
            otasdkUtils.updateResource(macAddress, filePath);
        }
    }

    /**
     * 搜索文件
     */
    private void searchFile() {
        fileList.clear();
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
            Toast.makeText(this, "sdcard not found", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onProcess(float process) {
        runOnUiThread(() -> {
            Log.d(TAG, "onProcess:" + process);
            int progress = Math.round(process);
            //设置当前进度
            loadingProgressbar.setProgress(progress);
        });
    }

    @Override
    public void onUpdateComplete() {
        runOnUiThread(() -> {
            Log.d(TAG, "onUpdateComplete");
            showMsg("升级成功");
            AutoActivity.this.finish();
        });
    }

    @Override
    public void onError(int code) {
        runOnUiThread(() -> {
            Log.d(TAG, "onError:" + code);
            showMsg("onError:" + code);
            flag = false;
            toolbar.setEnabled(true);
            loadingProgressbar.setVisibility(View.GONE);
            backgroundAlpha(1f);
        });
    }

    private void showMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void backgroundAlpha(float bgAlpha) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        // 0.0-1.0
        lp.alpha = bgAlpha;
        getWindow().setAttributes(lp);
        // everything behind this window will be dimmed.
        // 此方法用来设置浮动层，防止部分手机变暗无效
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (flag) {
                showMsg("正在升级固件版本，请不要退出");
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


}