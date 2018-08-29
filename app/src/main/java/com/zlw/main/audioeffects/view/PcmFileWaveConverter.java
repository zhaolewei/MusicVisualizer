package com.zlw.main.audioeffects.view;

import com.zlw.main.audioeffects.utils.ByteUtils;
import com.zlw.main.mp3playerlib.utils.FrequencyScanner;

public class PcmFileWaveConverter {

    private FrequencyScanner fftScanner;
    short[] cacheData;

    private int fftThruput;

    public PcmFileWaveConverter(int fftThruput) {
        this.fftThruput = fftThruput;
        fftScanner = new FrequencyScanner();
        cacheData = new short[fftThruput];
    }

    /**
     * 处理16Bit数据
     */
    public byte[] readyDataByte(byte[] data) {
        short[] shorts = ByteUtils.toShorts(data);
        return fft(shorts);
    }

    private byte[] fft(short[] sampleData) {
        byte[] result = new byte[sampleData.length / fftThruput];
        for (int i = 0; i < sampleData.length; i = i + fftThruput) {
            int end = i + fftThruput;
            if (end > sampleData.length) {
                break;
            }
            for (int j = i; j < end; j++) {
                cacheData[j % fftThruput] = sampleData[j];
            }
            double extractFrequency = fftScanner.getMaxFrequency(cacheData, 16000);
            result[i / fftThruput] = (byte) (extractFrequency > 127 ? 127 : extractFrequency);
        }

        return result;
    }
}
