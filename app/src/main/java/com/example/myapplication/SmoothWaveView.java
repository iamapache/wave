package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.Range;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.RequiresApi;

/**
 * 项目：  My Application
 * 类名：  SmoothWaveView.java
 * 时间：  2022/9/19 16:35
 * 描述：
 */
public class SmoothWaveView extends View {
    /**
     * 坐标区域上下左右边距占View宽或者高的百分比
     */
    private final RectF axisAreaPaddingPercent = new RectF(0.1f, 0f, 0.1f, 0.12f);
    /**
     * 坐标区域(像素单位)
     */
    private final Rect axisArea = new Rect();
    private SmoothWaveAxisDrawable axisDrawable;
    private List<SmoothWaveDrawable> waveDrawables;

    public SmoothWaveView(Context context) {
        super(context);
        init();
    }

    public SmoothWaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SmoothWaveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SmoothWaveView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    /**
     * 释放资源.防止内存泄漏.界面销毁的时候调用.
     */
    public void release() {
        if (axisDrawable != null) {
            axisDrawable.setCallback(null);
            axisDrawable = null;
        }

        if (waveDrawables != null) {
            for (Drawable drawable : waveDrawables) {
                if (drawable != null) {
                    drawable.setCallback(null);
                }
            }
            waveDrawables.clear();
            waveDrawables = null;
        }
    }

    private void init() {
        /****************测试数据******************/
        axisDrawable = new SmoothWaveAxisDrawable();
        axisDrawable.setAxisXRange(new Range<>(0f, 4f));
        axisDrawable.setAxisYRange(new Range<>(0f, 22f));
        List<Pair<String, Float>> axisXData = new ArrayList<>();
        axisXData.add(new Pair<String, Float>("4/1-4/7", 0f));
        axisXData.add(new Pair<String, Float>("4/8-4/14", 1f));
        axisXData.add(new Pair<String, Float>("4/15-4/21", 2f));
        axisXData.add(new Pair<String, Float>("4/22-4/28", 3f));
        axisXData.add(new Pair<String, Float>("4/29-5/4", 4f));
        axisXData.add(new Pair<String, Float>("5/11-5/14", 5f));
        axisXData.add(new Pair<String, Float>("5/19-5/24", 6f));
        axisDrawable.setOriginDataX(axisXData);
        List<Pair<String, Float>> axisYData = new ArrayList<>();
        axisYData.add(new Pair<String, Float>("1", 5f));
        axisYData.add(new Pair<String, Float>("10", 10f));
        axisYData.add(new Pair<String, Float>("15", 15f));
        axisYData.add(new Pair<String, Float>("20", 20f));
        axisYData.add(new Pair<String, Float>("25", 25f));
        axisYData.add(new Pair<String, Float>("26", 26f));
        axisYData.add(new Pair<String, Float>("35", 35f));
        axisDrawable.setOriginDataY(axisYData);
        axisDrawable.setFocusIndex(2);
        axisDrawable.setCallback(this);

        waveDrawables = new ArrayList<>();

        SmoothWaveDrawable waveDrawable1 = new SmoothWaveDrawable();
        List<PointF> data = new ArrayList<>();
        data.add(new PointF(0f, 0f));
        data.add(new PointF(1f, 4f));
        data.add(new PointF(2f, 9f));
        data.add(new PointF(3f, 0f));
        data.add(new PointF(4f, 13f));
        data.add(new PointF(5f, 13f));
        data.add(new PointF(6f, 23f));
        waveDrawable1.setOriginData(data);
        waveDrawable1.setAxisXRange(new Range<>(0f, 4f));
        waveDrawable1.setAxisYRange(new Range<>(0f, 22f));
        waveDrawable1.setFocusIndex(2);
        waveDrawable1.setLineColor(Color.RED);
        waveDrawable1.setWaveColorStart(Color.RED);
        waveDrawables.add(waveDrawable1);
        waveDrawable1.setCallback(this);

        SmoothWaveDrawable waveDrawable2 = new SmoothWaveDrawable();
        List<PointF> data2 = new ArrayList<>();
        data2.add(new PointF(0f, 0f));
        data2.add(new PointF(1f, 12f));
        data2.add(new PointF(2f, 2f));
        data2.add(new PointF(3f, 0f));
        data2.add(new PointF(4f, 10f));
        data2.add(new PointF(5f, 15f));
        data2.add(new PointF(6f, 20f));
        waveDrawable2.setOriginData(data2);
        waveDrawable2.setAxisXRange(new Range<>(0f, 4f));
        waveDrawable2.setAxisYRange(new Range<>(0f, 22f));
        waveDrawable2.setFocusIndex(2);
        waveDrawable2.setLineColor(Color.BLUE);
        waveDrawable2.setWaveColorStart(Color.BLUE);
        waveDrawables.add(waveDrawable2);
        /****************测试数据******************/
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        super.invalidateDrawable(drawable);
        invalidate();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        initAxisArea();
        if (axisDrawable != null) {
            axisDrawable.setBounds(0, 0, getWidth(), getHeight());
            axisDrawable.setAxisArea(axisArea);
            axisDrawable.draw(canvas);
        }
        if (waveDrawables != null && !waveDrawables.isEmpty()) {
            for (SmoothWaveDrawable drawable : waveDrawables) {
                drawable.setBounds(axisArea);
                drawable.draw(canvas);
            }
        }
    }

    private void initAxisArea() {
        int width = getWidth();
        int height = getHeight();
        axisArea.left = (int) (width * axisAreaPaddingPercent.left + 0.5f);
        axisArea.top = (int) (height * axisAreaPaddingPercent.top + 0.5f);
        axisArea.right = (int) (width * (1 - axisAreaPaddingPercent.right) + 0.5f);
        axisArea.bottom = (int) (height * (1 - axisAreaPaddingPercent.bottom) + 0.5f);
    }
}