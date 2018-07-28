package com.hongbog.viewmodel;

import android.databinding.ObservableField;
import android.view.View;

import com.hongbog.contract.InfoViewContract;
import com.tzutalin.dlibtest.Dlog;

/**
 * Created by taein on 2018-07-09.
 */
public class InfoViewModel {

    final InfoViewContract infoViewContract;
    public ObservableField<String> label = new ObservableField<>();
    private SensorDTO sensorDTO = new SensorDTO();

    public InfoViewModel(InfoViewContract infoViewContract){
        this.infoViewContract = infoViewContract;
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if(s != null){
            label.set(String.valueOf(s));
        }
    }

    public void loadViewItem(String label){
        this.label.set(label);
    }

    public void startCameraActivity(View v){
        infoViewContract.startCameraActivity(label.get());
    }

    public class SensorDTO {

        private byte[] imageByteArray;
        private double gyroX;
        private double gyroY;
        private double gyroZ;
        private String pitch;
        private String roll;
        private String yaw;
        private double dt;
        private String br;
        private float accelX;
        private float accelY;
        private float accelZ;

        public SensorDTO(){}

        public SensorDTO(byte[] imageByteArray, double gyroX, double gyroY, double gyroZ, String pitch, String roll, String yaw, double dt, String br, float accelX, float accelY, float accelZ) {
            this.imageByteArray = imageByteArray;
            this.gyroX = gyroX;
            this.gyroY = gyroY;
            this.gyroZ = gyroZ;
            this.pitch = pitch;
            this.roll = roll;
            this.yaw = yaw;
            this.dt = dt;
            this.br = br;
            this.accelX = accelX;
            this.accelY = accelY;
            this.accelZ = accelZ;
        }
    }
}
