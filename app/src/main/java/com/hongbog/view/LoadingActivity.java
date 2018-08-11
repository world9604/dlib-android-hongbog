package com.hongbog.view;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlibtest.R;
import com.tzutalin.dlibtest.TensorFlowClassifier;
import com.victor.loading.rotate.RotateLoading;

import hugo.weaving.DebugLog;

public class LoadingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        RotateLoading rotateLoading = (RotateLoading) findViewById(R.id.rotateloading);
        rotateLoading.start();

        new InitAsyncTask().execute();
    }


    private void initTensorAndFaceDet(){
        //FaceDet.getInstance().setmLandMarkPath(Constants.getFaceShapeModelPath());
        TensorFlowClassifier.getInstance().initTensorFlowAndLoadModel(getAssets());
    }


    /* Checks if external storage is available to at least read */
    @DebugLog
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }


    private class InitAsyncTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {

            if(isExternalStorageReadable()){
                initTensorAndFaceDet();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            finish();
        }
    }
}
