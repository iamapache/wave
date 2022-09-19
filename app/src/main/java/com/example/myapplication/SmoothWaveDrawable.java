package com.example.myapplication;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Pair;
import android.util.Range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.RequiresApi;

/**
 * 项目：  My Application
 * 类名：  SmoothWaveDrawable.java
 * 时间：  2022/9/19 16:34
 * 描述：
 */
public class SmoothWaveDrawable extends Drawable {
    public static final int MASK_HEX_2 = 0xff;
    public static final int MASK_HEX_6 = 0xffffff;
    public static final int BIN_LENGTH_24 = 24;
    private static final float SLOP_RATE = 0.4f;
    private final Paint paint = new Paint();
    private final Path path = new Path();
    private final Path wavePath = new Path();
    private final RectF boundRectF = new RectF();
    /**
     * 要展示的数据(需要基于原始数据经过转换,基于当前drawable上的像素点坐标系)
     */
    private List<PointF> showData;
    private ArrayMap<PointF, Pair<PointF, PointF>> assistPointData;
    /**
     * 外部输入的原始数据
     */
    private List<PointF> originData;
    /**
     * 画面上要展示的X坐标轴范围(基于原始数据)
     */
    private Range<Float> axisXRange;
    /**
     * 画面上要展示的Y坐标轴范围(基于原始数据)
     */
    private Range<Float> axisYRange;
    private float minDataY = 0;
    private float maxDataY = 600;

    /**
     * 波形渐变色的各节点透明度
     */
    @FloatRange(from = 0f, to = 1.0f)
    private final float[] waveAlphas = new float[]{0.10f, 0.016f};

    /**
     * 波形渐变色的各节点所在位置百分比
     */
    private final float[] waveColorPositions = new float[]{0, 1f};

    /**
     * 波形渐变色的各节点色值(需要通过waveColorStart和waveAlphas计算出来)
     */
    @ColorInt
    private final int[] waveColors = new int[waveAlphas.length];

    /**
     * 焦点圆点前景色透明度(圆点小圈)
     */
    @FloatRange(from = 0f, to = 1.0f)
    private final float focusDotAlpha = 1.0f;

    /**
     * 焦点圆点背景色透明度(圆点大圈)
     */
    @FloatRange(from = 0f, to = 1.0f)
    private final float focusDotBgAlpha = 0.3f;

    /**
     * 是否画出辅助点(仅调试用)
     */
    private final boolean isShowAssistDot = false;

    /**
     * 调试点颜色(仅调试用)
     */
    @ColorInt
    private final int assistColor = Color.GREEN;

    @IntRange(from = -1)
    private int focusIndex = -1;

    /**
     * 波线条宽度
     */
    private int lineStrokeWidth = 6;

    /**
     * 波线条颜色
     */
    @ColorInt
    private int lineColor = Color.RED;

    /**
     * 波形起始色
     */
    @ColorInt
    private int waveColorStart = Color.RED;

    public SmoothWaveDrawable() {
        paint.setAntiAlias(true);
        setWaveColorStart(waveColorStart);
        setLineColor(lineColor);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void convertData() {
        if (originData == null || originData.isEmpty() || axisXRange == null || axisYRange == null) {
            showData = Collections.emptyList();
            return;
        }
        int width = getBounds().width();
        int height = getBounds().height() - lineStrokeWidth;
        float scaleX = width / (axisXRange.getUpper() - axisXRange.getLower()); // 原始数据X轴每1个单位占几个像素点
        float scaleY = height / (axisYRange.getUpper() - axisYRange.getLower()); // 原始数据Y轴每1个单位占几个像素点
        showData = new ArrayList<>();
        for (PointF pointF : originData) {
            float pixelX = (pointF.x - axisXRange.getLower()) * scaleX;
            float pixelY = (pointF.y - axisYRange.getLower()) * scaleY;
            pixelY = height - pixelY + lineStrokeWidth / 2f;
            showData.add(new PointF(pixelX, pixelY));
        }
        assistPointData = calAssistPoints(showData);
        if (showData != null && !showData.isEmpty()) {
            minDataY = showData.stream().map(it -> it.y).min(Float::compare).orElse(0f);
            maxDataY = showData.stream().map(it -> it.y).max(Float::compare).orElse(maxDataY);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void draw(Canvas canvas) {
        convertData();
        if (showData == null || showData.isEmpty()) {
            return;
        }
        if (assistPointData == null || assistPointData.isEmpty()) {
            return;
        }
        boundRectF.set(getBounds());
        int layoutId = canvas.saveLayer(boundRectF, paint, Canvas.ALL_SAVE_FLAG);
        canvas.translate(getBounds().left, getBounds().top);

        path.reset();
        path.moveTo(showData.get(0).x, showData.get(0).y);

        wavePath.reset();
        wavePath.moveTo(0, getBounds().height());
        wavePath.lineTo(0, showData.get(0).y);
        wavePath.lineTo(showData.get(0).x, showData.get(0).y);

        for (int i = 0; i < showData.size() - 1; i++) {
            PointF currP = showData.get(i);
            PointF nextP = showData.get(i + 1);
            PointF assistP1 = assistPointData.get(currP).second;
            PointF assistP2 = assistPointData.get(nextP).first;
            path.cubicTo(assistP1.x, assistP1.y, assistP2.x, assistP2.y, nextP.x, nextP.y);
            wavePath.cubicTo(assistP1.x, assistP1.y, assistP2.x, assistP2.y, nextP.x, nextP.y);

            if (isShowAssistDot) { // 画"三次贝塞尔曲线"的辅助点,调试找bug专用
                paint.setStyle(Paint.Style.FILL);
                paint.setStrokeWidth(lineStrokeWidth);
                paint.setColor(assistColor);
                canvas.drawCircle(currP.x, currP.y, lineStrokeWidth * 2, paint);
                canvas.drawCircle(assistP1.x, assistP1.y, lineStrokeWidth * 2, paint);
                canvas.drawCircle(assistP2.x, assistP2.y, lineStrokeWidth * 2, paint);
                canvas.drawLine(currP.x, currP.y, assistP1.x, assistP1.y, paint);
                canvas.drawLine(assistP2.x, assistP2.y, nextP.x, nextP.y, paint);
            }
        }

        wavePath.lineTo(showData.get(showData.size() - 1).x, getBounds().height());
        wavePath.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, minDataY, 0, getBounds().height() * 0.8f, waveColors,
                waveColorPositions, Shader.TileMode.CLAMP));
        canvas.drawPath(wavePath, paint); // 画出波形下方和X轴之间的填充色
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(lineStrokeWidth);
        paint.setColor(lineColor);
        canvas.drawPath(path, paint); // 画出波形线条

        if (focusIndex >= 0 && focusIndex < showData.size()) {
            int lineAlphaInt = (lineColor >> BIN_LENGTH_24) & MASK_HEX_2;
            int lineBaseColor = lineColor & MASK_HEX_6;
            int focusDotFgAlphaInt = (int) (lineAlphaInt * focusDotAlpha + 0.5f);
            int focusDotBgAlphaInt = (int) (lineAlphaInt * focusDotBgAlpha + 0.5f);
            int focusDotFgColor = ((focusDotFgAlphaInt & MASK_HEX_2) << BIN_LENGTH_24) + lineBaseColor;
            int focusDotBgColor = ((focusDotBgAlphaInt & MASK_HEX_2) << BIN_LENGTH_24) + lineBaseColor;

            PointF focusP = showData.get(focusIndex);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(focusDotBgColor);
            canvas.drawCircle(focusP.x, focusP.y, lineStrokeWidth * 4, paint);

            paint.setColor(focusDotFgColor);
            canvas.drawCircle(focusP.x, focusP.y, lineStrokeWidth * 2, paint);
        }

        canvas.restoreToCount(layoutId);
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

    public void setLineColor(int lineColor) {
        this.lineColor = lineColor;
        invalidateSelf();
    }

    public void setWaveColorStart(@ColorInt int waveColorStart) {
        this.waveColorStart = waveColorStart;
        int alphaInt = (waveColorStart >> BIN_LENGTH_24) & MASK_HEX_2;
        if (alphaInt == 0) {
            alphaInt = 255;
        }
        int baseColorInt = waveColorStart & MASK_HEX_6;
        for (int i = 0; i < waveColors.length; i++) {
            int alpha = (int) (alphaInt * waveAlphas[i] + 0.5f);
            waveColors[i] = ((alpha & MASK_HEX_2) << BIN_LENGTH_24) + baseColorInt;
        }
        invalidateSelf();
    }

    public void setFocusIndex(@IntRange(from = -1) int focusIndex) {
        this.focusIndex = focusIndex;
    }

    private static ArrayMap<PointF, Pair<PointF, PointF>> calAssistPoints(List<PointF> baseDots) {
        if (baseDots == null || baseDots.isEmpty()) {
            return new ArrayMap<>(0);
        }
        ArrayMap<PointF, Pair<PointF, PointF>> result = new ArrayMap<>();
        for (int i = 0; i < baseDots.size(); i++) {
            float slop = getAssistPointSlop(baseDots, i); // 当前基准点及其左右两个基准点3点一线的目标斜率
            PointF curr = baseDots.get(i); // 当前基准点
            PointF pre = i <= 0 ? null : baseDots.get(i - 1); // 前一个基准点
            PointF next = i >= baseDots.size() - 1 ? null : baseDots.get(i + 1); // 下一个基准点
            PointF leftAssistPoint = null; // 当前基准点左边的辅助点
            PointF rightAssistPoint = null; // 当前基准点右边的辅助点

            float leftAssistX = curr.x; // 当前基准点左边辅助点的X坐标
            float leftAssistY = curr.y; // 当前基准点左边辅助点的Y坐标
            if (pre != null) {
                leftAssistX = curr.x - (curr.x - pre.x) * SLOP_RATE;
                leftAssistY = curr.y - slop * (curr.x - leftAssistX);
            }
            leftAssistPoint = new PointF(leftAssistX, leftAssistY);

            float rightAssistX = curr.x; // 当前基准点右边辅助点的X坐标
            float rightAssistY = curr.y; // 当前基准点右边辅助点的Y坐标
            if (next != null) {
                rightAssistX = curr.x + (next.x - curr.x) * SLOP_RATE;
                rightAssistY = curr.y + slop * (rightAssistX - curr.x);
            }
            rightAssistPoint = new PointF(rightAssistX, rightAssistY);

            result.put(new PointF(curr.x, curr.y), new Pair<>(leftAssistPoint, rightAssistPoint));
        }
        return result;
    }

    /**
     * 获取一个基准点(数据点)左右两个辅助点的斜率
     *
     * @param baseDots 基准点列表
     * @param index 当前求斜率的点序号
     * @return 斜率,从左上到右下走向的线段斜率为正;从左下到右上走向的线段斜率为负.
     */
    private static float getAssistPointSlop(List<PointF> baseDots, int index) {
//        return 0;
        if (baseDots == null || baseDots.isEmpty()) {
            return 0;
        }
        if (index <= 0 || index >= baseDots.size() - 1) {
            return 0;
        }
        PointF prePoint = baseDots.get(index - 1);
        PointF nextPoint = baseDots.get(index + 1);
        PointF currPoint = baseDots.get(index);
        if ((prePoint.y <= currPoint.y && nextPoint.y <= currPoint.y)
                || (prePoint.y >= currPoint.y && nextPoint.y >= currPoint.y)) {
            return 0;
        }
        return (nextPoint.y - prePoint.y) / (nextPoint.x - prePoint.x);
    }

    /**
     * 设置数据
     *
     * @param points 数据点
     * @param axisXRange 要展示的X轴范围
     * @param axisYRange 要展示的Y轴范围
     */
    public void setData(List<PointF> points, Range<Float> axisXRange, Range<Float> axisYRange) {
        this.originData = points;
        this.axisXRange = axisXRange;
        this.axisYRange = axisYRange;
        invalidateSelf();
    }

    public void setOriginData(List<PointF> originData) {
        this.originData = originData;
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