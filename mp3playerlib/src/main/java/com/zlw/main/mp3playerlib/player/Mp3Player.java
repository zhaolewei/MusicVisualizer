package com.zlw.main.mp3playerlib.player;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.zlw.main.mp3playerlib.utils.ByteUtils;
import com.zlw.main.mp3playerlib.utils.FrequencyScanner;
import com.zlw.main.mp3playerlib.utils.Logger;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Mp3播放器
 *
 * @author zhaolewei on 2018/8/24.
 */
public class Mp3Player {
    private static Mp3Player instance;
    private static final String TAG = Mp3Player.class.getSimpleName();
    private Mp3Decoder mp3Decoder;
    private PcmChunkPlayer pcmChunkPlayer;
    private ExecutorService threadPool;
    private PlayInfoListener infoListener;
    private FrequencyScanner frequencyScanner = new FrequencyScanner();
    /**
     * 缓冲文件，用于放置解码后的PCM文件
     */
    private File cacheFile = new File("sdcard/Record/result.pcm");

    private boolean vad;
    private int raw;

    public static Mp3Player getInstance() {
        if (instance == null) {
            synchronized (Mp3Player.class) {
                if (instance == null) {
                    instance = new Mp3Player();
                }
            }
        }
        return instance;
    }

    private Mp3Player() {
        threadPool = new ThreadPoolExecutor(1, 1, 10000L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(@NonNull Runnable runnable) {
                        return new Thread(runnable, "Mp3Player");
                    }
                });
    }

    public void init(int raw, PlayInfoListener playInfoListener) {
        this.infoListener = playInfoListener;
        this.raw = raw;
        mp3Decoder = new Mp3Decoder();
        pcmChunkPlayer = PcmChunkPlayer.getInstance();
    }

    private boolean isPrepare = true;

    public void prepare(Context context) {
        Mp3Decoder newMp3Decoder = new Mp3Decoder();
        newMp3Decoder.init(context.getApplicationContext(), raw, new Mp3Decoder.InfoListener() {
            @Override
            public void onFinish() {
                infoListener.onDecodeFinish(cacheFile);
                isPrepare = false;
            }

            @Override
            public void onDecodeData(byte[] pcmChunk) {
                mergerDecodeDataAsync(pcmChunk);
            }
        });
    }

    public void play(Context context, boolean vad) {
        if (isPrepare) {
            Logger.e(TAG, "正在初始化文件");
            Toast.makeText(context, "正在初始化文件", Toast.LENGTH_LONG).show();
            return;
        }
        release();
        this.vad = vad;
        pcmChunkPlayer.init(vad, new PcmChunkPlayer.PcmChunkPlayerListener() {
            @Override
            public void onFinish() {
            }

            @Override
            public void onPlaySize(long size) {
                infoListener.onPlaySize(size);
            }

            @Override
            public void onPlayData(byte[] size) {
                double maxFrequency = frequencyScanner.getMaxFrequency(ByteUtils.toShorts(size));
                if (infoListener != null) {
                    infoListener.onVoiceSize((int) maxFrequency);
                }
            }
        });

        mp3Decoder.init(context.getApplicationContext(), raw, new Mp3Decoder.InfoListener() {
            @Override
            public void onFinish() {
                pcmChunkPlayer.over();
                infoListener.onDecodeFinish(cacheFile);
            }

            @Override
            public void onDecodeData(byte[] pcmChunk) {
                pcmChunkPlayer.putPcmData(pcmChunk, pcmChunk.length);
                mergerDecodeDataAsync(pcmChunk);
            }
        });
    }


    private void mergerDecodeDataAsync(final byte[] pcmChunk) {
        if (threadPool != null) {
            threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    ByteUtils.byte2File(pcmChunk, cacheFile);
                }
            });
        }
    }

    public void release() {
        createCacheFile();
        if (pcmChunkPlayer != null) {
            pcmChunkPlayer.release();
        }
        if (mp3Decoder != null) {
            mp3Decoder.release();
        }
    }

    private void createCacheFile() {
        try {
            if (cacheFile.exists()) {
                cacheFile.delete();
            }
            boolean newFile = cacheFile.createNewFile();
            if (!newFile) {
                Logger.e(TAG, "创建文件失败");
                return;
            }
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
        }
    }


    public interface PlayInfoListener {
        void onPlayProgress();

        void onDecodeData(byte[] data);

        void onDecodeFinish(File file);

        void onPlaySize(long playsize);

        void onVoiceSize(int playsize);
    }


}
