package com.tzutalin.dlibtest;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.os.TraceCompat;
import android.util.Log;

import com.hongbog.contract.TensorFlowClassifierContract;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TensorFlowSimilarityClassifier {
    private static final String TAG = "TFClassifier";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    /**
     * Tensorflow 관련 파라미터 설정
     */
    private static final String[] RIGHT_INPUT_NAMES = {"right/input_module/ori_low_res_X", "right/input_module/query_low_res_X",
            "right/input_module/ori_mid_res_X", "right/input_module/query_mid_res_X",
            "right/input_module/ori_high_res_X", "right/input_module/query_high_res_X"};
    private static final String[] LEFT_INPUT_NAMES = {"left/input_module/ori_low_res_X", "left/input_module/query_low_res_X",
            "left/input_module/ori_mid_res_X", "left/input_module/query_mid_res_X",
            "left/input_module/ori_high_res_X", "left/input_module/query_high_res_X"};
    private static final String[] OUTPUT_NAMES = {"right/output_module/softmax","left/output_module/softmax"};

    private static final int[] WIDTHS = {100, 150, 200};
    private static final int[] HEIGHTS = {46, 70, 92};
    private static final int BATCH_SIZE = 5;
    private static final String MODEL_FILE = "file:///android_asset/model_graph_similarity.pb";
    private static final String LABEL_FILE = "file:///android_asset/label_strings_similarity.txt";
    private static final int MULTISCALE_CNT = 3;

    /**
     * 입력 데이터 셋
     */
    private float[] rightLowOriData = new float[(WIDTHS[0] * HEIGHTS[0]) * BATCH_SIZE];
    private float[] rightLowQueryData = new float[(WIDTHS[0] * HEIGHTS[0]) * BATCH_SIZE];
    private float[] rightMidOriData = new float[(WIDTHS[1] * HEIGHTS[1]) * BATCH_SIZE];
    private float[] rightMidQueryData = new float[(WIDTHS[1] * HEIGHTS[1]) * BATCH_SIZE];
    private float[] rightHighOriData = new float[(WIDTHS[2] * HEIGHTS[2]) * BATCH_SIZE];
    private float[] rightHighQueryData = new float[(WIDTHS[2] * HEIGHTS[2]) * BATCH_SIZE];

    private float[] leftLowOriData = new float[(WIDTHS[0] * HEIGHTS[0]) * BATCH_SIZE];
    private float[] leftLowQueryData = new float[(WIDTHS[0] * HEIGHTS[0]) * BATCH_SIZE];
    private float[] leftMidOriData = new float[(WIDTHS[1] * HEIGHTS[1]) * BATCH_SIZE];
    private float[] leftMidQueryData = new float[(WIDTHS[1] * HEIGHTS[1]) * BATCH_SIZE];
    private float[] leftHighOriData = new float[(WIDTHS[2] * HEIGHTS[2]) * BATCH_SIZE];
    private float[] leftHighQueryData = new float[(WIDTHS[2] * HEIGHTS[2]) * BATCH_SIZE];

    private Executor executor = Executors.newSingleThreadExecutor();

    private TensorFlowClassifier classifier = TensorFlowClassifier.getInstance();

    private static final int MAX_RESULTS = 3;  // result 개수 제한
    private static final float THRESHOLD = 0.1f;  // outputs 값의 threshold 설정

    // neural network 관련 parameters
    private String[] rightInputNames;  // neural network right 입력 노드 이름
    private String[] leftInputNames;  // neural network left 입력 노드 이름
    private String[] outputNames;  // neural network 출력 노드 이름
    private int[] widths;  // 입력 이미지 가로 길이
    private int[] heights;  // 입력 이미지 세로 길이
    private Vector<String> labels = new Vector<>();  // label 정보
    private int numClasses = 2;
    private int batchSize = 5;
    private float[][] logits = new float[2][numClasses * batchSize];  // logit 정보
    private boolean runStats = false;

    private TensorFlowInferenceInterface tii;

    private AssetManager mAssetManager = null;

    private TensorFlowSimilarityClassifier() {}

    private static class SimilarityClassifierSingleToneHolder {
        static final TensorFlowSimilarityClassifier instance = new TensorFlowSimilarityClassifier();
    }

    public static TensorFlowSimilarityClassifier getInstance() {
        return SimilarityClassifierSingleToneHolder.instance;
    }

    /**
     * 텐서플로우 classifier 초기화 함수
     */
    public void initTensorFlowAndLoadModel(AssetManager assetManager) {

        if(mAssetManager == null){
            mAssetManager = assetManager;

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        classifier.createClassifier(
                                mAssetManager,
                                MODEL_FILE,
                                LABEL_FILE,
                                WIDTHS,
                                HEIGHTS,
                                RIGHT_INPUT_NAMES,
                                LEFT_INPUT_NAMES,
                                OUTPUT_NAMES);
                        Log.d(TAG, "Load Success");
                    } catch (final Exception e) {
                        throw new RuntimeException("Error initializing TensorFlow!", e);
                    }
                }
            });
        }
    }

    /**
     * 텐서플로우 classifier 생성 관련 초기화 함수
     * @param assetManager
     * @param modelFilename
     * @param labelFilename
     * @param widhts
     * @param heights
     * @param rightInputNames
     * @param leftInputNames
     * @param outputNames
     */
    public void createClassifier(
            AssetManager assetManager,
            String modelFilename,
            String labelFilename,
            int[] widhts,
            int[] heights,
            String[] rightInputNames,
            String[] leftInputNames,
            String[] outputNames) {
        this.rightInputNames = rightInputNames;
        this.leftInputNames = leftInputNames;
        this.outputNames = outputNames;
        this.widths = widhts;
        this.heights = heights;

        // label names 설정
        BufferedReader br = null;
        try {
            String actualFilename = labelFilename.split("file:///android_asset/")[1];
            br = new BufferedReader(new InputStreamReader(assetManager.open(actualFilename)));
            String line = "";
            while((line = br.readLine()) != null) {
                this.labels.add(line);
            }
        } catch (IOException e) {
            Log.d(TAG, e.toString());
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                Log.d(TAG, e.toString());
            }
        }

        this.tii = new TensorFlowInferenceInterface(assetManager, modelFilename);
    }


    /**
     * 오른쪽/왼쪽 눈에 대한 verification 을 수행하는 함수
     * @param rightLowOriData
     * @param rightLowQueryData
     * @param rightMidOriData
     * @param rightMidQueryData
     * @param rightHighOriData
     * @param rightHighQueryData
     * @param leftLowOriData
     * @param leftLowQueryData
     * @param leftMidOriData
     * @param leftMidQueryData
     * @param leftHighOriData
     * @param leftHighQueryData
     * @return
     */
    public int[] verificationEye(float[] rightLowOriData, float[] rightLowQueryData,
                               float[] rightMidOriData, float[] rightMidQueryData,
                               float[] rightHighOriData, float[] rightHighQueryData,
                               float[] leftLowOriData, float[] leftLowQueryData,
                               float[] leftMidOriData, float[] leftMidQueryData,
                               float[] leftHighOriData, float[] leftHighQueryData) {
        long startTime = System.currentTimeMillis();

        TraceCompat.beginSection("verification");

        TraceCompat.beginSection("feed");
        for (int i = 0; i < this.widths.length; i++) {
            if (i == 0) {
                tii.feed(this.rightInputNames[i*2], rightLowOriData, 5, this.heights[i], this.widths[i], 1);
                tii.feed(this.rightInputNames[(i*2)+1], rightLowQueryData, 5, this.heights[i], this.widths[i], 1);
                tii.feed(this.leftInputNames[i*2], leftLowOriData, 5, this.heights[i], this.widths[i], 1);
                tii.feed(this.leftInputNames[(i*2)+1], leftLowQueryData, 5, this.heights[i], this.widths[i], 1);
            } else if (i == 1) {
                tii.feed(this.rightInputNames[i*2], rightMidOriData, 5, this.heights[i], this.widths[i], 1);
                tii.feed(this.rightInputNames[(i*2)+1], rightMidQueryData, 5, this.heights[i], this.widths[i], 1);
                tii.feed(this.leftInputNames[i*2], leftMidOriData, 5, this.heights[i], this.widths[i], 1);
                tii.feed(this.leftInputNames[(i*2)+1], leftMidQueryData, 5, this.heights[i], this.widths[i], 1);
            } else {
                tii.feed(this.rightInputNames[i*2], rightHighOriData, 5, this.heights[i], this.widths[i], 1);
                tii.feed(this.rightInputNames[(i*2)+1], rightHighQueryData, 5, this.heights[i], this.widths[i], 1);
                tii.feed(this.leftInputNames[i*2], leftHighOriData, 5, this.heights[i], this.widths[i], 1);
                tii.feed(this.leftInputNames[(i*2)+1], leftHighQueryData, 5, this.heights[i], this.widths[i], 1);
            }
        }
        TraceCompat.endSection();

        TraceCompat.beginSection("run");
        tii.run(this.outputNames, this.runStats);
        TraceCompat.endSection();

        TraceCompat.beginSection("fetch");
        for (int i = 0; i < this.outputNames.length; i++) {
            float[] outputs = new float[this.numClasses * this.batchSize];
            tii.fetch(this.outputNames[i], outputs);
            this.logits[i] = outputs;
        }
        TraceCompat.endSection();

        float[][] mergeResult = {{0f, 0f}, {0f, 0f}, {0f, 0f}, {0f, 0f}, {0f, 0f}};
        for (int i = 0; i < this.logits.length; i++) {
            Log.d(TAG, outputNames[i]);
            for (int j = 0; j < this.logits[0].length; j=j+2) {
                mergeResult[(int)(j/2)][0] += logits[i][j];
                mergeResult[(int)(j/2)][1] += logits[i][j+1];
                Log.d(TAG, this.logits[i][j]+", "+this.logits[i][j+1]);
            }
        }

        String temp = "";
        for (int i = 0; i < mergeResult.length; i++) {
            temp += "(" + mergeResult[i][0] + "," + mergeResult[i][1] + ") ";
        }
        Log.d(TAG, temp);

        int result[] = new int[this.batchSize];
        for (int i = 0; i < mergeResult.length; i++) {
            if (mergeResult[i][0] > mergeResult[i][1]) {
                result[i] = 0;
            } else {
                result[i] = 1;
            }
        }
        long endTime = System.currentTimeMillis();

        Log.d(TAG, (endTime-startTime) + " ms");
        return result;
    }


    private float[] grayScaleAndNorm(Bitmap bitmap) {
        int mWidth = bitmap.getWidth();
        int mHeight = bitmap.getHeight();

        int[] ori_pixels = new int[mWidth * mHeight];
        float[] norm_pixels = new float[mWidth * mHeight];

        bitmap.getPixels(ori_pixels, 0, mWidth, 0, 0, mWidth, mHeight);
        for (int i = 0; i < ori_pixels.length; i++) {
            int alpha = Color.alpha(ori_pixels[i]);
//            int grayPixel = (int) ((Color.red(ori_pixels[i]) * 0.2126) + (Color.green(ori_pixels[i]) * 0.7152) + (Color.blue(ori_pixels[i]) * 0.0722));
            int grayPixel = (int) ((Color.red(ori_pixels[i]) * 0.299) + (Color.green(ori_pixels[i]) * 0.587) + (Color.blue(ori_pixels[i]) * 0.114));  // decode_png -> grayscale 변환과 일치
            if (grayPixel < 0) grayPixel = 0;
            if (grayPixel > 255) grayPixel = 255;
            norm_pixels[i] = grayPixel / 255.0f;
        }
        return norm_pixels;
    }


    public void mergeArray(float[] mergeArr, float[] arr1, float[] arr2, float[] arr3, float[] arr4, float[] arr5) {
        int arrLen = arr1.length;

        for (int i=0; i<mergeArr.length; i++) {
            switch ((int)(i / arrLen)) {
                case 0:
                    mergeArr[i] = arr1[i % arrLen];
                    break;
                case 1:
                    mergeArr[i] = arr2[i % arrLen];
                    break;
                case 2:
                    mergeArr[i] = arr3[i % arrLen];
                    break;
                case 3:
                    mergeArr[i] = arr4[i % arrLen];
                    break;
                case 4:
                    mergeArr[i] = arr5[i % arrLen];
                    break;
            }
        }
    }
}
