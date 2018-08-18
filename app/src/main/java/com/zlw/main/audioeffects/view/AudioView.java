package com.zlw.main.audioeffects.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.zlw.main.audioeffects.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;

/**
 * @author zhaolewei on 2018/8/17.
 */
public class AudioView extends View {

    /**
     * 频谱数量
     */
    private static final int LUMP_COUNT = 128;
    private static final int LUMP_WIDTH = 6;
    private static final int LUMP_MIN_HEIGHT = LUMP_WIDTH;
    private static final int LUMP_MAX_HEIGHT = 200;
    private static final int LUMP_SPACE = 2;
    private static final int LUMP_COLOR = Color.parseColor("#FFBBFF");


    private byte[] waveData;

    private Paint lumpPaint;

    public AudioView(Context context) {
        super(context);
        init();
    }

    public AudioView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AudioView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        lumpPaint = new Paint();
        lumpPaint.setAntiAlias(true);
        lumpPaint.setColor(LUMP_COLOR);
    }

    Map<Integer, Byte> maxValueMap;

    public void setWaveData(byte[] data) {
        maxValueMap = new HashMap<>();
        List<Byte> list = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            list.add((byte) (128 - Math.abs(data[i])));
        }
        byte max = 0;
        int maxIndex = 0;
        byte last = 0;

        for (int i = 0; i < 32; i++) {
            Byte value = list.get(i);
            if (value > max) {
                max = value;
                maxIndex = i;
            }
        }
        byte value1 = (byte) (max * 1.5);
        if (value1 < 0) {
            value1 = 127;
        }

        byte maxValue = value1;
        byte maxValue11 = (byte) (maxValue * Math.random());
        byte maxValue12 = (byte) (maxValue * Math.random());
        byte maxValue21 = (byte) (maxValue11 * Math.random());
        byte maxValue22 = (byte) (maxValue12 * Math.random());

        if (maxIndex - 2 >= 0) {
            maxValueMap.put(maxIndex - 2, maxValue21);
        }
        if (maxIndex - 1 >= 0) {
            maxValueMap.put(maxIndex - 1, maxValue11);
        }
        maxValueMap.put(maxIndex, maxValue);
        maxValueMap.put(maxIndex + 1, maxValue12);
        maxValueMap.put(maxIndex + 2, maxValue22);


        max = 0;
        int laseIndex = 0;
        last = 0;
        for (int i = 32; i < list.size(); i++) {
            Byte value = list.get(i);
            if (value > max) {
                max = value;
                maxIndex = i;
            } else {
                //开始记录制高点
                if (value < last) {
                    if (maxIndex - laseIndex > 8) {
                        maxValueMap.put(maxIndex, (byte) (maxValue/6));
                        laseIndex = maxIndex;
                    }
                    max = 0;
                }
            }
            last = value;
        }

        waveData = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            waveData[i] = list.get(i);
        }

        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < LUMP_COUNT; i++) {

            if (waveData == null) {
                canvas.drawRect((LUMP_WIDTH + LUMP_SPACE) * i, LUMP_MAX_HEIGHT - LUMP_MIN_HEIGHT, (LUMP_WIDTH + LUMP_SPACE) * i + LUMP_WIDTH, LUMP_MAX_HEIGHT, lumpPaint);
                continue;
            }

            Byte value = maxValueMap.get(i);
            if (value == null) {
                canvas.drawRect((LUMP_WIDTH + LUMP_SPACE) * i, LUMP_MAX_HEIGHT - LUMP_MIN_HEIGHT, (LUMP_WIDTH + LUMP_SPACE) * i + LUMP_WIDTH, LUMP_MAX_HEIGHT, lumpPaint);
                continue;
            }


            canvas.drawRect((LUMP_WIDTH + LUMP_SPACE) * i, (float) (LUMP_MAX_HEIGHT - LUMP_MIN_HEIGHT - value * 1.5), (LUMP_WIDTH + LUMP_SPACE) * i + LUMP_WIDTH, LUMP_MAX_HEIGHT, lumpPaint);

        }
    }

}

