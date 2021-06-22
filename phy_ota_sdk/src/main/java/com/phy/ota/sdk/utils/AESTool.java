package com.phy.ota.sdk.utils;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;


public class AESTool {
    /**
     * AES加密字符串
     *
     * @param content  需要被加密的字符串
     * @param password 加密需要的密码
     * @return 密文
     */
    public static String encrypt(String content, String password) {

        try {
            // 判断Key是否为16位
            if (password.length() != 32) {
                return null;
            }
            byte[] raw = parseHexStr2Byte(password);
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");//"算法/模式/补码方式"
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] result = cipher.doFinal(parseHexStr2Byte(content));
            return parseByte2HexStr(result);

        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解密AES加密过的字符串
     *
     * @param content  AES加密过过的内容
     * @param password 加密时的密码
     * @return 明文
     */
    public static byte[] decrypt(byte[] content, String password) {
        try {
            // 判断Key是否正确
            if (password == null) {
                System.out.print("Key为空null");
                return null;
            }
            // 判断Key是否为16位
            if (password.length() != 32) {
                System.out.print("Key长度不是16位");
                return null;
            }
            byte[] raw = parseHexStr2Byte(password);
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);

            try {
                byte[] original = cipher.doFinal(content);
                return original;
            } catch (Exception e) {
                System.out.println(e.toString());
                return null;
            }

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String decrypt(String content, String password) {
        byte[] decryptFrom = parseHexStr2Byte(content);
        byte[] decrypt = decrypt(decryptFrom, password);
        if (decrypt != null && decrypt.length != 0) {
            return parseByte2HexStr(decrypt);
        }
        return null;
    }

    public static String parseByte2HexStr(byte buf[]) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1)
            return null;
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    public static void test() {
        String password = "c0e247e227e68497f52f0700b64f1b9e";
        String str = "958643f347459701bb0c497f68293756";

        String encryptResultStr = encrypt(str, password);
        System.out.println("加密后：" + encryptResultStr);


        byte[] decryptFrom = parseHexStr2Byte(encryptResultStr);
        byte[] decryptResult = decrypt(decryptFrom, password);
        System.out.println("解密后：" + parseByte2HexStr(decryptResult));

    }
}
