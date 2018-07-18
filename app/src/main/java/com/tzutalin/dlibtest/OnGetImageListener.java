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
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;

import junit.framework.Assert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


// preview "1) 프레임을 가져 와서",   이미지를 "2)비트 맵으로 변환"하여   dlib lib로 처리하는 클래스
public class OnGetImageListener implements OnImageAvailableListener {

    private static final int INPUT_SIZE = 500;//500; //224;
    private static final String TAG = "OnGetImageListener";

    private int mScreenRotation = 90;

    private int mPreviewWdith = 0;
    private int mPreviewHeight = 0;
    private byte[][] mYUVBytes;
    private int[] mRGBBytes = null;
    private Bitmap mRGBframeBitmap = null;
    private Bitmap mCroppedBitmap = null;

    private boolean mIsComputing = false;
    private Handler mInferenceHandler;

    private Context mContext;
    private FaceDet mFaceDet;

    //private TrasparentTitleView mTransparentTitleView;  // timecost를 보여주기위해

//    private FloatingCameraWindow mWindow;               // Landmark point를 보여주는 preview

    private Paint mFaceLandmardkPaint;

    private HttpConnection httpConn = HttpConnection.getInstance();

    private SensorDTO mSensorDTO = new SensorDTO();

    private SensorChangeHandler mSensorChangeHandler;

    public OnGetImageListener() {
        Dlog.d("OnGetImageListener::Construct");

        mSensorChangeHandler = new SensorChangeHandler();
        SensorListener.setHandler(mSensorChangeHandler);
    }

    public void initialize(final Context context, final AssetManager assetManager, final Handler handler) {
        Log.i(TAG,"1 initialize()");
        this.mContext = context;
        this.mInferenceHandler = handler;
        mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
//        mWindow = new FloatingCameraWindow(mContext);

        mFaceLandmardkPaint = new Paint();
        mFaceLandmardkPaint.setColor(Color.GREEN);
        mFaceLandmardkPaint.setStrokeWidth(2);
        mFaceLandmardkPaint.setStyle(Paint.Style.STROKE);
    }

    public void deInitialize() {
        Log.i(TAG,"deInitialize()");
        Dlog.d("CameraActivty::deInitialize()");

        synchronized (OnGetImageListener.this) {
            if (mFaceDet != null) {
                mFaceDet.release();
            }

            /*if (mWindow != null) {
                mWindow.release();
            }*/
        }
    }

    private void drawResizedBitmap(final Bitmap src, final Bitmap dst) {

        Log.i(TAG,"3 drawResizedBitmap()");
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
        // Log.i(TAG, "translate "+ String.valueOf(translateX)+", "+String.valueOf(translateY));    //translate -240.0, -0.0
        matrix.preTranslate(translateX, translateY);

        float scaleFactor = dst.getHeight() / minDim;
        // Log.i(TAG, "scaleFactor "+ String.valueOf(scaleFactor)); // caleFactor 0.6944444
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
        try {
            image = reader.acquireLatestImage();    // ImageReader 에서image 를 얻어온다

            if (image == null) {
                Log.i(TAG, "onImageAvailable-1 image null");
                return;
            }
            // No mutex needed as this method is not reentrant.
            if (mIsComputing) {
                Log.i(TAG, "onImageAvailable-2 mIsComputing");
                image.close();
                return;
            }
            mIsComputing = true;

            final Plane[] planes = image.getPlanes();

            // 해상도가 알려진 경우 저장소 비트 맵을 한 번 초기화
            if (mPreviewWdith != image.getWidth() || mPreviewHeight != image.getHeight()) {
                Log.i(TAG, "onImageAvailable-0 초기화");
                mPreviewWdith = image.getWidth();
                mPreviewHeight = image.getHeight();

                Log.i(TAG, String.format("0 mPreview size (%d,%d)", mPreviewWdith, mPreviewHeight));
                mRGBBytes = new int[mPreviewWdith * mPreviewHeight];
                mRGBframeBitmap = Bitmap.createBitmap(mPreviewWdith, mPreviewHeight, Config.ARGB_8888);
                mCroppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888);

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
            Log.i(TAG, "onImageAvailable-3 convertYUV420ToARGB8888");
            ImageUtils.convertYUV420ToARGB8888(
                    mYUVBytes[0],
                    mYUVBytes[1],
                    mYUVBytes[2],
                    mRGBBytes,
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
        drawResizedBitmap(mRGBframeBitmap, mCroppedBitmap);

        Log.i(TAG, String.format("3 mRGBBytes size (%d,%d)", mPreviewWdith, mPreviewHeight));

        mInferenceHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "3- mInferenceHandler");
                        if (!new File(Constants.getFaceShapeModelPath()).exists()) {
                            Log.i(TAG,"Copying landmark model to " + Constants.getFaceShapeModelPath());
                        }
                        List<VisionDetRet> results;
                        synchronized (OnGetImageListener.this) {
                            results = mFaceDet.detect(mCroppedBitmap);
                        }

                        // Draw on bitmap
                        if (results != null) {
                            Log.i(TAG, "3- mInferenceHandler -results mFaceDet");

                            for (final VisionDetRet ret : results) {

                                long startTime = System.currentTimeMillis();

                                //detecting 이미지를 보여준다
                                /*float resizeRatio = 1.0f;
                                Rect bounds = new Rect();
                                bounds.left = (int) (ret.getLeft() * resizeRatio);
                                bounds.top = (int) (ret.getTop() * resizeRatio);
                                bounds.right = (int) (ret.getRight() * resizeRatio);
                                bounds.bottom = (int) (ret.getBottom() * resizeRatio);
                                Canvas canvas = new Canvas(mCroppedBitmap);
                                canvas.drawRect(bounds, mFaceLandmardkPaint);
                                // Draw landmark
                                ArrayList<Point> landmarks = ret.getFaceLandmarks();
                                for (int j=0; j<landmarks.size(); j++) {
                                    Point point = landmarks.get(j);
                                    //for (Point point : landmarks) {
                                    int pointX = (int) (point.x * resizeRatio);
                                    int pointY = (int) (point.y * resizeRatio);
                                    if(j>35  && j<42) {     // 왼쪽(36 ~ 41)
                                        canvas.drawCircle(pointX, pointY, 2, mFaceLandmardkPaint);
                                    }if(j>41  && j<48){      //오른쪽(42 ~ 47)
                                        canvas.drawCircle(pointX, pointY, 3, mFaceLandmardkPaint);
                                    }
                                }*/

                                // 이미지 crop
                                float scale = 1.0f;
                                int stx = (int)(ret.mStartRightX *scale);
                                int xty = (int)(ret.mStartRightY *scale);
                                int width = (int)(ret.mWidthRight*scale);
                                int height = (int)(ret.mHightRight*scale);
                                Log.i(TAG,"3 all image "+String.valueOf(mCroppedBitmap.getWidth()) +", "+ String.valueOf(mCroppedBitmap.getHeight())+")");
                                Log.i(TAG,"3 Eye crop ("+  String.valueOf(width) +", "+ String.valueOf(height)+")");

                                Bitmap bitmapCropped = null;

                                try{
                                    bitmapCropped = Bitmap.createBitmap(mCroppedBitmap, stx ,xty, width, height);

                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                                    StringBuilder sb = new StringBuilder();
                                    sb.setLength(0);

                                    sb.append(" [roll] : " + mSensorDTO.getRoll())
                                            .append(" [pitch] : " + mSensorDTO.getPitch())
                                            .append(" [yaw] : " + mSensorDTO.getYaw())
                                            .append(" [br] : " + mSensorDTO.getBr());

                                    Dlog.d(sb.toString());

                                    if(bitmapCropped.compress(Bitmap.CompressFormat.PNG, 100, baos)){
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

                                // 이미지 파일로 저장 saveBitmap by png
                                long endTime = System.currentTimeMillis();
                                //mTransparentTitleView.setText("Time cost: " + String.valueOf((endTime - startTime) / 1000f) + " sec");
                                String time = String.valueOf((endTime - startTime) / 10f) ;
                                ImageUtils.saveBitmap(bitmapCropped, time);
                                startTime = System.currentTimeMillis();
                            }
                        }

//                        mWindow.setRGBBitmap(mCroppedBitmap);
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