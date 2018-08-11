/*
 * Copyright 2016-present Tzutalin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tzutalin.dlibtest;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.hongbog.view.GforceFragment;
import com.hongbog.view.ResultActivity;
import com.hongbog.view.ResultTestActivity;
import com.victor.loading.rotate.RotateLoading;

import java.util.ArrayList;

import static com.tzutalin.dlibtest.MainActivity.ACTIVITY_FLOW_EXTRA;
import static com.tzutalin.dlibtest.MainActivity.DEVELOP_MODE_EXTRA;
import static com.tzutalin.dlibtest.MainActivity.VERIFY_EXTRA;

/**
 * Created by darrenl on 2016/5/20.
 */
public class CameraActivity extends Activity {

    private static final String TAG = "i99";

    private static int OVERLAY_PERMISSION_REQ_CODE = 1;

    private long startTime;

    private RelativeLayout loadingLayout;

    private RotateLoading rotateLoading;

    private FrameLayout gforceFrameLayout;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Dlog.d("onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this.getApplicationContext())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
            }
        }

        startTime = System.currentTimeMillis();

        if (null == savedInstanceState) {
            getFragmentManager()
                .beginTransaction()
                .replace(R.id.detect_container, CameraConnectionFragment.newInstance())
//                .replace(R.id.gforce_container, GforceFragment.newInstance())
//                .replace(R.id.state_container, StateFragment.newInstance())
                .commit();
        }
    }


    public void goMain(Bitmap bitmap_left[], Bitmap bitmap_right[]) {

        ArrayList<ParcelBitmap> left_lst = new ArrayList<>();
        ArrayList<ParcelBitmap> right_lst = new ArrayList<>();

        for (int i = 0; i <5; i++) {
            left_lst.add(new ParcelBitmap( bitmap_left[i] ));
            right_lst.add(new ParcelBitmap( bitmap_right[i] ));
        }

        // 리스트에 이미지 넣기
        Intent intent = getIntent();
        String mode = intent.getStringExtra(ACTIVITY_FLOW_EXTRA);

        if (mode != null && DEVELOP_MODE_EXTRA.equals(mode)) {
            intent.setClass(this, ResultTestActivity.class);
        }else if(mode != null && VERIFY_EXTRA.equals(mode)){
            intent.setClass(this, ResultActivity.class);
        }else{
            return;
        }

        Bundle bundle = new Bundle();

        // 데이터 전달
        bundle.putParcelableArrayList("LeftEyeList", left_lst);
        bundle.putParcelableArrayList("RightEyeList", right_lst);
        intent.putExtras(bundle);
        startActivity(intent);
        finish();

        long endTime = System.currentTimeMillis();
        long verificationtime = (endTime-startTime)/100;
        Log.i(TAG, "Time= "+String.valueOf(verificationtime));
    }


    @Override
    protected void onStart() {
        Dlog.d("onStart");
        super.onStart();
    }


    @Override
    protected void onPause() {
        Dlog.d("onPause");
        super.onPause();
    }


    @Override
    protected void onStop() {
        Dlog.d("onStop");
        super.onStop();
    }


    @Override
    protected void onResume() {
        Dlog.d("onResume");
        super.onResume();
    }


    @Override
    protected void onDestroy() {
        Dlog.d("onDestroy");
        super.onDestroy();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this.getApplicationContext())) {
                    Dlog.d("CameraActivity onActivityResult");
                    Toast.makeText(CameraActivity.this, "CameraActivity\", \"SYSTEM_ALERT_WINDOW, permission not granted...", Toast.LENGTH_SHORT).show();
                } else {
                    Dlog.d("Settings.canDrawOverlays!!!");
                    Toast.makeText(CameraActivity.this, "Restart CameraActivity", Toast.LENGTH_SHORT).show();
                    Intent intent = getIntent();
                    startActivity(intent);
                    finish();
                }
            }
        }
    }


    public void stopLoadingAnimation() {

        Dlog.d("stopLoadingAnimation");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rotateLoading.stop();
                loadingLayout.setVisibility(View.GONE);
                gforceFrameLayout.setVisibility(View.VISIBLE);
            }
        });
    }
}