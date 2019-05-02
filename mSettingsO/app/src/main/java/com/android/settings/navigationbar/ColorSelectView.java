/******************************************************************************/
/*                                                               Date:09/2017 */
/*                                PRESENTATION                                */
/*                                                                            */
/*       Copyright 2017 TCL Communication Technology Holdings Limited.        */
/*                                                                            */
/* This material is company confidential, cannot be reproduced in any form    */
/* without the written permission of TCL Communication Technology Holdings    */
/* Limited.                                                                   */
/*                                                                            */
/* -------------------------------------------------------------------------- */
/*  Author :  zhibin.zhong                                                    */
/*  Email  :  zhibin.zhong@tcl.com                                            */
/*  Role   :                                                                  */
/*  Reference documents :                                                     */
/* -------------------------------------------------------------------------- */
/*  Comments :                                                                */
/*  File     :                                                                */
/*  Labels   :                                                                */
/* -------------------------------------------------------------------------- */
/* ========================================================================== */
/*     Modifications on Features list / Changes Request / Problems Report     */
/* -------------------------------------------------------------------------- */
/*    date   |        author        |         Key          |     comment      */
/* ----------|----------------------|----------------------|----------------- */
/* 22/03/2018| zhibin.zhong@tcl.com |       XR6137780, Navigationbar Ergo Dev.on 2018/03/21        |UE Design list|   */
/*           |                      |                      |Navigation bar    */
/* ----------|----------------------|----------------------|----------------- */
/******************************************************************************/
package com.android.settings.navigationbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class ColorSelectView extends View {

    private Context mContext;
    private Paint mPaint;

    private int index = 0;

    private int mColorFill = Color.BLACK;

    private boolean mSelected = false;
    private float mWidth;
    private float mWidthStroke;
    //begin-zhibin.zhong modify for Defect 7140299 on 2018.12.05
    private static final String COLOR_SELECT_SEL = "#1BB0F4";
    private static final String COLOR_UNSELECT_SEL = "#9B9B9B";
    //end-zhibin.zhong modify for Defect 7140299 on 2018.12.05

    public ColorSelectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mWidthStroke = dip2px(mContext, 1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mWidth = getWidth();
        RectF rect = new RectF();
        rect.left = rect.top = mWidthStroke-1;
        rect.right = rect.bottom = mWidth-mWidthStroke+1;
        //begin-zhibin.zhong add for Defect 7140299 on 2018.12.05
        if(this.index == 0) {
            RectF rect1 = new RectF();
            rect1.left = rect1.top = mWidthStroke-1;
            rect1.right = rect1.bottom = mWidth-mWidthStroke+1-4;
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(2);
            mPaint.setColor(Color.parseColor(mSelected ? COLOR_SELECT_SEL : COLOR_UNSELECT_SEL));
            canvas.drawRoundRect(rect1, 24, 24, mPaint);
        }
        //end-zhibin.zhong add for Defect 7140299 on 2018.12.05
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mColorFill);
        canvas.drawRoundRect(rect, 24, 24, mPaint);

        if (mSelected) {
            /*mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(mWidthStroke);
            mPaint.setColor(COLOR_SELECT_RECT);
            RectF rects = new RectF();
            rects.left = rects.top = 0;
            rects.right = rects.bottom = mWidth-mWidthStroke+1;
            canvas.drawRoundRect(rects, 24, 24, mPaint);*/
            //begin-zhibin.zhong modify for Defect 7140299 on 2018.12.05
            mPaint.setStrokeWidth(2);
            mPaint.setColor(Color.parseColor(mSelected?COLOR_SELECT_SEL:COLOR_UNSELECT_SEL));
            //end-zhibin.zhong modify for Defect 7140299 on 2018.12.05
            int widthtmp = (int)mWidth;
            Point point1 = new Point(widthtmp/5, widthtmp/2);
            Point point2 = new Point(widthtmp*2/5, widthtmp*7/10);
            Point point3 = new Point(widthtmp*4/5, widthtmp*3/10);
            canvas.drawLine(point1.x, point1.y, point2.x, point2.y, mPaint);
            canvas.drawLine(point2.x, point2.y, point3.x, point3.y, mPaint);
        }
        super.onDraw(canvas);
    }

    public static int dip2px(Context context, float dpValue) {  
        final float scale = context.getResources().getDisplayMetrics().density;  
        return (int) (dpValue * scale + 0.5f);  
    } 

    public void setColorSelected(boolean selected) {
        mSelected = selected;
        invalidate();
    }

    public void setColor(int color) {
        mColorFill = color;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }

}
