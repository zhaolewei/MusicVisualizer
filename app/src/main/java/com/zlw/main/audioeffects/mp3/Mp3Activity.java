package com.zlw.main.audioeffects.mp3;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zlw.main.audioeffects.R;
import com.zlw.main.audioeffects.base.MyApp;
import com.zlw.main.audioeffects.utils.Logger;
import com.zlw.main.audioeffects.view.PcmFileWaveView;
import com.zlw.main.mp3playerlib.player.Mp3Player;

import java.io.File;

/**
 * @author zhaolewei on 2018/8/23.
 */
public class Mp3Activity extends AppCompatActivity {
    private static final String TAG = Mp3Activity.class.getSimpleName();
    Mp3Player mp3Player;
    PcmFileWaveView audioView;

    private Button btPlay, btPlayVad;
    private TextView tvText;
    private long time;

    private boolean isFirst = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_mp3);
        initView();
        init();
    }

    private void play(boolean vad) {
        mp3Player.play(this, vad);
    }

    private void init() {
        mp3Player = Mp3Player.getInstance();
        mp3Player.release();
        mp3Player.init(R.raw.test, new Mp3Player.PlayInfoListener() {
            @Override
            public void onPlayProgress() {

            }

            @Override
            public void onDecodeFinish(File file) {
                Logger.d(TAG, "onDecodeFinish");
                audioView.showPcmFileWave(file);
                isFirst = false;
            }

            @Override
            public void onPlaySize(long playsize) {
                audioView.setProgress(playsize);
            }

            @Override
            public void onVoiceSize(final int playsize) {
                tvText.post(new Runnable() {
                    @Override
                    public void run() {
                        tvText.setText("当前音量： " + playsize);
                    }
                });
            }

            @Override
            public void onDecodeData(final byte[] data) {
                if (!isFirst) {
                    return;
                }
                if (System.currentTimeMillis() - time < 500) {
                    return;
                }
                time = System.currentTimeMillis();
                audioView.post(new Runnable() {
                    @Override
                    public void run() {
                        audioView.setWaveData(data);
                    }
                });
            }
        });
        mp3Player.prepare(MyApp.getInstance());
    }

    private void initView() {
        audioView = findViewById(R.id.audioView);
        btPlay = findViewById(R.id.btPlay);
        btPlayVad = findViewById(R.id.btPlayVad);
        tvText = findViewById(R.id.tvText);
        btPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(false);
            }
        });
        btPlayVad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(true);
            }
        });
    }

}
