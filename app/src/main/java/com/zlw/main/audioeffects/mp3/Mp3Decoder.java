package com.zlw.main.audioeffects.mp3;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;

import com.zlw.main.audioeffects.base.MyApp;
import com.zlw.main.audioeffects.utils.Logger;
import com.zlw.main.audioeffects.utils.RecordUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * MP3解码器
 * MP3->PCM
 *
 * @author zhaolewei on 2018/8/23.
 */
public class Mp3Decoder {

    private static final String TAG = Mp3Decoder.class.getSimpleName();
    private MediaExtractor mediaExtractor;
    private MediaCodec mediaDecode;

    private ByteBuffer[] decodeInputBuffers, decodeOutputBuffers;
    private MediaCodec.BufferInfo decodeBufferInfo;
    private boolean codeOver;
    private MediaCodec mediaEncode;

    private ByteBuffer[] encodeInputBuffers, encodeOutputBuffers;
    private MediaCodec.BufferInfo encodeBufferInfo;
    private PcmChunkPlayer pcmChunkPlayer = PcmChunkPlayer.getInstance();
    private File file = new File("sdcard/Record/result.pcm");

    private InfoListener infoListener;

    private long decodeSize = 0L;
    private int dataIndex = 0;

    public void init(boolean vad, int raw, final InfoListener infoListener) {
        this.infoListener = infoListener;
        Logger.d(TAG, "init...");
        try {
            if (file.exists()) {
                file.delete();
            }
            boolean newFile = file.createNewFile();
            if (!newFile) {
                Logger.e(TAG, "创建文件失败");
                return;
            }
        } catch (Exception e) {
            Logger.e(TAG, e.getMessage());
        }
        pcmChunkPlayer.init();
        pcmChunkPlayer.setVad(vad);
        pcmChunkPlayer.setEncordFinishListener(new PcmChunkPlayer.EncordFinishListener() {
            @Override
            public void onFinish() {
            }

            @Override
            public void onPlaySize(long size) {
                infoListener.onProgress(size);
            }
        });
        try {
            mediaExtractor = new MediaExtractor();
            Uri uri = Uri.parse(String.format(Locale.getDefault(),
                    "android.resource://%s/%s", MyApp.getInstance().getPackageName(), raw));
            mediaExtractor.setDataSource(MyApp.getInstance(), uri, null);

            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                MediaFormat format = mediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                long duration = format.getLong(MediaFormat.KEY_DURATION);
                if (mime.startsWith("audio")) {
                    format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 200 * 1024);
                    mediaExtractor.selectTrack(i);
                    mediaDecode = MediaCodec.createDecoderByType(mime);
                    mediaDecode.configure(format, null, null, 0);
                    Logger.i(TAG, "音频信息：mime：%s, duration: %ss", mime, duration / 1000L);
                    break;
                }
            }
        } catch (IOException e) {
            Logger.e(e, TAG, e.getMessage());
        }

        if (mediaDecode == null) {
            Logger.e(TAG, "解码器初始化失败");
            return;
        }
        mediaDecode.start();
        decodeInputBuffers = mediaDecode.getInputBuffers();
        decodeOutputBuffers = mediaDecode.getOutputBuffers();
        decodeBufferInfo = new MediaCodec.BufferInfo();
        Logger.i(TAG, "input buffers: %s", decodeInputBuffers.length);
        startAsync();
    }

    private void initEncoder() {
        MediaFormat encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_MPEG, 16000, 1);
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 16000 * 1 * 16 / 8);
        encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100 * 1024);
        try {
            mediaEncode = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_MPEG);
            mediaEncode.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            Logger.e(e, TAG, e.getMessage());
        }
        if (mediaEncode == null) {
            Logger.e(TAG, "编码器初始化失败");
            return;
        }
        mediaEncode.start();
        encodeInputBuffers = mediaEncode.getInputBuffers();
        encodeOutputBuffers = mediaEncode.getOutputBuffers();
        encodeBufferInfo = new MediaCodec.BufferInfo();
    }


    /**
     * 开始解码 生成PCM数据块
     */
    private void startDecoder() {
        for (int i = 0; i < decodeInputBuffers.length - 1; i++) {
            int inputIndex = mediaDecode.dequeueInputBuffer(10000);
            if (inputIndex < 0) {
                codeOver = true;
                Logger.d(TAG, "解码完成");
                return;
            }
            ByteBuffer inputBuffer = decodeInputBuffers[inputIndex];
            inputBuffer.clear();
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
            if (sampleSize >= 0) {
                mediaDecode.queueInputBuffer(inputIndex, 0, sampleSize, 0, 0);
                mediaExtractor.advance();
                decodeSize += sampleSize;
//                Logger.d(TAG, "进度： %s/", decodeSize);
            } else {
                codeOver = true;
                Logger.d(TAG, "解码完成");
                putPcmChunk(null);
            }


            int outputIndex = mediaDecode.dequeueOutputBuffer(decodeBufferInfo, 10000);
            ByteBuffer outputBuffer;
            byte[] chunkPCM;
            while (outputIndex >= 0) {
                outputBuffer = decodeOutputBuffers[outputIndex];
                chunkPCM = new byte[decodeBufferInfo.size];
                outputBuffer.get(chunkPCM);
                outputBuffer.clear();
                //TODO: 记录chunkPCM
                putPcmChunk(chunkPCM);
                mediaDecode.releaseOutputBuffer(outputIndex, false);
                outputIndex = mediaDecode.dequeueOutputBuffer(decodeBufferInfo, 10000);
            }
        }
    }

    /**
     * 解码线程
     */
    private class DecodeRunnable implements Runnable {

        @Override
        public void run() {
            while (!codeOver) {
                startDecoder();
            }
        }
    }

    public void startAsync() {
        new Thread(new DecodeRunnable()).start();
    }

    PcmFileWaveData waveData = new PcmFileWaveData();

    private void putPcmChunk(byte[] pcmChunk) {
        if (pcmChunk == null) {
            pcmChunkPlayer.over();
            byte[] bytes = waveData.getBytes();
            Logger.i(TAG, "finish: %s", Arrays.toString(bytes));
            infoListener.onFinish(bytes);
            return;
        }
//        Logger.i(TAG, "putPcmChunk size: %s", pcmChunk.length);
        pcmChunkPlayer.putPcmData(pcmChunk, 0, pcmChunk.length);
        waveData.addData(pcmChunk);
        ByteUtils.byte2File(pcmChunk, file);
        dataIndex += pcmChunk.length;
    }

    /**
     * PCM 文件 波形数据
     * 用于可视化
     */
    private class PcmFileWaveData {
        List<Byte> voiceList = new ArrayList<>();

        public void addData(byte[] pcmChunk) {
            long maxDecibels = RecordUtils.getMaxDecibels(pcmChunk);
            voiceList.add((byte) maxDecibels);
        }

        public byte[] getBytes() {
            byte[] bytes = new byte[voiceList.size()];
            for (int i = 0; i < voiceList.size(); i++) {
                bytes[i] = voiceList.get(i);
            }
            return bytes;
        }
    }

    public interface InfoListener {
        void onFinish(byte[] data);

        void onProgress(long progress);
    }

}
