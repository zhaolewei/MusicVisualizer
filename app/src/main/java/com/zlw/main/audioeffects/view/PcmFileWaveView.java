package com.zlw.main.audioeffects.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.zlw.main.audioeffects.utils.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import static android.content.ContentValues.TAG;

/**
 * @author zhaolewei on 2018/8/17.
 */
public class PcmFileWaveView extends View {

    private int lumpCount;
    private static final int LUMP_WIDTH = 2;
    private static final int LUMP_SPACE = 1;
    private static final int LUMP_MIN_HEIGHT = 2;
    /**
     * Lump的最大高度，也是基准线的Y值，height = LUMP_MAX_HEIGHT*2
     */
    private static final int LUMP_MAX_HEIGHT = 300;
    private static final int MAX_HEIGHT = LUMP_MAX_HEIGHT * 2;
    private static final int LUMP_SIZE = LUMP_WIDTH + LUMP_SPACE;
    private static final int LUMP_COLOR = Color.parseColor("#cccccc");
    private static final float SCALE = LUMP_MAX_HEIGHT / 200;
    /**
     * 数据计算的吞吐量
     * 数据越小，采样值越多，越精确
     */
    public static final int DEFAULT_FFT_THRUPUT = 16 * 64 * 8;

    private byte[] waveData;
    private Paint lumpPaint;
    private Paint linePaint;
    private int lineOffsetX = 10;

    private PcmFileWaveConverter pcmFileWaveConverter;

    public PcmFileWaveView(Context context) {
        super(context);
        init();
    }

    public PcmFileWaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PcmFileWaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        /**
         * 数转换策略： 将数据拆分成N段，每段数据大小为DEFAULT_FFT_THRUPUT，对每段数据进行FFT转换，提取特征频谱的相对振幅，再用于数据的可视化
         */
        pcmFileWaveConverter = new PcmFileWaveConverter(DEFAULT_FFT_THRUPUT);
        lumpPaint = new Paint();
        lumpPaint.setAntiAlias(true);
        lumpPaint.setColor(LUMP_COLOR);
        lumpPaint.setStrokeWidth(1);
        lumpPaint.setStyle(Paint.Style.STROKE);

        linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setColor(Color.RED);
        linePaint.setStrokeWidth(1);
    }

    /**
     * @param data pcm数据
     */
    public void setWaveData(byte[] data) {
        this.waveData = pcmFileWaveConverter.readyDataByte(data);
        lumpCount = waveData.length;
        postInvalidate();
    }

    public void showPcmFileWave(File file) {
        byte[] buffer;
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] b = new byte[1024 * 4];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }

            buffer = bos.toByteArray();
            setWaveData(buffer);
        } catch (Exception e) {
            Logger.e(e, TAG, e.getMessage());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 0;
        int height = 0;
        int modeW = MeasureSpec.getMode(widthMeasureSpec);
        if (modeW == MeasureSpec.AT_MOST) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }
        if (modeW == MeasureSpec.EXACTLY) {
            width = widthMeasureSpec;
        }
        if (modeW == MeasureSpec.UNSPECIFIED) {
            width = 96000;
        }
        int modeH = MeasureSpec.getMode(height);
        if (modeH == MeasureSpec.AT_MOST) {
            height = MeasureSpec.getSize(heightMeasureSpec);
        }
        if (modeH == MeasureSpec.EXACTLY) {
            height = heightMeasureSpec;
        }
        if (modeH == MeasureSpec.UNSPECIFIED) {
            height = MAX_HEIGHT;
        }
        setMeasuredDimension(width, height);
    }


    public void setProgress(float progress) {
        int width = lumpCount * LUMP_SIZE;
        lineOffsetX = (int) (width * progress);
        postInvalidate();
    }

    public void setProgress(long size) {
        int width = lumpCount * LUMP_SIZE;
        if (waveData == null || waveData.length == 0) {
            return;
        }
        Logger.v(TAG, "size : %s", size);
        long len = (waveData.length * DEFAULT_FFT_THRUPUT * 2);
        lineOffsetX = (int) (width * size / len);
        postInvalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);

        canvas.drawLine(lineOffsetX, 0, lineOffsetX, MAX_HEIGHT, linePaint);
        for (int i = 0; i < lumpCount; i++) {
            if (waveData == null) {
                canvas.drawRect(LUMP_SIZE * i,
                        LUMP_MAX_HEIGHT - LUMP_MIN_HEIGHT,
                        LUMP_SIZE * i + LUMP_WIDTH,
                        LUMP_MAX_HEIGHT,
                        lumpPaint);
                continue;
            }

            int value = waveData[i];
            canvas.drawRect(LUMP_SIZE * i,
                    (LUMP_MAX_HEIGHT - LUMP_MIN_HEIGHT - value * SCALE),
                    LUMP_SIZE * i + LUMP_WIDTH,
                    LUMP_MAX_HEIGHT + LUMP_MIN_HEIGHT + value * SCALE,
                    lumpPaint);
        }
    }

}

