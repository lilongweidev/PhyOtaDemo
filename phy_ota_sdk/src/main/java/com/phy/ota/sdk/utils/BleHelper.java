package com.phy.ota.sdk.utils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import androidx.annotation.NonNull;

import com.phy.ota.sdk.ble.FirmWareFile;
import com.phy.ota.sdk.ble.Partition;
import com.phy.ota.sdk.constant.BaseConstant;
import com.phy.ota.sdk.constant.BleConstant;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

import static com.phy.ota.sdk.constant.BaseConstant.HEXE16;

/**
 * Ble帮助类
 */
public class BleHelper {

    /**
     * 启用通知
     */
    public static boolean enableNotification(BluetoothGatt bluetoothGatt) {
        //获取Gatt服务
        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(BleConstant.SERVICE_UUID));
        if (service == null) {
            return false;
        }
        //获取Gatt特征（特性）
        BluetoothGattCharacteristic gattCharacteristic = service.getCharacteristic(UUID.fromString(BleConstant.CHARACTERISTIC_WRITE_UUID));
        //设置特征通知
        return setCharacteristicNotification(bluetoothGatt, gattCharacteristic);
    }

    /**
     * 启用指令通知
     */
    public static boolean enableIndicateNotification(BluetoothGatt bluetoothGatt) {
        //获取Gatt OTA服务
        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(BleConstant.OTA_SERVICE_UUID));
        if (service == null) {
            return false;
        }
        //获取Gatt OTA特征（特性）
        BluetoothGattCharacteristic gattCharacteristic = service.getCharacteristic(UUID.fromString(BleConstant.OTA_CHARACTERISTIC_INDICATE_UUID));
        Log.d("OTA","enableIndicateNotification 启用指令通知");
        return setCharacteristicNotification(bluetoothGatt, gattCharacteristic);
    }

    /**
     * 设置特征通知
     */
    private static boolean setCharacteristicNotification(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic gattCharacteristic) {
        boolean isEnableNotification = bluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
        if (isEnableNotification) {
            BluetoothGattDescriptor gattDescriptor = gattCharacteristic.getDescriptor(UUID.fromString(BleConstant.DESCRIPTOR_UUID));
            gattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Log.d("OTA","setCharacteristicNotification 设置特征通知");
            return bluetoothGatt.writeDescriptor(gattDescriptor);
        } else {
            return false;
        }
    }

    /**
     * 检查是否为 OTA
     */
    public static boolean checkIsOTA(BluetoothGatt bluetoothGatt) {
        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(BleConstant.OTA_SERVICE_UUID));
        if (service == null) {
            return false;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(BleConstant.OTA_DATA_CHARACTERISTIC_WRITE_UUID));
        return characteristic != null;
    }

    /**
     * 发送OTA命令
     * @param bluetoothGatt Gatt
     * @param command 命令
     * @param isResponse 是否响应结果
     * @return
     */
    public static boolean sendOTACommand(BluetoothGatt bluetoothGatt, String command, boolean isResponse) {
        Log.d("OTA","sendOTACommand ");
        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(BleConstant.OTA_SERVICE_UUID));
        if (service == null) {
            return false;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(
                BleConstant.OTA_CHARACTERISTIC_WRITE_UUID));
        characteristic.setWriteType(!isResponse ? BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE :
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        characteristic.setValue(HexString.parseHexString(command));

        boolean isOK = bluetoothGatt.writeCharacteristic(characteristic);
        Log.d("OTA",isOK ? "成功：" + command : "失败：" + command);
        KLog.i("send ota command", isOK ? "成功：" + command : "失败：" + command);
        return true;
    }

    /**
     * 获取OTA Mac地址
     */
    public static String getOTAMac(String deviceAddress) {
        //获取第一个地址字节
        final String firstBytes = deviceAddress.substring(0, 15);
        // assuming that the device address is correct
        //获取最后一个地址字节
        final String lastByte = deviceAddress.substring(15);
        //字节增加
        final String lastByteIncremented = String.format("%02X", (Integer.valueOf(lastByte, 16) + 1) & 0xFF);
        //返回Mac 地址
        return firstBytes + lastByteIncremented;
    }

    /**
     * 发送OTA数据
     */
    public static boolean sendOTAData(BluetoothGatt bluetoothGatt, String cmd) {
        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(BleConstant.OTA_SERVICE_UUID));
        if (service == null) {
            return false;
        }

        BluetoothGattCharacteristic gattCharacteristic = service.getCharacteristic(UUID.fromString(BleConstant.OTA_DATA_CHARACTERISTIC_WRITE_UUID));
        gattCharacteristic.setValue(HexString.parseHexString(cmd.toLowerCase()));
        Log.d("OTA","sendOTAData ");
        bluetoothGatt.writeCharacteristic(gattCharacteristic);
        return true;
    }

    /**
     * 制作部分 cmd
     */
    public static String makePartitionCmd(int index, long flashAddress, String runAddress, int size, int checkSum) {
        String fa = ByteUtils.translateStr(ByteUtils.strAdd0(Long.toHexString(flashAddress), 8));
        String ra = ByteUtils.translateStr(ByteUtils.strAdd0(runAddress, 8));
        String sz = ByteUtils.translateStr(ByteUtils.strAdd0(Integer.toHexString(size), 8));
        String cs = ByteUtils.translateStr(ByteUtils.strAdd0(Integer.toHexString(checkSum), 4));
        String in = ByteUtils.strAdd0(Integer.toHexString(index), 2);

        return "02" + in + fa + ra + sz + cs;
    }

    /**
     * 制作部分 cmd
     */
    public static String makePartitionCmd(int index, long flashAddress, String runAddress, int size, String micCode) {
        String fa = ByteUtils.translateStr(ByteUtils.strAdd0(Long.toHexString(flashAddress), 8));
        String ra = ByteUtils.translateStr(ByteUtils.strAdd0(runAddress, 8));
        String sz = ByteUtils.translateStr(ByteUtils.strAdd0(Integer.toHexString(size), 8));
        String cs = ByteUtils.strAdd0(micCode, 8);
        String in = ByteUtils.strAdd0(Integer.toHexString(index), 2);

        return "02" + in + fa + ra + sz + cs;
    }

    /**
     * 制作资源 cmd
     */
    public static String makeResourceCmd(@NonNull FirmWareFile firmWareFile) {
        String startAddress = firmWareFile.getList().get(0).getAddress();
        //&0x12000
        long flashLongAdd = Long.parseLong(startAddress, 16) & 0xfffff000;
        long flashLongSize = Long.parseLong(startAddress, 16) & 0xfff;
        for (Partition partition : firmWareFile.getList()) {
            flashLongSize += partition.getPartitionLength();
        }
        flashLongSize = (flashLongSize + 0xfff) & 0xfffff000;

        String fa = ByteUtils.translateStr(ByteUtils.strAdd0(Long.toHexString(flashLongAdd), 8));
        String sz = ByteUtils.translateStr(ByteUtils.strAdd0(Long.toHexString(flashLongSize), 8));

        return "05" + fa + sz;
    }

    /**
     * 发送分区
     */
    public static boolean sendPartition(BluetoothGatt gatt, FirmWareFile firmWareFile, int partitionIndex, long flashAddress) {
        //获取分区
        Partition partition = firmWareFile.getList().get(partitionIndex);
        //获取校验和
        int checkSum = getPartitionCheckSum(partition);
        String cmd = makePartitionCmd(partitionIndex, flashAddress, partition.getAddress(), partition.getPartitionLength(), checkSum);
        if (firmWareFile.getPath().endsWith(HEXE16)) {
            List<List<String>> blocks = partition.getBlocks();
            List<String> lastBlock = blocks.get(blocks.size() - 1);
            String lastData = lastBlock.get(lastBlock.size() - 1);
            if (lastData.length() < 8) {
                lastData = lastBlock.get(lastBlock.size() - 2) + lastData;
            }
            String micCode = lastData.substring(lastData.length() - 8);
            cmd = makePartitionCmd(partitionIndex, flashAddress, partition.getAddress(), partition.getPartitionLength(), micCode);
        }
        Log.e("TAG", "sendPartition: ==================" + cmd);
        return sendOTACommand(gatt, cmd, true);
    }

    /**
     * 发送资源
     */
    public static boolean sendResource(BluetoothGatt gatt, FirmWareFile firmWareFile) {
        return sendOTACommand(gatt, makeResourceCmd(firmWareFile), true);
    }

    /**
     * 获取分区校验和
     */
    public static int getPartitionCheckSum(Partition partition) {
        return checkSum(0, HexString.parseHexString(partition.getData()));
    }

    /**
     * 校验和
     */
    private static int checkSum(int crc, byte[] data) {
        // 存储需要产生校验码的数据
        byte[] buf = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            buf[i] = data[i];
        }
        int len = buf.length;

        for (int pos = 0; pos < len; pos++) {
            if (buf[pos] < 0) {
                // XOR byte into least sig. byte of
                crc ^= (int) buf[pos] + 256;
                // crc
            } else {
                // XOR byte into least sig. byte of crc
                crc ^= (int) buf[pos];
            }
            // Loop over each bit
            for (int i = 8; i != 0; i--) {
                // If the LSB is set
                if ((crc & 0x0001) != 0) {
                    // Shift right and XOR 0xA001
                    crc >>= 1;
                    crc ^= 0xA001;
                } else {
                    // Else LSB is not set
                    // Just shift right
                    crc >>= 1;
                }
            }
        }

        return crc;
    }

    /**
     * 获取随机字符串
     */
    public static String getRandomStr() {
        StringBuffer buffer = new StringBuffer();
        int length = 32;
        char[] allChar = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            buffer.append(allChar[random.nextInt(allChar.length)]);
        }
        return String.valueOf(buffer);
    }

    /**
     * 解析设备名称
     * @param scanRecord 扫描记录
     * @return 返回名称字符串（成功）或空（失败）
     */
    public static String parseDeviceName(byte[] scanRecord) {
        String result = null;
        if (scanRecord == null) {
            return result;
        }

        //ByteBuffer手动封装byte数组
        //按小端字节排序  （释义：小端（Little Endian），是高位在后，低位在前的存储方式）
        java.nio.ByteBuffer buffer = ByteBuffer.wrap(scanRecord).order(ByteOrder.LITTLE_ENDIAN);
        //剩余字节长度大于2 则循环
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) {
                break;
            }

            byte type = buffer.get();
            length -= 1;
            switch (type) {
                case 0x01: // Flags
                    buffer.get(); // flags
                    length--;
                    break;
                case 0x02: // 16 位 UUID 的部分列表
                case 0x03: // 16 位 UUID 的完整列表
                case 0x14: // 16 位服务请求 UUID 列表
                    while (length >= 2) {
                        buffer.getShort();
                        length -= 2;
                    }
                    break;
                case 0x04: // 32 位服务 UUID 的部分列表
                case 0x05: // 32 位服务 UUID 的完整列表
                    while (length >= 4) {
                        buffer.getInt();
                        length -= 4;
                    }
                    break;
                case 0x06: // 128 位 UUID 的部分列表
                case 0x07: // 128 位 UUID 的完整列表
                case 0x15: // 128 位服务请求 UUID 列表
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        length -= 16;
                    }
                    break;
                case 0x08: // 本地设备短名称
                case 0x09: // 本地设备完整名称
                    byte sb[] = new byte[length];
                    buffer.get(sb, 0, length);
                    length = 0;
                    result = new String(sb).trim();
                    return result;
                case (byte) 0xFF: // 制造商特定数据
                    buffer.getShort();
                    length -= 2;
                    break;
                default: // skip
                    break;
            }
            if (length > 0) {
                if (buffer.position() + length > buffer.limit()){
                    return "越界";
                }
                buffer.position(buffer.position() + length);
            }
        }

        return result;
    }

}
