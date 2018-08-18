package com.hongbog.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.hongbog.viewmodel.Ball;
import com.tzutalin.dlibtest.Dlog;
import com.tzutalin.dlibtest.R;
import com.tzutalin.dlibtest.SensorDTO;
import com.tzutalin.dlibtest.SensorListener;

/**
 * Created by taein on 2018-07-12.
 */

public class BallSurFaceView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private DrawThread mThread;
    private final int BALL_SIZE = 0;
    private Handler mHandler;
    private Bitmap mBitmap;

    public BallSurFaceView(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ball);
        mHolder = getHolder();
        mHolder.setFormat(PixelFormat.TRANSLUCENT);
        mHolder.addCallback(this);
        mHolder.setFixedSize(getWidth(), getHeight());
        setFocusable(true);
        setZOrderOnTop(true);
        mHandler = new SensorChangeHandler();
        SensorListener.setHandler(mHandler);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mThread = new DrawThread();
        mThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        if(mThread != null){
            mThread.setSize(width, height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder paramSurfaceHolder) {
        boolean retry = true;

        mThread.setExit(true);

        while (retry) {
            try {
                mThread.join();
                retry = false;
            } catch (InterruptedException e) {
                Dlog.e("InterruptedException Message : " + e.getMessage());
                return;
            }
        }
    }

    private class DrawThread extends Thread{
        private boolean bExit;
        private int dspWidth, dspHeight;
        private int ballSize;
        private SurfaceHolder mHolder;
        private float acelX;
        private float acelZ;
        private Ball ball;

        DrawThread(){
            mHolder = getHolder();
            bExit = false;
            ball = new Ball();
        }

        public void setExit(boolean exit){
            bExit = exit;
        }

        public void setAcel(float x, float z){
            acelX = x;
            acelZ = z;
        }

        public void setSize(int width, int height) {
            dspWidth = width;
            dspHeight = height;
            ballSize = (dspWidth / (16 - BALL_SIZE) / 2);
        }

        public void run(){

            while(bExit == false){
                Canvas canvas = null;
                try {
                    canvas = mHolder.lockCanvas();
                    ball.calcBall(acelX, acelZ, dspWidth, dspHeight, ballSize);

                    synchronized (mHolder) {
                        if (canvas == null) break;
                        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                        canvas.drawBitmap(mBitmap, ball.getBallSrc(), ball.getBallDst(), null);
                    }
                }catch(IllegalArgumentException ex){
                    Dlog.e("IllegalArgumentException Message : " + ex.getMessage());
                    mThread.setExit(true);
                    return;
                }finally {
                    if(canvas != null){
                        mHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
    }

    public class SensorChangeHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 0:
                    SensorDTO sensorDTO = (SensorDTO)msg.obj;
                    if(mThread != null && mThread.isAlive()){
                        mThread.setAcel(sensorDTO.getAccelX(), sensorDTO.getAccelZ());
                    }
                    break;
            }
        }
    }

}