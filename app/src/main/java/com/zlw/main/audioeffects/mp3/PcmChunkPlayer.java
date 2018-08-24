package com.zlw.main.audioeffects.mp3;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.zlw.main.audioeffects.mp3.utils.FrequencyScanner;
import com.zlw.main.audioeffects.utils.ByteUtils;
import com.zlw.main.audioeffects.utils.Logger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author zhaolewei on 2018/8/23.
 */
public class PcmChunkPlayer {
    private static final String TAG = PcmChunkPlayer.class.getSimpleName();

    private static final int PARAM_SAMPLE_RATE_IN_HZ = 16000;

    private static PcmChunkPlayer instance;
    private AudioTrack player;
    private boolean isPlaying;
    private PcmChunkPlayerThread pcmChunkPlayerThread;

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

    public synchronized void setVad(boolean vad) {
        isVad = vad;
    }

    public void init() {
        int bufferSizeInBytes = AudioTrack.getMinBufferSize(PARAM_SAMPLE_RATE_IN_HZ,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        player = new AudioTrack(AudioManager.STREAM_MUSIC, PARAM_SAMPLE_RATE_IN_HZ,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSizeInBytes, AudioTrack.MODE_STREAM);

        player.play();
        pcmChunkPlayerThread = new PcmChunkPlayerThread();
        pcmChunkPlayerThread.start();
    }

    public void putPcmData(byte[] chunk, int start, int end) {
        if (pcmChunkPlayerThread == null) {
            Logger.w(TAG, "pcmChunkPlayerThread is null");
            return;
        }
        pcmChunkPlayerThread.addChangeBuffer(new ChangeBuffer(chunk, start, end));
    }

    public void over() {
        if (pcmChunkPlayerThread == null) {
            Logger.w(TAG, "pcmChunkPlayerThread is null");
            return;
        }
        pcmChunkPlayerThread.isOver = true;
    }

    private EncordFinishListener encordFinishListener;

    public void setEncordFinishListener(EncordFinishListener encordFinishListener) {
        this.encordFinishListener = encordFinishListener;
    }

    /**
     * PCM数据调度器
     */
    public class PcmChunkPlayerThread extends Thread {

        /**
         * PCM 缓冲数据
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
            if (encordFinishListener != null) {
                encordFinishListener.onFinish();
            }
        }

        @Override
        public void run() {
            super.run();

            while (start) {
                ChangeBuffer next = next();
                if (next == null) {
                    return;
                }
                next.add(next());
                play(next);
            }
        }

        long readSize = 0L;
        FrequencyScanner fftScanner = new FrequencyScanner();

        private void play(ChangeBuffer chunk) {
            if (chunk == null) {
                return;
            }
            readSize += chunk.getSize();
            if (encordFinishListener != null) {
                encordFinishListener.onPlaySize(readSize);
            }

            double maxFrequency = fftScanner.getMaxFrequency(ByteUtils.toShorts(chunk.getRawData()));
            Logger.w(TAG, "此段声音： %s", maxFrequency);
            if (!isVad || maxFrequency > 20) {
                player.write(chunk.getRawData(), chunk.getStartIndex(), chunk.getSize());
            }
        }
    }

    public static class ChangeBuffer {
        private byte[] rawData;

        private int startIndex;
        private int size;

        public ChangeBuffer(byte[] rawData, int startIndex, int size) {
            this.rawData = rawData.clone();
            this.startIndex = startIndex;
            this.size = size;
        }

        public byte[] getRawData() {
            return rawData;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public int getSize() {
            return size;
        }

        public ChangeBuffer add(ChangeBuffer nextData) {
            if (nextData != null) {
                this.rawData = ByteUtils.byteMerger(this.rawData, nextData.rawData);
                this.size = this.size + nextData.size;
            }
            return this;
        }
    }


    public interface EncordFinishListener {
        /**
         * 格式转换完毕
         */
        void onFinish();

        void onPlaySize(long size);
    }

}
