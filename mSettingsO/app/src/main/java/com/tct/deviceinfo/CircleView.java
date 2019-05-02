package com.tct.deviceinfo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * TODO: Created by miaoliu for adding red dot in Updates menu on 2018/8/25
 */
public class CircleView extends View {
    private Paint mPaint = new Paint();
    public CircleView(Context context) {
        super(context);
    }

    public CircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO Auto-generated method stub
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        int max = Math.max(measuredWidth, measuredHeight);
        setMeasuredDimension(max, max);
    }

     @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //给画笔设置颜色
        mPaint.setColor(Color.RED);
        //设置画笔属性
        //mPaint.setStyle(Paint.Style.FILL);//画笔属性是实心圆
        //paint.setStyle(Paint.Style.STROKE);//画笔属性是空心圆
        //mPaint.setStrokeWidth(2);//设置画笔粗细
        mPaint.setAntiAlias(true);
        /*四个参数：
                参数一：圆心的x坐标
                参数二：圆心的y坐标
                参数三：圆的半径
                参数四：定义好的画笔
                */
        canvas.drawCircle(getWidth()/2, getHeight()/2, Math.max(getWidth(), getHeight())/2, mPaint);
    }
}
