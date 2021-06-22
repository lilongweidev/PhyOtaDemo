package com.phy.ota.sdk.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences工具类
 *
 * @author llw
 */
public class SPUtils {

    private static final String NAME = "data_config";
    
    private static Context mContext;

    public static void init(Context context){
        mContext = context;
    }

    public static void putBoolean(String key, boolean value) {
        mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply();
    }

    public static boolean getBoolean(String key, boolean defValue) {
        return mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE).getBoolean(key, defValue);
    }

    public static void putString(String key, String value) {
         mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString(key, value).apply();
    }

    public static String getString(String key, String defValue) {
        if (mContext != null) {
            return mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(key, defValue);
        }
        return "";

    }

    public static void putInt(String key, int value) {
         mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putInt(key, value).apply();
    }

    public static int getInt(String key, int defValue) {
        return mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE).getInt(key, defValue);
    }

    public static void remove(String key) {
        mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().remove(key).apply();
    }

}
