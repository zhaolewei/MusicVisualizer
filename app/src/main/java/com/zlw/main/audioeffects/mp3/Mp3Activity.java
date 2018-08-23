package com.zlw.main.audioeffects.mp3;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.zlw.main.audioeffects.R;
import com.zlw.main.audioeffects.utils.FFT;
import com.zlw.main.audioeffects.utils.Logger;
import com.zlw.main.audioeffects.view.AudioView2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 * @author zhaolewei on 2018/8/23.
 */
public class Mp3Activity extends AppCompatActivity {
    private static final String TAG = Mp3Activity.class.getSimpleName();
    Mp3Decoder mp3Decoder;
    AudioView2 audioView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_mp3);
        audioView = findViewById(R.id.audioView);
        showWave();
        byte[] bytes = new byte[128 * 4];
//        audioView.setWaveData(bytes);
//        mp3Decoder = new Mp3Decoder();
//        mp3Decoder.init(R.raw.test, new Mp3Decoder.FinishListener() {
//            @Override
//            public void onFinish(final byte[] data) {
//                audioView.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        showWave();
//                    }
//                });
//            }
//        });

    }

    private void showWave() {
        Logger.i(TAG, "showWave");
        File file = new File("sdcard/Record/result.pcm");
        byte[] buffer;
        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
            audioView.setWaveData(readyDataByte(buffer));
        } catch (Exception e) {
            Logger.e(e, TAG, e.getMessage());
        }
    }

    /**
     * 预处理16Bit数据
     *
     * @return
     */
    public byte[] readyDataByte(byte[] data) {
//        short[] shorts = com.zlw.main.audioeffects.utils.ByteUtils.toShorts(data);

//        Logger.d(TAG, "大小：%s", shorts.length);
//        for (int i = 0; i < shorts.length; i++) {
//            short aShort = shorts[i];
//            bytes[i] = (byte) (aShort);
//        }
        byte[] newData = data;
//        for (int i = 0; i < newData.length; i++) {
//            newData[i] = (byte) (data[i]);
//        }
//        FloatFFT_1DTest fftHelper = new FloatFFT_1DTest(1024);
        FFT fftHelper = new FFT();
        int amplitudeLength = (int) Math.pow(2,
                32 - Integer.numberOfLeadingZeros(newData.length / 2 - 1));

        float[] result = byteToFloat(newData);
        fftHelper.calculate(result);
        float[] spectrum;
        spectrum = fetchSpectrum(result, 5);

        Logger.i(TAG, "amplitudeLength : %s", amplitudeLength);
        Logger.i(TAG, "fft ：%s", Arrays.toString(result));
        Logger.i(TAG, "spectrum ：%s", Arrays.toString(spectrum));


        byte[] bytes = new byte[result.length];
        for (int i = 0; i < result.length; i++) {
            bytes[i] = (byte) result[i];
        }

        return bytes;
    }

    /**
     * Changes spectrum values according to volume level
     *
     * @param buffer   - chunk of music
     * @param spectrum - current spectrum values
     */
    private void calculateVolumeLevel(byte[] buffer, float[] spectrum) {
        long currentMaxDb = getMaxDecibels(buffer);
        float coefficient = (float) currentMaxDb / maxVolumeDb;
        float maxCoefficient = 0;
        for (int i = 0; i < NUMBER_OF_FREQ_BARS; i++) {
            if (maxCoefficient < spectrum[i]) {
                maxCoefficient = spectrum[i];
            }
        }
        if (maxCoefficient > 0) {
            coefficient /= maxCoefficient;
            for (int i = 0; i < NUMBER_OF_FREQ_BARS; i++) {
                spectrum[i] *= coefficient;
            }
        }
    }


    private long getMaxDecibels(byte[] input) {
        float[] amplitudes = byteToFloat(input);
        if (amplitudes == null) return 0;
        float maxAmplitude = 2;
        for (float amplitude : amplitudes) {
            if (Math.abs(maxAmplitude) < Math.abs(amplitude)) {
                maxAmplitude = amplitude;
            }
        }
        return Math.round(20 * Math.log10(maxAmplitude)); //formula dB = 20 * log(a / a0);
    }

    private static final int NUMBER_OF_FREQ_BARS = 5;
    private int bytesPerSample = 2;
    private int maxVolumeDb = 90;

    private float[] byteToFloat(byte[] input) {
        ByteBuffer buffer = ByteBuffer.wrap(input);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer floatBuffer = FloatBuffer.allocate(input.length / bytesPerSample);
        switch (bytesPerSample) {
            case 1:
                for (int i = 0; i < floatBuffer.capacity(); i++) {
                    floatBuffer.put(buffer.get(i * bytesPerSample));
                }
                return floatBuffer.array();
            case 2:
                for (int i = 0; i < floatBuffer.capacity(); i++) {
                    floatBuffer.put(buffer.getShort(i * bytesPerSample));
                }
                return floatBuffer.array();
            case 4:
                for (int i = 0; i < floatBuffer.capacity(); i++) {
                    floatBuffer.put(buffer.getInt(i * bytesPerSample));
                }
                return floatBuffer.array();

        }
        return null;
    }

    /**
     * Calculates how strong are frequencies of each group represented in the provided spectrum
     *
     * @param amplitudes   current spectrum
     * @param groupsNumber amount of groups to separate
     * @return array of each group strength. Each value meets [0;1] interval
     */
    private float[] fetchSpectrum(float[] amplitudes, int groupsNumber) {
        int approximateGroupLength = amplitudes.length / groupsNumber;
        float[] result = new float[groupsNumber];
        double tmpSum;
        double wholeSum = 0;
        for (int i = 0; i < groupsNumber; i++) {
            tmpSum = 0;
            for (int j = i * approximateGroupLength; j < (i + 1) * approximateGroupLength; j++) {
                tmpSum += amplitudes[j];
            }
            result[i] = (float) (tmpSum / approximateGroupLength);
            wholeSum += result[i];
        }
        for (int i = 0; i < groupsNumber; i++) {
            result[i] /= wholeSum;
        }
        return result;
    }

}
