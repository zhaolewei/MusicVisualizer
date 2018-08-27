package com.zlw.main.mp3playerlib.player;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;

import com.zlw.main.mp3playerlib.utils.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
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
    private MediaCodec.BufferInfo decodeBufferInfo;

    private long decodeSize = 0L;
    private Mp3DecoderListener mp3DecoderListener;
    private ByteBuffer[] decodeInputBuffers, decodeOutputBuffers;
    private volatile boolean start = false;
    private volatile boolean decodeOver = false;

    private int sampleBit, channelCount, sampleRate;

    //TODO : File
    public void init(Context context, int raw, final Mp3DecoderListener mp3DecoderListener) {
        if (mp3DecoderListener == null) {
            return;
        }
        this.mp3DecoderListener = mp3DecoderListener;
        Logger.d(TAG, "init...");
        try {
            mediaExtractor = new MediaExtractor();
            Uri uri = Uri.parse(String.format(Locale.getDefault(),
                    "android.resource://%s/%s", context.getPackageName(), raw));
            mediaExtractor.setDataSource(context, uri, null);

            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                MediaFormat format = mediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio")) {
                    long duration = format.getLong(MediaFormat.KEY_DURATION);
                    sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    int bitRate = format.getInteger(MediaFormat.KEY_BIT_RATE);
                    sampleBit = bitRate / sampleRate / channelCount * 8;
                    format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8 * 1024);
                    mediaExtractor.selectTrack(i);
                    mediaDecode = MediaCodec.createDecoderByType(mime);
                    mediaDecode.configure(format, null, null, 0);
                    Logger.i(TAG, "音频信息：类型：%s, 时长: %ss,采样率：%s Hz,声道数：%s,位宽：%s,码率：%s kbps",
                            mime, duration / 1000000L, sampleRate, channelCount, sampleBit, bitRate / 1000L);
                    mp3DecoderListener.onPrepare(sampleRate, 16, channelCount);
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
        start = true;
        mediaDecode.start();
        decodeInputBuffers = mediaDecode.getInputBuffers();
        decodeOutputBuffers = mediaDecode.getOutputBuffers();
        decodeBufferInfo = new MediaCodec.BufferInfo();
        Logger.i(TAG, "input buffers: %s", decodeInputBuffers.length);
        startAsync();
    }

    public void release() {
        mp3DecoderListener = null;
        decodeOver = false;
        start = false;
        if (mediaExtractor != null) {
            mediaExtractor.release();
            mediaExtractor = null;
        }
        if (mediaDecode != null) {
            mediaDecode.release();
            mediaDecode = null;
        }
        decodeBufferInfo = null;
    }

    /**
     * 开始解码 生成PCM数据块
     */
    private void startDecoder() {
        try {
            for (int i = 0; i < decodeInputBuffers.length - 1; i++) {
                if (mediaDecode == null) {
                    return;
                }
                int inputIndex = mediaDecode.dequeueInputBuffer(10000);
                if (inputIndex < 0) {
                    decodeOver = true;
                    Logger.d(TAG, "解码完成");
                    return;
                }
                ByteBuffer inputBuffer = decodeInputBuffers[inputIndex];
                inputBuffer.clear();
                if (mediaExtractor == null) {
                    return;
                }
                int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
                if (sampleSize >= 0) {
                    mediaDecode.queueInputBuffer(inputIndex, 0, sampleSize, 0, 0);
                    mediaExtractor.advance();
                    decodeSize += sampleSize;
                    Logger.v(TAG, "解码进度： %s/", decodeSize);
                } else {
                    decodeOver = true;
                    Logger.d(TAG, "解码完成");
                    putPcmChunk(null);
                }

                if (mediaDecode == null) {
                    return;
                }
                int outputIndex = mediaDecode.dequeueOutputBuffer(decodeBufferInfo, 10000);
                ByteBuffer outputBuffer;
                byte[] chunkPCM;
                while (outputIndex >= 0) {
                    outputBuffer = decodeOutputBuffers[outputIndex];
                    chunkPCM = new byte[decodeBufferInfo.size];
                    outputBuffer.get(chunkPCM);
                    outputBuffer.clear();
                    putPcmChunk(chunkPCM);
                    if (mediaDecode == null) {
                        return;
                    }
                    mediaDecode.releaseOutputBuffer(outputIndex, false);
                    if (mediaDecode == null) {
                        return;
                    }
                    outputIndex = mediaDecode.dequeueOutputBuffer(decodeBufferInfo, 10000);
                }
            }
        } catch (Exception e) {
            Logger.w(e, TAG, "中断解码");
        }

    }

    /**
     * 解码线程
     */
    private class DecodeRunnable implements Runnable {

        @Override
        public void run() {
            while (!decodeOver && start) {
                startDecoder();
            }
        }
    }

    private void startAsync() {
        new Thread(new DecodeRunnable()).start();
    }

    private void putPcmChunk(byte[] pcmChunk) {
        if (mp3DecoderListener == null) {
            return;
        }
        if (pcmChunk == null) {
            mp3DecoderListener.onFinish();
            return;
        }
        mp3DecoderListener.onDecodeData(pcmChunk);
    }

    public interface Mp3DecoderListener {

        void onPrepare(int sampleRate, int sampleBit, int channelCount);

        void onDecodeData(byte pcmChunk[]);

        void onFinish();
    }

}
