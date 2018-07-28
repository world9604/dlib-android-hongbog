package com.hongbog.view;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.tzutalin.dlibtest.CameraActivity;
import com.tzutalin.dlibtest.MainActivity;
import com.tzutalin.dlibtest.ParcelBitmap;
import com.tzutalin.dlibtest.ParcelBitmapList;
import com.tzutalin.dlibtest.R;
import com.tzutalin.dlibtest.ResultProb;
import com.tzutalin.dlibtest.ResultProbList;
import com.tzutalin.dlibtest.TensorFlowClassifier;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by taein on 2018-07-26.
 */

public class ResultTestActivity extends AppCompatActivity{

    // Tensorflow parameter
    // v1(ResNet)
    //    private static final String[] RIGHT_INPUT_NAMES = {"right/input_scope/low_res_X", "right/input_scope/mid_res_X", "right/input_scope/high_res_X"};
    //    private static final String[] LEFT_INPUT_NAMES = {"left/input_scope/low_res_X", "left/input_scope/mid_res_X", "left/input_scope/high_res_X"};
    //    private static final String[] OUTPUT_NAMES = {"right_1/softmax", "left_1/softmax"};
    // v2(mobilenet v2)
    private static final String[] RIGHT_INPUT_NAMES = {"right/input_module/low_res_X","right/input_module/mid_res_X","right/input_module/high_res_X"};
    private static final String[] LEFT_INPUT_NAMES = {"left/input_module/low_res_X","left/input_module/mid_res_X","left/input_module/high_res_X"};
    private static final String[] OUTPUT_NAMES = {"right/output_module/softmax","left/output_module/softmax"};

    private static final int[] WIDTHS = {160, 200, 240};
    private static final int[] HEIGHTS = {60, 80, 100};
    private static final String MODEL_FILE = "file:///android_asset/model_graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/label_strings.txt";
    private static final int MULTISCALE_CNT = 3;

    Bitmap bitmap_left[] = new Bitmap[5];
    Bitmap bitmap_right[] = new Bitmap[5];

    float[] lowRightData = new float[WIDTHS[0] * HEIGHTS[0]];
    float[] midRightData = new float[WIDTHS[1] * HEIGHTS[1]];
    float[] highRightData = new float[WIDTHS[2] * HEIGHTS[2]];
    float[] lowLeftData = new float[WIDTHS[0] * HEIGHTS[0]];
    float[] midLeftData = new float[WIDTHS[1] * HEIGHTS[1]];
    float[] highLeftData = new float[WIDTHS[2] * HEIGHTS[2]];

    Intent intent;
    Bundle bundle;

    // UI
    private ImageView rightLowImg;
    private ImageView leftLowImg;
    private ImageView rightMidImg;
    private ImageView leftMidImg;
    private ImageView rightHighImg;
    private ImageView leftHighImg;
    private Button galleryBtn;
    private Button verificationBtn;
    private Button grayscaleBtn;
    private TextView resultClassText;
    private TextView resultText;

    //    private int[] testData = new int[WIDTHS[0] * HEIGHTS[0]];
    Bitmap bmpGrayScale = Bitmap.createBitmap(WIDTHS[0], HEIGHTS[0], Bitmap.Config.ARGB_4444);

    private Executor executor = Executors.newSingleThreadExecutor();
    private TensorFlowClassifier classifier = TensorFlowClassifier.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_test);

        intent = getIntent();
        bundle = intent.getExtras();

        rightLowImg = (ImageView) findViewById(R.id.imgLowRight);
        leftLowImg = (ImageView) findViewById(R.id.imgLowLeft);
        rightMidImg = (ImageView) findViewById(R.id.imgMidRight);
        leftMidImg = (ImageView) findViewById(R.id.imgMidLeft);
        rightHighImg = (ImageView) findViewById(R.id.imgHighRight);
        leftHighImg = (ImageView) findViewById(R.id.imgHighLeft);

        galleryBtn = (Button) findViewById(R.id.btnGallery);
        galleryBtn.setOnClickListener(new ResultTestActivity.ButtonEventHandler());
        verificationBtn = (Button) findViewById(R.id.btnVerification);
        verificationBtn.setOnClickListener(new ResultTestActivity.ButtonEventHandler());

        resultText = (TextView) findViewById(R.id.textResult);
        resultClassText = (TextView) findViewById(R.id.textResultClass);
        initTensorFlowAndLoadModel();
    }


    /**
     * 텐서플로우 classifier 초기화 함수
     */
    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier.createClassifier(
                            getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            WIDTHS,
                            HEIGHTS,
                            RIGHT_INPUT_NAMES,
                            LEFT_INPUT_NAMES,
                            OUTPUT_NAMES);
                    //Log.i(TAG, "Load Success");
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }


    /**
     * 버튼 이벤트를 처리하는 함수
     */
    public class ButtonEventHandler implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            int objectID = v.getId();

            if (objectID == R.id.btnGallery) {

                // 눈 영상 촬영하러 감(Camera Activity)
                Intent intent1 = new Intent(ResultTestActivity.this, CameraActivity.class);
                finish();
                startActivity(intent1);

            } else if (objectID == R.id.btnVerification) {
                Verification();
            }
            //rightImg.setImageBitmap(bmpGrayScale);
            //rightImg.setImageBitmap(BitmapFactory.decodeByteArray(testData, 0, WIDTHS[0] * HEIGHTS[0]));
        }
    }


    public void Verification() {
        long startTime = System.currentTimeMillis();

        ParcelBitmapList leftList = new ParcelBitmapList();
        ParcelBitmapList rightList = new ParcelBitmapList();

        ArrayList<ParcelBitmap> left = bundle.getParcelableArrayList("LeftEyeList");
        ArrayList<ParcelBitmap> right = bundle.getParcelableArrayList("RightEyeList");

        // 리스트에서 이미지 꺼내오기
        for (int num = 0; num < 5; num++) {
            ParcelBitmap addData = left.get(num);
            bitmap_left[num] = addData.getBitmap();

            ParcelBitmap addDataa = right.get(num);
            bitmap_right[num] = addDataa.getBitmap();
        }

        ResultProbList resultList = new ResultProbList();

        for (int num = 0; num < 5; num++) {
            ResultProb resultPro = new ResultProb();

            Bitmap oriLeftBitmap = bitmap_left[num];
            Bitmap oriRightBitmap = bitmap_right[num];
            /*Log.d(TAG, "right: (" + oriRightBitmap.getWidth() + "," + oriRightBitmap.getHeight() + "), " +
                    "left: (" + oriLeftBitmap.getWidth() + "," + oriLeftBitmap.getHeight() + ")");*/


            for (int i = 0; i < MULTISCALE_CNT; i++) {

                Bitmap tmpLeftBitmap = Bitmap.createScaledBitmap(oriLeftBitmap, WIDTHS[i], HEIGHTS[i], false);
                Bitmap tmpRightBitmap = Bitmap.createScaledBitmap(oriRightBitmap, WIDTHS[i], HEIGHTS[i], false);

                /*Log.d(TAG, i + "right: (" + tmpRightBitmap.getWidth() + "," + tmpRightBitmap.getHeight() + "), " +
                        "left: (" + tmpLeftBitmap.getWidth() + "," + tmpLeftBitmap.getHeight() + ")");*/

                if (i == 0) {
                    lowRightData = grayScaleAndNorm(tmpRightBitmap);
                    lowLeftData = grayScaleAndNorm(tmpLeftBitmap);
                    rightLowImg.setImageBitmap(tmpRightBitmap);
                    leftLowImg.setImageBitmap(tmpLeftBitmap);
                } else if (i == 1) {
                    midRightData = grayScaleAndNorm(tmpRightBitmap);
                    midLeftData = grayScaleAndNorm(tmpLeftBitmap);
                    rightMidImg.setImageBitmap(tmpRightBitmap);
                    leftMidImg.setImageBitmap(tmpLeftBitmap);
                } else {
                    highRightData = grayScaleAndNorm(tmpRightBitmap);
                    highLeftData = grayScaleAndNorm(tmpLeftBitmap);
                    rightHighImg.setImageBitmap(tmpRightBitmap);
                    leftHighImg.setImageBitmap(tmpLeftBitmap);
                }
            }
            float[] tempResult = classifier.verificationEye(lowRightData, midRightData, highRightData, lowLeftData, midLeftData, highLeftData);
            resultPro.setProbResult(tempResult);
            resultList.add(resultPro);

            // 확률이 가장 큰 클래스 고르기
            float maxValue = -1;
            int result = -1;
            for (int i = 0; i < 7; i++) {
                if (maxValue < tempResult[i]) {
                    maxValue = tempResult[i];
                    result = i;
                }
            }
            String sResult = "ㅁ";
            if (result == 0) {
                sResult = "조원태씨~";
            } else if (result == 1) {
                sResult = "김태인씨~";
            } else if (result == 2) {
                sResult = "길용현씨~";
            } else if (result == 3) {
                sResult = "이재선씨~";
            } else if (result == 4) {
                sResult = "이다희씨~";
            } else if (result == 5) {
                sResult = "남궁희주씨~";
            } else if (result == 6) {
                sResult = "박홍화씨~";
            } else if (result == 7) {
                sResult = "이재원씨~";
            } else if (result == 8) {
                sResult = "대표님~~";
            }
            if(num==3){resultClassText.append("\n");}
            resultClassText.append(String.valueOf(num) +": " + sResult+ " / ");
        }

        for (int num = 0; num < 5; num++) {
            resultText.append(String.valueOf(num) + ": \n");

            ResultProb resultPro = resultList.get(num);
            float[] pro = resultPro.getProbResult();
            for (int i = 0; i < 7; i++) {
                if(i==3){resultText.append("\n");}
                resultText.append( i + " :"+ String.format("%.2f", pro[i]) + " / ");
            }
            resultText.append("\n");
        }

        long endTime = System.currentTimeMillis();
        long verificationtime = (endTime-startTime)/100;
        resultClassText.append("\nVerification Time: "+String.valueOf(verificationtime));
    }


    private static float[] grayScaleAndNorm(Bitmap bitmap) {
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

}
