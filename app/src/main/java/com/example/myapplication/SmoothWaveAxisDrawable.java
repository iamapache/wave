package com.example.myapplication;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.util.Range;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.IntRange;

/**
 * 项目：  My Application
 * 类名：  SmoothWaveAxisDrawable.java
 * 时间：  2022/9/19 16:33
 * 描述：
 */
public class SmoothWaveAxisDrawable extends Drawable {
    private final Paint paint = new Paint();
    private final RectF boundRectF = new RectF();
    private final Rect axisArea = new Rect();
    private final DashPathEffect dashEffect = new DashPathEffect(new float[]{10, 10}, 0);
    private final int textColor = Color.GRAY;

    private final List<Float> dataPositionX = new ArrayList<>();
    private final List<Float> dataPositionY = new ArrayList<>();
    /**
     * 画面上要展示的X坐标轴范围(基于原始数据)
     */
    private Range<Float> axisXRange;
    /**
     * 画面上要展示的Y坐标轴范围(基于原始数据)
     */
    private Range<Float> axisYRange;

    private List<Pair<String, Float>> originDataX;
    private List<Pair<String, Float>> originDataY;

    @IntRange(from = -1)
    private int focusIndex = -1;

    public SmoothWaveAxisDrawable() {
        paint.setAntiAlias(true);
        paint.setStrokeWidth(2);
    }

    @Override
    public void draw(Canvas canvas) {
        if (originDataX == null || originDataX.isEmpty()) {
            return;
        }
        if (originDataY == null || originDataY.isEmpty()) {
            return;
        }
        if (axisXRange == null || axisYRange == null) {
            return;
        }
        boundRectF.set(getBounds());
        int layoutId = canvas.saveLayer(boundRectF, paint, Canvas.ALL_SAVE_FLAG);
        canvas.translate(axisArea.left, axisArea.top);

        convertData();
        drawAxisXLine(canvas);
        drawAxisXText(canvas);
        drawAssistLines(canvas);
        drawAxisYText(canvas);

        canvas.restoreToCount(layoutId);
    }

    private void convertData() {
        dataPositionX.clear();
        dataPositionY.clear();
        if (originDataX != null && !originDataX.isEmpty() && axisXRange != null) {
            float scaleX = axisArea.width() / (axisXRange.getUpper() - axisXRange.getLower());

            for (Pair<String, Float> item : originDataX) {
                float originData = item.second;
                float pixelData = (originData - axisXRange.getLower()) * scaleX;
                dataPositionX.add(pixelData);
            }
        }
        if (originDataY != null && !originDataY.isEmpty() && axisYRange != null) {
            float scaleY = axisArea.height() / (axisYRange.getUpper() - axisYRange.getLower());

            for (Pair<String, Float> item : originDataY) {
                float originData = item.second;
                float pixelData = (originData - axisYRange.getLower()) * scaleY;
                pixelData = axisArea.height() - pixelData;
                dataPositionY.add(pixelData);
            }
        }
    }

    private void drawAxisXLine(Canvas canvas) { // 画X轴线
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(textColor);
        int offsetX = getBounds().left - axisArea.left;
        canvas.drawLine(offsetX, axisArea.bottom, getBounds().width(), axisArea.bottom, paint);
    }

    private void drawAxisXText(Canvas canvas) { // 画X轴文字
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(textColor);
        float textSize = getBounds().width() * 0.03f;
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);
        int paddingBottom = getBounds().bottom - axisArea.bottom;
        float offsetY = Math.min(textSize * 1.3f, (paddingBottom - textSize) / 2 + textSize);
        for (int i = 0; i < dataPositionX.size(); i++) {
            Float pixelX = dataPositionX.get(i);
            if (pixelX < 0 || pixelX > axisArea.width()) {
                continue;
            }
            canvas.drawText(originDataX.get(i).first, pixelX, axisArea.height() + offsetY, paint);
        }
    }

    private void drawAxisYText(Canvas canvas) { // 画Y轴文字
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(textColor);
        float textSize = getBounds().width() * 0.03f;
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.LEFT);
        int offsetX = getBounds().left - axisArea.left;
        for (int i = 0; i < dataPositionY.size(); i++) {
            Float pixelY = dataPositionY.get(i);
            if (pixelY < 0 || pixelY > axisArea.height()) {
                continue;
            }
            canvas.drawText(originDataY.get(i).first, offsetX, pixelY + textSize / 2f, paint);
        }
    }

    private void drawAssistLines(Canvas canvas) { // 画辅助线
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(textColor);
        for (int i = 0; i < dataPositionX.size(); i++) {
            Float pixelX = dataPositionX.get(i);
            if (pixelX < 0 || pixelX > axisArea.width()) {
                continue;
            }
            if (focusIndex == i) {
                paint.setPathEffect(null);
            } else {
                paint.setPathEffect(dashEffect);
            }
            canvas.drawLine(pixelX, 0, pixelX, axisArea.height(), paint);
        }
        paint.setPathEffect(null);
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public void setFocusIndex(@IntRange(from = -1) int focusIndex) {
        this.focusIndex = focusIndex;
    }

    public void setAxisArea(Rect axisArea) {
        this.axisArea.set(axisArea);
    }

    public void setOriginDataX(List<Pair<String, Float>> originData) {
        this.originDataX = originData;
        invalidateSelf();
    }

    public void setOriginDataY(List<Pair<String, Float>> originData) {
        this.originDataY = originData;
        invalidateSelf();
    }

    public void setAxisXRange(Range<Float> axisXRange) {
        this.axisXRange = axisXRange;
        invalidateSelf();
    }

    public void setAxisYRange(Range<Float> axisYRange) {
        this.axisYRange = axisYRange;
        invalidateSelf();
    }

    public Range<Float> getAxisXRange() {
        return axisXRange;
    }

    public Range<Float> getAxisYRange() {
        return axisYRange;
    }
}