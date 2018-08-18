package com.zlw.main.audioeffects.player;

import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

import com.zlw.main.audioeffects.base.MyApp;
import com.zlw.main.audioeffects.utils.Logger;

import java.io.File;

/**
 * @author zhaolewei on 2018/7/26.
 */
public class MyMediaPlayer {
    private static final String TAG = MyMediaPlayer.class.getSimpleName();
    private static final MyMediaPlayer INSTANCE = new MyMediaPlayer();
    private static final float DEFAULT_PLAY_SPEED = 1.0f;

    private Handler playThreadHandler;
    private MediaPlayer mediaPlayer;
    private Equalizer equalizer;
    private PlayStateListener playStateListener;

    private float playSpeed = DEFAULT_PLAY_SPEED;

    private MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            notifyState(PlayState.STATE_IDLE);
        }
    };
    private MediaPlayer.OnInfoListener infoListener = new MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            return false;
        }
    };
    private MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            notifyState(PlayState.STATE_IDLE);
            return false;
        }
    };
    private MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mp.start();
            if (playSpeed != DEFAULT_PLAY_SPEED) {
                changePlayerSpeed(playSpeed);
            }
            notifyState(PlayState.STATE_PLAYING);
        }
    };

    public int getMediaPlayerId() {
        return mediaPlayer.getAudioSessionId();
    }

    public static MyMediaPlayer getInstance() {
        return INSTANCE;
    }

    private MyMediaPlayer() {
        initHandlerThread();
    }


    public synchronized void play(final int raw) {
        playThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                doPlay(raw);
            }
        });
    }

    public synchronized void stop() {
        if (mediaPlayer == null) {
            return;
        }
        playThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    notifyState(PlayState.STATE_IDLE);
                }
            }
        });
    }

    public synchronized void pause() {
        if (mediaPlayer == null || !mediaPlayer.isPlaying()) {
            return;
        }
        playThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyState(PlayState.STATE_PAUSE);
                mediaPlayer.pause();
            }
        });
    }

    public void resume() {
        if (mediaPlayer == null || mediaPlayer.isPlaying()) {
            return;
        }
        playThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    notifyState(PlayState.STATE_PLAYING);
                    mediaPlayer.start();
                }
            }
        });

    }

    public synchronized void release() {
        if (mediaPlayer == null) {
            return;
        }
        playSpeed = DEFAULT_PLAY_SPEED;
        playThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        });
    }

    public void changePlayerSpeed(final float speed) {
        Logger.d(TAG, "changePlayerSpeed: %s", speed);
        playSpeed = speed;
        if (mediaPlayer == null) {
            return;
        }
        playThreadHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
                    } else {
                        mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
                        mediaPlayer.pause();
                    }
                }
            }
        }, 500);
    }

    public void setPlayStateListener(PlayStateListener playStateListener) {
        this.playStateListener = playStateListener;
    }

    /**
     * 播放音频
     *
     * @param raw 资源文件id
     */
    private synchronized void doPlay(final int raw) {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }

        try {
            mediaPlayer = MediaPlayer.create(MyApp.getInstance(), raw);
            if (mediaPlayer == null) {
                Logger.e(TAG, "mediaPlayer is null");
                return;
            }

            mediaPlayer.setOnInfoListener(infoListener);
            mediaPlayer.setOnErrorListener(errorListener);
            mediaPlayer.setOnCompletionListener(completionListener);
            mediaPlayer.setOnPreparedListener(preparedListener);
        } catch (Exception e) {
            Logger.e(e, TAG, e.getMessage());
            stop();
            release();
            mediaPlayer = null;
        }
    }

    /**
     * 注：此方法在子线程中
     *
     * @param state 播放状态
     */
    private void notifyState(final PlayState state) {
        if (playStateListener != null) {
            playStateListener.onStateChange(state);
        }
    }

    private void initHandlerThread() {
        HandlerThread playThread = new HandlerThread("play");
        playThread.start();
        playThreadHandler = new Handler(playThread.getLooper());
    }


    public enum PlayState {
        /**
         * 播放器初始化中，不可用
         */
        STATE_INITIALIZING,
        /**
         * 空闲状态,可用
         */
        STATE_IDLE,
        /**
         * 正在播放
         */
        STATE_PLAYING,
        /**
         * 暂停状态
         */
        STATE_PAUSE
    }

    public interface PlayStateListener {
        void onStateChange(PlayState state);
    }
}
