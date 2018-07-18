package com.tzutalin.dlibtest;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;

import java.util.HashMap;

/**
 * Created by taein on 2018-06-14.
 */
public class SensorListener implements SensorEventListener {

    //Roll and Pitch
    private double pitch;
    private double roll;
    private double yaw;

    //timestamp and dt
    private double timestamp;
    private double dt;

    // for radian -> dgree
    private double RAD2DGR = 180 / Math.PI;
    private static final float NS2S = 1.0f/1000000000.0f;

    private static Handler mHandler;
    private SensorDTO mSensorDto = new SensorDTO();

    public static void setHandler(Handler handler) {
        mHandler = handler;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(event.sensor.getType() == Sensor.TYPE_LIGHT){

            mSensorDto.setBr(String.valueOf(event.values[0]));

            mHandler.obtainMessage(1, mSensorDto).sendToTarget();

        }else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){

            /* 각 축의 각속도 성분을 받는다. */
            double gyroX = event.values[0];
            double gyroY = event.values[1];
            double gyroZ = event.values[2];

            /* 각속도를 적분하여 회전각을 추출하기 위해 적분 간격(dt)을 구한다.
             * dt : 센서가 현재 상태를 감지하는 시간 간격
             * NS2S : nano second -> second */
            dt = (event.timestamp - timestamp) * NS2S;
            timestamp = event.timestamp;

            /* 맨 센서 인식을 활성화 하여 처음 timestamp가 0일때는 dt값이 올바르지 않으므로 넘어간다. */
            if (dt - timestamp*NS2S != 0) {

                /* 각속도 성분을 적분 -> 회전각(pitch, roll)으로 변환.
                 * 여기까지의 pitch, roll의 단위는 '라디안'이다.
                 * SO 아래 로그 출력부분에서 멤버변수 'RAD2DGR'를 곱해주어 degree로 변환해줌.  */
                pitch = pitch + gyroY*dt;
                roll = roll + gyroX*dt;
                yaw = yaw + gyroZ*dt;
            }

            mSensorDto.setPitch(String.format("%.1f", pitch*RAD2DGR));
            mSensorDto.setRoll(String.format("%.1f", roll*RAD2DGR));
            mSensorDto.setYaw(String.format("%.1f", yaw*RAD2DGR));

            mHandler.obtainMessage(1, mSensorDto).sendToTarget();

        }else if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){

            mSensorDto.setAccelX(event.values[0]);
            mSensorDto.setAccelZ(event.values[2]);

            mHandler.obtainMessage(0, mSensorDto).sendToTarget();

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}