package com.hongbog.view;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.tzutalin.dlibtest.CameraActivity;
import com.tzutalin.dlibtest.ParcelBitmap;
import com.tzutalin.dlibtest.ParcelBitmapList;
import com.tzutalin.dlibtest.R;
import com.tzutalin.dlibtest.ResultProb;
import com.tzutalin.dlibtest.ResultProbList;
import com.tzutalin.dlibtest.TensorFlowClassifier;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.tzutalin.dlibtest.TensorFlowClassifier.HEIGHTS;
import static com.tzutalin.dlibtest.TensorFlowClassifier.MULTISCALE_CNT;
import static com.tzutalin.dlibtest.TensorFlowClassifier.WIDTHS;

/**
 * Created by taein on 2018-07-26.
 */

public class ResultTestActivity extends AppCompatActivity{

    Intent intent;
    Bundle bundle;

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
                //Intent intent1 = getIntent();
                //intent1.setClass(ResultTestActivity.this, CameraActivity.class);
                finish();
                //startActivity(intent1);

            } else if (objectID == R.id.btnVerification) {
                ResultProbList resultProbList = classifier.Verification(bundle);
                setResult(resultProbList);
            }
            //rightImg.setImageBitmap(bmpGrayScale);
            //rightImg.setImageBitmap(BitmapFactory.decodeByteArray(testData, 0, WIDTHS[0] * HEIGHTS[0]));
        }
    }


    private void setResult(ResultProbList resultProbList){

        if(resultProbList == null) return;

        long verificationtime = resultProbList.getVerificationtime();

        int num = 0;

        for(ResultProb resultProb : resultProbList){

            Bitmap leftBitmap = resultProb.getLeftBitmap();
            Bitmap rightBitmap = resultProb.getRightBitmap();

            for (int i = 0; i < MULTISCALE_CNT; i++) {

                if(leftBitmap == null || rightBitmap == null) return;

                Bitmap tmpLeftBitmap = Bitmap.createScaledBitmap(leftBitmap, WIDTHS[i], HEIGHTS[i], false);
                Bitmap tmpRightBitmap = Bitmap.createScaledBitmap(rightBitmap, WIDTHS[i], HEIGHTS[i], false);

                if (i == 0) {
                    rightLowImg.setImageBitmap(tmpRightBitmap);
                    leftLowImg.setImageBitmap(tmpLeftBitmap);
                } else if (i == 1) {
                    rightMidImg.setImageBitmap(tmpRightBitmap);
                    leftMidImg.setImageBitmap(tmpLeftBitmap);
                } else {
                    rightHighImg.setImageBitmap(tmpRightBitmap);
                    leftHighImg.setImageBitmap(tmpLeftBitmap);
                }
            }

            float[] tempResult = resultProb.getProbResult();

            // 확률이 가장 큰 클래스 고르기
            float maxValue = -1;
            int result = -1;
            for (int i = 0; i < 7; i++) {
                if (maxValue < tempResult[i]) {
                    maxValue = tempResult[i];
                    result = i;
                }
            }

            String sResult = null;
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

            if(num==3) resultClassText.append("\n");

            if(sResult == null) return;
            resultClassText.append(String.valueOf(num) + ": " + sResult + " / ");
            num++;
        }

        for (int num2 = 0; num2 < 5; num2++) {

            resultText.append(String.valueOf(num2) + ": \n");

            ResultProb resultPro = resultProbList.get(num2);

            float[] pro = resultPro.getProbResult();

            for (int i = 0; i < 7; i++) {
                if(i==3){resultText.append("\n");}
                resultText.append( i + " :"+ String.format("%.2f", pro[i]) + " / ");
            }

            resultText.append("\n");
        }

        resultClassText.append("\nVerification Time: "+String.valueOf(verificationtime));
    }
}
