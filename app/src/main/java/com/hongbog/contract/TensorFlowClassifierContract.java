package com.hongbog.contract;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.os.TraceCompat;
import android.util.Log;

import com.tzutalin.dlibtest.Dlog;
import com.tzutalin.dlibtest.ImageUtils;
import com.tzutalin.dlibtest.ParcelBitmap;
import com.tzutalin.dlibtest.ResultProb;
import com.tzutalin.dlibtest.ResultProbList;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.CV_8UC1;

public abstract class TensorFlowClassifierContract {

    /**
     * 텐서플로우 classifier 초기화 함수
     */
    protected abstract void initTensorFlowAndLoadModel(AssetManager assetManager);

    /**
     * 텐서플로우 classifier 생성 관련 초기화 함수
     * @param assetManager
     * @param modelFilename
     * @param labelFilename
     * @param widths
     * @param heights
     * @param rightInputNames
     * @param leftInputNames
     * @param outputNames
     */
    protected abstract void createClassifier(
            AssetManager assetManager,
            String modelFilename,
            String labelFilename,
            int[] widths,
            int[] heights,
            String[] rightInputNames,
            String[] leftInputNames,
            String[] outputNames);


    /**
     * 오른쪽/왼쪽 눈에 대한 verification 을 수행하는 함수
     * @return
     */
    protected abstract float[] verificationEye();


    protected abstract ResultProbList Verification(Bundle bundle);


    protected abstract float[] grayScaleAndNorm(Bitmap bitmap);


    /**
     * 추후 신경망 모델 업데이트시 Equlization 추가
     * @param bitmap
     * @return
     */
    protected abstract float[] grayScale_Equalization_Norm(Bitmap bitmap);
}