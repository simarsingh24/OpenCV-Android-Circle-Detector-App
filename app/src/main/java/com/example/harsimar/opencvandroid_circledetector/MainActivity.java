package com.example.harsimar.opencvandroid_circledetector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import com.flurgle.camerakit.CameraKit;
import com.flurgle.camerakit.CameraListener;
import com.flurgle.camerakit.CameraView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private CameraView cameraView;
    private Button button;
    private ImageView capturedImage;
    private ImageView processedImage;

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
        cameraView.setJpegQuality(40);

        cameraView.setCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] picture) {
                super.onPictureTaken(picture);
                int INPUT_SIZE=227;
                Bitmap bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.length);
                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
                final Bitmap finalBitmap = bitmap;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        });
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
        button=(Button)findViewById(R.id.detect);
        capturedImage=(ImageView)findViewById(R.id.captured_image);
        processedImage=(ImageView)findViewById(R.id.processed_image);
    }
}