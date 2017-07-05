package com.example.harsimar.opencvandroid_circledetector;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.flurgle.camerakit.CameraKit;
import com.flurgle.camerakit.CameraListener;
import com.flurgle.camerakit.CameraView;

import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.R.attr.bitmap;

public class MainActivity extends AppCompatActivity {
    private CameraView cameraView;
    private Button capture_button;
    private ImageView processedImage;
    private EditText dpText;
    private EditText param1Text;
    private EditText param2Text;
    private EditText minRText;
    private EditText maxRText;
    private static double dp, param1, param2;
    private static int minRadius, maxRadius;

    static {
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        linkViews();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cameraView.setFocus(CameraKit.Constants.FOCUS_CONTINUOUS);
        cameraView.setMethod(CameraKit.Constants.METHOD_STILL);
        cameraView.setJpegQuality(80);
        capture_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                param2 = Double.parseDouble(param2Text.getText().toString());
                param1 = Double.parseDouble(param1Text.getText().toString());
                dp = Double.parseDouble(dpText.getText().toString());
                minRadius = Integer.parseInt(minRText.getText().toString());
                maxRadius = Integer.parseInt(maxRText.getText().toString());
                takePicture();
            }
        });

        cameraView.setCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] picture) {
                super.onPictureTaken(picture);
                int INPUT_SIZE = 400;
                Bitmap original_bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.length);
                //original_bitmap = Bitmap.createScaledBitmap(original_bitmap, INPUT_SIZE, INPUT_SIZE, false);
                final Bitmap finalBitmap = original_bitmap;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        processedImage.setImageBitmap(opencvCenterKV(finalBitmap));
                    }
                });

            }
        });
    }

    private Bitmap opencvCenterDetectMethod2(Bitmap bitmap) {///for black background
        Mat mat = new Mat(bitmap.getWidth(), bitmap.getHeight(),
                CvType.CV_8UC1);

        Mat grayMat = new Mat(bitmap.getWidth(), bitmap.getHeight(),
                CvType.CV_8UC1);

        Utils.bitmapToMat(bitmap, mat);


/* convert to grayscale */
        int colorChannels = (mat.channels() == 3) ? Imgproc.COLOR_BGR2GRAY
                : ((mat.channels() == 4) ? Imgproc.COLOR_BGRA2GRAY : 1);

        Imgproc.cvtColor(mat, grayMat, colorChannels);


        Mat threshed = new Mat(bitmap.getWidth(),bitmap.getHeight(), CvType.CV_8UC1);
        Imgproc.adaptiveThreshold(grayMat, threshed, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 75, 5);//15, 8 were original tests. Casey was 75,10
        Core.bitwise_not(threshed, threshed);
        Utils.matToBitmap(threshed, bitmap);

        Mat dilated = new Mat(bitmap.getWidth(),bitmap.getHeight(), CvType.CV_8UC1);
        Imgproc.dilate(threshed, dilated,
                Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new org.opencv.core.Size (16, 16)));
        Utils.matToBitmap(dilated, bitmap);
        Mat eroded = new Mat(bitmap.getWidth(),bitmap.getHeight(), CvType.CV_8UC1);
        Imgproc.erode(dilated, eroded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new org.opencv.core.Size(15, 15)));
        Utils.matToBitmap(eroded, bitmap);
        Mat circles = new Mat();

        int iCannyUpperThreshold = 100;
        int iMinRadius = 20;
        int iMaxRadius = 400;
        int iAccumulator = 100;

        Imgproc.HoughCircles(grayMat, circles, Imgproc.CV_HOUGH_GRADIENT,
                1.0, grayMat.rows() / 8, iCannyUpperThreshold, iAccumulator,
                iMinRadius, iMaxRadius);

// draw
        if (circles.cols() > 0)
        {
            Toast.makeText(this, "Coins : " +circles.cols() , Toast.LENGTH_LONG).show();
        }
        else
        {
            Toast.makeText(this, "No coins found", Toast.LENGTH_LONG).show();
        }
        return bitmap;
    }

    private Bitmap opencvCenterDetect(Bitmap bitmap) {

        Mat mat = new Mat(bitmap.getWidth(), bitmap.getHeight(),
                CvType.CV_8UC1);

        Mat grayMat = new Mat(bitmap.getWidth(), bitmap.getHeight(),
                CvType.CV_8UC1);

        Utils.bitmapToMat(bitmap, mat);


/* convert to grayscale */
        int colorChannels = (mat.channels() == 3) ? Imgproc.COLOR_BGR2GRAY
                : ((mat.channels() == 4) ? Imgproc.COLOR_BGRA2GRAY : 1);

        Imgproc.cvtColor(mat, grayMat, colorChannels);

        //Imgproc.blur(grayMat,grayMat,new Size(9, 9));
       // Mat tempM=new Mat();

        Imgproc.GaussianBlur(grayMat,grayMat, new Size(9, 9), 2, 2);
        //Imgproc.bilateralFilter(grayMat,tempM,15,80,80,Core.BORDER_DEFAULT);

///* reduce the noise so we avoid false circle detection */
// accumulator value
        //double dp = 1d;
// minimum distance between the center coordinates of detected circles in pixels
       // double minDist = grayMat.rows() / 8;//double minDist = 100; original
            double minDist=100;
// min and max radii (set these values as you desire)
        //int minRadius = 20, maxRadius = 100;

// param1 = gradient value used to handle edge detection
// param2 = Accumulator threshold value for the
// cv2.CV_HOUGH_GRADIENT method.
// The smaller the threshold is, the more circles will be
// detected (including false circles).
// The larger the threshold is, the more circles will
// potentially be returned.
        // double param1 =
        // , param2 = 60;//70,72 original

/* create a Mat object to store the circles detected */
        Mat circles = new Mat(bitmap.getWidth(),
                bitmap.getHeight(), CvType.CV_8UC1);

/* find the circle in the image */
        Imgproc.HoughCircles(grayMat, circles,
                Imgproc.CV_HOUGH_GRADIENT, dp, minDist, param1,
                param2, minRadius, maxRadius);


/* get the number of circles detected */
        int numberOfCircles = (circles.rows() == 0) ? 0 : circles.cols();

        Log.d("harsimarSingh", "circles = " + numberOfCircles);
/* draw the circles found on the image */
        for (int i = 0; i < numberOfCircles; i++) {


/* get the circle details, circleCoordinates[0, 1, 2] = (x,y,r)
 * (x,y) are the coordinates of the circle's center
 */
            double[] circleCoordinates = circles.get(0, i);


            int x = (int) circleCoordinates[0], y = (int) circleCoordinates[1];

            Point center = new Point(x, y);

            int radius = (int) circleCoordinates[2];

    /* circle's outline */
            Imgproc.circle(mat, center, radius, new Scalar(0,
                    255, 0), 4);
            Imgproc.putText(mat,"Rad = "+radius,center,5,2,new Scalar(0,100,100));
    /* circle's center outline */
            Imgproc.rectangle(mat, new Point(x - 5, y - 5),
                    new Point(x + 5, y + 5),
                    new Scalar(0, 128, 255), -1);
        }

        Utils.matToBitmap(grayMat,bitmap);
      /*
        /////////////////////////////////
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "title");
        values.put(MediaStore.Images.Media.BUCKET_ID, "test");
        values.put(MediaStore.Images.Media.DESCRIPTION, "test Image taken");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.
                EXTERNAL_CONTENT_URI, values);
        OutputStream outstream;
        try {
            outstream = getContentResolver().openOutputStream(uri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outstream);
            outstream.close();
        } catch (FileNotFoundException e) {
            //
        } catch (IOException e) {
            //
        }*/
/* convert back to bitmap */
        Utils.matToBitmap(mat, bitmap);
/////////////////////////////
        /*
        values.put(MediaStore.Images.Media.TITLE, "title");
        values.put(MediaStore.Images.Media.BUCKET_ID, "test");
        values.put(MediaStore.Images.Media.DESCRIPTION, "test Image taken");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        uri = getContentResolver().insert(MediaStore.Images.Media.
                EXTERNAL_CONTENT_URI, values);
        try {
            outstream = getContentResolver().openOutputStream(uri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outstream);
            outstream.close();
        } catch (FileNotFoundException e) {
            //
        } catch (IOException e) {
            //
        }*/
/////////////////////////////////////////
        takePicture();
        return bitmap;
    }

    private Bitmap opencvCenterKV(Bitmap bitmap) {

        Mat mat = new Mat(bitmap.getWidth(), bitmap.getHeight(),
                CvType.CV_8UC3);
        Mat grayMat = new Mat(bitmap.getWidth(), bitmap.getHeight(),
                CvType.CV_8UC1);
        Mat rgbMat = new Mat(bitmap.getWidth(),bitmap.getHeight(),CvType.CV_8UC3);
        Mat hsvMat=new Mat(bitmap.getWidth(),bitmap.getHeight(),CvType.CV_8UC3);
        Mat thresh= new Mat(bitmap.getWidth(), bitmap.getHeight(),
                CvType.CV_8UC3);
        Mat opening= new Mat(bitmap.getWidth(), bitmap.getHeight(),
                CvType.CV_8UC3);

        Utils.bitmapToMat(bitmap, mat);
        Imgproc.cvtColor(mat,rgbMat,Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(rgbMat, hsvMat, Imgproc.COLOR_RGB2HSV);
        Imgproc.cvtColor(hsvMat,grayMat,Imgproc.COLOR_RGB2GRAY);

        Imgproc.threshold(grayMat,thresh,0,255,Imgproc.THRESH_BINARY_INV+Imgproc.THRESH_OTSU);

        Size size=new Size(10,10);
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,size);

        Imgproc.morphologyEx(thresh,opening,1,kernel);

        Utils.matToBitmap(thresh, bitmap);



        //Imgproc.blur(grayMat,grayMat,new Size(9, 9));
        // Mat tempM=new Mat();

//        Imgproc.GaussianBlur(grayMat,grayMat, new Size(9, 9), 2, 2);
//        //Imgproc.bilateralFilter(grayMat,tempM,15,80,80,Core.BORDER_DEFAULT);
//
/////* reduce the noise so we avoid false circle detection */
//// accumulator value
//        //double dp = 1d;
//// minimum distance between the center coordinates of detected circles in pixels
//        // double minDist = grayMat.rows() / 8;//double minDist = 100; original
//        double minDist=100;
//// min and max radii (set these values as you desire)
//        //int minRadius = 20, maxRadius = 100;
//
//// param1 = gradient value used to handle edge detection
//// param2 = Accumulator threshold value for the
//// cv2.CV_HOUGH_GRADIENT method.
//// The smaller the threshold is, the more circles will be
//// detected (including false circles).
//// The larger the threshold is, the more circles will
//// potentially be returned.
//        // double param1 =
//        // , param2 = 60;//70,72 original
//
///* create a Mat object to store the circles detected */
//        Mat circles = new Mat(bitmap.getWidth(),
//                bitmap.getHeight(), CvType.CV_8UC1);
//
///* find the circle in the image */
//        Imgproc.HoughCircles(grayMat, circles,
//                Imgproc.CV_HOUGH_GRADIENT, dp, minDist, param1,
//                param2, minRadius, maxRadius);
//
//
///* get the number of circles detected */
//        int numberOfCircles = (circles.rows() == 0) ? 0 : circles.cols();
//
//        Log.d("harsimarSingh", "circles = " + numberOfCircles);
///* draw the circles found on the image */
//        for (int i = 0; i < numberOfCircles; i++) {
//
//
///* get the circle details, circleCoordinates[0, 1, 2] = (x,y,r)
// * (x,y) are the coordinates of the circle's center
// */
//            double[] circleCoordinates = circles.get(0, i);
//
//
//            int x = (int) circleCoordinates[0], y = (int) circleCoordinates[1];
//
//            Point center = new Point(x, y);
//
//            int radius = (int) circleCoordinates[2];
//
//    /* circle's outline */
//            Imgproc.circle(mat, center, radius, new Scalar(0,
//                    255, 0), 4);
//            Imgproc.putText(mat,"Rad = "+radius,center,5,2,new Scalar(0,100,100));
//    /* circle's center outline */
//            Imgproc.rectangle(mat, new Point(x - 5, y - 5),
//                    new Point(x + 5, y + 5),
//                    new Scalar(0, 128, 255), -1);
//        }

//        Utils.matToBitmap(grayMat,bitmap);
      /*
        /////////////////////////////////
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "title");
        values.put(MediaStore.Images.Media.BUCKET_ID, "test");
        values.put(MediaStore.Images.Media.DESCRIPTION, "test Image taken");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.
                EXTERNAL_CONTENT_URI, values);
        OutputStream outstream;
        try {
            outstream = getContentResolver().openOutputStream(uri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outstream);
            outstream.close();
        } catch (FileNotFoundException e) {
            //
        } catch (IOException e) {
            //
        }*/
/* convert back to bitmap */
//        Utils.matToBitmap(mat, bitmap);
/////////////////////////////
        /*
        values.put(MediaStore.Images.Media.TITLE, "title");
        values.put(MediaStore.Images.Media.BUCKET_ID, "test");
        values.put(MediaStore.Images.Media.DESCRIPTION, "test Image taken");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        uri = getContentResolver().insert(MediaStore.Images.Media.
                EXTERNAL_CONTENT_URI, values);
        try {
            outstream = getContentResolver().openOutputStream(uri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outstream);
            outstream.close();
        } catch (FileNotFoundException e) {
            //
        } catch (IOException e) {
            //
        }*/
/////////////////////////////////////////
        takePicture();
        return bitmap;
    }

    private void takePicture() {
        cameraView.captureImage();
    }

    @Override
    protected void onResume() {
        cameraView.start();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }

    private void linkViews() {
        cameraView = (CameraView) findViewById(R.id.cameraView);
        capture_button = (Button) findViewById(R.id.detect);
        processedImage = (ImageView) findViewById(R.id.processed_image);
        param1Text = (EditText) findViewById(R.id.param1_edit_text);
        param2Text = (EditText) findViewById(R.id.param2_edit_text);
        dpText = (EditText) findViewById(R.id.dp_edit_text);
        minRText = (EditText) findViewById(R.id.minR_editText);
        maxRText = (EditText) findViewById(R.id.maxR_editText);


        processedImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

    }
}