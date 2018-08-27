//package com.zlw.main.audioeffects.mp3;
//
//import android.media.AudioFormat;
//import android.media.AudioTrack;
//import android.support.annotation.NonNull;
//
//import com.zlw.main.audioeffects.utils.Logger;
//
//import java.io.BufferedInputStream;
//import java.io.DataInputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.ThreadFactory;
//import java.util.concurrent.ThreadPoolExecutor;
//import java.util.concurrent.TimeUnit;
//
///**
// * Created by admin on 2018/8/23.
// */
//
//public class AudioTrackHelper {
//
//    private static final String TAG = AudioTrackHelper.class.getSimpleName();
//    private static final String MARK_PCM = ".pcm";
//
//    private static final int PARAM_SAMPLE_RATE_IN_HZ = 16000;
//
//    private static AudioTrackHelper instance;
//    private AudioTrack player;
//    private ExecutorService threadPool;
//
//    private PcmChunkPlayer chunkPlayer = PcmChunkPlayer.getInstance();
//
//    private boolean canPlay;
//
//    private boolean isPlaying;
//
//    private AudioTrackHelper() {
//        threadPool = new ThreadPoolExecutor(1, 1, 10000L,
//                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
//                new ThreadFactory() {
//                    @Override
//                    public Thread newThread(@NonNull Runnable runnable) {
//                        return new Thread(runnable, "AudioTrackHelper");
//                    }
//                });
//    }
//
//    public static AudioTrackHelper getInstance() {
//        if (instance == null) {
//            synchronized (AudioTrackHelper.class) {
//                if (instance == null) {
//                    instance = new AudioTrackHelper();
//                }
//            }
//        }
//        return instance;
//    }
//
//    public void playPcmAsync(final File pcmFile) {
//        if (isPlaying) {
//            cancel();
//        }
//        if (threadPool != null) {
//            threadPool.execute(new Runnable() {
//                @Override
//                public void run() {
//                    playPcm(pcmFile);
//                }
//            });
//        }
//    }
//
//    /**
//     * 播放PCM
//     *
//     * @param pcmFile PCM文件
//     */
//    private void playPcm(File pcmFile) {
//        if (isPlaying) {
//            Logger.w(TAG, "播放重叠，请播放完成后重试");
//            return;
//        }
//        if (pcmFile == null || !pcmFile.exists()) {
//            Logger.e(TAG, "pcmFile is null");
//            return;
//        }
//
//        if (!pcmFile.getName().endsWith(MARK_PCM)) {
//            Logger.e(TAG, "这不是一个PCM文件");
//            return;
//        }
//
//        isPlaying = true;
//        canPlay = true;
//        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(pcmFile)))) {
//
//            int bufferSizeInBytes = AudioTrack.getMinBufferSize(PARAM_SAMPLE_RATE_IN_HZ,
//                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
////            player = new AudioTrack(AudioManager.STREAM_MUSIC, PARAM_SAMPLE_RATE_IN_HZ,
////                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
////                    bufferSizeInBytes, AudioTrack.MODE_STREAM);
//
//            byte[] data = new byte[bufferSizeInBytes];
////            player.play();
//            chunkPlayer.init();
//
//            while (canPlay) {
//                int i = 0;
//                while (dis.available() > 0 && i < data.length) {
//                    data[i] = dis.readByte();
//                    i++;
//                }
//
////                player.write(data, 0, data.length);
//                chunkPlayer.putPcmData(data, 0, data.length);
//                if (i != bufferSizeInBytes) {
//                    chunkPlayer.over();
////                    player.stop();
////                    player.release();
//                    isPlaying = false;
//                    break;
//                }
//            }
//        } catch (Exception e) {
//            Logger.e(e, TAG, e.getMessage());
//        }
//    }
//
//    /**
//     * 取消播放
//     */
//    public void cancel() {
//        Logger.d(TAG, "cancel : 取消播放");
//        try {
//            canPlay = false;
//            if (player != null && isPlaying) {
//                player.stop();
//                player.release();
//                isPlaying = false;
//            }
//        } catch (Exception e) {
//            Logger.e(e, TAG, e.getMessage());
//        }
//    }
//}
