/*
 * Copyright 2016-present Tzutalin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tzutalin.dlibtest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hongbog.view.CustomView;
import com.victor.loading.rotate.RotateLoading;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class CameraConnectionFragment extends Fragment {

    //private static final String TAG = "CameraFragment";
    private static final String TAG = "i99";

    //카메라 미리보기 크기가 DESIRED_SIZE x DESIRED_SIZE 사각형을 포함 할 수있는 픽셀 크기로 가장 작은 프레임으로 선택됨
    private static final int MINIMUM_PREVIEW_SIZE = 500;       // 1000일때, S6와 Note5는 1088x1088 // 500일때 1280x720
//    private static final int MINIMUM_PREVIEW_SIZE = 600;       // 1000일때, S6와 Note5는 1088x1088 // 500일때 1280x720

    public static final String LABEL_NAME = "LABEL_NAME";

    //private TrasparentTitleView mScoreView;
    //private TextView mTextView;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final String FRAGMENT_DIALOG = "dialog";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String cameraId;
    private AutoFitTextureView textureView;
    private CameraCaptureSession captureSession;
    private CameraDevice cameraDevice;
    private Size readerSize;

    private ImageReader reader;

    private CaptureRequest.Builder previewRequestBuilder;
    private CaptureRequest previewRequest;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private HandlerThread inferenceThread;
    private Handler inferenceHandler;

    private Handler UiHandler;

    private final Semaphore cameraOpenCloseLock = new Semaphore(1);
    private HandlerThread sensorThread;
    private Handler sensorHandler;

    //Using the Accelometer & Gyroscoper
    private SensorManager mSensorManager;
    private SensorEventListener mSensorLis;
    private Sensor mGgyroSensor;
    private Sensor mLightSensor;

    //member label name
    private String mLabel;

    public static final int FULL_CAPTURE_COMPLETE = 3;
    public static final int LOAD_VIEW_COMPLETE = 4;
    public static final int QUALITY_CHECK = 5;
    public static final int STOP_ACTIVITY = 6;

    private CustomView eyeOverlayView;
    private RelativeLayout eyeOverlaySurfaceView;
    private TextView mStateTextView;
    private Size deviceLargestSize;
    private FrameLayout gforceFrameLayout;

    public static final int ENROLL_INPUT_DATA_SIZE = 3;
    public static final int VERIFY_INPUT_DATA_SIZE = 5;

    private Bitmap bitmap_both[];
    private Bitmap bitmap_left[];
    private Bitmap bitmap_right[];

    // 카메라의 상태가 변경되었을 때 상태콜백함수(StateCallback)가 호출된다.
    private final CameraDevice.StateCallback stateCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(final CameraDevice cd) {
                    Dlog.d("CameraDevice.StateCallback::onOpend");

                    // 카메라 켜졌을 때  ( We start camera preview here.)
                    cameraOpenCloseLock.release();
                    cameraDevice = cd;
                    createCameraPreviewSession();
                }

                @Override
                public void onDisconnected(final CameraDevice cd) {
                    Dlog.d("CameraDevice.StateCallback::onDisconnected");

                    // 카메라 꺼졌을 때
                    cameraOpenCloseLock.release();
                    cd.close();
                    cameraDevice = null;

                    if (mOnGetPreviewListener != null) {
                        mOnGetPreviewListener.deInitialize();
                    }
                }

                @Override
                public void onError(final CameraDevice cd, final int error) {
                    Dlog.d("CameraDevice.StateCallback::onError");

                    cameraOpenCloseLock.release();
                    cd.close();
                    cameraDevice = null;
                    final Activity activity = getActivity();
                    if (null != activity) {
                        activity.finish();
                    }

                    if (mOnGetPreviewListener != null) {
                        mOnGetPreviewListener.deInitialize();
                    }
                }
            };


    public static CameraConnectionFragment newInstance() {
        Dlog.d("CameraConnectionFragment::newInstance");
        return new CameraConnectionFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Dlog.d("onCreate");
        super.onCreate(savedInstanceState);

        //Using the Gyroscope & Light
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mGgyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorLis = new SensorListener();
        mSensorManager.registerListener(mSensorLis, mGgyroSensor, SensorManager.SENSOR_DELAY_UI, sensorHandler);
        mSensorManager.registerListener(mSensorLis, mLightSensor, SensorManager.SENSOR_DELAY_UI, sensorHandler);

        Intent intent = getActivity().getIntent();
        String label = intent.getStringExtra(LABEL_NAME);

        if(label != null && !"".equals(label)){
            mLabel = label;
            intent.removeExtra(LABEL_NAME);
            bitmap_both = new Bitmap[ENROLL_INPUT_DATA_SIZE];
            bitmap_left = new Bitmap[ENROLL_INPUT_DATA_SIZE];
            bitmap_right = new Bitmap[ENROLL_INPUT_DATA_SIZE];
        }else{
            bitmap_both = new Bitmap[VERIFY_INPUT_DATA_SIZE];
            bitmap_left = new Bitmap[VERIFY_INPUT_DATA_SIZE];
            bitmap_right = new Bitmap[VERIFY_INPUT_DATA_SIZE];
        }
    }


    // TextureView에서 여러 라이프사이클 이벤트를 처리합니다.
    private final TextureView.SurfaceTextureListener surfaceTextureListener =  new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(
                final SurfaceTexture texture, final int width, final int height) {
            openCamera(width, height);
//            eyeOverlayView.setAspectRatio(textureView.mTextureViewWidth, textureView.mTextureViewHeight, deviceLargestSize);
//            surfaceView.invalidate();
        }

        @Override
        public void onSurfaceTextureSizeChanged(
                final SurfaceTexture texture, final int width, final int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(
                final SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(final SurfaceTexture texture) {
        }
    };


    @Override
    public void onDestroy() {
        Dlog.d("onDestroy");
        mSensorManager.unregisterListener(mSensorLis, mGgyroSensor);
        mSensorManager.unregisterListener(mSensorLis, mLightSensor);
        super.onDestroy();
    }


    @Override
    public View onCreateView(
            final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        Dlog.d("onCreateView");
        return inflater.inflate(R.layout.fragment_camera_connection, container, false);
    }


    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        Dlog.d("onViewCreated");
        textureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        eyeOverlaySurfaceView = (RelativeLayout)view.findViewById(R.id.view);
        eyeOverlayView = new CustomView(this);
        eyeOverlaySurfaceView.addView(eyeOverlayView);
    }


    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        Dlog.d("CameraConnectionFragment::onActivityCreated");
        super.onActivityCreated(savedInstanceState);

        mStateTextView = getActivity().findViewById(R.id.state_textview);
        gforceFrameLayout = getActivity().findViewById(R.id.gforce_container);
    }


    @Override
    public void onResume() {
        super.onResume();
        Dlog.d("onResume");
        startBackgroundThread();
        // 화면이 꺼지고 다시 켜지면 SurfaceTexture는 이미 사용 가능하며 "onSurfaceTextureAvailable"은 호출되지 않음
        // 이 경우 카메라를 열고 여기에서 미리보기를 시작할 수 있음
        // 그렇지 않으면 표면이 SurfaceTextureListener에서 준비 될 때까지 기다린다.
        // else 실행 후 if 실행
       if (textureView.isAvailable()) {
           // onSurfaceTextureAvailable 이벤트 실행. 이 이벤트 안에서 openCamera()  수행
           openCamera(textureView.getWidth(), textureView.getHeight());
//           eyeOverlayView.setAspectRatio(textureView.mTextureViewWidth, textureView.mTextureViewHeight, deviceLargestSize);

       } else {
            // textureView에 Listener를 등록하고 (setSurfaceTextureListener)
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }


    @Override
    public void onPause() {
        Dlog.d("onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }


    // Opens the camera specified by CameraConnectionFragment #cameraId
    @SuppressLint("LongLogTag")
    @DebugLog
    private void openCamera(final int width, final int height) {
        Dlog.d("openCamera");
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        final Activity activity = getActivity();
        final CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            if (ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //Timber.tag(TAG).w("checkSelfPermission CAMERA");
            }

            //full screen method
            //setAspectRatioTextureView(width, height);

            manager.openCamera(cameraId, stateCallback, backgroundHandler);
            //Timber.tag(TAG).d("open Camera");
        } catch (final CameraAccessException e) {
            //Timber.tag(TAG).e("Exception!", e);
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }


    //width :available size for camera preview / height: available size for camera preview
    @DebugLog
    @SuppressLint("LongLogTag")
    private void setUpCameraOutputs(final int width, final int height) {
        Dlog.d("setUpCameraOutputs");
        final Activity activity = getActivity();
        final CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            SparseArray<Integer> cameraFaceTypeMap = new SparseArray<>();

            String cameraId = manager.getCameraIdList()[1]; // cameraId이 1은 전면카메라  (0이 back, 1이 front)
            final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                if (cameraFaceTypeMap.get(CameraCharacteristics.LENS_FACING_FRONT) != null) {
                    cameraFaceTypeMap.append(CameraCharacteristics.LENS_FACING_FRONT, cameraFaceTypeMap.get(CameraCharacteristics.LENS_FACING_FRONT) + 1);
                } else {
                    cameraFaceTypeMap.append(CameraCharacteristics.LENS_FACING_FRONT, 1);
                }
            }

            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                if (cameraFaceTypeMap.get(CameraCharacteristics.LENS_FACING_FRONT) != null) {
                    cameraFaceTypeMap.append(CameraCharacteristics.LENS_FACING_BACK, cameraFaceTypeMap.get(CameraCharacteristics.LENS_FACING_BACK) + 1);
                } else {
                    cameraFaceTypeMap.append(CameraCharacteristics.LENS_FACING_BACK, 1);
                }
            }

            Integer num_facing_back_camera = cameraFaceTypeMap.get(CameraCharacteristics.LENS_FACING_BACK);

            final StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            // map.getOutputSizes: 카메라에서 지원하는 크기 목록이 Size 객체의 배열로 반환됨, 이 값을 이용하여 사진 촬영 시 사진 크기를 지정할 수 있습니다.
            final Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888)), new CompareSizesByArea());

            deviceLargestSize = largest;

            // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera, bus' bandwidth limitation, resulting in gorgeous previews but the storage of garbage capture data.
            readerSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, largest);

            // We fit the aspect ratio of TextureView to the size of preview we picked.
            final int orientation = getResources().getConfiguration().orientation;

            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(readerSize.getWidth(), readerSize.getHeight());
            } else {
                textureView.setAspectRatio(readerSize.getHeight(), readerSize.getWidth());
            }

            CameraConnectionFragment.this.cameraId = cameraId;
            return;

        } catch (final CameraAccessException e) {
            Log.i(TAG,"Exception!", e);
        } catch (final NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }


    /**
     * support full screen in preivew
     * @param ResolutionWidth
     * @param ResolutionHeight
     */
    /*private void setAspectRatioTextureView( int ResolutionWidth , int ResolutionHeight ){
        if(ResolutionWidth > ResolutionHeight){
            int newWidth = DSI_width;
            int newHeight = ((DSI_width * ResolutionWidth)/ResolutionHeight);
            updateTextureViewSize(newWidth,newHeight);

        }else {
            int newWidth = DSI_width;
            int newHeight = ((DSI_width * ResolutionHeight)/ResolutionWidth);
            updateTextureViewSize(newWidth,newHeight);
        }
    }*/


    /**
     * update preview size
     * @param viewWidth
     * @param viewHeight
     */
    /*private void updateTextureViewSize(int viewWidth, int viewHeight) {
        Log.d(TAG, "TextureView Width : " + viewWidth + " TextureView Height : " + viewHeight);
        textureView.setLayoutParams(new FrameLayout.LayoutParams(viewWidth, viewHeight));
    }*/


    /**
     * 카메라가 지원하는 크기의 선택을 감안할 때, 너비와 높이가 각각의 요청 된 값만큼 크고
     * 종횡비가 지정된 값과 일치하는 가장 작은 것을 선택합니다.
     */
    @SuppressLint("LongLogTag")
    @DebugLog
    private static Size  chooseOptimalSize( final Size[] choices, final int width, final int height, final Size aspectRatio) {
        Dlog.d("chooseOptimalSize");

        // 적어도 미리보기 Surface만큼 큰 지원 해상도를 "수집"되어야 한다.
        final List<Size> bigEnough = new ArrayList<Size>();
        for (final Size option : choices) {
            //Log.i(TAG,"map Option size: " + option.getWidth() + "x" + option.getHeight());

            if (option.getHeight() >= MINIMUM_PREVIEW_SIZE && option.getWidth() >= MINIMUM_PREVIEW_SIZE) {
                //Log.i(TAG,"Adding size: " + option.getWidth() + "x" + option.getHeight());
                bigEnough.add(option);
            } else {
                //Log.i(TAG,"Not adding size: " + option.getWidth() + "x" + option.getHeight());
            }
        }
        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            final Size chosenSize = Collections.min(bigEnough, new CompareSizesByArea());
            Log.i(TAG,"Chosen size: " + chosenSize.getWidth() + "x" + chosenSize.getHeight());
            return chosenSize;
        } else {
            Log.i(TAG,"Couldn't find any suitable preview size");
            return choices[0];
        }
    }


    @DebugLog
    private void closeCamera() {
        Dlog.d("closeCamera");
        try {
            cameraOpenCloseLock.acquire();
            if (null != captureSession) {
                captureSession.close();
                captureSession = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != reader) {
                reader.close();
                reader = null;
            }
            if (null != mOnGetPreviewListener) {
                mOnGetPreviewListener.deInitialize();
            }
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            cameraOpenCloseLock.release();
        }
    }


    @DebugLog
    private void startBackgroundThread() {
        Dlog.d("startBackgroundThread");

        backgroundThread = new HandlerThread("ImageListener");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        inferenceThread = new HandlerThread("InferenceThread");
        inferenceThread.start();
        inferenceHandler = new Handler(inferenceThread.getLooper());

        sensorThread = new HandlerThread("SensorThread");
        sensorThread.start();
        sensorHandler = new Handler(sensorThread.getLooper());

        UiHandler = new UIChangeHandler(Looper.getMainLooper());
    }


    @SuppressLint("LongLogTag")
    @DebugLog
    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        inferenceThread.quitSafely();
        sensorThread.quitSafely();

        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;

            inferenceThread.join();
            inferenceThread = null;
            inferenceHandler = null;

            sensorThread.join();
            sensorThread = null;
            sensorHandler = null;

            UiHandler = null;
        } catch (final InterruptedException e) {
            Log.i(TAG, "error", e);
        }
    }

    private final OnGetImageListener mOnGetPreviewListener = new OnGetImageListener();

    private final CameraCaptureSession.CaptureCallback captureCallback =  new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureProgressed(
                final CameraCaptureSession session,
                final CaptureRequest request,
                final CaptureResult partialResult) {
        }

        @Override
        public void onCaptureCompleted(
                final CameraCaptureSession session,
                final CaptureRequest request,
                final TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }
    };


    // Creates a new 'CameraCaptureSession' for camera preview
    @SuppressLint("LongLogTag")
    @DebugLog
    private void createCameraPreviewSession() {
        Dlog.d("createCameraPreviewSession");
        try {
            final SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(readerSize.getWidth(), readerSize.getHeight()); // We configure the size of default buffer to be the size of camera preview we want.

            final Surface surface = new Surface(texture); // This is the output Surface we need to start preview.

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);
            Log.i(TAG, "reader Size: " + readerSize.getWidth() + "x" + readerSize.getHeight());

            // reader: 카메라에서 읽어올 이미지
            reader = ImageReader.newInstance(readerSize.getWidth(), readerSize.getHeight(), ImageFormat.YUV_420_888, 2);
            reader.setOnImageAvailableListener(mOnGetPreviewListener, backgroundHandler);
            previewRequestBuilder.addTarget(reader.getSurface());

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice.createCaptureSession( Arrays.asList(surface, reader.getSurface()), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(final CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == cameraDevice) {
                                return;
                            }
                            // When the session is ready, we start displaying the preview.
                            captureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                previewRequestBuilder.set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                // Flash is automatically enabled when necessary.
                                previewRequestBuilder.set(
                                        CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                // Finally, we start displaying the camera preview.
                                previewRequest = previewRequestBuilder.build();
                                captureSession.setRepeatingRequest(
                                        previewRequest, captureCallback, backgroundHandler);
                            } catch (final CameraAccessException e) {
                                Log.i(TAG, "Exception!", e);
                            }
                        }
                        @Override
                        public void onConfigureFailed(final CameraCaptureSession cameraCaptureSession) {
                            //showToast("Failed");
                        }
                    },
                    null);
        } catch (final CameraAccessException e) {
            Log.i(TAG, "Exception!", e);
        }

        mOnGetPreviewListener.initialize(getActivity().getApplicationContext(),
                getActivity().getAssets(), inferenceHandler, backgroundHandler,
                mLabel, eyeOverlayView, textureView, UiHandler,
                bitmap_both, bitmap_left, bitmap_right);
    }


    /**
     *  이 메소드는 setUpCameraOutputs에서 카메라 미리보기 크기(readerSize)가 결정되고, 보여주는 화면의 크기(textureView)가 고정 된 후에 호출해야합니다.
     */
    @DebugLog
    private void configureTransform(final int viewWidth, final int viewHeight) {
        Dlog.d("configureTransform");

        final Activity activity = getActivity();
        if (null == textureView || null == readerSize || null == activity) {
            return;
        }
        final int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        final Matrix matrix = new Matrix();
        final RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        final RectF bufferRect = new RectF(0, 0, readerSize.getHeight(), readerSize.getWidth());
        final float centerX = viewRect.centerX();
        final float centerY = viewRect.centerY();

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            final float scale = Math.max( (float) viewHeight / readerSize.getHeight(),  (float) viewWidth / readerSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }

        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        int statusBarHeight = screenHeight - viewHeight;

        Dlog.d("statusBarHeight " + statusBarHeight);
        Dlog.d("viewHeight " + viewHeight);
        Dlog.d("screenHeight " + screenHeight);


        /**
         * mStateTextView.height 는 statusBar를 포함해서 전체 Display에 1/5
         * gforceFrameLayout 는 전체 Display에 3/5
         */
        int division = (screenHeight/5) - statusBarHeight;
        mStateTextView.getLayoutParams().height = division;
        Dlog.d("division " + division);

        int gforceHeightPixel = (screenHeight/5) * 3;
        gforceFrameLayout.getLayoutParams().height = gforceHeightPixel;
        Dlog.d("gforceHeightPixel " + gforceHeightPixel);


        /**
         * 눈이 카메라 가운데에 올수있도록 전체 Display에 1/5 이동
         */
        int yShift = (screenHeight/5);
        Dlog.d("yShift " + yShift);

        matrix.setTranslate(0, -yShift);
        textureView.setTransform(matrix);


        /**
         * textureView 와 eyeOverlaySurfaceView 에 TOP 좌표값을 맞추기 위해서
         * 추후에 detected eye 좌표값과 비교 로직을 위해서
         */
        eyeOverlaySurfaceView.setTranslationY( -yShift );
//        eyeOverlaySurfaceView.getLayoutParams().height = viewHeight;
        Dlog.d("eyeOverlaySurfaceView height " + eyeOverlaySurfaceView.getLayoutParams().height);

        eyeOverlayView.setAspectRatio(viewWidth, yShift, (yShift + yShift - statusBarHeight), statusBarHeight);
    }


    public static float dpFromPx(final Context context, final float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }


    public static float pxFromDp(final Context context, final float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }


    // Compares two Sizes based on their areas
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(final Size lhs, final Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum(
                    (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }


    public class UIChangeHandler extends Handler {

        public UIChangeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what){
                case FULL_CAPTURE_COMPLETE:
                    Dlog.d("FULL_CAPTURE_COMPLETE");
                    final Map<String, Bitmap[]> eyeBitmap = (Map<String, Bitmap[]>)msg.obj;

                    if(eyeBitmap == null){
                        ErrorDialog.newInstance("eyeBitmap is Null")
                                .show(getFragmentManager(), this.getClass().getSimpleName());
                    }

                    CameraActivity activity = ((CameraActivity)getActivity());
                    activity.goMain(eyeBitmap.get("bitmap_left"), eyeBitmap.get("bitmap_right"));
                    break;
                case STOP_ACTIVITY:
                    Dlog.d("STOP_ACTIVITY");
                    ((CameraActivity)getActivity()).finish();
                    break;
                case LOAD_VIEW_COMPLETE:
                    Dlog.d("LOAD_VIEW_COMPLETE");
                    ((CameraActivity)getActivity()).stopLoadingAnimation();
                    break;
                case QUALITY_CHECK:
                    final String message = String.valueOf(msg.obj);
                    Dlog.d("QUALITY_CHECK : " + message);
                    mStateTextView.setText(message);
                    mStateTextView.setTextColor(getResources().getColor(R.color.Red));
                    break;
            }
        }
    }


    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    }).create();
        }

    }

}
