package com.example.myapplication;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.util.Range;
import android.view.Gravity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SmoothCylindricalAxisDrawable extends Drawable {
    private final DashPathEffect dashEffect = new DashPathEffect(new float[]{10, 10}, 0);
    private final int colorLine = Color.GRAY;
    private final RectF visibleRect = new RectF();
    private final Paint paint = new Paint();

    private List<Pair<Float, Drawable>> iconData;
    private List<Pair<Float, String>> axisData;
    private Range<Float> axisRange;
    private float iconSize;
    private float iconRectSize;

    private float textSize = 20;

    /**
     * 当前进度条依附的方向
     *
     * @since 2022-06-09
     */
    @SmoothCylindricalDrawable.Align
    private int align = Gravity.BOTTOM;

    public SmoothCylindricalAxisDrawable() {
        paint.setAntiAlias(true);
        paint.setStrokeWidth(2);
        paint.setColor(colorLine);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void draw(Canvas canvas) {
        drawAxis(canvas);
        drawIcons(canvas);
    }

    private void drawAxis(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        switch (align) {
            case Gravity.LEFT: // 画图标轴(左方) canvas.drawLine(visibleRect.left, getBounds().top, visibleRect.left, getBounds().bottom, paint);

                // 画数字轴(下方)
                canvas.drawLine(visibleRect.left, visibleRect.bottom, visibleRect.right, visibleRect.bottom, paint);
                break;
            case Gravity.RIGHT:
                // 画图标轴(右方)
                canvas.drawLine(visibleRect.right, getBounds().top, visibleRect.right, getBounds().bottom, paint);

                // 画数字轴(下方)
                canvas.drawLine(visibleRect.left, visibleRect.bottom, visibleRect.right, visibleRect.bottom, paint);
                break;
            case Gravity.TOP:
                // 画图标轴(上方)
                canvas.drawLine(getBounds().left, visibleRect.top, getBounds().right, visibleRect.top, paint);

                // 画数字轴(左方)
                canvas.drawLine(visibleRect.left, visibleRect.top, visibleRect.left, visibleRect.bottom, paint);
                break;
            case Gravity.BOTTOM:
            default:
                // 画图标轴(下方)
                canvas.drawLine(getBounds().left, visibleRect.bottom, getBounds().right, visibleRect.bottom, paint);

                // 画数字轴(左方)
                canvas.drawLine(visibleRect.left, visibleRect.top, visibleRect.left, visibleRect.bottom, paint);
                break;
        }
        if (axisData == null || axisData.isEmpty() || axisRange == null) {
            return;
        }
        paint.setTextSize(textSize);
        for (Pair<Float, String> textInfo : axisData) {
            float scale;
            switch (align) {
                case Gravity.LEFT:
                case Gravity.RIGHT:
                    scale = visibleRect.width() / (axisRange.getUpper() - axisRange.getLower());
                    float offsetX = (textInfo.first - axisRange.getLower()) * scale;
                    float x;
                    if (align == Gravity.LEFT) {
                        x = visibleRect.left + offsetX;
                    } else {
                        x = visibleRect.right - offsetX;
                    }

                    paint.setPathEffect(dashEffect);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawLine(x, visibleRect.top, x, visibleRect.bottom, paint); // 画象限内的辅助虚线

                    paint.setPathEffect(null);
                    float baseLineY =
                            SmoothCylindricalDrawable.getTextY(paint, visibleRect.bottom, getBounds().bottom);
                    if (Math.abs(x - visibleRect.left) < 1f && align == Gravity.LEFT) { // 坐标轴上的数字,不做居中
                        paint.setTextAlign(Paint.Align.LEFT);
                        x += textSize * 0.5f;
                    } else if (Math.abs(x - visibleRect.right) < 1f && align == Gravity.RIGHT) { // 坐标轴上的数字,不做居中
                        paint.setTextAlign(Paint.Align.RIGHT);
                        x -= textSize * 0.5f;
                    } else {
                        paint.setTextAlign(Paint.Align.CENTER);
                    }
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawText(textInfo.second, x, baseLineY, paint); // 画坐标轴上的文字
                    break;
                case Gravity.TOP:
                case Gravity.BOTTOM:
                default:
                    scale = visibleRect.height() / (axisRange.getUpper() - axisRange.getLower());
                    float offsetY = (textInfo.first - axisRange.getLower()) * scale;
                    float y;
                    if (align == Gravity.BOTTOM) {
                        y = visibleRect.bottom - offsetY;
                    } else {
                        y = visibleRect.top + offsetY;
                    }

                    paint.setPathEffect(dashEffect);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawLine(visibleRect.left, y, visibleRect.right, y, paint); // 画象限内的辅助虚线


                    paint.setPathEffect(null);
                    paint.setTextAlign(Paint.Align.LEFT);
                    float baseLineY2;
                    if (Math.abs(y - visibleRect.bottom) < 1f && align == Gravity.BOTTOM) { // 坐标轴上的数字,不做居中
                        baseLineY2 = y - textSize * 0.2f;
                    } else if (Math.abs(y - visibleRect.top) < 1f && align == Gravity.TOP) { // 坐标轴上的数字,不做居中
                        baseLineY2 = y + textSize * 1.2f;
                    } else {
                        baseLineY2 =
                                SmoothCylindricalDrawable.getTextY(paint, y - textSize / 2, y + textSize / 2);
                    }
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawText(textInfo.second, getBounds().left, baseLineY2, paint); // 画坐标轴上的文字
                    break;
            }
        }
    }

    private void drawIcons(Canvas canvas) {
        if (iconData == null || iconData.isEmpty() || iconSize <= 0) {
            return;
        }
        float ultraIconSize = Math.min(iconSize, iconRectSize * 0.8f);
        for (Pair<Float, Drawable> icon : iconData) {
            float iconCenterX;
            float iconCenterY;
            if (align == Gravity.BOTTOM || align == Gravity.TOP) {
                iconCenterX = visibleRect.left + visibleRect.width() * icon.first;
                if (align == Gravity.BOTTOM) {
                    iconCenterY = visibleRect.bottom + iconRectSize / 2;
                } else {
                    iconCenterY = visibleRect.top - iconRectSize / 2;
                }
            } else {
                iconCenterY = visibleRect.top + visibleRect.height() * icon.first;
                if (align == Gravity.LEFT) {
                    iconCenterX = visibleRect.left - iconRectSize / 2;
                } else {
                    iconCenterX = visibleRect.right + iconRectSize / 2;
                }
            }
            int left = (int) (iconCenterX - ultraIconSize / 2 + 0.5f);
            int right = (int) (left + ultraIconSize);
            int top = (int) (iconCenterY - ultraIconSize / 2 + 0.5f);
            int bottom = (int) (top + ultraIconSize);
            icon.second.setBounds(left, top, right, bottom);
            icon.second.draw(canvas);
        }
    }

    /**
     * 设置可见区域(此处特指XY轴之间的象限范围)
     *
     * @param left   左
     * @param top    上
     * @param right  右
     * @param bottom 下
     */
    public void setVisibleRect(float left, float top, float right, float bottom) {
        visibleRect.set(left, top, right, bottom);
    }

    /**
     * 设置图标数据
     *
     * @param data List<Pair<Float, Drawable>> Float在轴上的位置百分比;Drawable图标
     */
    public void setIconData(List<Pair<Float, Drawable>> data) {
        iconData = data == null ? Collections.emptyList() : new ArrayList<>(data);
    }

    /**
     * 设置图标尺寸
     *
     * @param rectSize 图标所在容器的大小
     * @param iconSize 图标宽高
     */
    public void setIconSize(float rectSize, float iconSize) {
        iconRectSize = rectSize;
        this.iconSize = iconSize;
    }

    /**
     * 设置柱状图数据
     *
     * @param data      List<Pair<Float, String>> Float在轴上的位置百分比;String要在坐标轴上标识的文本.
     * @param axisRange 坐标轴显示的数字范围
     */
    public void setCylindricalData(List<Pair<Float, String>> data, Range axisRange) {
        axisData = data == null ? Collections.emptyList() : new ArrayList<>(data);
        this.axisRange = axisRange;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
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

    /**
     * 设置进度条底座方向
     *
     * @param align Gravity
     */
    public void setAlign(int align) {
        this.align = align;
    }
}