package com.tzutalin.dlibtest;

import android.graphics.Point;

import com.tzutalin.dlib.VisionDetRet;

import java.util.ArrayList;

/**
 * Created by jslee on 2018-08-03.
 */

public class EAR {

    private double EYE_AR_THRESH = 0.2;

    private VisionDetRet ret;

    private double left_ear;
    private double right_ear;




    public EAR(VisionDetRet ret) {
        this.ret = ret;
    }

    public void calculateEarLeft() {
        ArrayList<Point> landmarks = this.ret.getFaceLandmarks();
        // 왼쪽 눈 이미지 (42 ~ 47)
        double a = calculateDistance(landmarks.get(43), landmarks.get(47));  // 44 48
        double b = calculateDistance(landmarks.get(44), landmarks.get(46));  // 45 47
        double c = calculateDistance(landmarks.get(42), landmarks.get(45));  // 43  46


        this.left_ear = ((a + b)/(2*c));
    }


    public void calculateEarRight() {
        ArrayList<Point> landmarks = this.ret.getFaceLandmarks();

        // 오른쪽 눈 이미지 (36 ~ 41)
        double a = calculateDistance(landmarks.get(37), landmarks.get(41));  // 38 42
        double b = calculateDistance(landmarks.get(38), landmarks.get(40));  // 39 41
        double c = calculateDistance(landmarks.get(36), landmarks.get(39)); //  37 40

        this.right_ear = ((a + b)/(2*c));
    }

    public boolean eyeBlink(double ear){

        // average the eye aspect ratio together for both eyes
        //double ear = (this.left_ear + this.right_ear) / 2.0;
        boolean isEAR = false;
        // check to see if the eye aspect ratio is below the blink
        // threshold, and if so, increment the blink frame counter
        if (ear < EYE_AR_THRESH){
            isEAR = true;
        }
        return  isEAR;

    }



    private double calculateDistance(Point p1, Point p2){

        double distance = Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));

        return distance;
    }


    public double getLeft_ear() {
        return left_ear;
    }

    public double getRight_ear() {
        return right_ear;
    }
}
