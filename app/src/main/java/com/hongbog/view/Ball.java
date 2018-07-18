package com.hongbog.view;

import android.graphics.Rect;

/**
 * Created by taein on 2018-07-12.
 */

public class Ball {
    private Rect balldst = null;
    private Rect ballsrc = null;
    private int mCircleX = 0;
    private int mCircleZ = 0;

    public void calcBall(float f, float f2, int i, int i2, int i3) {
        int i4 = 1 ;
        float f3 = (float) (((double) (f2 / 10.0f)) * 1.0204d);
        this.mCircleX = (int) (((float) (i / 2)) - ((((float) (((double) (f / 10.0f)) * 1.0204d)) * ((float) (i / 2))) / ((float) i4)));
        this.mCircleZ = (int) (((float) (i / 2)) - ((f3 * ((float) (i / 2))) / ((float) i4)));
        if (this.mCircleX > i) {
            this.mCircleX = i;
        }
        if (this.mCircleZ > i2) {
            this.mCircleZ = i;
        }
        if (this.mCircleX < 0) {
            this.mCircleX = 0;
        }
        if (this.mCircleZ < 0) {
            this.mCircleZ = 0;
        }
        this.balldst = new Rect(this.mCircleX - i3, this.mCircleZ - i3, this.mCircleX + i3, this.mCircleZ + i3);
    }

    public Rect getBallDst() {
        return this.balldst;
    }

    public Rect getBallSrc() {
        return this.ballsrc;
    }
}

