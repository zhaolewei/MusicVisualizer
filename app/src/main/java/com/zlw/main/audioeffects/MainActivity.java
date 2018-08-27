package com.zlw.main.audioeffects;

import android.Manifest;
import android.content.Intent;
import android.media.AudioManager;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.zlw.main.audioeffects.player.AudioVisualConverter;
import com.zlw.main.audioeffects.player.MyMediaPlayer;
import com.zlw.main.audioeffects.utils.Logger;
import com.zlw.main.audioeffects.view.AudioView;

import java.util.HashMap;
import java.util.Locale;
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
        MUSIC_DATA.put("雨霖铃.mp3", R.raw.yulinling);
        MUSIC_DATA.put("清平调.mp3", R.raw.qingpingdiao);
        MUSIC_DATA.put("China-X.mp3", R.raw.china_x);
        MUSIC_DATA.put("8000Hz音频.mp3", R.raw.m8000hz);
        MUSIC_DATA.put("忆夏思乡.mp3", R.raw.yixia);
        MUSIC_DATA.put("Axero.mp3", R.raw.axero);
    }

    private Visualizer visualizer;
    private AudioVisualConverter audioVisualConverter;
    private boolean isInit = false;
    private AudioView audioView;
    private AudioView audioView2;
    private Spinner spinner;
    private TextView tvVoiceSize;
    private ImageView ivSwitch;

    private AudioManager audioManager;
    private MyMediaPlayer mediaPlayer;

    private Visualizer.OnDataCaptureListener dataCaptureListener = new Visualizer.OnDataCaptureListener() {
        @Override
        public void onWaveFormDataCapture(Visualizer visualizer, final byte[] waveform, int samplingRate) {
            audioView.post(new Runnable() {
                @Override
                public void run() {
                    audioView.setWaveData(waveform);
                }
            });
        }

        @Override
        public void onFftDataCapture(Visualizer visualizer, final byte[] fft, int samplingRate) {
            audioView2.post(new Runnable() {
                @Override
                public void run() {
                    audioView2.setWaveData(fft);
                    tvVoiceSize.setText(String.format(Locale.getDefault(), "当前分贝: %s db", audioVisualConverter.getVoiceSize(fft)));
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, 1);
        audioManager = (AudioManager) this.getSystemService(AUDIO_SERVICE);
        initView();
        initEvent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (visualizer != null) {
            visualizer.release();
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Logger.d(TAG, "requestCode:%s", requestCode);
        initVisualizer();
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
        audioView.setStyle(AudioView.ShowStyle.STYLE_HOLLOW_LUMP, AudioView.ShowStyle.STYLE_NOTHING);
        audioView2 = findViewById(R.id.audioView2);
        audioView2.setStyle(AudioView.ShowStyle.STYLE_HOLLOW_LUMP, AudioView.ShowStyle.STYLE_WAVE);
        spinner = findViewById(R.id.spinner);
        tvVoiceSize = findViewById(R.id.tvVoiceSize);
        ivSwitch = findViewById(R.id.ivSwitch);
        ivSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Mp3Activity.class));
                finish();
            }
        });
    }

    private void play(int res) {
        isInit = false;
        mediaPlayer = MyMediaPlayer.getInstance();
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

            int captureSize = Visualizer.getCaptureSizeRange()[1];
            int captureRate = Visualizer.getMaxCaptureRate() * 3 / 4;
            Logger.d(TAG, "精度: %s", captureSize);
            Logger.d(TAG, "刷新频率: %s", captureRate);

            visualizer.setCaptureSize(captureSize);
            visualizer.setDataCaptureListener(dataCaptureListener, captureRate, true, true);
            visualizer.setScalingMode(Visualizer.SCALING_MODE_NORMALIZED);
            visualizer.setEnabled(true);
        } catch (Exception e) {
            Logger.e(TAG, "请检查录音权限");
            isInit = false;
        }
    }

}
