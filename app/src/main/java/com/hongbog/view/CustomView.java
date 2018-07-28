package com.hongbog.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import com.tzutalin.dlibtest.CameraConnectionFragment;
import com.tzutalin.dlibtest.Dlog;
import com.tzutalin.dlibtest.R;

/**
 * Created by darts on 2017-10-07.
 */

public class CustomView extends View {

    private Bitmap mBitmap;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private int mEyeWidth = 0;
    private int mEyeHeight = 0;
    private int mStartLeft = 0;
    private int mStartTop = 0;
    private int mEye2Eye = 0;

    public CustomView(CameraConnectionFragment context) {
        super(context.getActivity().getBaseContext());
        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.guide_eye);
        setBackgroundColor(Color.TRANSPARENT);
    }


    public void setAspectRatio(final int width, final int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }

        mRatioWidth = width;
        mRatioHeight = height;
        mEyeWidth = mRatioWidth/8;
        mEyeHeight = mRatioHeight/8;
        mStartLeft = (mRatioWidth/8) * 2;
        mStartTop = mRatioWidth/8;
        mEye2Eye = (mRatioWidth/8) * 2;
        mBitmap = Bitmap.createScaledBitmap(mBitmap, mEyeWidth, mEyeHeight,  true);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, mStartLeft, mStartTop, null);
        canvas.drawBitmap(mBitmap, mStartLeft + mEyeWidth + mEye2Eye, mStartTop, null);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setAspectRatio(w, h);
    }
}