package com.phy.demo.dialog;

import android.content.Context;

import com.phy.demo.R;
import com.phy.ota.sdk.utils.SizeUtils;

public class DialogHelper {

    //加载弹窗
    private static CustomDialog loadingDialog;
    //升级固件结果弹窗
    private static AlertDialog updateResultDialog;

    /**
     * 显示加载弹窗
     */
    public static void showLoading(Context context) {
        loadingDialog = new CustomDialog(context);
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

    /**
     * 升级结果弹窗
     */
    public static void showUpdateResultDialog(Context context, boolean isSuccess, int code) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .addDefaultAnimation()
                .setContentView(R.layout.dialog_update_result)
                .setCancelable(true)
                .setIcon(R.id.iv_update_result, isSuccess ? R.mipmap.ic_success : R.mipmap.ic_failure)
                .setText(R.id.tv_update_result, isSuccess ? "升级成功" : "失败：" + code)
                .setWidthAndHeight(SizeUtils.dp2px(context, 120), SizeUtils.dp2px(context, 120));
        updateResultDialog = builder.create();
        updateResultDialog.show();
    }
}
