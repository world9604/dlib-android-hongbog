package com.tzutalin.dlib;

/**
 * Created by Tzutalin on 2015/10/20.
 */

import android.graphics.Point;

import java.util.ArrayList;

/**
 * A VisionDetRet contains all the information identifying the location and confidence value of the detected object in a bitmap.
 */
public final class VisionDetRet {
    private String mLabel;
    private float mConfidence;
    private int mLeft;
    private int mTop;
    private int mRight;
    private int mBottom;

    // 오른쪽 눈
    public int mStartRightX;
    public int mStartRightY;
    public int mEndRightX;
    public int mEndRightY;
    public int mHightRight;
    public int mWidthRight;

    // 왼쪽 눈
    public int mStartLeftX;
    public int mStartLeftY;
    public int mEndLeftX;
    public int mEndLeftY;
    public int mHightLeft;
    public int mWidthLeft;


    private ArrayList<Point> mLandmarkPoints = new ArrayList<>();



    VisionDetRet() {
    }

    public VisionDetRet(String label, float confidence, int l, int t, int r, int b) {
        mLabel = label;
        mLeft = l;
        mTop = t;
        mRight = r;
        mBottom = b;
        mConfidence = confidence;
    }

    /**
     * @return The X coordinate of the left side of the result
     */
    public int getLeft() {
        return mLeft;
    }

    /**
     * @return The Y coordinate of the top of the result
     */
    public int getTop() {
        return mTop;
    }

    /**
     * @return The X coordinate of the right side of the result
     */
    public int getRight() {
        return mRight;
    }

    /**
     * @return The Y coordinate of the bottom of the result
     */
    public int getBottom() {
        return mBottom;
    }

    /**
     * @return A confidence factor between 0 and 1. This indicates how certain what has been found is actually the label.
     */
    public float getConfidence() {
        return mConfidence;
    }

    /**
     * @return The label of the result
     */
    public String getLabel() {
        return mLabel;
    }
    // 오른쪽 눈
    public int  getStartRightX() {
        return mStartRightX;
    }
    public int  getStartRightY() {
        return mStartRightY;
    }
    public int  getEndRightX() {
        return mEndRightX;
    }
    public int  getEndRightY() {
        return mEndRightY;
    }
    public int  getHightRight() {
        return mHightRight;
    }
    public int  getWidthRight() {
        return mWidthRight;
    }
    //  왼쪽 눈
    public int  getStartLeftX() {
        return mStartLeftX;
    }
    public int  getStartLeftY() { return mStartLeftY; }
    public int  getEndLeftX() {
        return mEndLeftX;
    }
    public int  getEndLeftY() {
        return mEndLeftY;
    }
    public int  getHightLeft() {
        return mHightLeft;
    }
    public int  getWidthLeft() {
        return mWidthLeft;
    }


    /**
     * Add landmark to the list. Usually, call by jni
     * @param x Point x
     * @param y Point y
     * @return true if adding landmark successfully
     */
    public boolean addLandmark(int x, int y) {
        return mLandmarkPoints.add(new Point(x, y));
    }

    /**
     * Return the list of landmark points
     * @return ArrayList of android.graphics.Point
     */
    public ArrayList<Point> getFaceLandmarks() {
        return mLandmarkPoints;
    }


}
