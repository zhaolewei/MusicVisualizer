package com.zlw.main.mp3playerlib.player;

import android.support.annotation.NonNull;

import com.zlw.main.mp3playerlib.utils.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author zhaolewei
 */
public class PcmFilePlayer {

    private static final String TAG = PcmFilePlayer.class.getSimpleName();
    private static final String MARK_PCM = ".pcm";
    private static PcmFilePlayer instance;

    private ExecutorService threadPool;
    private PcmChunkPlayer chunkPlayer;
    private boolean isPlaying;

    private PcmFilePlayer() {
        threadPool = new ThreadPoolExecutor(1, 1, 10000L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(@NonNull Runnable runnable) {
                        return new Thread(runnable, "PcmFilePlayer");
                    }
                });
        chunkPlayer = PcmChunkPlayer.getInstance();
        chunkPlayer.init(false, new PcmChunkPlayer.PcmChunkPlayerListener() {
            @Override
            public void onFinish() {

            }

            @Override
            public void onPlaySize(long size) {

            }

            @Override
            public void onPlayData(byte[] size) {

            }
        });
    }

    public static PcmFilePlayer getInstance() {
        if (instance == null) {
            synchronized (PcmFilePlayer.class) {
                if (instance == null) {
                    instance = new PcmFilePlayer();
                }
            }
        }
        return instance;
    }

    public void playPcmAsync(final File pcmFile) {
        if (isPlaying) {
            cancel();
        }
        if (threadPool != null) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    playPcm(pcmFile);
                }
            });
        }
    }

    /**
     * 播放PCM
     *
     * @param pcmFile PCM文件
     */
    private void playPcm(File pcmFile) {
        if (isPlaying) {
            Logger.w(TAG, "播放重叠，请播放完成后重试");
            return;
        }
        if (pcmFile == null || !pcmFile.exists()) {
            Logger.e(TAG, "pcmFile is null");
            return;
        }

        if (!pcmFile.getName().endsWith(MARK_PCM)) {
            Logger.e(TAG, "这不是一个PCM文件");
            return;
        }

        isPlaying = true;
        int bufferSize = 2048;
        try (FileInputStream dis = new FileInputStream(pcmFile)) {
            chunkPlayer.setFormat(16000, 16, 1);
            int len = -1;
            byte[] data = new byte[bufferSize];
            while ((len = dis.read(data)) != -1) {
                chunkPlayer.putPcmData(data, len);
            }
            chunkPlayer.over();
        } catch (Exception e) {
            Logger.e(e, TAG, e.getMessage());
        }
    }

    /**
     * 取消播放
     */
    public void cancel() {
        Logger.d(TAG, "cancel : 取消播放");
        try {
            if (chunkPlayer != null && isPlaying) {
                chunkPlayer.release();
                isPlaying = false;
            }
        } catch (Exception e) {
            Logger.e(e, TAG, e.getMessage());
        }
    }
}

