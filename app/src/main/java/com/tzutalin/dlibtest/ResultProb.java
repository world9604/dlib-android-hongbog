package com.tzutalin.dlibtest;

/**
 * Created by jslee on 2018-07-17.
 */

public class ResultProb {
    private float probResult[];
    public int numClasses = 7;

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
}
