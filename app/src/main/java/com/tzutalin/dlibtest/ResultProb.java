package com.tzutalin.dlibtest;

import android.graphics.Bitmap;

/**
 * Created by jslee on 2018-07-17.
 */

public class ResultProb {

    private float probResult[];
    Bitmap leftBitmap;
    Bitmap rightBitmap;

    public ResultProb() {
        super();
    }

    public int sizeClass;
    public float sumProResult[];

    public float[] getProbResult() {
        return probResult;
    }

    public void setProbResult(float[] probResult) {
        this.probResult = probResult;
    }

    public float[] getSumProResult() {
        return sumProResult;
    }

    public Bitmap getLeftBitmap() {
        return leftBitmap;
    }

    public Bitmap getRightBitmap() {
        return rightBitmap;
    }

    public void setBitmap(Bitmap leftBitmap, Bitmap rightBitmap) {
        this.leftBitmap = leftBitmap;
        this.rightBitmap = rightBitmap;
    }
}
