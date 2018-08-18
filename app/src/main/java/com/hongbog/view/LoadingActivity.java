package com.hongbog.view;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlibtest.Dlog;
import com.tzutalin.dlibtest.R;
import com.tzutalin.dlibtest.TensorFlowClassifier;
import com.victor.loading.rotate.RotateLoading;

import hugo.weaving.DebugLog;

public class LoadingActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSION = 2;

    private static String[] PERMISSIONS_REQ = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        RotateLoading rotateLoading = (RotateLoading) findViewById(R.id.rotateloading);
        rotateLoading.start();

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= Build.VERSION_CODES.M) {
            if(verifyPermissions(this)){
                new InitAsyncTask().execute();
            }
        }
    }


    /*
     *  Checks if the app has permission to write to device storage or open camera
     *  If the app does not has permission then the user will be prompted to grant permissions
     */
    @DebugLog
    private static boolean verifyPermissions(Activity activity) {
        // Check if we have write permission
        int write_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int read_persmission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int camera_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (write_permission != PackageManager.PERMISSION_GRANTED ||
                read_persmission != PackageManager.PERMISSION_GRANTED ||
                camera_permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_REQ,
                    REQUEST_CODE_PERMISSION
            );
            return false;
        } else {
            return true;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (grantResults.length > 1
                            && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                        new InitAsyncTask().execute();

                    }else{
                        Toast.makeText(LoadingActivity.this, "승인 실패", Toast.LENGTH_LONG).show();
                    }

                } else {
                    Toast.makeText(LoadingActivity.this, "승인 실패", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }


    private void initTensorAndFaceDet(){
        Dlog.d("initTensorAndFaceDet");
        Dlog.d("Constants.getFaceShapeModelPath() : " + Constants.getFaceShapeModelPath());
        FaceDet.getInstance().setmLandMarkPath(Constants.getFaceShapeModelPath());
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


    /* Checks if external storage is available for read and write */
    @DebugLog
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    private class InitAsyncTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {

            isExternalStorageReadable();

            if(isExternalStorageWritable()){
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
