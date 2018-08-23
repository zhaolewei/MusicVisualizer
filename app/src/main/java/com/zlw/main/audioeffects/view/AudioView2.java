package com.zlw.main.audioeffects.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.zlw.main.audioeffects.utils.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * @author zhaolewei on 2018/8/17.
 */
public class AudioView2 extends View {

    /**
     * 频谱数量
     */
    private static int LUMP_COUNT = 128 * 4;
    private static final int LUMP_WIDTH = 1;
    private static final int LUMP_MIN_HEIGHT = LUMP_WIDTH;
    private static final int LUMP_MAX_HEIGHT = 300;//TODO: HEIGHT
    private static final int LUMP_SPACE = 0;
    private static final int LUMP_SIZE = LUMP_WIDTH + LUMP_SPACE;

    private static final int LUMP_COLOR = Color.parseColor("#cccccc");

    private static final float SCALE = 1;

    private byte[] waveData;
    List<Point> pointList;

    private Paint lumpPaint;
    Path wavePath = new Path();


    public AudioView2(Context context) {
        super(context);
        init();
    }

    public AudioView2(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AudioView2(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        lumpPaint = new Paint();
//        lumpPaint.setAntiAlias(true);
        lumpPaint.setColor(LUMP_COLOR);

        lumpPaint.setStrokeWidth(1);
        lumpPaint.setStyle(Paint.Style.STROKE);
    }

    public void setWaveData(byte[] data) {
        Logger.d("TAG", "setWaveData");
        Logger.d("TAG", Arrays.toString(data));
        this.waveData = data;
        LUMP_COUNT = data.length;
        postInvalidate();
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
            height = 800;
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        wavePath.reset();
        canvas.drawColor(Color.WHITE);

        for (int i = 0; i < LUMP_COUNT; i++) {
            if (waveData == null) {
                canvas.drawRect((LUMP_WIDTH + LUMP_SPACE) * i,
                        LUMP_MAX_HEIGHT - LUMP_MIN_HEIGHT,
                        (LUMP_WIDTH + LUMP_SPACE) * i + LUMP_WIDTH,
                        LUMP_MAX_HEIGHT,
                        lumpPaint);
                continue;
            }

            int value = waveData[i];
            canvas.drawRect((LUMP_WIDTH + LUMP_SPACE) * i,
                    (LUMP_MAX_HEIGHT - LUMP_MIN_HEIGHT - value * SCALE),
                    (LUMP_WIDTH + LUMP_SPACE) * i + LUMP_WIDTH,
                    LUMP_MAX_HEIGHT,
                    lumpPaint);
        }

    }

}

