package com.phy.demo.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import com.phy.demo.R;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * 欢迎页面
 * @author llw
 */
public class SplashActivity extends AppCompatActivity {

    private TextView tvTranslate;
    /**
     * 位移动画
     */
    private TranslateAnimation translateAnimation;
    public static final int REQUEST_PERMISSION_CODE = 9527;
    private static int REQUEST_ENABLE_BLUETOOTH = 1;//请求码

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        initView();
    }

    /**
     * 初始化
     */
    private void initView() {
        tvTranslate = findViewById(R.id.tv_translate);

        tvTranslate.post(() -> {
            //通过post拿到的tvTranslate.getWidth()不会为0。
            translateAnimation = new TranslateAnimation(0, tvTranslate.getWidth(), 0, 0);
            translateAnimation.setDuration(1000);
            translateAnimation.setFillAfter(true);
            tvTranslate.startAnimation(translateAnimation);

            //动画监听
            translateAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    //检查Android版本
                    checkAndroidVersion();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        });

    }

    /**
     * 检查Android版本
     */
    private void checkAndroidVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Android 6.0及以上动态请求权限
            requestPermission();
        } else {
            //动画结束时跳转到主页面
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        }
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_CODE)
    private void requestPermission() {
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            //动画结束时跳转到主页面
            startActivity(new Intent(SplashActivity.this, MainPlusActivity.class));
        } else {
            // 没有权限
            EasyPermissions.requestPermissions(this, "需要权限", REQUEST_PERMISSION_CODE, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // 将结果转发给 EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
}