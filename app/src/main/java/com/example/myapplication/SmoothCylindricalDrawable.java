package com.example.myapplication;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.animation.DecelerateInterpolator;

import java.lang.annotation.Retention;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;

import static com.example.myapplication.SmoothWaveDrawable.BIN_LENGTH_24;
import static com.example.myapplication.SmoothWaveDrawable.MASK_HEX_2;
import static com.example.myapplication.SmoothWaveDrawable.MASK_HEX_6;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * 项目：  My Application
 * 类名：  SmoothCylindricalDrawable.java
 * 时间：  2022/9/19 16:30
 * 描述：
 */
public class SmoothCylindricalDrawable extends Drawable {
    /**
     * 文本大小是进度条粗细的百分之多少
     */
    public static final float TEXT_SIZE_PERCENT = 0.5f;
    /**
     * 进度条可用(展示真实数据)时候的颜色.正式版使用自定义属性从布局读入
     */
    @ColorInt
    private final int progressColorEnable = Color.BLUE;

    /**
     * 进度条不可用(空白数据)时候的颜色.正式版使用自定义属性从布局读入
     */
    @ColorInt
    private final int progressColorDisable = Color.GRAY;
    /**
     * 文字颜色
     */
    @ColorInt
    private final int textColor = Color.GRAY;
    /**
     * 画笔
     */
    private final Paint paint = new Paint();

    private final Path path = new Path();

    private final RectF visibleRect = new RectF();

    private final PorterDuffXfermode porterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    /**
     * 进度条底部是否是圆润的
     */
    private boolean isProgressBottomCircle = false;

    /**
     * 设置是否灰色
     */
    private boolean isGray = false;

    /**
     * 进度条变化动效播放时间
     */
    private long duration = 1000L;

    private float percent = 0.0f;

    private float showPercent = percent;

    private String topText = "";

    private float alpha = 1.0f;

    /**
     * 进度条依附的方向可选值
     *
     * @since 2022-06-09
     */
    @IntDef({Gravity.LEFT, Gravity.TOP, Gravity.RIGHT, Gravity.BOTTOM})
    @Retention(SOURCE)
    public @interface Align {
    }

    /**
     * 当前进度条依附的方向
     *
     * @since 2022-06-09
     */
    @Align
    private int align = Gravity.BOTTOM;

    private ValueAnimator valueAnimator;

    public SmoothCylindricalDrawable() {
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
    }

    public void setTopText(String topText) {
        this.topText = topText;
        invalidateSelf();
    }

    /**
     * 平滑的播放一个进度条变化的动画.从上一次百分比开始(View初始化后默认进度0),播放到参数指定的百分比
     * 注:使用默认动画播放时长
     *
     * @param toPercent 进度条动画目标百分比
     */
    public void smoothProgressTo(@FloatRange(from = 0f, to = 1.0f) float toPercent) {
        smoothProgressTo(percent, toPercent, duration);
    }

    /**
     * 平滑的播放一个进度条变化的动画.从上一次百分比开始(View初始化后默认进度0),播放到参数指定的百分比
     *
     * @param toPercent 进度条动画目标百分比
     * @param animDuration 进度条变化动画的播放时间,单位毫秒.此参数<=0的时候没有动画.
     */
    public void smoothProgressTo(@FloatRange(from = 0f, to = 1.0f) float toPercent, long animDuration) {
        smoothProgressTo(percent, toPercent, animDuration);
    }

    /**
     * 平滑的播放一个进度条变化的动画.参数指定开始百分比和结束百分比
     * 注:使用默认动画播放时长
     *
     * @param beginPercent 进度条开始后的百分比
     * @param endPercent 进度条结束时候的百分比
     */
    public void smoothProgressTo(@FloatRange(from = 0f, to = 1.0f) float beginPercent,
                                 @FloatRange(from = 0f, to = 1.0f) float endPercent) {
        smoothProgressTo(beginPercent, endPercent, duration);
    }

    /**
     * 平滑的播放一个进度条变化的动画.参数指定开始百分比和结束百分比
     *
     * @param beginPercent 进度条开始后的百分比
     * @param endPercent 进度条结束时候的百分比
     * @param animDuration 进度条变化动画的播放时间,单位毫秒.此参数<=0的时候没有动画.
     */
    public void smoothProgressTo(@FloatRange(from = 0f, to = 1.0f) float beginPercent,
                                 @FloatRange(from = 0f, to = 1.0f) float endPercent, long animDuration) {
        if (valueAnimator != null && valueAnimator.isRunning()) {
            if (Math.abs(endPercent - percent) <= 0.0001f) { // 如上一次同一目标进度的动画,还未播放完成,则继续播放.
                percent = endPercent;
                return;
            }
            valueAnimator.pause();
            valueAnimator.removeAllUpdateListeners();
            valueAnimator = null;
        }
        percent = endPercent;
        if (animDuration <= 0) {
            showPercent = endPercent;
            invalidateSelf();
            return;
        }
        valueAnimator = ValueAnimator.ofFloat(beginPercent, endPercent);
        valueAnimator.setRepeatCount(0);
        valueAnimator.setDuration(animDuration);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(animation -> {
            showPercent = (float) animation.getAnimatedValue();
//            Log.i("test", "showPercent 赋值:" + showPercent);
            invalidateSelf();
        });
        valueAnimator.start();
    }

    /**
     * 立即刷新进度
     *
     * @param percent 进度百分比
     * @param isInvalid 是否要刷新界面
     */
    public void setPercent(float percent, boolean isInvalid) {
        this.percent = percent;
        this.showPercent = percent;
        if (isInvalid) {
            invalidateSelf();
        }
    }

    /**
     * 设置进度条所在区域
     *
     * @param visibleRect 进度条所在区域
     */
    public void setVisibleRect(Rect visibleRect) {
        this.visibleRect.set(visibleRect);
    }

    /**
     * 设置进度条所在区域
     *
     * @param left 左
     * @param top 上
     * @param right 右
     * @param bottom 下
     */
    public void setVisibleRect(float left, float top, float right, float bottom) {
        this.visibleRect.set(left, top, right, bottom);
    }

    @Override
    public void draw(Canvas canvas) {
        drawPercent(canvas, showPercent);
    }

    /**
     * 绘制静态进度条关键方法.进度条由两层绘制setXfermode交叉得出
     *
     * @param canvas 画布
     * @param percent 进度条要占其View长(或宽,取决于进度条Gravity方向)的百分比.(0.0f-1.0f)
     */
    @SuppressLint("RtlHardcoded")
    private void drawPercent(Canvas canvas, float percent) {
        switch (align) {
            case Gravity.LEFT:
                drawProgressAlignLeft(canvas, percent);
                break;
            case Gravity.TOP:
                drawProgressAlignTop(canvas, percent);
                break;
            case Gravity.RIGHT:
                drawProgressAlignRight(canvas, percent);
                break;
            case Gravity.BOTTOM:
            default:
                drawProgressAlignBottom(canvas, percent);
                break;
        }
    }

    /**
     * 画依附于上部的进度条,第一层;进度条由两层绘制setXfermode交叉得出
     *
     * @param canvas 画布
     * @param percent 百分比
     */
    private void drawProgressAlignTop(Canvas canvas, float percent) {
        Rect rect = getBounds();
        int layoutId = canvas.saveLayer(rect.left, rect.top, rect.right, rect.bottom, paint, Canvas.ALL_SAVE_FLAG);

        float progressLength = visibleRect.height();
        float progressWidth = visibleRect.width();
        float radius = progressWidth / 2f;

        float progressHeadY = visibleRect.top + progressLength * percent;
        path.reset();
        path.moveTo(visibleRect.left, progressHeadY - radius); // 初始化移动到左上角
        path.arcTo(visibleRect.left, progressHeadY - radius * 2, visibleRect.right, // 逆时针画半圆
                progressHeadY, 180, -180, false);
        path.lineTo(visibleRect.right, visibleRect.top); // 连线到右上角
        path.lineTo(visibleRect.left, visibleRect.top); // 连线到左上角
        path.close();

        paint.setColor(getFillColor());
        canvas.drawPath(path, paint);

        clearSurplus(canvas);

        float textSize = progressWidth * TEXT_SIZE_PERCENT;
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(textColor);
        float textX = visibleRect.centerX() + 0.5f;
        float textY = progressHeadY + textSize * 1.3f;
        canvas.drawText(topText, textX, textY, paint);

        canvas.restoreToCount(layoutId);
    }

    /**
     * 画依附于底部的进度条,第一层;进度条由两层绘制setXfermode交叉得出
     *
     * @param canvas 画布
     * @param percent 百分比
     */
    private void drawProgressAlignBottom(Canvas canvas, float percent) {
        Rect rect = getBounds();
        int layoutId = canvas.saveLayer(rect.left, rect.top, rect.right, rect.bottom, paint, Canvas.ALL_SAVE_FLAG);

        float progressLength = visibleRect.height();
        float progressWidth = visibleRect.width();
        float radius = progressWidth / 2f;

        float progressHeadY = visibleRect.top + progressLength * (1 - percent);
        path.reset();
        path.moveTo(visibleRect.left, progressHeadY + radius); // 初始移动到左上角
        path.arcTo(visibleRect.left, progressHeadY, visibleRect.right, // 顺时针画半圆
                progressHeadY + radius * 2, 180, 180, false);
        path.lineTo(visibleRect.right, visibleRect.bottom); // 连线到右下角
        path.lineTo(visibleRect.left, visibleRect.bottom); // 连线到左下角
        path.close();

        paint.setColor(getFillColor());
        canvas.drawPath(path, paint);

        clearSurplus(canvas);

        float textSize = progressWidth * TEXT_SIZE_PERCENT;
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(textColor);
        float textX = visibleRect.centerX() + 0.5f;
        float textY = progressHeadY - textSize * 0.5f;
        canvas.drawText(topText, textX, textY, paint);

        canvas.restoreToCount(layoutId);
    }

    /**
     * 画依附于左边的进度条,第一层;进度条由两层绘制setXfermode交叉得出
     *
     * @param canvas 画布
     * @param percent 百分比
     */
    private void drawProgressAlignLeft(Canvas canvas, float percent) {
        Rect rect = getBounds();
        int layoutId = canvas.saveLayer(rect.left, rect.top, rect.right, rect.bottom, paint, Canvas.ALL_SAVE_FLAG);

        float progressWidth = visibleRect.height();
        float progressLength = visibleRect.width();
        float radius = progressWidth / 2f;

        float progressHeadX = visibleRect.left + progressLength * percent;
        path.reset();
        path.moveTo(progressHeadX - radius, visibleRect.top); // 初始移动到右上角
        path.arcTo(progressHeadX - radius * 2, visibleRect.top, progressHeadX, // 顺时针画半圆
                visibleRect.bottom, -90, 180, false);
        path.lineTo(visibleRect.left, visibleRect.bottom); // 连线到左下角
        path.lineTo(visibleRect.left, visibleRect.top); // 连线到左上角
        path.close();

        paint.setColor(getFillColor());
        canvas.drawPath(path, paint);

        clearSurplus(canvas);

        float textSize = progressWidth * TEXT_SIZE_PERCENT;
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(textColor);
        float textX = progressHeadX + textSize * 0.5f;
        float textY = getTextY(paint, visibleRect.top, visibleRect.bottom);
        canvas.drawText(topText, textX, textY, paint);

        canvas.restoreToCount(layoutId);
    }


    /**
     * 画依附于右边的进度条,第一层;进度条由两层绘制setXfermode交叉得出
     *
     * @param canvas 画布
     * @param percent 百分比
     */
    private void drawProgressAlignRight(Canvas canvas, float percent) {
        Rect rect = getBounds();
        int layoutId = canvas.saveLayer(rect.left, rect.top, rect.right, rect.bottom, paint, Canvas.ALL_SAVE_FLAG);

        float progressWidth = visibleRect.height();
        float progressLength = visibleRect.width();
        float radius = progressWidth / 2f;

        float progressHeadX = visibleRect.right - progressLength * percent;
        path.reset();
        path.moveTo(progressHeadX + radius, visibleRect.top); // 初始移动左上角
        path.arcTo(progressHeadX, visibleRect.top, progressHeadX + radius * 2, // 逆时针画半圆
                visibleRect.bottom, -90, -180, false);
        path.lineTo(visibleRect.right, visibleRect.bottom); // 连线到右下角
        path.lineTo(visibleRect.right, visibleRect.top); // 连线到右上角
        path.close();

        paint.setColor(getFillColor());
        canvas.drawPath(path, paint);

        clearSurplus(canvas);

        float textSize = progressWidth * TEXT_SIZE_PERCENT;
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(textColor);
        float textX = progressHeadX - textSize * 0.5f;
        float textY = getTextY(paint, visibleRect.top, visibleRect.bottom);
        canvas.drawText(topText, textX, textY, paint);

        canvas.restoreToCount(layoutId);
    }

    @SuppressLint("RtlHardcoded")
    private void clearSurplus(Canvas canvas) {
        paint.setXfermode(porterDuffXfermode);
        paint.setColor(Color.WHITE);

        if (!isProgressBottomCircle()) {
            switch (align) {
                case Gravity.LEFT:
                    canvas.drawRect(getBounds().left, visibleRect.top, visibleRect.left, visibleRect.bottom, paint);
                    break;
                case Gravity.RIGHT:
                    canvas.drawRect(visibleRect.right, visibleRect.top, getBounds().right, visibleRect.bottom, paint);
                    break;
                case Gravity.TOP:
                    canvas.drawRect(visibleRect.left, getBounds().top, visibleRect.right, visibleRect.top, paint);
                    break;
                case Gravity.BOTTOM:
                default:
                    canvas.drawRect(visibleRect.left, visibleRect.bottom, visibleRect.right, getBounds().bottom, paint);
                    break;
            }

            paint.setXfermode(null);
            return;
        }
        path.reset();
        float progressWidth;
        float radius;
        switch (align) {
            case Gravity.LEFT:
                progressWidth = visibleRect.height();
                radius = progressWidth / 2f;
                path.moveTo(visibleRect.left - radius, visibleRect.bottom); // 初始化到左下角
                path.lineTo(visibleRect.left - radius, visibleRect.top); // 连线到左上角
                path.lineTo(visibleRect.left + radius, visibleRect.top); // 连线到右上角
                path.arcTo(visibleRect.left, visibleRect.top, visibleRect.left + 2 * radius, visibleRect.bottom,
                        -90, -180, false); // 逆时针画半圆到右下角
                break;
            case Gravity.RIGHT:
                progressWidth = visibleRect.height();
                radius = progressWidth / 2f;
                path.moveTo(visibleRect.right + radius, visibleRect.top); // 初始化到右上角
                path.lineTo(visibleRect.right + radius, visibleRect.bottom); // 连线到右下角
                path.lineTo(visibleRect.right - radius, visibleRect.bottom); // 连线到左下角
                path.arcTo(visibleRect.right - 2 * radius, visibleRect.top, visibleRect.right,
                        visibleRect.bottom, 90, -180, false); //逆时针画半圆到左上角
                break;
            case Gravity.TOP:
                progressWidth = visibleRect.width();
                radius = progressWidth / 2f;
                path.moveTo(visibleRect.left, visibleRect.top - radius); // 初始化到左上角
                path.lineTo(visibleRect.right, visibleRect.top - radius); // 连线到右上角
                path.lineTo(visibleRect.right, visibleRect.top + radius); // 连线到右下角
                path.arcTo(visibleRect.left, visibleRect.top, visibleRect.right, visibleRect.top + 2 * radius, 0,
                        -180, false); // 逆时针画半圆到左下角
                break;
            case Gravity.BOTTOM:
            default:
                progressWidth = visibleRect.width();
                radius = progressWidth / 2f;
                path.moveTo(visibleRect.right, visibleRect.bottom + radius); // 初始化到右下角
                path.lineTo(visibleRect.left, visibleRect.bottom + radius); // 连线到左下角
                path.lineTo(visibleRect.left, visibleRect.bottom - radius); // 连线到左上角
                path.arcTo(visibleRect.left, visibleRect.bottom - 2 * radius, visibleRect.right,
                        visibleRect.bottom, 180, -180, false); // 逆时针画半圆到右上角
                break;
        }
        path.close();

        canvas.drawPath(path, paint);

        paint.setXfermode(null);
    }

    /**
     * 计算文字居中需要设置的文本Y坐标大小
     *
     * @param paint 画笔
     * @param topY 容器上边缘Y坐标
     * @param bottomY 容器下边缘Y坐标
     * @return
     */
    public static float getTextY(Paint paint, float topY, float bottomY) {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float top = fontMetrics.top;
        float bottom = fontMetrics.bottom;
        return topY + (bottomY - topY) / 2f - top / 2f - bottom / 2f;
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
     * 获取进度条底座的方向
     *
     * @return Gravity
     */
    @Align
    public int getAlign() {
        return align;
    }

    /**
     * 设置进度条依附方向
     *
     * @param align
     */
    public void setAlign(@Align int align) {
        if (this.align != align) {
            this.align = align;
            invalidateSelf();
        }
    }

    public boolean isProgressBottomCircle() {
        return isProgressBottomCircle;
    }

    /**
     * 设置进度条底部是否圆润
     *
     * @param progressBottomCircle true圆润,false直角
     */
    public void setProgressBottomCircle(boolean progressBottomCircle) {
        if (isProgressBottomCircle != progressBottomCircle) {
            isProgressBottomCircle = progressBottomCircle;
            invalidateSelf();
        }
    }

    public boolean isGray() {
        return isGray;
    }

    /**
     * 设置进度条是否灰色
     *
     * @param gray 是否灰色
     */
    public void setGray(boolean gray) {
        this.isGray = gray;
        invalidateSelf();
    }

    public void setProgressAlpha(float alpha) {
        this.alpha = alpha;
    }

    private int getFillColor() {
        if (isGray()) {
            return progressColorDisable;
        }
        int originAlphaInt = (progressColorEnable >> BIN_LENGTH_24) & MASK_HEX_2;
        int ultraAlphaInt = ((int) (originAlphaInt * alpha + 0.5f)) & MASK_HEX_2;

        return (ultraAlphaInt << BIN_LENGTH_24) + (progressColorEnable & MASK_HEX_6);
    }
}