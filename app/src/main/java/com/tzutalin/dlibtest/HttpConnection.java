package com.tzutalin.dlibtest;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.HashMap;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by taein on 2018-07-06.
 */
public class HttpConnection {

    private OkHttpClient client;
    private static HttpConnection instance = new HttpConnection();
    public static HttpConnection getInstance() {
        return instance;
    }

    private HttpConnection(){ this.client = new OkHttpClient(); }

    public String requestUploadPhoto(byte[] BitmapBytes, String label, SensorDTO sensorVales,  Callback callback) {
        try {
            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("photo", label + ".png", RequestBody.create(MediaType.parse("image/png"), BitmapBytes))
                    .addFormDataPart("label", label)
                    .addFormDataPart("roll", sensorVales.getRoll())
                    .addFormDataPart("pitch", sensorVales.getPitch())
                    .addFormDataPart("yaw", sensorVales.getYaw())
                    .addFormDataPart("br", sensorVales.getBr())
                    .build();

            String url = "http://192.168.0.2:8080/uploadImage.do";
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(callback);

        } catch (Exception e) {

            e.printStackTrace();
        }
        return null;
    }

}
