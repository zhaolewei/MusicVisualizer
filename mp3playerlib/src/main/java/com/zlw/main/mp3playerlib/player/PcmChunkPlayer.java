package com.zlw.main.mp3playerlib.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.zlw.main.mp3playerlib.utils.ByteUtils;
import com.zlw.main.mp3playerlib.utils.FrequencyScanner;
import com.zlw.main.mp3playerlib.utils.Logger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author zhaolewei on 2018/8/23.
 */
public class PcmChunkPlayer {
    private static final String TAG = PcmChunkPlayer.class.getSimpleName();
    private static final int DEFAULT_SAMPLE_RATE = 16000;
    private static final int DEFAULT_CHANNEL_COUNT = 1;

    private static final int VAD_THRESHOLD = 8;

    private static PcmChunkPlayer instance;

    private AudioTrack player;
    private PcmChunkPlayerThread pcmChunkPlayerThread;

    private int sampleRate = DEFAULT_SAMPLE_RATE;
    /**
     * 单声道
     */
    private int sampleBitConfig = AudioFormat.CHANNEL_OUT_MONO;
    private int channelCountConfig = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * 是否开启静音跳过功能
     */
    private volatile boolean isVad = false;

    public static PcmChunkPlayer getInstance() {
        if (instance == null) {
            synchronized (PcmChunkPlayer.class) {
                if (instance == null) {
                    instance = new PcmChunkPlayer();
                }
            }
        }
        return instance;
    }

    private PcmChunkPlayer() {

    }

    public synchronized void setVad(boolean vad) {
        isVad = vad;
    }

    /**
     * 设置Pcm的音频格式
     */
    public void setFormat(int sampleRate, int sampleBit, int channelCount) {
        this.sampleRate = sampleRate;

        if (sampleBit == 16) {
            this.sampleBitConfig = AudioFormat.ENCODING_PCM_16BIT;
        } else if (sampleBit == 8) {
            this.sampleBitConfig = AudioFormat.ENCODING_PCM_8BIT;
        } else {
            this.sampleBitConfig = AudioFormat.ENCODING_PCM_FLOAT;
        }

        if (channelCount == 1) {
            this.channelCountConfig = AudioFormat.CHANNEL_OUT_MONO;
        } else if (channelCount == 2) {
            this.channelCountConfig = AudioFormat.CHANNEL_OUT_STEREO;
        } else {
            Logger.e(TAG, "不支持该声道数：%s", channelCount);
        }
    }

    public void init(boolean vad, PcmChunkPlayerListener pcmChunkPlayerListener) {
        release();
        init();
        setVad(vad);
        setPcmChunkPlayerListener(pcmChunkPlayerListener);
    }

    private void init() {
        Logger.d(TAG, "音频：sampleRate: %s ，sampleBitConfig：%s,channelCountConfig：%s ", sampleRate, sampleBitConfig, channelCountConfig);
        int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRate,
                channelCountConfig, sampleBitConfig);
        player = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                channelCountConfig, sampleBitConfig,
                bufferSizeInBytes, AudioTrack.MODE_STREAM);

        player.play();
        pcmChunkPlayerThread = new PcmChunkPlayerThread();
        pcmChunkPlayerThread.start();
    }

    public void putPcmData(byte[] chunk, int size) {
        if (pcmChunkPlayerThread == null) {
            Logger.w(TAG, "pcmChunkPlayerThread is null");
            return;
        }
        pcmChunkPlayerThread.addChangeBuffer(new ChangeBuffer(chunk, size, sampleRate));
    }

    public void over() {
        if (pcmChunkPlayerThread == null) {
            Logger.w(TAG, "pcmChunkPlayerThread is null");
            return;
        }
        pcmChunkPlayerThread.stopSafe();
    }

    private PcmChunkPlayerListener pcmChunkPlayerListener;

    public void setPcmChunkPlayerListener(PcmChunkPlayerListener pcmChunkPlayerListener) {
        this.pcmChunkPlayerListener = pcmChunkPlayerListener;
    }

    public void release() {
        if (pcmChunkPlayerThread != null) {
            pcmChunkPlayerThread.stopNow();
            pcmChunkPlayerThread = null;
        }

        if (player != null) {
            player.release();
            player = null;
        }
    }

    /**
     * PCM数据调度器
     */
    public class PcmChunkPlayerThread extends Thread {

        /**
         * PCM 数据缓冲队列
         */
        private List<ChangeBuffer> cacheBufferList = Collections.synchronizedList(new LinkedList<ChangeBuffer>());

        /**
         * 是否已停止
         */
        private volatile boolean isOver = false;

        /**
         * 是否继续轮询数据队列
         */
        private volatile boolean start = true;

        public void addChangeBuffer(ChangeBuffer changeBuffer) {
            if (changeBuffer != null) {
                cacheBufferList.add(changeBuffer);
                synchronized (this) {
                    notify();
                }
            }
        }

        public void stopSafe() {
            isOver = true;
            synchronized (this) {
                notify();
            }
        }

        public void stopNow() {
            isOver = true;
            start = false;
            synchronized (this) {
                notify();
            }
        }

        private ChangeBuffer next() {
            for (; ; ) {
                if (cacheBufferList == null || cacheBufferList.size() == 0) {
                    try {
                        if (isOver) {
                            finish();
                            return null;
                        }
                        synchronized (this) {
                            wait();
                        }
                    } catch (Exception e) {
                        Logger.e(e, TAG, e.getMessage());
                    }
                } else {
                    return cacheBufferList.remove(0);
                }
            }
        }

        private void finish() {
            start = false;
            if (pcmChunkPlayerListener != null) {
                pcmChunkPlayerListener.onFinish();
            }
        }

        @Override
        public void run() {
            super.run();

            while (start) {
                play(next());
            }
        }

        long readSize = 0L;

        private void play(ChangeBuffer chunk) {
            if (chunk == null) {
                return;
            }
            readSize += chunk.getSize();

            if (!isVad || chunk.getVoiceSize() > VAD_THRESHOLD) {
                if (pcmChunkPlayerListener != null) {
                    pcmChunkPlayerListener.onPlayData(chunk.getRawData());
                    pcmChunkPlayerListener.onPlaySize(readSize);
                }
                if (player != null && start) {
                    player.write(chunk.getRawData(), 0, chunk.getSize());
                }
            }
        }
    }

    private static FrequencyScanner fftScanner = new FrequencyScanner();

    public static class ChangeBuffer {

        private byte[] rawData;

        private int size;
        private int voiceSize;

        public ChangeBuffer(byte[] rawData, int size, int sampleRate) {
            this.rawData = rawData.clone();
            this.size = size;
            this.voiceSize = (int) fftScanner.getMaxFrequency(ByteUtils.toShorts(rawData), sampleRate);
        }

        public byte[] getRawData() {
            return rawData;
        }

        public int getSize() {
            return size;
        }

        public int getVoiceSize() {
            return voiceSize;
        }
    }


    public interface PcmChunkPlayerListener {
        /**
         * 播放完成
         */
        void onFinish();

        /**
         * 已播放的数据量
         */
        void onPlaySize(long size);

        /**
         * 播放的数据
         */
        void onPlayData(byte[] size);
    }

}
