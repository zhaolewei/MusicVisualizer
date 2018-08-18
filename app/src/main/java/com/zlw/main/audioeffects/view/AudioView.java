package com.zlw.main.audioeffects.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

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
    private static final int LUMP_COLOR = Color.parseColor("#FFBBFF");

    private static final float scale = 2;


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

    public void setWaveData(byte[] data) {
        this.waveData = data;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
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
                    (LUMP_MAX_HEIGHT - LUMP_MIN_HEIGHT - value * scale),
                    (LUMP_WIDTH + LUMP_SPACE) * i + LUMP_WIDTH,
                    LUMP_MAX_HEIGHT,
                    lumpPaint);

        }
    }

}

