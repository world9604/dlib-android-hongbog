package com.hongbog.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.util.Size;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

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
    private float mViewTop = 0;
    private float mStatusBarHeight = 0;

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


    public void setAspectRatio(final int width, final int height, final int drawStartTop, final int statusBarHeight) {
        Dlog.d("setAspectRatio");

        if (width <= 0 || height <= 0) {
            Dlog.e("width <= 0 || height <= 0");
            return;
        }

        mViewTop = drawStartTop;
        mStatusBarHeight = statusBarHeight;

        Dlog.d("mViewTop : " + mViewTop);
        Dlog.d("width : " + width);
        Dlog.d("height : " + height);
        Dlog.d("mStatusBarHeight : " + mStatusBarHeight);

        mRatioWidth = width;
        mRatioHeight = height;

        mEyeWidth = mRatioWidth * (8f/35f);
        mEyeHeight = mRatioHeight * (17f/35f);

        mStartRightX = mRatioWidth * (9f/35f);
        mStartRightY = mRatioHeight * (9f/35f);

        mEndRightX = mStartRightX + mEyeWidth;
        mEndRightY = mStartRightY + mEyeHeight;

        mEye2Eye = mRatioWidth * (1f/35f);

        mStartLeftX = mStartRightX + mEyeWidth + mEye2Eye;
        mStartLeftY = mStartRightY;

        mEndLeftX = mStartLeftX + mEyeWidth;
        mEndLeftY = mStartLeftY + mEyeHeight;

        Dlog.d("mStartRightY : " + mStartRightY);

        mBitmap = Bitmap.createScaledBitmap(mBitmap, (int)mEyeWidth, (int)mEyeHeight,  true);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        Dlog.d("onDraw");
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, mStartRightX, mViewTop + mStartRightY, null);
        canvas.drawBitmap(mBitmap, mStartLeftX, mViewTop + mStartLeftY, null);
    }


    /**
     * @see CameraConnectionFragment::configureTransform()
     * @param ratioWidth
     * @param ratioHeight
     */
    public void convertPreviewRatio(float ratioWidth, float ratioHeight){

        if(ratioWidth == 0 || ratioHeight == 0) return;

        mStartRightX = mStartRightX * ratioWidth;
        mStartRightY = (mViewTop + mStartRightY) * ratioHeight + mStatusBarHeight;

        mEndRightX = mEndRightX * ratioWidth;
        mEndRightY = (mViewTop + mEndRightY) * ratioHeight + mStatusBarHeight;

        mStartLeftX = mStartLeftX * ratioWidth;
        mStartLeftY = (mViewTop + mStartLeftY) * ratioHeight + mStatusBarHeight;

        mEndLeftX = mEndLeftX * ratioWidth;
        mEndLeftY = (mViewTop + mEndLeftY) * ratioHeight + mStatusBarHeight;

    }
}