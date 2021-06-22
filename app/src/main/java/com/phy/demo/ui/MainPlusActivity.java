package com.phy.demo.ui;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import com.phy.demo.adapter.FileAdapter;
import com.phy.demo.R;
import com.phy.demo.OTAHelper;
import com.phy.ota.sdk.OTASDKUtils;
import com.phy.ota.sdk.ble.BandUtil;

import java.util.ArrayList;
import java.util.List;

import static com.phy.demo.OTAHelper.showMsg;
import static com.phy.ota.sdk.constant.BaseConstant.FILE_HEXE16;

/**
 * 新主页逻辑
 *
 * @author llw
 */
public class MainPlusActivity extends AppCompatActivity {

    public static final String TAG = MainPlusActivity.class.getSimpleName();
    private static int REQUEST_ENABLE_BLUETOOTH = 1;//请求码
    private TextView tvDeviceName, tvMacAddress;
    private RecyclerView rvFile;
    private List<String> fileList = new ArrayList<>();
    private FileAdapter fileAdapter;

    private OTASDKUtils otasdkUtils;
    private boolean flag = false;

    //private CircleProgressView loadingProgressbar;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_plus);
        OTAHelper.init(this);
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

        //文件列表适配器设置
        fileAdapter = new FileAdapter(R.layout.item_file_rv, fileList);
        rvFile.setLayoutManager(new LinearLayoutManager(this));
        //item点击事件
        fileAdapter.setOnItemClickListener((adapter, view, position) -> {
            String fileName = fileList.get(position);
            if (fileName.endsWith(FILE_HEXE16)) {
                //设置秘钥弹窗
                OTAHelper.showAESKeyDialog(fileName);
            } else {
                if (OTAHelper.parseFile(fileName)) {//解析文件成功
                    if (BandUtil.isBleOpen(this)) {
                        //搜索设备弹窗
                        OTAHelper.showSearchDeviceDialog();
                    } else {
                        showMsg("请打开蓝牙");
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
                    }
                } else {
                    //解析文件失败
                    showMsg("解析文件失败");
                }
            }
        });
        rvFile.setAdapter(fileAdapter);

        //搜索文件
        OTAHelper.searchFile(fileList, fileAdapter);
    }
}