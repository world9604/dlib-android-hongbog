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
import java.lang.reflect.Array;
import java.security.interfaces.DSAKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.tzutalin.dlibtest.CameraConnectionFragment.ENROLL_INPUT_DATA_SIZE;
import static com.tzutalin.dlibtest.CameraConnectionFragment.VERIFY_INPUT_DATA_SIZE;


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
    private Handler mUiHandler;

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

    private int mNumCrop = 0;
    public Bitmap bitmap_both[];
    public Bitmap bitmap_left[];
    public Bitmap bitmap_right[];

    private HttpConnection httpConn = HttpConnection.getInstance();
    private SensorDTO mSensorDTO = new SensorDTO();
    private SensorChangeHandler mSensorChangeHandler;
    private float textureviewAndPreviewWidthRatio = 0f;
    private float textureviewAndPreviewHeightRatio = 0f;

    private Message mMsg;

    private String STATE_TEXT_CHECK_OVERLAY;
    private String STATE_TEXT_CHECK_ACEL;
    private String STATE_TEXT_NOTHING;


    public OnGetImageListener() {
        mSensorChangeHandler = new SensorChangeHandler();
        SensorListener.setHandler(mSensorChangeHandler);
    }


    public void initialize(final Context context, final AssetManager assetManager,
                           final Handler inferenceHandler , final Handler backgroundHandler,
                           final String label, final CustomView eyeOverlayView,
                           final AutoFitTextureView textureView, final Handler uiHandler,
                           final Bitmap[] bitmapBoth, final Bitmap[] bitmapLeft, final Bitmap[] bitmapRight) {
        Dlog.d("initialize");

        mContext = context;
        mInferenceHandler = inferenceHandler;
        mBackgroundHandler = backgroundHandler;
        mUiHandler = uiHandler;
        bitmap_both = bitmapBoth;
        bitmap_left = bitmapLeft;
        bitmap_right = bitmapRight;

        mFaceDet = FaceDet.getInstance();
//        mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
        Dlog.d("mFaceDet : " + (null == mFaceDet));

        STATE_TEXT_CHECK_OVERLAY = mContext.getString(R.string.state_text_check_overlay);
        STATE_TEXT_CHECK_ACEL = mContext.getString(R.string.state_text_check_acel);
        STATE_TEXT_NOTHING = mContext.getString(R.string.state_text_nothing);

        mSensorDTO.setLabel(label);

        try{
            /*textureviewAndPreviewWidthRatio = Float.parseFloat(String.format("%.2f", (float)INPUT_SIZE / (float)mTextureViewWidth));
            textureviewAndPreviewHeightRatio = Float.parseFloat(String.format("%.2f", (float)INPUT_SIZE / (float)mTextureViewHeight));*/
            textureviewAndPreviewWidthRatio = (float)INPUT_SIZE / (float)textureView.getWidth();
            textureviewAndPreviewHeightRatio = (float)INPUT_SIZE / (float)textureView.getHeight();
        }catch (ArithmeticException  ex){
            ex.printStackTrace();
        }

        eyeOverlayView.convertPreviewRatio(textureviewAndPreviewWidthRatio, textureviewAndPreviewHeightRatio);

        mOverlayStartLeftX = eyeOverlayView.getStartLeftX();
        mOverlayStartLeftY = eyeOverlayView.getStartLeftY();

        mOverlayStartRightX = eyeOverlayView.getStartRightX();
        mOverlayStartRightY = eyeOverlayView.getStartRightY();

        mOverlayEndLeftX = eyeOverlayView.getEndLeftX();
        mOverlayEndLeftY = eyeOverlayView.getEndLeftY();

        mOverlayEndRightX = eyeOverlayView.getEndRightX();
        mOverlayEndRightY = eyeOverlayView.getEndRightY();

        mOverlayEye2Eye = eyeOverlayView.getEye2Eye();
        mOverlayEyeHeight = eyeOverlayView.getEyeHeight();
        mOverlayEyeWidth = eyeOverlayView.getEyeWidth();

    }


    public void deInitialize() {
        Dlog.d("deInitialize");
        /*synchronized (OnGetImageListener.this) {
            if (mFaceDet != null) {
                Dlog.d("mFaceDet.release()");
                mFaceDet.release();
            }
        }*/
    }


    private void drawResizedBitmap(final Bitmap src, final Bitmap dst) {
        Dlog.d("drawResizedBitmap");

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

        try {
            image = reader.acquireLatestImage();

            if (image == null) return;

            if (mIsComputing) {
                image.close();
                return;
            }

            mIsComputing = true;

            final Plane[] planes = image.getPlanes();

            // Original image size (2592,1944) -> 500만 화소
            // Dlog.d(String.format("image size (%d,%d)", image.getWidth(), image.getHeight()));

            // 해상도가 알려진 경우 저장소 비트 맵을 한 번 초기화
            if (mPreviewWdith != image.getWidth() || mPreviewHeight != image.getHeight()) {

                // loading animation stop
                mUiHandler.obtainMessage(CameraConnectionFragment.LOAD_VIEW_COMPLETE).sendToTarget();

                mPreviewWdith = image.getWidth();
                mPreviewHeight = image.getHeight();

                Dlog.d(String.format("Preview size (%d,%d)", mPreviewWdith, mPreviewHeight));

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
            Dlog.e("Exception message : " +  e.getMessage());
            Trace.endSection();
            return;
        }

        mRGBframeBitmap.setPixels(mRGBBytes, 0, mPreviewWdith, 0, 0, mPreviewWdith, mPreviewHeight);
        drawResizedBitmap(mRGBframeBitmap, mBitmap);

        mInferenceHandler.post(new AfterTreatment());

        Trace.endSection();
    }


    /**
     * 디텍팅 한 눈의 이미지와 좌표값을 토대로 필터링 작업을 하고
     * 만족 하는 이미지를 저장 한다.
     */
    private class AfterTreatment implements Runnable{

        @Override
        public void run() {

            List<VisionDetRet> results;

            synchronized (OnGetImageListener.this) {
                results = mFaceDet.detect(mBitmap);
                if (results == null) {
                    results = Arrays.asList(new VisionDetRet[]{});
                }
            }

            for (final VisionDetRet ret : results) {

                /*Dlog.d("");
                Dlog.d("mOverlayStartRightX <= ret.start_right_x " + (mOverlayStartRightX <= ret.start_right_x));
                Dlog.d("mOverlayEndRightX >= ret.end_right_x " + (mOverlayEndRightX >= ret.end_right_x));
                Dlog.d("mOverlayStartRightY <= ret.start_right_y " + (mOverlayStartRightY <= ret.start_right_y));
                Dlog.d("mOverlayEndRightY >= ret.end_right_y " + (mOverlayEndRightY >= ret.end_right_y));
                Dlog.d("mOverlayStartLeftX <= ret.start_left_x " + (mOverlayStartLeftX <= ret.start_left_x));
                Dlog.d("mOverlayEndLeftX >= ret.end_left_x " + (mOverlayEndLeftX >= ret.end_left_x));
                Dlog.d("mOverlayStartLeftY <= ret.start_left_y " + (mOverlayStartLeftY <= ret.start_left_y));
                Dlog.d("mOverlayEndLeftY >= ret.end_left_y " + (mOverlayEndLeftY >= ret.end_left_y));
                Dlog.d("");
                Dlog.d("mOverlayStartRightY " + mOverlayStartRightY);
                Dlog.d("ret.start_right_y " + ret.start_right_y);
                Dlog.d("");
                Dlog.d("ret.getHightLeft() " + ret.getHightLeft());
                Dlog.d("");
                Dlog.d("mOverlayEndRightY " + mOverlayEndRightY);
                Dlog.d("ret.end_right_y " + ret.end_right_y);
                Dlog.d("");
                Dlog.d("mOverlayEndLeftY " + mOverlayEndLeftY);
                Dlog.d("ret.end_left_y " + ret.end_left_y);
                Dlog.d("");*/

                Bitmap bitCrop_B;
                Bitmap bitCrop_L;
                Bitmap bitCrop_R;

                try{
                    // 눈 영역만 crop
                    bitCrop_B = Bitmap.createBitmap(mBitmap, ret.mStartRightX, ret.mStartRightY, ret.mWidth, ret.mHight);
                    bitCrop_L = Bitmap.createBitmap(mBitmap, ret.mStartLeftX, ret.mStartLeftY, ret.mWidthLeft, ret.mHightLeft);
                    bitCrop_R = Bitmap.createBitmap(mBitmap, ret.mStartRightX, ret.mStartRightY, ret.mWidthRight, ret.mHightRight);
                }catch (final IllegalArgumentException ex){
                    Dlog.e("IllegalArgumentException Message : " + ex.getMessage());
                    break;
                }

                CheckQuality quality = new CheckQuality(ret, mBitmap, bitCrop_B, bitCrop_L, bitCrop_R);

                float acelX = Float.parseFloat(String.format("%.2f", mSensorDTO.getAccelX()));
                float acelZ = Float.parseFloat(String.format("%.2f", mSensorDTO.getAccelZ()));

                /**
                 *  눈 모양 오버레이 좌표값과 디텍팅 눈 좌표값을 비교하여 오버레이 안에 눈이 들어와 있는지 확인
                 */
                /*if (!(mOverlayStartRightX <= ret.start_right_x
                        && mOverlayEndRightX >= ret.end_right_x
                        && mOverlayStartRightY <= ret.start_right_y
                        && mOverlayEndRightY >= ret.end_right_y
                        && mOverlayStartLeftX <= ret.start_left_x
                        && mOverlayEndLeftX >= ret.end_left_x
                        && mOverlayStartLeftY <= ret.start_left_y
                        && mOverlayEndLeftY >= ret.end_left_y)) {
                    mMsg = mUiHandler.obtainMessage(CameraConnectionFragment.QUALITY_CHECK);
                    mMsg.obj = STATE_TEXT_CHECK_OVERLAY;
                    mMsg.sendToTarget();
                    break;
                }*/

                /**
                 * Acelometer 값으로 X, Z 값 (-2) ~ (+2) 필터링
                 */
                if (!((acelX > -2f && acelX < 2f)
                        && (acelZ > -2f && acelZ < 2f))) {

                    mMsg = mUiHandler.obtainMessage(CameraConnectionFragment.QUALITY_CHECK);
                    mMsg.obj = STATE_TEXT_CHECK_ACEL;
                    mMsg.sendToTarget();
                    break;
                }

                /**
                 * 얼굴 크기가 너무 작거나 큰것 제외시킨다
                 */
                if (!"".equals(quality.acceptScope())) {
                    Dlog.d("scope");
                    mMsg = mUiHandler.obtainMessage(CameraConnectionFragment.QUALITY_CHECK);
                    mMsg.obj = quality.acceptScope();
                    mMsg.sendToTarget();
                    break;
                }

                /**
                 * 이미지가 스무딩 되었는지 체크
                 */
                else if (!"".equals(quality.acceptBlur())) {
                    Dlog.d("blur");
                    mMsg = mUiHandler.obtainMessage(CameraConnectionFragment.QUALITY_CHECK);
                    mMsg.obj = quality.acceptBlur();
                    mMsg.sendToTarget();
                    break;
                }

                /**
                 * 눈이 감겼는제 체크
                 */
                else if (!"".equals(quality.acceptEar())) {
                    Dlog.d("ear");
                    mMsg = mUiHandler.obtainMessage(CameraConnectionFragment.QUALITY_CHECK);
                    mMsg.obj = quality.acceptEar();
                    mMsg.sendToTarget();
                    break;
                }

                /**
                 * 옆으로 흘겨보는지 체크
                 */
                else if (!"".equals(quality.acceptGlace())) {
                    Dlog.d("glace");
                    mMsg = mUiHandler.obtainMessage(CameraConnectionFragment.QUALITY_CHECK);
                    mMsg.obj = quality.acceptGlace();
                    mMsg.sendToTarget();
                    break;
                }

                /**
                 * 고개를 기울였는지 체크
                 */
                else if (!"".equals(quality.acceptRotate())) {
                    Dlog.d("rotate");
                    mMsg = mUiHandler.obtainMessage(CameraConnectionFragment.QUALITY_CHECK);
                    mMsg.obj = quality.acceptRotate();
                    mMsg.sendToTarget();
                    break;
                }

                /**
                 * 모든 조건을 만족 할때
                 */
                else {
                    mMsg = mUiHandler.obtainMessage(CameraConnectionFragment.QUALITY_CHECK);
                    mMsg.obj = STATE_TEXT_NOTHING + " " + mNumCrop;
                    mMsg.sendToTarget();
                }

                if (quality.isAccept() == true) {

                    try{
                        bitmap_both[mNumCrop] = bitCrop_B;
                        bitmap_left[mNumCrop] = bitCrop_L;
                        bitmap_right[mNumCrop] = bitCrop_R;
                    }catch (ArrayIndexOutOfBoundsException ex){
                        Dlog.e("Exception Message : " + ex.getMessage());
                    }

                    // 라벨이 있으므로 등록 프로세스
                    if (mSensorDTO.getLabel() != null
                            && !"".equals(mSensorDTO.getLabel())) {

                        String bothFileName = "B_" + mSensorDTO.getLabel() + "_" + mNumCrop;
                        String leftFileName = "L_" + mSensorDTO.getLabel() + "_" + mNumCrop;
                        String rightFileName = "R_" + mSensorDTO.getLabel() + "_" + mNumCrop;

                        ImageUtils.saveBitmap(bitCrop_B, bothFileName, mSensorDTO.getLabel());
                        ImageUtils.saveBitmap(bitCrop_R, leftFileName, mSensorDTO.getLabel());
                        ImageUtils.saveBitmap(bitCrop_L, rightFileName, mSensorDTO.getLabel());

                        if(mNumCrop == (ENROLL_INPUT_DATA_SIZE - 1)){
                            mNumCrop = 0;
                            mUiHandler.obtainMessage(CameraConnectionFragment.STOP_ACTIVITY).sendToTarget();
                            return;
                        }

                    // 라벨이 없으므로 인식 프로세스
                    } else {

                        if(mNumCrop == (VERIFY_INPUT_DATA_SIZE - 1)){
                            mNumCrop = 0;

                            Map<String, Bitmap[]> map = new HashMap<>();
                            map.put("bitmap_both", bitmap_both);
                            map.put("bitmap_left", bitmap_left);
                            map.put("bitmap_right", bitmap_right);

                            mMsg = mUiHandler.obtainMessage(CameraConnectionFragment.FULL_CAPTURE_COMPLETE);
                            mMsg.obj = map;
                            mMsg.sendToTarget();
                            return;
                        }

                    }

                    mNumCrop = mNumCrop + 1;

                    /**
                     * 파일서버로 이미지 보내기
                     */
                            /*try{
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
                            }*/

                }
            }
            mIsComputing = false;
        }

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
                    Dlog.d("SensorChangeHandler");
                    mSensorDTO = (SensorDTO) msg.obj;
                    break;
                case 0:
                    Dlog.d("SensorChange Acel");
                    mSensorDTO = (SensorDTO) msg.obj;
                    break;
            }
        }
    }

}