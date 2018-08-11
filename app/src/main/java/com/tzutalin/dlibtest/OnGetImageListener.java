package com.tzutalin.dlibtest;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Trace;
import android.text.Layout;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hongbog.view.CustomView;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;

import junit.framework.Assert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.interfaces.DSAKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


// preview "1) 프레임을 가져 와서",   이미지를 "2)비트 맵으로 변환"하여   dlib lib로 처리하는 클래스
public class OnGetImageListener implements OnImageAvailableListener {

    private static final int INPUT_SIZE = 720; //500;//500; //224;
    private static final String TAG = "i99";

    private int mScreenRotation = 90;
    private int mPreviewWdith = 0;
    private int mPreviewHeight = 0;
    private byte[][] mYUVBytes;
    private int[] mRGBBytes = null;
    private Bitmap mRGBframeBitmap = null;
    private Bitmap mBitmap = null;

    private boolean mIsComputing = false;
    private Handler mInferenceHandler;
    private Handler mBackgroundHandler;

    private float mOverlayStartLeftX = 0;
    private float mOverlayStartLeftY = 0;
    private float mOverlayStartRightX = 0;
    private float mOverlayStartRightY = 0;
    private float mOverlayEye2Eye = 0;
    private float mOverlayEyeHeight = 0;
    private float mOverlayEyeWidth = 0;
    private float mOverlayEndLeftX = 0;
    private float mOverlayEndLeftY = 0;
    private float mOverlayEndRightX = 0;
    private float mOverlayEndRightY = 0;

    private Context mContext;
    private FaceDet mFaceDet;

    int mNumCrop = 0;
    Bitmap bitmap_left[] = new Bitmap[6];
    Bitmap bitmap_right[]= new Bitmap[6];

    private Paint mFaceLandmardkPaint;
    private HttpConnection httpConn = HttpConnection.getInstance();
    private SensorDTO mSensorDTO = new SensorDTO();
    private SensorChangeHandler mSensorChangeHandler;
    private CustomView mEyeOverlayView;
    private int mTextureViewWidth;
    private int mTextureViewHeight;


    public OnGetImageListener() {
        mSensorChangeHandler = new SensorChangeHandler();
        SensorListener.setHandler(mSensorChangeHandler);
    }


    public void initialize(final Context context, final AssetManager assetManager,
                           final Handler inferenceHandler , final Handler backgroundHandler,
                           final String label, final CustomView eyeOverlayView, final AutoFitTextureView textureView) {
        Dlog.d("initialize");

        this.mContext = context;
        this.mInferenceHandler = inferenceHandler;
        this.mBackgroundHandler = backgroundHandler;
//        mFaceDet = FaceDet.getInstance();
        mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());

        mFaceLandmardkPaint = new Paint();
        mFaceLandmardkPaint.setColor(Color.GREEN);
        mFaceLandmardkPaint.setStrokeWidth(2);
        mFaceLandmardkPaint.setStyle(Paint.Style.STROKE);

        mSensorDTO.setLabel(label);

        mTextureViewWidth = textureView.getRatioWidth();
        mTextureViewHeight = textureView.getRatioHeight();

        mEyeOverlayView = eyeOverlayView;

        // loading animation stop
        //mBackgroundHandler.obtainMessage(CameraConnectionFragment.LOAD_VIEW_COMPLETE).sendToTarget();
    }


    public void deInitialize() {
        Dlog.d("deInitialize");
        synchronized (OnGetImageListener.this) {
            if (mFaceDet != null) {
                mFaceDet.release();
            }
        }
    }


    private void drawResizedBitmap(final Bitmap src, final Bitmap dst) {
        Display getOrient = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int orientation = Configuration.ORIENTATION_UNDEFINED;
        Point point = new Point();
        getOrient.getSize(point);
        int screen_width = point.x;
        int screen_height = point.y;
       // Log.i(TAG, String.format("screen size (%d,%d)", screen_width, screen_height));  // screen size (1080,1920)
        if (screen_width < screen_height) {
            orientation = Configuration.ORIENTATION_PORTRAIT;
            mScreenRotation = 270;
        } else {
            orientation = Configuration.ORIENTATION_LANDSCAPE;
            mScreenRotation = 180;
        }

        Assert.assertEquals(dst.getWidth(), dst.getHeight());
        final float minDim = Math.min(src.getWidth(), src.getHeight());
        final Matrix matrix = new Matrix();

        // We only want the center square out of the original rectangle.
        final float translateX = -Math.max(0, (src.getWidth() - minDim));
        final float translateY = -Math.max(0, (src.getHeight() - minDim));
        matrix.preTranslate(translateX, translateY);
        float scaleFactor = dst.getHeight() / minDim;
        matrix.postScale(scaleFactor, scaleFactor);
        // Rotate around the center if necessary.
        if (mScreenRotation != 0) {
            matrix.postTranslate(-dst.getWidth() / 2.0f, -dst.getHeight() / 2.0f);
            matrix.postRotate(mScreenRotation);
            matrix.postTranslate(dst.getWidth() / 2.0f, dst.getHeight() / 2.0f);
        }
        final Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, matrix, null);
    }


    @Override
    public void onImageAvailable(final ImageReader reader) {

        Image image = null;
        int textureviewAndPreviewWidthRatio = 0;
        int textureviewAndPreviewHeightRatio = 0;

        try {
            image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }
            if (mIsComputing) {
                image.close();
                return;
            }
            mIsComputing = true;

            final Plane[] planes = image.getPlanes();
            Log.i(TAG, String.format("image size (%d,%d)", image.getWidth(), image.getHeight())); //Original image size (2592,1944) -> 500만 화소

            // 해상도가 알려진 경우 저장소 비트 맵을 한 번 초기화
            if (mPreviewWdith != image.getWidth() || mPreviewHeight != image.getHeight()) {
                mPreviewWdith = image.getWidth();
                mPreviewHeight = image.getHeight();

                textureviewAndPreviewWidthRatio = mPreviewWdith / mTextureViewWidth;
                textureviewAndPreviewHeightRatio = mPreviewHeight / mTextureViewHeight;

                Log.i(TAG, String.format("Preview size (%d,%d)", mPreviewWdith, mPreviewHeight));
                mRGBBytes = new int[mPreviewWdith * mPreviewHeight];
                mRGBframeBitmap = Bitmap.createBitmap(mPreviewWdith, mPreviewHeight, Config.ARGB_8888);
                mBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888);

                mYUVBytes = new byte[planes.length][];
                for (int i = 0; i < planes.length; ++i) {
                    mYUVBytes[i] = new byte[planes[i].getBuffer().capacity()];
                }
            }

            for (int i = 0; i < planes.length; ++i) {
                planes[i].getBuffer().get(mYUVBytes[i]);
            }

            final int yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();
            ImageUtils.convertYUV420ToARGB8888( mYUVBytes[0], mYUVBytes[1], mYUVBytes[2], mRGBBytes,
                    mPreviewWdith,
                    mPreviewHeight,
                    yRowStride,
                    uvRowStride,
                    uvPixelStride,
                    false);

            image.close();
        } catch (final Exception e) {
            if (image != null) {
                image.close();
            }
            Log.i(TAG, "Exception!");
            Trace.endSection();
            return;
        }

        mRGBframeBitmap.setPixels(mRGBBytes, 0, mPreviewWdith, 0, 0, mPreviewWdith, mPreviewHeight);
        drawResizedBitmap(mRGBframeBitmap, mBitmap);

        //mEyeOverlayView.covertPreviewRatio(textureviewAndPreviewWidthRatio, textureviewAndPreviewHeightRatio);

/*        Bitmap b = Bitmap.createBitmap( INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        //mEyeOverlayView.layout(0, 0, mPreviewWdith, mPreviewHeight);
        mEyeOverlayView.draw(c);*/


//        if (v.getMeasuredHeight() <= 0) {
//            mEyeOverlayView.measure(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//            Bitmap b = Bitmap.createBitmap(mEyeOverlayView.getMeasuredWidth(), mEyeOverlayView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
//            Canvas c = new Canvas(b);
//            mEyeOverlayView.layout(0, 0, mEyeOverlayView.getMeasuredWidth(), mEyeOverlayView.getMeasuredHeight());
//            mEyeOverlayView.draw(c);
//        }

        /*ImageUtils.saveBitmap(b, "abc");
        ImageUtils.saveBitmap(mBitmap, "mBitmap");*/


        mOverlayStartLeftX = mEyeOverlayView.getStartLeftX();
        mOverlayStartLeftY = mEyeOverlayView.getStartLeftY();

        mOverlayStartRightX = mEyeOverlayView.getStartRightX();
        mOverlayStartRightY = mEyeOverlayView.getStartRightY();

        mOverlayEndLeftX = mEyeOverlayView.getEndLeftX();
        mOverlayEndLeftY = mEyeOverlayView.getEndLeftY();

        mOverlayEndRightX = mEyeOverlayView.getEndRightX();
        mOverlayEndRightY = mEyeOverlayView.getEndRightY();

        mOverlayEye2Eye = mEyeOverlayView.getEye2Eye();
        mOverlayEyeHeight = mEyeOverlayView.getEyeHeight();
        mOverlayEyeWidth = mEyeOverlayView.getEyeWidth();


        mInferenceHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        List<VisionDetRet> results;
                        synchronized (OnGetImageListener.this) {
                            results = mFaceDet.detect(mBitmap);
                        }

                        if (results != null) {
                            for (final VisionDetRet ret : results) {

                                Dlog.d("");
                                Dlog.d("mOverlayStartRightX : "+mOverlayStartRightX);
                                Dlog.d("mOverlayStartRightY : "+mOverlayStartRightY );
                                Dlog.d("mOverlayEndRightX : "+mOverlayEndRightX);
                                Dlog.d("mOverlayEndRightY : "+mOverlayEndRightY);
                                Dlog.d("mOverlayStartLeftX : "+mOverlayStartLeftX);
                                Dlog.d("mOverlayStartLeftY : "+mOverlayStartLeftY );
                                Dlog.d("mOverlayEndLeftX : "+mOverlayEndLeftX);
                                Dlog.d("mOverlayEndLeftY : "+mOverlayEndLeftY);
                                Dlog.d("");
                                Dlog.d("ret.start_right_x() : "+ret.start_right_x);
                                Dlog.d("ret.start_right_y() : "+ret.start_right_y);
                                Dlog.d("ret.end_right_x() : "+ret.end_right_x);
                                Dlog.d("ret.end_right_y() : "+ret.end_right_y);
                                Dlog.d("ret.start_left_x() : "+ret.start_left_x);
                                Dlog.d("ret.start_left_y() : "+ret.start_left_y);
                                Dlog.d("ret.end_left_x() : "+ret.end_left_x);
                                Dlog.d("ret.end_left_y() : "+ret.end_left_y);
                                Dlog.d("");
                                Dlog.d("mOverlayStartRightX <= ret.start_right_x() : " + String.valueOf(mOverlayStartRightX <= ret.start_right_x));
                                Dlog.d("mOverlayEndRightX >= ret.end_right_x() : " + String.valueOf(mOverlayEndRightX >= ret.end_right_x));
                                Dlog.d("mOverlayStartRightY <= ret.start_right_y() : " + String.valueOf(mOverlayStartRightY <= ret.start_right_y));
                                Dlog.d("mOverlayEndRightY >= ret.end_right_y() : " + String.valueOf(mOverlayEndRightY >= ret.end_right_y));
                                Dlog.d("mOverlayStartLeftX <= ret.start_left_x() : " + String.valueOf(mOverlayStartLeftX <= ret.start_left_x));
                                Dlog.d("mOverlayEndLeftX >= ret.end_left_x() : " + String.valueOf(mOverlayEndLeftX >= ret.end_left_x));
                                Dlog.d("mOverlayStartLeftY <= ret.start_left_y() : " + String.valueOf(mOverlayStartLeftY <= ret.start_left_y));
                                Dlog.d("mOverlayEndLeftY >= ret.end_left_y() : " + String.valueOf(mOverlayEndLeftY >= ret.end_left_y));

                                if(mOverlayStartRightX <= ret.start_right_x
                                        && mOverlayEndRightX >= ret.end_right_x
                                        && mOverlayStartRightY <= ret.start_right_y
                                        && mOverlayEndRightY >= ret.end_right_y
                                        && mOverlayStartLeftX <= ret.start_left_x
                                        && mOverlayEndLeftX >= ret.end_left_x
                                        && mOverlayStartLeftY <= ret.start_left_y
                                        && mOverlayEndLeftY >= ret.end_left_y){

                                    mBackgroundHandler.obtainMessage(CameraConnectionFragment.EYE_BOUNDARY_STEADY_STATE).sendToTarget();

                                }else{

                                    mBackgroundHandler.obtainMessage(CameraConnectionFragment.EYE_BOUNDARY_UNSTABLE_STATE).sendToTarget();

                                }

                                //detecting 이미지를 보여준다
                                /*Canvas canvas = new Canvas(mBitmap);
                                ArrayList<Point> landmarks = ret.getFaceLandmarks();
                                for (int j=0; j<landmarks.size(); j++) {
                                    Point point = landmarks.get(j);
                                    if(j>35  && j<42) {     // 왼쪽(36 ~ 41)
                                        canvas.drawCircle(point.x , point.y, 2, mFaceLandmardkPaint);
                                    }if(j>41  && j<48){      //오른쪽(42 ~ 47)
                                        canvas.drawCircle(point.x , point.y, 3, mFaceLandmardkPaint);
                                    }
                                }*/

                               /* CheckQuality quality = new CheckQuality(mBitmap, ret);
                                quality.setImageScope(false);
                                quality.setAccept(false);*/

                                /* -----------------------
                                * *      눈 영역만 crop
                                * * ----------------------- */
                                Bitmap bitCrop_L = Bitmap.createBitmap(mBitmap, ret.mStartLeftX, ret.mStartLeftY, ret.mWidthLeft, ret.mHightLeft);
                                Bitmap bitCrop_R = Bitmap.createBitmap(mBitmap, ret.mStartRightX, ret.mStartRightY, ret.mWidthRight, ret.mHightRight);

                                Log.i(TAG, String.format("%d: left size (%d,%d)", INPUT_SIZE, bitCrop_L.getWidth(), bitCrop_L.getHeight()));
                                //Log.i(TAG, String.format("%d: right size (%d,%d)", INPUT_SIZE, bitCrop_L.getWidth(), bitCrop_L.getHeight()));

                                // 몇가지 조건 만족여부 조사
                                //quality.isImageScope();
                                //quality.isBlur(bitCrop_L, bitCrop_R);

                                //if (quality.isAccept() == true && quality.isImageScope()==true) {

                                int sizeLeft = (bitCrop_L.getWidth()* bitCrop_L.getHeight());
                                int sizeRight = (bitCrop_L.getWidth()* bitCrop_L.getHeight());

                                //if(sizeLeft<40000 && sizeRight<40000){

                                //crop 한 눈 영상 파일로 저장 (임시 주석 taein)
                                /*String left = "left_" + String.valueOf(quality.mBlur_L);
                                String right = "right_" + String.valueOf(quality.mBlur_R);
                                */
                                String right = "right_"+String.valueOf(mNumCrop);
                                String left = "left_"+String.valueOf(mNumCrop) ;

                                //임시 주석 taein
                                //ImageUtils.saveBitmap(bitCrop_R, right);
//                                ImageUtils.saveBitmap(bitCrop_L, left);

                                try{
                                    bitmap_right[mNumCrop] = bitCrop_R;
                                    bitmap_left[mNumCrop] = bitCrop_L;
                                }catch (ArrayIndexOutOfBoundsException ex){
                                    ex.printStackTrace();
                                }

                                mNumCrop = mNumCrop + 1;

                                /*if(mNumCrop == 5){
                                    //mBackgroundHandler.obtainMessage(CameraConnectionFragment.FULL_CAPTURE_COMPLETE).sendToTarget();
                                    mNumCrop = 0;
                                }*/

                                //}
                                //else{
                                  //  Log.i(TAG,"Intent에 Bitmap을 put시킬 때, 안드로이드에서는 이미지 크기가 40KB로 제한되어 있다.");
                                //}

                                //}
                                /*
                                try{
                                    //bitmapCropped = Bitmap.createBitmap(mBitmap, stx ,xty, width, height);
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                                    StringBuilder sb = new StringBuilder();
                                    sb.setLength(0);

                                    sb.append(" [roll] : " + mSensorDTO.getRoll())
                                            .append(" [pitch] : " + mSensorDTO.getPitch())
                                            .append(" [yaw] : " + mSensorDTO.getYaw())
                                            .append(" [br] : " + mSensorDTO.getBr());

                                    Dlog.d(sb.toString());

                                    if(mBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)){
                                        byte[] imageBytes = baos.toByteArray();

                                        //String encodedImage = Base64.encodeToString(imageBytesimageBytes, Base64.DEFAULT);
                                        sendPngAndSensorData(imageBytes, "abc", mSensorDTO);

                                        try {
                                            baos.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                }catch (IllegalArgumentException e){
                                    Dlog.d("Exception Raise : " + e.getMessage());
                                }
                                */

                            }
                        }
                        /*
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        StringBuilder sb = new StringBuilder();
                        sb.setLength(0);
                        sb.append(" [roll] : " + mSensorDTO.getRoll())
                                .append(" [pitch] : " + mSensorDTO.getPitch())
                                .append(" [yaw] : " + mSensorDTO.getYaw())
                                .append(" [br] : " + mSensorDTO.getBr());
                        Dlog.d(sb.toString());
                        //mTextView.setText(sb.toString());
                        if(mBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)){
                            byte[] imageBytes = baos.toByteArray();
                            //String encodedImage = Base64.encodeToString(imageBytesimageBytes, Base64.DEFAULT);
                            sendPngAndSensorData(imageBytes, "abc", mSensorDTO);
                        }
                        */

                        Bitmap newbitmap = Bitmap.createBitmap(mBitmap, 0 ,mBitmap.getHeight()/2-50, mBitmap.getWidth(), mBitmap.getHeight()/2+50);
                        //mWindow.setRGBBitmap(newbitmap);
                        mIsComputing = false;
                    }
                });

        Trace.endSection();
    }

    /**
     *  웹 서버로 png(byte[]), sensor value, label 전송
     **/
    private void sendPngAndSensorData(final byte[] bytes, final String label, final SensorDTO sensorVales) {

        //for network processing
        new Thread() {
            public void run() {
                httpConn.requestUploadPhoto(bytes, label, sensorVales, httpCallback);
            }
        }.start();
    }

    private final Callback httpCallback = new Callback() {

        @Override
        public void onFailure(Call call, IOException e) {
            Log.e(TAG, "Callback Error Message : " + e.getMessage());
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            String body = response.body().string();
            Log.e(TAG, "Server Response Body : " + body);
        }
    };

    public class SensorChangeHandler extends Handler{

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what){
                case 1:
                    mSensorDTO = (SensorDTO) msg.obj;
                    break;
            }
        }
    }

}