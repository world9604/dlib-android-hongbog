package com.hongbog.view;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.tzutalin.dlibtest.MainActivity;
import com.tzutalin.dlibtest.R;
import com.tzutalin.dlibtest.ResultProb;
import com.tzutalin.dlibtest.ResultProbList;
import com.tzutalin.dlibtest.TensorFlowClassifier;

import static com.tzutalin.dlibtest.TensorFlowClassifier.HEIGHTS;
import static com.tzutalin.dlibtest.TensorFlowClassifier.WIDTHS;

public class ResultActivity extends AppCompatActivity {

    private TextView mTextView;
    private ImageView mLeftImageView;
    private ImageView mRightImageView;
    private TensorFlowClassifier classifier = TensorFlowClassifier.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        initView();

        // loading activity start
//        startActivity(new Intent(this, LoadingActivity.class));

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        ResultProbList resultProbList = classifier.Verification(bundle);
        setResult(resultProbList);
    }


    private void initView(){
        mTextView = (TextView)findViewById(R.id.label_textview);
        mLeftImageView = (ImageView)findViewById(R.id.detect_eye_left_image);
        mRightImageView = (ImageView)findViewById(R.id.detect_eye_right_image);
    }


   /* @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }*/


    private void setResult(ResultProbList resultProbList){

        if(resultProbList == null) return;

        for (ResultProb resultProb : resultProbList) {

            Bitmap leftBitmap = resultProb.getLeftBitmap();
            Bitmap rightBitmap = resultProb.getRightBitmap();

            Bitmap tmpLeftBitmap = Bitmap.createScaledBitmap(leftBitmap, WIDTHS[0], HEIGHTS[0], false);
            Bitmap tmpRightBitmap = Bitmap.createScaledBitmap(rightBitmap, WIDTHS[0], HEIGHTS[0], false);

            if(tmpLeftBitmap == null || tmpRightBitmap == null) return;

            mRightImageView.setImageBitmap(tmpLeftBitmap);
            mLeftImageView.setImageBitmap(tmpRightBitmap);

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
                sResult = "조원태";
            } else if (result == 1) {
                sResult = "김태인";
            } else if (result == 2) {
                sResult = "길용현";
            } else if (result == 3) {
                sResult = "이재선";
            } else if (result == 4) {
                sResult = "이다희";
            } else if (result == 5) {
                sResult = "남궁희주";
            } else if (result == 6) {
                sResult = "박홍화";
            } else if (result == 7) {
                sResult = "이재원";
            } else if (result == 8) {
                sResult = "대표님";
            }

            if(sResult == null) return;
            mTextView.append(sResult);

            // 5장 중 1장으로만 일단 결과값 출력
            return;
        }
    }
}