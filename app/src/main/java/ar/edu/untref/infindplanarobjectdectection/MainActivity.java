package ar.edu.untref.infindplanarobjectdectection;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.CV_HOUGH_STANDARD;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG="MainActivity";
    private int MY_PERMISSIONS_REQUEST_CAMERA;
    JavaCameraView javaCameraView;
    Mat mRgba;
    Mat lines;
    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this){
        @Override
        public void onManagerConnected(int status){
            switch(status){
                case BaseLoaderCallback.SUCCESS:{
                    javaCameraView.enableView();
                    break;
                }
                default:{
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    static {
        if(OpenCVLoader.initDebug()){
            Log.d(TAG, "Opencv sucessfully loaded");
        }
        else{
            Log.d(TAG, "Opencv not loaded");
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "tiene permisos");

        } else {
            Log.v(TAG, "no tiene permisos");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }
        javaCameraView = (JavaCameraView) findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(javaCameraView!=null){
            javaCameraView.disableView();
        }
    }
    @Override
    protected void onResume(){
        super.onResume();
        if(OpenCVLoader.initDebug()){
            Log.d(TAG, "Opencv sucessfully loaded");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else{
            Log.d(TAG, "Opencv not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9,this,baseLoaderCallback);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height,width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        Mat result = mRgba.clone();
        int threshold = 50;
        int minLineSize = 20;
        int lineGap = 20;
        lines = new Mat();
        //la transformo a escala de grises
        Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_RGB2GRAY);
        //Aplico medianBlur para remover el ruido de la imagen
        Imgproc.medianBlur(mRgba, mRgba, 5);
        Mat edges = new Mat();
        Imgproc.Canny(mRgba, edges, 300, 600, 5, true);
        //Imgproc.HoughLines(mRgba,lines,CV_HOUGH_STANDARD, Math.PI/180, 150);
        //Imgproc.HoughLinesP(mRgba,lines,CV_HOUGH_STANDARD,Math.PI/180, 7);
        Imgproc.HoughLinesP(edges, lines, 1, Math.PI/180, threshold, minLineSize, lineGap);
        Log.d(TAG, "Lines Rows: " + lines.rows());
        Log.d(TAG, "Lines Cols: " + lines.cols());
        if(lines.rows() > 0){
            for (int i = 0; i < lines.rows(); i++) {
                double[] val = lines.get(i, 0);
                //Imgproc.line(mRgba, new Point(val[0], val[1]), new Point(val[2], val[3]), new Scalar(0, 255, 0), 4);
                Imgproc.line(result, new Point(val[0], val[1]), new Point(val[2], val[3]), new Scalar(0, 255, 0), 4);
            }
        }
        edges.release();
        lines.release();
        mRgba.release();
        return result;
    }
}
