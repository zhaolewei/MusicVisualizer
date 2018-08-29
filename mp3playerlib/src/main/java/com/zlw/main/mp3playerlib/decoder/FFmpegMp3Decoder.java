package com.zlw.main.mp3playerlib.decoder;

import android.content.Context;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.zlw.main.mp3playerlib.utils.Logger;

import java.io.File;
import java.io.IOException;

/**
 * @author zhaolewei on 2018/8/28.
 */
public class FFmpegMp3Decoder {
    private static final String TAG = FFmpegMp3Decoder.class.getSimpleName();

    private FFmpeg ffmpeg;
    private ResultCallback callback;

    public FFmpegMp3Decoder(Context context) {
        ffmpeg = FFmpeg.getInstance(context);
        loadFFMpegBinary();
    }

    private void loadFFMpegBinary() {
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    Logger.e(TAG, "loadFFMpegBinary 初始化失败");
                }
            });
        } catch (FFmpegNotSupportedException e) {
            Logger.e(TAG, "loadFFMpegBinary 初始化失败");
        }
    }

    public void mp3ToPcm(File srcFile, File resultFile, ResultCallback callback) {
        this.callback = callback;
        try {
            if (!resultFile.exists()) {
                resultFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        execFFmpegBinary(new String[]{"-y", "-i", srcFile.getPath(), "-f", "s16le", resultFile.getPath()});
    }

    public void execFFmpegBinary(final String[] cmd) {
        try {
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                    Logger.i(TAG, "onStart");
                }

                @Override
                public void onProgress(String message) {
                    Logger.i(TAG, "onProgress-message: %s", message);
                }

                @Override
                public void onFailure(String message) {
                    Logger.e(TAG, "onFailure-message: %s", message);
                    if (callback != null) {
                        callback.onFailure();
                    }
                }

                @Override
                public void onSuccess(String message) {
                    Logger.i(TAG, "onSuccess-message: %s", message);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                }

                @Override
                public void onFinish() {
                    Logger.i(TAG, "onFinish");
                }
            });
        } catch (Exception e) {
            Logger.e(e, TAG, "execFFmpegBinary 失败");
        }

    }

    public interface ResultCallback {

        void onSuccess();

        void onFailure();
    }
}
