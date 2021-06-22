package com.phy.demo.adapter;


import android.view.View;
import android.widget.ImageView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.phy.demo.R;
import com.phy.ota.sdk.ble.Device;

import java.util.List;

/**
 * 蓝牙设备列表适配器
 *
 * @author llw
 */
public class DeviceAdapter extends BaseQuickAdapter<Device, BaseViewHolder> {

    public DeviceAdapter(int layoutResId, List<Device> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, Device item) {
        helper.setText(R.id.tv_device_name, item.getRealName())
                .setText(R.id.tv_mac_address, item.getDevice().getAddress());
        ImageView ivRssi = helper.getView(R.id.iv_rssi);

        if (item.getRssi() <= 0 && item.getRssi() >= -60) {
            ivRssi.setImageResource(R.mipmap.signal_4);
        } else if (-70 <= item.getRssi() && item.getRssi() < -60) {
            ivRssi.setImageResource(R.mipmap.signal_3);
        } else if (-80 <= item.getRssi() && item.getRssi() < -70) {
            ivRssi.setImageResource(R.mipmap.signal_2);
        } else {
            ivRssi.setImageResource(R.mipmap.signal_1);
        }
    }
}
