package com.example.harsimar.opencvandroid_circledetector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.flurgle.camerakit.CameraKit;
import com.flurgle.camerakit.CameraListener;
import com.flurgle.camerakit.CameraView;

import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

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
    private static double dp,param1,param2;
    private static int minRadius,maxRadius;

    static {
        System.loadLibrary("opencv_java3");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        linkViews();

        cameraView.setFocus(CameraKit.Constants.FOCUS_CONTINUOUS);
        cameraView.setMethod(CameraKit.Constants.METHOD_STILL);
        cameraView.setJpegQuality(100);
        capture_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                param2= Double.parseDouble(param2Text.getText().toString());
                param1= Double.parseDouble(param1Text.getText().toString());
                dp= Double.parseDouble(dpText.getText().toString());
                minRadius= Integer.parseInt(minRText.getText().toString());
                maxRadius=Integer.parseInt(maxRText.getText().toString());
                takePicture();
            }
        });

        cameraView.setCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] picture) {
                super.onPictureTaken(picture);
                int INPUT_SIZE=400;
                Bitmap original_bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.length);
                //original_bitmap = Bitmap.createScaledBitmap(original_bitmap, INPUT_SIZE, INPUT_SIZE, false);
                final Bitmap finalBitmap = original_bitmap;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        processedImage.setImageBitmap(opencvCenterDetect(finalBitmap));
                    }
                });

            }
        });
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



/* reduce the noise so we avoid false circle detection */
        Imgproc.GaussianBlur(grayMat, grayMat, new Size(9, 9), 2, 2);
// accumulator value
        //double dp = 1d;
// minimum distance between the center coordinates of detected circles in pixels
        double minDist = grayMat.rows()/8;//double minDist = 100; original
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

        Log.d("harsimarSingh","circles = "+numberOfCircles);
/* draw the circles found on the image */
        for (int i=0; i<numberOfCircles; i++) {


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

    /* circle's center outline */
            Imgproc.rectangle(mat, new Point(x - 5, y - 5),
                    new Point(x + 5, y + 5),
                    new Scalar(0, 128, 255), -1);
        }

/* convert back to bitmap */
        Utils.matToBitmap(mat, bitmap);
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
        cameraView=(CameraView)findViewById(R.id.cameraView);
        capture_button =(Button)findViewById(R.id.detect);
        processedImage=(ImageView)findViewById(R.id.processed_image);
        param1Text=(EditText)findViewById(R.id.param1_edit_text);
        param2Text=(EditText)findViewById(R.id.param2_edit_text);
        dpText=(EditText)findViewById(R.id.dp_edit_text);
        minRText=(EditText)findViewById(R.id.minR_editText);
        maxRText=(EditText)findViewById(R.id.maxR_editText);


        processedImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

    }
}