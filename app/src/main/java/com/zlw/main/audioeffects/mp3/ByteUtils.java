package com.zlw.main.audioeffects.mp3;

import com.zlw.main.audioeffects.utils.Logger;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * @author zhaolewei on 2018/8/23.
 */
public class ByteUtils {
    private static final String TAG = ByteUtils.class.getSimpleName();

    /**
     * 将byte[] 追加到文件末尾
     */
    public static void byte2File(byte[] buf, File file) {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            long fileLength = file.length();
            randomAccessFile.seek(fileLength);
            randomAccessFile.write(buf);
        } catch (Exception e) {
            Logger.e(e, TAG, e.getMessage());
        }
    }

}
