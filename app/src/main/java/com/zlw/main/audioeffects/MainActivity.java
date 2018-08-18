package com.zlw.main.audioeffects;

import android.Manifest;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.zlw.main.audioeffects.player.AudioVisualConverter;
import com.zlw.main.audioeffects.player.MyMediaPlayer;
import com.zlw.main.audioeffects.utils.Logger;
import com.zlw.main.audioeffects.view.AudioView;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
    };

    private static final Map<String, Integer> MUSIC_DATA = new HashMap<>();

    {
        MUSIC_DATA.put("剑墨.mp3", R.raw.mo);
        MUSIC_DATA.put("grace.mp3", R.raw.grace);
        MUSIC_DATA.put("faded.mp3", R.raw.faded);
    }

    private Visualizer visualizer;
    private AudioVisualConverter audioVisualConverter;
    private boolean isInit = false;

    private AudioView audioView, audioView2;
    private Spinner spinner;


    private Visualizer.OnDataCaptureListener dataCaptureListener = new Visualizer.OnDataCaptureListener() {
        @Override
        public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
            audioView.setWaveData(audioVisualConverter.readyDataByte(waveform));
        }

        @Override
        public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
//            Logger.d(TAG, "");
            audioView2.setWaveData(fft);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, 1);
        initView();
        initEvent();
        play(R.raw.grace);
    }

    private void initEvent() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                MUSIC_DATA.keySet().toArray(new String[MUSIC_DATA.size()]));
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object item = parent.getItemAtPosition(position);
                Logger.i(TAG, "选择： %s", item.toString());
                play(MUSIC_DATA.get(item.toString()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void initView() {
        setContentView(R.layout.activity_main);
        audioView = findViewById(R.id.audioView);
        audioView2 = findViewById(R.id.audioView2);
        spinner = findViewById(R.id.spinner);
    }

    private void play(int res) {
        isInit = false;
        MyMediaPlayer mediaPlayer = MyMediaPlayer.getInstance();
        mediaPlayer.play(res);
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
        audioVisualConverter = new AudioVisualConverter();
        Logger.d(TAG, "initVisualizer()");
        try {
            int mediaPlayerId = MyMediaPlayer.getInstance().getMediaPlayerId();
            Logger.i(TAG, "mediaPlayerId: %s", mediaPlayerId);
            if (visualizer != null) {
                visualizer.release();
            }
            visualizer = new Visualizer(mediaPlayerId);

            visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[0]);
            visualizer.setDataCaptureListener(dataCaptureListener, Visualizer.getMaxCaptureRate() / 2, true, true);
            visualizer.setEnabled(true);
        } catch (Exception e) {
            Logger.e(TAG, "请检查录音权限");
        }
    }

}
