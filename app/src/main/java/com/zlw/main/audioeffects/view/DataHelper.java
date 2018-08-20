package com.zlw.main.audioeffects.view;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author zhaolewei on 2018/8/20.
 */
public class DataHelper {
    private static final int MAX_SIZE = 128;

    /**
     * 预处理数据
     *
     * @return
     */
    public static byte[] readyData(byte[] fft) {
        byte[] newData = new byte[MAX_SIZE];
        for (int i = 0; i < MAX_SIZE; i++) {
            newData[i] = (byte) (Math.abs(fft[i]));
        }
        return newData;
    }

    public static byte[] getData(byte[] fft) {
        byte[] readyData = readyData(fft);
        Map<Integer, Byte> byteMap = pickAltData(readyData);
        return genWaveDate(readyData, byteMap);
    }

    /**
     * 提取有效数据
     *
     * @return
     */
    private static Map<Integer, Byte> pickAltData(byte[] data) {
        Map<Integer, Byte> maxValueMap = new HashMap<>();
        byte max = 0;
        int maxIndex = 0;
        byte last = 0;
        int laseIndex = 0;

        for (int i = 0; i < data.length; i++) {
            byte value = data[i];
            if (value > max) {
                max = value;
                maxIndex = i;
            } else {
                if (value < last) {
                    if (maxIndex - laseIndex > 8) {
                        maxValueMap.put(maxIndex, max);
                        laseIndex = maxIndex;
                    }
                    max = 0;
                }
            }
            last = value;
        }

        return adornData(maxValueMap);
    }

    /**
     * 数据加工
     *
     * @return
     */
    private static Map<Integer, Byte> adornData(Map<Integer, Byte> maxValueMap) {
        Map<Integer, Byte> newValueMap = new HashMap<>();

        Iterator<Map.Entry<Integer, Byte>> iterator = maxValueMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Byte> entry = iterator.next();
            Integer maxIndex = entry.getKey();
            byte maxValue = entry.getValue();

            byte maxValue11 = (byte) (maxValue * Math.random());
            byte maxValue12 = (byte) (maxValue * Math.random());
            byte maxValue21 = (byte) (maxValue11 * Math.random());
            byte maxValue22 = (byte) (maxValue12 * Math.random());
            if (maxIndex - 2 >= 0) {
                newValueMap.put(maxIndex - 2, maxValue21);
            }
            if (maxIndex - 1 >= 0) {
                newValueMap.put(maxIndex - 1, maxValue11);
            }
            if (maxIndex + 1 < MAX_SIZE) {
                newValueMap.put(maxIndex + 1, maxValue12);
            }
            if (maxIndex + 2 < MAX_SIZE) {
                newValueMap.put(maxIndex + 2, maxValue22);
            }
        }

        maxValueMap.putAll(newValueMap);

        return maxValueMap;
    }


    private static byte[] genWaveDate(byte[] data, Map<Integer, Byte> maxValueMap) {
        for (int i = 0; i < MAX_SIZE; i++) {
            Byte value = maxValueMap.get(i);
            if (value == null) {
                data[i] = (byte) (data[i] / 4);
            } else {
                byte b = (byte) (value * 1.5);
                data[i] = b < 0 ? 127 : b;
            }
        }

        return data;
    }

}
