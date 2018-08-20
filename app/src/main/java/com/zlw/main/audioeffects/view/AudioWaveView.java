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
public class AudioWaveView extends View {

    /**
     * 频谱数量
     */
    private static final int LUMP_COUNT = 128;
    private static final int LUMP_WIDTH = 6;
    private static final int LUMP_MIN_HEIGHT = LUMP_WIDTH;
    private static final int LUMP_MAX_HEIGHT = 200;//TODO: HEIGHT
    private static final int LUMP_SPACE = 2;
    private static final int LUMP_COLOR = Color.parseColor("#FFBBFF");

    private static final float scale = 2;
    private static final String TAG = AudioWaveView.class.getSimpleName();


    private byte[] waveData;
    List<Point> pointList;

    private Paint lumpPaint;

    public AudioWaveView(Context context) {
        super(context);
        init();
    }

    public AudioWaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AudioWaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        lumpPaint = new Paint();
        lumpPaint.setAntiAlias(true);
        lumpPaint.setStrokeWidth(6);
        lumpPaint.setStyle(Paint.Style.STROKE);
        lumpPaint.setColor(LUMP_COLOR);
    }

    public void setWaveData(byte[] data) {
        this.waveData = data;
        if (pointList == null) {
            pointList = new ArrayList<>();
        } else {
            pointList.clear();
        }
        pointList.add(new Point(0, 0));
        int rate = 1;
        for (int i = 0; i < data.length; i += rate) {
            pointList.add(new Point(8 * (i + 1), data[i]));
        }
//        pointList.add(new Point(10 * 6, 60));
//        pointList.add(new Point(20 * 6, 20));
//        pointList.add(new Point(30 * 6, 50));
//        pointList.add(new Point(40 * 6, 80));
//        pointList.add(new Point(50 * 6, 90));
//        pointList.add(new Point(60 * 6, 30));
//        pointList.add(new Point(70 * 6, 60));
//        pointList.add(new Point(80 * 6, 20));
//        pointList.add(new Point(90 * 6, 0));
        postInvalidate();
    }

    Path wavePath = new Path();

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (pointList == null) {
            Logger.e(TAG, "pointList == null");
            return;
        }
        wavePath.reset();
        for (int i = 0; i < pointList.size() - 1; i++) {
            Point point = pointList.get(i);
            Point nextPoint = pointList.get(i + 1);
            int midX = (point.x + nextPoint.x) / 2;
            if (i == 0) {
                wavePath.moveTo(point.x, point.y);
            }
            wavePath.cubicTo(midX, point.y, midX, nextPoint.y, nextPoint.x, nextPoint.y);

        }
        canvas.drawPath(wavePath, lumpPaint);
    }

}

