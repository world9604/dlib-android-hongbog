package com.hongbog.view;

import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.tzutalin.dlibtest.Dlog;
import com.tzutalin.dlibtest.R;

import static com.tzutalin.dlibtest.CameraConnectionFragment.ENROLL_INPUT_DATA_SIZE;
import static com.tzutalin.dlibtest.CameraConnectionFragment.VERIFY_INPUT_DATA_SIZE;


public class SettingActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText verify_edit_text;
    private EditText enroll_edit_text;
    private Button settingBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        initView();
    }

    private void initView(){
        verify_edit_text = (EditText) findViewById(R.id.verification_edit_text);
        enroll_edit_text = (EditText) findViewById(R.id.enroll_edit_text);
        settingBtn = (Button) findViewById(R.id.setting_btn);
        settingBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        switch (viewId){
            case R.id.setting_btn:
                try{
                    setInputDataSize();
                }catch (NumberFormatException ex){
                    return;
                }
                break;
        }

        finish();
    }

    @Override
    public void onBackPressed() {
        setInputDataSize();
        super.onBackPressed();
    }

    private void setInputDataSize(){
        try{
            ENROLL_INPUT_DATA_SIZE = Integer.parseInt(String.valueOf(enroll_edit_text.getText()));
            VERIFY_INPUT_DATA_SIZE = Integer.parseInt(String.valueOf(verify_edit_text.getText()));
            Dlog.d("ENROLL_INPUT_DATA_SIZE : " + ENROLL_INPUT_DATA_SIZE);
            Dlog.d("VERIFY_INPUT_DATA_SIZE : " + VERIFY_INPUT_DATA_SIZE);
        }catch (NumberFormatException ex){
            Dlog.e("NumberFormatException Message : " +  ex.getMessage());
            Toast.makeText(this, R.string.not_number_format, Toast.LENGTH_LONG).show();
            throw ex;
        }
    }

}
