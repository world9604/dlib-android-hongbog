package com.tzutalin.dlibtest;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;

import com.tzutalin.dlib.VisionDetRet;

import java.util.ArrayList;

import static java.lang.Math.abs;

/**
 * Created by jslee on 2018-08-07.
 */

public class Glace {
    private VisionDetRet mRet;
    private Bitmap mBitmap;

    private Paint mPaint;


    public Glace( VisionDetRet ret, Bitmap bitmap) {

        this.mRet = ret;
        this.mBitmap = bitmap;
    }

    public int calculateSideGlace(){

        // side-glance
        mPaint = new Paint();
        mPaint.setStrokeWidth(2);
        mPaint.setStyle(Paint.Style.STROKE);

        //detecting 이미지를 보여준다
        Canvas canvas = new Canvas(this.mBitmap);
        ArrayList<Point> landmarks = this.mRet.getFaceLandmarks();

        int n0 = 0;
        int n36 = 0;
        int n45 = 0;
        int n16 = 0;

        for (int j=0; j<landmarks.size(); j++) {
            Point point = landmarks.get(j);
            //if(j>35  && j<42) {     // 실제 눈의 오른쪽(36 ~ 41)
            if(j==36) {
                mPaint.setColor(Color.CYAN);
                canvas.drawCircle(point.x , point.y, 1, mPaint);
                n36 = point.x;
            }
            //else if(j>41  && j<48){      //실제 눈의 왼쪽 (42 ~ 47)
            else if(j==45){      //실제 눈의 왼쪽 (42 ~ 47)
                mPaint.setColor(Color.GREEN);
                canvas.drawCircle(point.x , point.y, 1, mPaint);
                n45 = point.x;
            }
            else if(j==0){ //(오른쪽)
                mPaint.setColor(Color.RED);
                canvas.drawCircle(point.x , point.y, 1, mPaint);
                n0 = point.x;
            }
            else if(j==16){ // 왼쪽
                mPaint.setColor(Color.BLUE);
                canvas.drawCircle(point.x , point.y, 1, mPaint);
                n16 = point.x;
            }

        }
        int leftSideWidth = abs(n16 - n45);
        int rightSideFWidth = abs(n36 - n0);

        Log.i("GLACE", String.format("right side & eye: (%d, %d), left side & eye: (%d, %d)",n0, n36, n45, n16));
        Log.i("GLACE", String.format("rightSideFWidth: %d, leftSideWidth: %d",rightSideFWidth, leftSideWidth));

        // Image print
        String all = " ("+String.valueOf(rightSideFWidth)+" + "+String.valueOf(leftSideWidth)+") ";
        // ImageUtils.saveBitmap(mBitmap, all);

        int diff = abs(leftSideWidth - rightSideFWidth);

        return diff;



    }


}
