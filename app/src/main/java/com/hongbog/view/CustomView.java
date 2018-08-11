package com.hongbog.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.tzutalin.dlibtest.CameraConnectionFragment;
import com.tzutalin.dlibtest.Dlog;
import com.tzutalin.dlibtest.OnGetImageListener;
import com.tzutalin.dlibtest.R;

/**
 * Created by darts on 2017-10-07.
 */

public class CustomView extends View {

    private Bitmap mBitmap;
    private float mRatioWidth = 0;
    private float mRatioHeight = 0;
    private float mEyeWidth = 0;
    private float mEyeHeight = 0;
    private float mStartRightX = 0;
    private float mStartRightY = 0;
    private float mEndRightX = 0;
    private float mEndRightY = 0;
    private float mStartLeftX = 0;
    private float mStartLeftY = 0;
    private float mEndLeftX = 0;
    private float mEndLeftY = 0;
    private float mEye2Eye = 0;

    public float getEyeWidth() {
        return mEyeWidth;
    }

    public float getEyeHeight() {
        return mEyeHeight;
    }

    public float getStartRightX() { return mStartRightX; }

    public float getStartRightY() {
        return mStartRightY;
    }

    public float getEye2Eye() {
        return mEye2Eye;
    }

    public float getStartLeftX() { return mStartLeftX; }

    public float getStartLeftY() { return mStartLeftY; }

    public float getEndRightX() { return mEndRightX; }

    public float getEndRightY() { return mEndRightY; }

    public float getEndLeftX() { return mEndLeftX; }

    public float getEndLeftY() { return mEndLeftY; }

    public CustomView(CameraConnectionFragment context) {
        super(context.getActivity().getBaseContext());
        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.guide_eye);
        setBackgroundColor(Color.TRANSPARENT);
    }

    public void setAspectRatio(final int width, final int height) {
        Dlog.d("setAspectRatio");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }

        mRatioWidth = width;
        mRatioHeight = height;

        final float hundredDp = TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 100,
                getResources().getDisplayMetrics() );

        mEyeWidth = mRatioWidth/5;
        mEyeHeight = mRatioHeight/7;

        mStartRightX = mEyeWidth * 1;
//        mStartRightY = (mEyeHeight * 2) + hundredDp;
//        mStartRightY = (mEyeHeight * 4);
        mStartRightY = mRatioHeight - mEyeHeight;

        mEndRightX = mStartRightX + mEyeWidth;
        mEndRightY = mStartRightY + mEyeHeight;

        mEye2Eye = mEyeWidth/2;
//        mEye2Eye = mEyeWidth;
//        mEye2Eye = 200;

        mStartLeftX = mStartRightX + mEyeWidth + mEye2Eye;
        mStartLeftY = mStartRightY;

        mEndLeftX = mStartLeftX + mEyeWidth;
        mEndLeftY = mStartLeftY + mEyeHeight;

        Dlog.d("mStartRightX : " + mStartRightX);
        Dlog.d("mStartRightY : " + mStartRightY);

        mBitmap = Bitmap.createScaledBitmap(mBitmap, (int)mEyeWidth, (int)mEyeHeight,  true);
//        mBitmap = Bitmap.createScaledBitmap(mBitmap, 220, 200,  true);

        // onDraw 호출
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        Dlog.d("onDraw");
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, mStartRightX, mStartRightY, null);
        canvas.drawBitmap(mBitmap, mStartLeftX, mStartLeftY, null);
    }


    /*@Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Dlog.d("onSizeChanged");
        Dlog.d("width : " + w);
        Dlog.d("height : " + h);
        super.onSizeChanged(w, h, oldw, oldh);
        setAspectRatio(w, h);
    }*/


    /*@Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        Dlog.d("onLayout");
        setAspectRatio(right, bottom);
        super.onLayout(changed, left, top, right, bottom);
    }*/


    public void covertPreviewRatio(int ratioWidth, int ratioHeight){

        if(ratioWidth == 0 || ratioHeight == 0) return;

        mStartRightX = mStartRightX * ratioWidth;
        mStartRightY = mStartRightY * ratioHeight;

        mEndRightX = mEndRightX * ratioWidth;
        mEndRightY = mEndRightY * ratioHeight;

        mStartLeftX = mStartLeftX * ratioWidth;
        mStartLeftY = mStartLeftY * ratioHeight;

        mEndLeftX = mEndLeftX * ratioWidth;
        mEndLeftY = mEndLeftY * ratioHeight;

    }
}