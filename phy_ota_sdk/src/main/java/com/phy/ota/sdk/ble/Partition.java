package com.phy.ota.sdk.ble;

import android.util.Log;

import com.phy.ota.sdk.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

import static com.phy.ota.sdk.utils.OTAUtils.MTU_SIZE;

/**
 * Partition
 *
 * @author:zhoululu
 * @date:2018/5/19
 */

public class Partition {

    private String address;
    private String data;
    private int partitionLength;

    private List<List<String>> blocks = new ArrayList<>();

    public Partition(String address, String data) {
        this.address = address;
        this.data = data;

        partitionLength = data.length()/2;

        analyzePartition(data);
    }

    public String getAddress() {
        return address;
    }

    public String getData() {
        return data;
    }

    public int getPartitionLength() {
        return partitionLength;
    }

    public List<List<String>> getBlocks() {
        return blocks;
    }

    /**
     * 解析分区
     * @param data
     */
    private void analyzePartition(String data) {
        Log.d("Partition time", DateUtils.getNowTimeDetail());
        String partitionStr = data;

        int size = MTU_SIZE-3;

        int index = 0;
        List<String> list = null;
        while (true){
            if(index == 0){
                list = new ArrayList<>();
            }

            if(partitionStr.length() <= size*2){
                list.add(partitionStr);
                blocks.add(list);
                break;
            }else{
                String str = partitionStr.substring(0,size*2);
                partitionStr = partitionStr.substring(size*2,partitionStr.length());
                list.add(str);

                index ++;
            }


            if (list.size() == 16){
                blocks.add(list);
                index = 0;
            }

        }
    }

    public static String Make_CRC(int crc, byte[] data) {
        byte[] buf = new byte[data.length];// 存储需要产生校验码的数据
        for (int i = 0; i < data.length; i++) {
            buf[i] = data[i];
        }
        int len = buf.length;

        for (int pos = 0; pos < len; pos++) {
            if (buf[pos] < 0) {
                crc ^= (int) buf[pos] + 256; // XOR byte into least sig. byte of
                // crc
            } else {
                crc ^= (int) buf[pos]; // XOR byte into least sig. byte of crc
            }
            for (int i = 8; i != 0; i--) { // Loop over each bit
                if ((crc & 0x0001) != 0) { // If the LSB is set
                    crc >>= 1; // Shift right and XOR 0xA001
                    crc ^= 0xA001;
                } else{
                    // Else LSB is not set
                    crc >>= 1; // Just shift right
                }
            }
        }
        String c = Integer.toHexString(crc);
        if (c.length() == 4) {
            c = c.substring(2, 4) + c.substring(0, 2);
        } else if (c.length() == 3) {
            c = "0" + c;
            c = c.substring(2, 4) + c.substring(0, 2);
        } else if (c.length() == 2) {
            c = "0" + c.substring(1, 2) + "0" + c.substring(0, 1);
        }
        return c;
    }

}