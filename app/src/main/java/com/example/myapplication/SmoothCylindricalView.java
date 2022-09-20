package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.Range;
import android.view.Gravity;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.myapplication.SmoothCylindricalDrawable.TEXT_SIZE_PERCENT;

public class SmoothCylindricalView extends View {
    private final static int COLUMN_COUNT = 6;
    private final float progressWidthPercent = 0.053f;
    private final float paddingIcon = 0.2f;
    private final float paddingAxisText = 0.0736f;
    private final SmoothCylindricalAxisDrawable axisDrawable = new SmoothCylindricalAxisDrawable();
    private final List<SmoothCylindricalDrawable> progressDrawables = new ArrayList<>();
    private List<Pair<Integer, Drawable>> originData;
    /**
     * 当前进度条依附的方向
     *
     * @since 2022-06-09
     */
    @SmoothCylindricalDrawable.Align
    private int align = Gravity.BOTTOM;

    public SmoothCylindricalView(Context context) {
        super(context);
        init();
    }

    public SmoothCylindricalView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SmoothCylindricalView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SmoothCylindricalView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    /**
     * 释放资源.防止内存泄漏.界面销毁的时候调用.
     */
    public void release() {
        if (progressDrawables != null) {
            for (Drawable drawable : progressDrawables) {
                if (drawable != null) {
                    drawable.setCallback(null);
                }
            }
            progressDrawables.clear();
        }
        if (originData != null) {
            originData.clear();
        }
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        super.invalidateDrawable(drawable);
        invalidate();
    }

    /**
     * 设置原始数据
     *
     * @param data List<Pair<Integer, Drawable>>
     */
    public void setOriginData(List<Pair<Integer, Drawable>> data) {
        this.originData = data == null ? Collections.emptyList() : new ArrayList<>(data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.originData.sort((o1, o2) -> Integer.compare(o2.first, o1.first));
        }

        progressDrawables.clear();
        for (int i = 0; i < COLUMN_COUNT && i < originData.size(); i++) {
            SmoothCylindricalDrawable drawable = new SmoothCylindricalDrawable();
            drawable.setAlign(align);
            drawable.setCallback(this);
            drawable.setTopText(String.valueOf(originData.get(i).first));
            progressDrawables.add(drawable);
        }
        postInvalidate();
    }

    private void init() {
        axisDrawable.setCallback(this);
        axisDrawable.setAlign(align);

        Drawable testDrawable = new ColorDrawable(0xffff0000);
        List<Pair<Integer, Drawable>> list = new ArrayList<>();
        list.add(new Pair<>(160, testDrawable));
        list.add(new Pair<>(112, testDrawable));
        list.add(new Pair<>(98, testDrawable));
        list.add(new Pair<>(68, testDrawable));
        list.add(new Pair<>(39, testDrawable));
        list.add(new Pair<>(19, testDrawable));
        setOriginData(list);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        convertData();

        drawAxis(canvas);

        drawProgressBars(canvas);
    }

    /**
     * 画坐标轴
     *
     * @param canvas 画布
     */
    private void drawAxis(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        axisDrawable.setBounds(0, 0, getWidth(), getHeight());
        float iconPadding;
        float axisTextPadding;
        switch (align) {
            case Gravity.LEFT:
            case Gravity.RIGHT:
                iconPadding = paddingIcon * width;
                axisTextPadding = paddingAxisText * height;
                if (align == Gravity.LEFT) {
                    axisDrawable.setVisibleRect(iconPadding, 0, width, height - axisTextPadding);
                } else {
                    axisDrawable.setVisibleRect(0, 0, width - iconPadding,
                            height - axisTextPadding);
                }
                break;
            case Gravity.TOP:
            case Gravity.BOTTOM:
                iconPadding = paddingIcon * height;
                axisTextPadding = paddingAxisText * width;
                if (align == Gravity.BOTTOM) {
                    axisDrawable.setVisibleRect(axisTextPadding, 0, width, height - iconPadding);
                } else {
                    axisDrawable.setVisibleRect(axisTextPadding, iconPadding, width, height);
                }
            default:
                break;
        }
        axisDrawable.draw(canvas);
    }

    /**
     * 画进度条
     *
     * @param canvas 画布
     */
    private void drawProgressBars(Canvas canvas) {
        if (progressDrawables != null && !progressDrawables.isEmpty()) {
            for (int i = 0; i < progressDrawables.size(); i++) {
                SmoothCylindricalDrawable drawable = progressDrawables.get(i);
                drawable.draw(canvas);
            }
        }
    }

    /**
     * 把原始数据转化为Drawable可用的数据
     */
    private void convertData() {
        if (originData == null || originData.isEmpty()) {
            return;
        }
        int width = getWidth();
        int height = getHeight();
        float progressWidth; // 进度条粗细
        float divide; // 进度条间隔长度
        float paddingAxisTextPixel; // 坐标轴文本留出的空间,实际像素值
        float paddingIconPixel; // 坐标轴图标留出的空间,实际像素值
        List<Pair<Float, Drawable>> iconData = new ArrayList<>(); // 坐标轴上要显示的图标信息<位置(百分比):图片>
        switch (align) {
            case Gravity.LEFT:
            case Gravity.RIGHT:
                progressWidth = height * progressWidthPercent;
                paddingAxisTextPixel = height * paddingAxisText;
                paddingIconPixel = width * paddingIcon;
                divide = (height - paddingAxisTextPixel) / COLUMN_COUNT - progressWidth;
                for (int i = 0; i < COLUMN_COUNT && i < originData.size(); i++) {
                    SmoothCylindricalDrawable drawable = progressDrawables.get(i);
                    drawable.setBounds(0, 0, width, height);
                    float top = (progressWidth + divide) * i + divide / 2;
                    float bottom = top + progressWidth;
                    float left;
                    float right;
                    if (align == Gravity.LEFT) {
                        left = paddingIconPixel;
                        right = width;
                    } else {
                        right = width - paddingIconPixel;
                        left = 0;
                    }
                    drawable.setVisibleRect(left, top, right, bottom);

                    float percentInAxis = ((bottom + top) / 2) / (height - paddingAxisTextPixel);
                    iconData.add(new Pair<>(percentInAxis, originData.get(i).second));
                }
                break;
            case Gravity.TOP:
            case Gravity.BOTTOM:
            default:
                progressWidth = width * progressWidthPercent;
                paddingAxisTextPixel = width * paddingAxisText;
                paddingIconPixel = height * paddingIcon;
                divide = (width - paddingAxisTextPixel) / COLUMN_COUNT - progressWidth;
                for (int i = 0; i < COLUMN_COUNT && i < originData.size(); i++) {
                    SmoothCylindricalDrawable drawable = progressDrawables.get(i);
                    drawable.setBounds(0, 0, width, height);
                    float left = paddingAxisTextPixel + (progressWidth + divide) * i + divide / 2;
                    float right = left + progressWidth;
                    float top;
                    float bottom;
                    if (align == Gravity.BOTTOM) {
                        top = 0;
                        bottom = height - paddingIconPixel;
                    } else {
                        top = paddingIconPixel;
                        bottom = height;
                    }
                    drawable.setVisibleRect(left, top, right, bottom);

                    float percentInAxis =
                            ((right + left) / 2 - paddingAxisTextPixel) / (width - paddingAxisTextPixel);
                    iconData.add(new Pair<>(percentInAxis, originData.get(i).second));
                }
                break;
        }
        axisDrawable.setTextSize(progressWidth * TEXT_SIZE_PERCENT);
        axisDrawable.setIconData(iconData);
        axisDrawable.setIconSize(paddingIconPixel, progressWidth * 1.2f);

        // 从原始数据中提取原始数值集合
        List<Integer> originValues = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            originValues = originData.stream().map(it -> it.first).collect(Collectors.toList());
        }

        // 计算得到坐标轴需要显示的刻度数值的集合
        List<Integer> marks = getMarks(originValues);

        // 计算得到坐标轴需要显示的各个刻度及其位置
        List<Pair<Float, String>> textData = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            textData = marks.stream()
                    .map(it -> new Pair<Float, String>((float) it, String.valueOf(it))).collect(Collectors.toList());
        }

        // 根据原始数据计算坐标轴需要显示的范围
        Range<Float> axisRange = getRange(originValues);
        axisDrawable.setCylindricalData(textData, axisRange);

        for (int i = 0; i < originData.size(); i++) {
            int value = originData.get(i).first;
            float percent = (value - axisRange.getLower()) / (axisRange.getUpper() - axisRange.getLower());
            progressDrawables.get(i).setPercent(percent, false);
            progressDrawables.get(i).setProgressAlpha(1 - 0.2f * i);
        }
    }

    private static Range<Float> getRange(List<Integer> data) {
        List<Integer> marks = getMarks(data);
        float startMark = marks.isEmpty() ? 0 : marks.get(0);
        float endMark = marks.isEmpty() ? (startMark + 1) : marks.get(marks.size() - 1);

        float endValue = (endMark - startMark) / 0.9f + startMark;
        return new Range<Float>(startMark, endValue);
    }

    private static List<Integer> getMarks(List<Integer> data) {
        float max = Collections.max(data);
        float min = Collections.min(data);
        float threshold = (max - min);
        float divide = threshold / 3; // 粒度
        int zero = 0; // 位数统计(几个0)
        float tmp = divide;
        while (tmp >= 10) {
            tmp /= 10;
            zero++;
        }
        int num = (int) Math.ceil(tmp);
        num *= Math.pow(10, zero); // 粒度
        int maxMark = (int) Math.ceil(max / num);
        int minMark = (int) Math.ceil(min / num);

        int beginMark = minMark - 1;
        if (beginMark < 0) {
            beginMark = 0;
        }
        List<Integer> marks = new ArrayList<>();
        for (int i = beginMark; i <= maxMark; i++) {
            marks.add(i * num);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            marks.sort(Integer::compareTo);
        }

        return marks;
    }

    /**
     * 设置进度条底座方向
     *
     * @param align Gravity
     */
    public void setAlign(int align) {
        this.align = align;
        postInvalidate();
    }
}