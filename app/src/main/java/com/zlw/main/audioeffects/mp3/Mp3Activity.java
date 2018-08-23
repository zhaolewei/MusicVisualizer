package com.zlw.main.audioeffects.mp3;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.zlw.main.audioeffects.R;
import com.zlw.main.audioeffects.mp3.utils.FrequencyScanner;
import com.zlw.main.audioeffects.utils.Logger;
import com.zlw.main.audioeffects.view.AudioView2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * @author zhaolewei on 2018/8/23.
 */
public class Mp3Activity extends AppCompatActivity {
    private static final String TAG = Mp3Activity.class.getSimpleName();
    Mp3Decoder mp3Decoder;
    AudioView2 audioView;

    private Button btPlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_mp3);
        audioView = findViewById(R.id.audioView);
        btPlay = findViewById(R.id.btPlay);

        btPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });
        showWave();


    }

    private void play() {
        mp3Decoder = new Mp3Decoder();
        mp3Decoder.init(R.raw.test, new Mp3Decoder.InfoListener() {
            @Override
            public void onFinish(final byte[] data) {
                audioView.post(new Runnable() {
                    @Override
                    public void run() {
//                        showWave();
                    }
                });
            }

            @Override
            public void onProgress(long progress) {
                audioView.setProgress(progress);
            }
        });
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
        short[] shorts = com.zlw.main.audioeffects.utils.ByteUtils.toShorts(data);
        return fft(shorts);
    }


    /**
     * @param data
     * @param sampleRate
     * @return
     */
    private byte[] fft(short[] sampleData) {
        Logger.i(TAG, "sampleData sie：%s ", sampleData.length);
        Logger.i(TAG, "估算时长：%s s", sampleData.length / 16000);

        int que = 16 * 8;
        Logger.i(TAG, "循环次数：%s s", sampleData.length / que);
        byte[] reslut = new byte[sampleData.length / que];

        short[] data = new short[que];
        for (int i = 0; i < sampleData.length; i = i + que) {
            int end = i + que;
            if (end > sampleData.length) {
                break;
            }
            for (int j = i; j < end; j++) {
                data[j % que] = sampleData[j];
            }
            double extractFrequency = new FrequencyScanner().getMaxFrequency(data);
            reslut[i / que] = (byte) ((byte) extractFrequency > 127 ? 127 : extractFrequency);
            Logger.i(TAG, "取值[%s-%s] 相对振幅：%s ", i, end, extractFrequency);
        }

        return reslut;
    }

}
