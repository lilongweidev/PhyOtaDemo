package com.phy.demo.dialog;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.phy.demo.R;
import com.phy.ota.sdk.constant.BaseConstant;
import com.phy.ota.sdk.utils.SPUtils;
import com.phy.ota.sdk.utils.SizeUtils;

public class DialogHelper {

    //加载弹窗
    private static CustomDialog loadingDialog;
    //秘钥弹窗
    private static AlertDialog aesDialog;
    //设置秘钥弹窗
    private static AlertDialog settingKeyDialog;
    //秘钥输入框
    private static EditText etKey;
    //清空输入框
    private static ImageView ivClear;
    //查看秘钥弹窗
    private static AlertDialog lookKeyDialog;
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
     * 显示秘钥弹窗
     */
    public static void showAESKeyDialog(Context context) {
        String keyStr = SPUtils.getString(BaseConstant.AES_KEY, "");
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .addDefaultAnimation()
                .setContentView(R.layout.dialog_aes)
                .setCancelable(true)
                .setWidthAndHeight(SizeUtils.dp2px(context, 280), LinearLayout.LayoutParams.WRAP_CONTENT)
                .setOnClickListener(R.id.tv_setting_key, v -> {
                    showSettingKeyDialog(context);
                    aesDialog.dismiss();
                }).setOnClickListener(R.id.tv_show_key, v -> {
                    showKeyDialog(context,keyStr);
                    aesDialog.dismiss();
                });
        aesDialog = builder.create();
        aesDialog.show();
    }

    /**
     * 显示设置秘钥弹窗
     */
    private static void showSettingKeyDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .addDefaultAnimation()
                .setContentView(R.layout.dialog_setting_key)
                .setCancelable(true)
                .setWidthAndHeight(SizeUtils.dp2px(context, 280), LinearLayout.LayoutParams.WRAP_CONTENT)
                .setOnClickListener(R.id.iv_clear, v -> {
                    etKey.setText("");
                    ivClear.setVisibility(View.GONE);
                })
                .setOnClickListener(R.id.tv_cancel, v -> settingKeyDialog.dismiss())
                .setOnClickListener(R.id.tv_submit, v -> {
                    //提交
                    String key = etKey.getText().toString();
                    if (key.length() != 32) {
                        showMsg(context,"数据长度不足");
                        return;
                    }
                    SPUtils.putString(BaseConstant.AES_KEY, key);
                    copyKey(context,key);
                    showMsg(context,"设置成功且已复制");
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
    private static void showKeyDialog(Context context,String key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .addDefaultAnimation()
                .setContentView(R.layout.dialog_look_key)
                .setCancelable(true)
                .setWidthAndHeight(SizeUtils.dp2px(context, 300), LinearLayout.LayoutParams.WRAP_CONTENT)
                .setText(R.id.tv_key, key)
                .setOnClickListener(R.id.iv_copy, v -> {
                    copyKey(context,key);
                    showMsg(context,"秘钥已复制");
                    lookKeyDialog.dismiss();
                });
        lookKeyDialog = builder.create();
        lookKeyDialog.show();
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

    /**
     * 复制文本
     *
     * @param key
     */
    private static void copyKey(Context context, String key) {
        //获取剪贴板管理器：
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        // 创建普通字符型ClipData
        ClipData mClipData = ClipData.newPlainText("Label", key);
        // 将ClipData内容放到系统剪贴板里。
        cm.setPrimaryClip(mClipData);
    }

    public static void showMsg(Context context,String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }
}
