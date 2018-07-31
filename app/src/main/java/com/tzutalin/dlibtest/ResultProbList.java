package com.tzutalin.dlibtest;

import java.util.ArrayList;

/**
 * Created by jslee on 2018-07-18.
 */

public class ResultProbList extends ArrayList<ResultProb> {

    public int getSumProResult;

    public int result;

    private long verificationtime;

    public ResultProbList(int initialCapacity) {
        super(initialCapacity);
    }

    public ResultProbList() {
        super();
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    public Object[] toArray() {
        return super.toArray();
    }

    @Override
    public ResultProb get(int index) {
        return super.get(index);
    }

    @Override
    public ResultProb set(int index, ResultProb element) {
        return super.set(index, element);
    }

    @Override
    public boolean add(ResultProb resultProb) {
        return super.add(resultProb);
    }

    @Override
    public void add(int index, ResultProb element) {
        super.add(index, element);
    }

    public int getGetSumProResult() {
        return getSumProResult;
    }

    public void setGetSumProResult(int getSumProResult) {
        this.getSumProResult = getSumProResult;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public long getVerificationtime() { return verificationtime; }

    public void setVerificationtime(long verificationtime) { this.verificationtime = verificationtime; }
}
