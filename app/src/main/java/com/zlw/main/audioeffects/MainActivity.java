package com.zlw.main.audioeffects;

import android.Manifest;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import com.zlw.main.audioeffects.player.MyMediaPlayer;
import com.zlw.main.audioeffects.utils.Logger;
import com.zlw.main.audioeffects.view.AudioView;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
    };
    private Visualizer visualizer;
    private boolean isInit = false;
    private AudioView audioView;


    private Visualizer.OnDataCaptureListener dataCaptureListener = new Visualizer.OnDataCaptureListener() {
        @Override
        public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
            audioView.setWaveData(waveform);
        }

        @Override
        public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        audioView = findViewById(R.id.audioView);
        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, 1);
        play();
    }

    private void play() {
        MyMediaPlayer mediaPlayer = MyMediaPlayer.getInstance();
        mediaPlayer.play(R.raw.grace);
        mediaPlayer.setPlayStateListener(new MyMediaPlayer.PlayStateListener() {
            @Override
            public void onStateChange(MyMediaPlayer.PlayState state) {
                if (state == MyMediaPlayer.PlayState.STATE_PLAYING) {
                    initVisualizer();
                }
            }
        });
    }

    private void initVisualizer() {
        if (isInit) {
            return;
        }
        isInit = true;

        Logger.d(TAG, "initVisualizer()");
        try {
            int mediaPlayerId = MyMediaPlayer.getInstance().getMediaPlayerId();
            Logger.i(TAG, "mediaPlayerId: %s", mediaPlayerId);
            visualizer = new Visualizer(mediaPlayerId);
            visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[0]);
            visualizer.setDataCaptureListener(dataCaptureListener, Visualizer.getMaxCaptureRate() / 2, true, false);
            visualizer.setEnabled(true);
        } catch (Exception e) {
            Logger.e(TAG, "请检查录音权限");
        }
    }

}
