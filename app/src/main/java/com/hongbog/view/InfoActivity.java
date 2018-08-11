package com.hongbog.view;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.hongbog.contract.InfoViewContract;
import com.hongbog.viewmodel.InfoViewModel;
import com.tzutalin.dlibtest.CameraActivity;
import com.tzutalin.dlibtest.CameraConnectionFragment;
import com.tzutalin.dlibtest.R;
import com.tzutalin.dlibtest.databinding.ActivityInfoBinding;

public class InfoActivity extends AppCompatActivity implements InfoViewContract {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActivityInfoBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_info);
        final InfoViewModel infoViewModel = new InfoViewModel((InfoViewContract) this);
        binding.setViewModel(infoViewModel);
    }

    @Override
    public void showError() {}

    @Override
    public void startCameraActivity(String label) {
        Intent intent = getIntent();
        intent.setClass(this, CameraActivity.class);
        intent.putExtra(CameraConnectionFragment.LABEL_NAME, label);
        startActivity(intent);
        finish();
    }
}
