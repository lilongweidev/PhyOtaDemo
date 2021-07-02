package com.phy.ota.sdk.constant;

/**
 * 基础常量
 * @author llw
 */
public class BaseConstant {
    /**
     * 加密
     */
    public static final String AES_KEY = "AESKey";

    /**
     * Hex文件 初始化
     */
    public static final String HEX_INIT = "04";
    /**
     * Hex文件 开始
     */
    public static final String HEX_START = "05";
    /**
     * Hex文件 结束
     */
    public static final String HEX_END = "01";

    /**
     * Hexe16 文件格式
     */
    public static final String HEXE16 = "hexe16";

    /**
     * OTA 数据发送成功
     */
    public static final int OTA_DATA_SEND_SUCCESS = 0;

    /**
     * 准备接收成功 进⼊下⼀步
     */
    public static final String READY_RECEIVE_SUCCESS = "0081";

    /**
     * 固件端接收成功 0084
     */
    public static final String FIRMWARE_RECEIVE_SUCCESS = "0084";

    /**
     * 一组16*20 ota数据发送成功，开始下一组
     */
    public static final String ONCE_OTA_SEND_SUCCESS = "0087";

    /**
     * 地址 0089
     */
    public static final String ADDRESS_89 = "0089";

    /**
     * 一个partition 数据发送成功，发送下一个partition命令
     */
    public static final String ONCE_PARTITION_SEND_SUCCESS = "0085";

    /**
     * 所有ota数据发送成功
     */
    public static final String ALL_OTA_SEND_SUCCESS = "0083";

    /**
     * 高速模式下 发送reboot
     */
    public static final String HIGH_SPEED_SEND_REBOOT = "008a";

    /**
     * hex格式
     */
    public static final String FILE_HEX = ".hex";
    /**
     * hex格式
     */
    public static final String FILE_HEX16 = ".hex16";
    /**
     * hexe格式
     */
    public static final String FILE_HEXE = ".hexe";
    /**
     * hexe16格式
     */
    public static final String FILE_HEXE16 = ".hexe16";

    /**
     * res格式
     */
    public static final String FILE_RES = ".res";

    /**
     * 解析文件成功
     */
    public static final int FILE_PARSE_SUCCESS = 200;
}
