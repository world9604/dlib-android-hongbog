package com.tzutalin.dlib;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by houzhi on 16-10-20.
 * Modified by tzutalin on 16-11-15
 */
public class FaceDet {
    private static final String TAG = "dlib";

    // accessed by native methods
    @SuppressWarnings("unused")
    private long mNativeFaceDetContext;
    private String mLandMarkPath = "";

    static {
        try {
            System.loadLibrary("android_dlib");
            jniNativeClassInit();
            Log.d(TAG, "jniNativeClassInit success");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "library not found");
        }
    }

    @SuppressWarnings("unused")
    public FaceDet() {
        jniInit(mLandMarkPath);
    }

    public FaceDet(String landMarkPath) {
        mLandMarkPath = landMarkPath;
        jniInit(mLandMarkPath);
    }

    @Nullable
    @WorkerThread
    public List<VisionDetRet> detect(@NonNull String path) {
        VisionDetRet[] detRets = jniDetect(path);
        return Arrays.asList(detRets);
    }

    @Nullable
    @WorkerThread
    public List<VisionDetRet> detect(@NonNull Bitmap bitmap) {
        VisionDetRet[] detRets = jniBitmapDetect(bitmap);

        //
        for (int i=0; i<detRets.length; i++) {
            VisionDetRet detect = detRets[i];

            ArrayList<Point> landmark = detect.getFaceLandmarks();
            Log.d(TAG,"DetRet Landmarks.size"+String.valueOf(landmark.size()));
            for (int j=0; j<landmark.size(); j++) {
                Point point = landmark.get(j);
                Log.d(TAG, "DetRet point: (" + String.valueOf(point.x) + "," + String.valueOf(point.y) + ")");
            }
            //-------------------------------------------
            // 오른쪽 눈 이미지 (36 ~ 41)
            detect.mStartRightX= landmark.get(36).x ;
            detect.mEndRightX= landmark.get(39).x ;

            detect.mStartRightY = landmark.get(37).y;
            if(landmark.get(37).y > landmark.get(38).y){
                detect.mStartRightY = landmark.get(38).y;
            }

            detect.mEndRightY = landmark.get(40).y;
            if(landmark.get(41).y > landmark.get(40).y){
                detect.mEndRightY = landmark.get(41).y;
            }
            detect.mHightRight = detect.mEndRightY - detect.mStartRightY;
            detect.mWidthRight = detect.mEndRightX - detect.mStartRightX;


            // 왼쪽 눈 이미지 (42 ~ 47)
            detect.mStartLeftX = landmark.get(42).x ;
            detect.mEndLeftX = landmark.get(45).x;

            detect.mStartLeftY = landmark.get(43).y;
            if(landmark.get(43).y > landmark.get(44).y){
                detect.mStartLeftY = landmark.get(44).y;
            }

            detect.mEndLeftY = landmark.get(47).y;
            if(landmark.get(47).y > landmark.get(46).y){
                detect.mEndLeftY = landmark.get(46).y;
            }
            detect.mHightLeft = detect.mEndLeftY - detect.mStartLeftY;
            detect.mWidthLeft = detect.mEndLeftX - detect.mStartLeftX;
            // 범위 재정의
            // 오른쪽 눈
            int h = detect.mHightRight;
            int w = detect.mWidthRight;

            int x_scope1 = (int)((3*h - w)/2 + 3*h*0.4);
            int x_scope2 = (int)((3*h - w)/2 + 3*h*0.6);
            int y_scope = (int)(h * 0.9);
            Log.i(TAG,"y_scope");
            Log.i(TAG,String.valueOf(x_scope1));
            Log.i(TAG,String.valueOf(x_scope2));
            Log.i(TAG,String.valueOf(y_scope));

            detect.mStartRightX = detect.mStartRightX - x_scope2;
            detect.mStartRightY = detect.mStartRightY - y_scope;
            detect.mEndRightX = detect.mEndRightX + x_scope1;
            detect.mEndRightY = detect.mEndRightY + y_scope;

            detect.mHightRight = detect.mEndRightY - detect.mStartRightY;
            detect.mWidthRight = detect.mEndRightX - detect.mStartRightX;

            // 왼쪽 눈
            h = detect.mHightLeft;
            w = detect.mWidthLeft;

            x_scope1 = (int)((3*h - w)/2 + 3*h*0.4);
            x_scope2 = (int)((3*h - w)/2 + 3*h*0.6);
            y_scope = (int)(h * 0.9);

            detect.mStartLeftX= detect.mStartLeftX - x_scope1;
            detect.mStartLeftY = detect.mStartLeftY - y_scope;
            detect.mEndLeftX = detect.mEndLeftX + x_scope2;
            detect.mEndLeftY = detect.mEndLeftY + y_scope;

            detect.mHightLeft = detect.mEndLeftY - detect.mStartLeftY;
            detect.mWidthLeft = detect.mEndLeftX - detect.mStartLeftX;
        }

        return Arrays.asList(detRets);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        release();
    }

    public void release() {
        jniDeInit();
    }

    @Keep
    private native static void jniNativeClassInit();

    @Keep
    private synchronized native int jniInit(String landmarkModelPath);

    @Keep
    private synchronized native int jniDeInit();

    @Keep
    private synchronized native VisionDetRet[] jniBitmapDetect(Bitmap bitmap);

    @Keep
    private synchronized native VisionDetRet[] jniDetect(String path);
}