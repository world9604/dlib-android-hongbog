package com.tzutalin.dlibtest;

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

/**
 * Created by taein on 2018-07-16.
 */

public class GforceFragment extends Fragment {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private SensorListener mSensorLis;
    private HandlerThread sensorThread;
    private Handler sensorHandler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorLis = new SensorListener();

        sensorThread = new HandlerThread("SensorThread");
        sensorThread.start();
        sensorHandler = new Handler(sensorThread.getLooper());

        mSensorManager.registerListener(mSensorLis, mAccelerometer, SensorManager.SENSOR_DELAY_GAME, sensorHandler);
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(mSensorLis, mAccelerometer);
        sensorThread.quitSafely();

        try {
            sensorThread.join();
            sensorThread = null;
            sensorHandler = null;
        } catch (InterruptedException e) {
            Dlog.d("error : " + e.getMessage());
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gforce, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public static GforceFragment newInstance() {
        // 다른 Fragment View 및 데이터로 교체시 사용
        //new GforceFragment().setArguments(new Bundle());
        return new GforceFragment();
    }
}