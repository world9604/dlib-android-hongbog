package com.tzutalin.dlibtest;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.hongbog.view.InfoActivity;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import hugo.weaving.DebugLog;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PERMISSION = 2;
    public static final String ACTIVITY_FLOW_EXTRA = "ACTIVITY_FLOW_EXTRA";
    public static final String VERIFY_EXTRA = "VERIFY_EXTRA";
    public static final String ENROLL_EXTRA = "ENROLL_EXTRA";
    public static final String DEVELOP_MODE_EXTRA = "DEVELOP_MODE_EXTRA";
    // Storage Permissions
    private static String[] PERMISSIONS_REQ = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 로딩과 동시에 신경망 초기화
        TensorFlowClassifier.getInstance().initTensorFlowAndLoadModel(getAssets());

        initView();

        isExternalStorageWritable();
        isExternalStorageReadable();

        // For API 23+ you need to request the read/write permissions even if they are already in your manifest.
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= Build.VERSION_CODES.M) {
            if(verifyPermissions(this)){
                /*Intent intent = new Intent(this, InfoActivity.class);
                finish();
                startActivity(intent);*/
            }
        }
    }


    private void initView(){
        Button verifyBtn = (Button)findViewById(R.id.verify_btn);
        Button enrollBtn = (Button)findViewById(R.id.enroll_btn);

        verifyBtn.setOnClickListener(this);
        enrollBtn.setOnClickListener(this);
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

                    }else{
                        Toast.makeText(MainActivity.this, "승인 실패", Toast.LENGTH_LONG).show();
                    }

                } else {
                    Toast.makeText(MainActivity.this, "승인 실패", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();

        Intent intent = new Intent();

        switch (id){
            case R.id.verify_btn:
                Dlog.d("verify_btn");
                intent.setClass(this, CameraActivity.class);
                intent.putExtra(ACTIVITY_FLOW_EXTRA, VERIFY_EXTRA);
                break;
            case R.id.enroll_btn:
                Dlog.d("enroll_btn");
                intent.setClass(this, InfoActivity.class);
                intent.putExtra(ACTIVITY_FLOW_EXTRA, ENROLL_EXTRA);
                break;
        }

        Dlog.d("onClick");
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_list, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int selectedId = item.getItemId();

        switch (selectedId){
            case R.id.developer_mode :
                Intent intent = new Intent(this, InfoActivity.class);
                intent.putExtra(ACTIVITY_FLOW_EXTRA, DEVELOP_MODE_EXTRA);
                Dlog.d("onOptionsItemSelected");
                finish();
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
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
}