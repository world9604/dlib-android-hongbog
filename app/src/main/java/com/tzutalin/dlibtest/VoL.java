package com.tzutalin.dlibtest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;

import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.CV_8UC1;

/**
 * Created by jslee on 2018-08-06.
 */

public class VoL {
    private Bitmap bitCrop_L;
    private Bitmap bitCrop_R;
    private Bitmap bitCrop_eye;

    public int mBlur_R;
    public int mBlur_L;

    public VoL() {

    }

    public VoL(Bitmap bitCrop_eye, Bitmap bitCrop_L, Bitmap bitCrop_R) {
        this.bitCrop_eye = bitCrop_eye;
        this.bitCrop_L = bitCrop_L;
        this.bitCrop_R = bitCrop_R;
    }

    public int calculateBlur (Bitmap bitmap){

        int blur = Var_of_Laplacian(bitmap);
        //Log.i("Blur", "Blur:" + String.valueOf(bitmap));

        return blur;

    }

    private int Var_of_Laplacian(Bitmap bitmap) {
        // S: int, F: float, U: unsigned char
        // ARGB_8888 ==  CV_8UC4 8bit x4 = 32bit (0-)
        // Gray      ==  CV_8U   8bit            (0-255)

        Mat matbit = new Mat(bitmap.getWidth(), bitmap.getHeight(), CV_32F);  //CV_32F   CV_32FC3 (CV_32SC3: 32bit-float)  CV_8UC4
        Mat matGray = new Mat();
        Mat matGrayLapl = new Mat();
        Mat matGrayEquli= new Mat();
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble std = new MatOfDouble();

        // 1. 입력으로 들어온 bitmap 이미지(ARGB_8888-32bit)를 Mat 형식으로 바꾼다.
        Utils.bitmapToMat(bitmap, matbit);

        // 2.
        Imgproc.cvtColor(matbit, matGray, Imgproc.COLOR_BGR2GRAY);

        // 3. int
        Imgproc.Laplacian(matGray, matGrayLapl, CV_8UC1, 3, 1, 0); // -> Gray    CV_16S   CV_16SC3

        //Imgproc.equalizeHist(matGray, matGrayEquli);

        // converting back to CV_8U
        //Core.convertScaleAbs( matGrayL, matGrayL );

        // Imgproc.Laplacian(matGray, matGray, CV_64F, 3,1,0);

        Core.meanStdDev(matGrayLapl, mean, std);
        int stdDev = (int) (std.get(0, 0)[0]);
        // stdDev = Double.parseDouble(String.format("%.0f",stdDev));

        // -----------------Laplacian 이미지 print
        //matGrayLapl.convertTo(matGrayLapl, CV_8UC1);  // CV_8UC1
        //Bitmap savebit = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(matGrayLapl, savebit);
        //ImageUtils.saveBitmap(savebit, "L" + String.valueOf(stdDev));

        // -----------------equalized 이미지 print
        //matGrayEquli.convertTo(matGrayEquli, CV_8UC1);  // CV_8UC1
        //Bitmap savebit2 = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        //Utils.matToBitmap(matGrayEquli, savebit2);
        //ImageUtils.saveBitmap(savebit2, "E" + String.valueOf(stdDev));



        // -----------------------


        return stdDev;
    }





    public int getmBlur_R() {
        return mBlur_R;
    }

    public int getmBlur_L() {
        return mBlur_L;
    }
}



/*
    public boolean isBlur(Bitmap bitCrop_L, Bitmap bitCrop_R){

        Mat matGray = new Mat();
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble std = new MatOfDouble();


// Bitmap -> Mat(matOri, matLeft, matRight)
// 전체 Mat: matOri
Mat matOri = new Mat (mBitmap.getWidth(), mBitmap.getHeight(), CV_32F);  //CV_8UC1
        Utils.bitmapToMat(mBitmap, matOri);
                // 왼쪽눈 Mat: matLeft
                Mat matLeft = new Mat (bitCrop_L.getWidth(), bitCrop_L.getHeight(), CV_32F);  //CV_8UC1
                Utils.bitmapToMat(bitCrop_L, matLeft);
                // 오른쪽눈 Mat: matRight
                Mat matRight = new Mat (bitCrop_R.getWidth(), bitCrop_R.getHeight(), CV_32F);
                Utils.bitmapToMat(bitCrop_R, matRight);

                // blur : cv2.Laplacian(image, cv2.CV_64F).var()--------------------
                // 전체
                Imgproc.cvtColor(matOri, matGray, Imgproc.COLOR_BGR2GRAY);
                //Imgproc.Laplacian(matLeftGray, matLeftGray, CV_16S); //-> Gray CV_16S
                Imgproc.Laplacian(matGray, matGray, CV_16S, 3, 1, 0, BORDER_DEFAULT );
                Core.meanStdDev(matGray, mean, std);
                this.mBlur_Bitmap = std.get(0,0)[0];
                Log.i(TAG, "Blur all:" + String.valueOf(this.mBlur_Bitmap));

                // 왼쪽눈
                Imgproc.cvtColor(matLeft, matGray, Imgproc.COLOR_BGR2GRAY);
                //Imgproc.Laplacian(matLeftGray, matLeftGray, CV_16S); //-> Gray CV_16S
                Imgproc.Laplacian(matGray, matGray, CV_16S, 3, 1, 0, BORDER_DEFAULT );
                Core.meanStdDev(matGray, mean, std);
                //Imgproc.Laplacian(matLeft,matLeft,CV_64F);//-> Color
                //Core.meanStdDev(matLeft, mean, std);
                //this.mBlur_L = Math.pow(std.get(0,0)[0], 2);  //double stdDev = std.get(0,0)[0];
                this.mBlur_L = std.get(0,0)[0];  //double stdDev = std.get(0,0)[0];
                Log.i(TAG,"Blur Left:"+String.valueOf( this.mBlur_L));

                // 오른쪽눈
                Imgproc.cvtColor(matLeft, matGray, Imgproc.COLOR_BGR2GRAY);
                Imgproc.Laplacian(matGray, matGray, CV_16S); //-> Gray
                Core.meanStdDev(matGray, mean, std);
                //Imgproc.Laplacian(matLeft,matLeft,CV_64F);//-> Color
                //Core.meanStdDev(matLeft, mean, std);
                this.mBlur_R = std.get(0,0)[0]; // Math.pow(std.get(0,0)[0], 2);  //double stdDev = std.get(0,0)[0];
                Log.i(TAG,"Blur Right:"+String.valueOf( this.mBlur_R));

                boolean blur = false;
                if(mBlur_L>0 && mBlur_R > 0){
                blur = true;
                }

                return blur;
                }
 */