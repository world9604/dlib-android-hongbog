package com.hongbog.view;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.hongbog.contract.InfoViewContract;
import com.hongbog.viewmodel.InfoViewModel;
import com.tzutalin.dlibtest.CameraActivity;
import com.tzutalin.dlibtest.CameraConnectionFragment;
import com.tzutalin.dlibtest.R;

public class InfoActivity extends AppCompatActivity implements View.OnClickListener{

    private String label;
    private Button button;
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*final ActivityInfoBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_info2);
        final InfoViewModel infoViewModel = new InfoViewModel((InfoViewContract) this);
        binding.setViewModel(infoViewModel);*/

        setContentView(R.layout.activity_info);

        button = (Button) findViewById(R.id.button);
        editText = (EditText) findViewById(R.id.editText);

        button.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id){
            case R.id.button:
                label = editText.getText().toString();
                InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                startCameraActivity(label);
                break;
        }
    }


    public void startCameraActivity(String label) {
        Intent intent = getIntent();
        intent.setClass(this, CameraActivity.class);
        intent.putExtra(CameraConnectionFragment.LABEL_NAME, label);
        startActivity(intent);
        finish();
    }

}
