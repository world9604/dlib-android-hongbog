package com.hongbog.view;

import android.app.Fragment;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tzutalin.dlibtest.Dlog;
import com.tzutalin.dlibtest.R;
import com.tzutalin.dlibtest.SensorListener;

import org.w3c.dom.Text;

/**
 * Created by taein on 2018-07-16.
 */

public class StateFragment extends Fragment {

    private TextView mTextView;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_state, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    public static StateFragment newInstance() {
        // 다른 Fragment View 및 데이터로 교체시 사용
        // new GforceFragment().setArguments(new Bundle());
        return new StateFragment();
    }
}