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

import java.util.ArrayList;
import java.util.List;

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
    private static final int LUMP_MAX_HEIGHT = 200;//TODO: HEIGHT
    private static final int LUMP_SPACE = 2;
    private static final int LUMP_SIZE = LUMP_WIDTH + LUMP_SPACE;

    private static final int LUMP_COLOR = Color.parseColor("#FFBBFF");

    private static final float SCALE = LUMP_MAX_HEIGHT / LUMP_COUNT;


    private byte[] waveData;
    List<Point> pointList;

    private Paint lumpPaint;
    Path wavePath = new Path();


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

        lumpPaint.setStrokeWidth(2);
        lumpPaint.setStyle(Paint.Style.STROKE);
    }

    public void setWaveData(byte[] data) {
        Logger.d("TAG", "setWaveData");
        this.waveData = data;

        if (pointList == null) {
            pointList = new ArrayList<>();
        } else {
            pointList.clear();
        }
        pointList.add(new Point(0, 0));
        int rate = 2;
        for (int i = 0; i < LUMP_COUNT; i += rate) {
            pointList.add(new Point(LUMP_SIZE * (i + 1), waveData[i]));
        }
        pointList.add(new Point(LUMP_SIZE * LUMP_COUNT, 0));
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (pointList == null) {
            return;
        }
        wavePath.reset();

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


            if (i + 1 < pointList.size()) {
                Point point = pointList.get(i);
                Point nextPoint = pointList.get(i + 1);
                int midX = (point.x + nextPoint.x) / 2;
                if (i == 0) {
                    wavePath.moveTo(point.x, point.y * SCALE + LUMP_MAX_HEIGHT);
                }
                wavePath.cubicTo(midX, point.y * SCALE + LUMP_MAX_HEIGHT, midX, nextPoint.y * SCALE + LUMP_MAX_HEIGHT, nextPoint.x, nextPoint.y * SCALE + LUMP_MAX_HEIGHT);

                canvas.drawPath(wavePath, lumpPaint);
            }

        }

    }

}

