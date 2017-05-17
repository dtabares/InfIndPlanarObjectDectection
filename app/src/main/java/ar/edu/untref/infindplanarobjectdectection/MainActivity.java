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

    private Point computeIntersect(double[] lineA, double[] lineB){
        double x1 = lineA[0];
        double y1 = lineA[1];
        double x2 = lineA[2];
        double y2 = lineA[3];
        double x3 = lineB[0];
        double y3 = lineB[1];
        double x4 = lineB[2];
        double y4 = lineB[3];
        double d =  ((x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4));

        if(d != 0){
            //float d = (float)e;
            double x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
            double y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
            return new Point(x,y);
        }
        else {
            return new Point(-1,-1);
        }
    }

    private List<Point> getCorners(Mat lines){
        List<Point> corners = new ArrayList<>();
        for (int i = 0; i < lines.rows(); i++) {
            for (int j = i+1; j < lines.rows(); j++) {
                Point point  = computeIntersect(lines.get(i, 0),lines.get(j, 0));
                if(point.x > 0 && point.y > 0){
                    corners.add(point);
                }
            }
        }
        return corners;
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        Mat result = mRgba.clone();
        int threshold = 140;
        int minLineSize = 100;
        int lineGap = 50;
        //double rho = CV_HOUGH_STANDARD;
        double rho = 1;
        lines = new Mat();
        //la transformo a escala de grises
        Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_RGB2GRAY);
        //Aplico medianBlur para remover el ruido de la imagen
        Imgproc.medianBlur(mRgba, mRgba, 5);
        Mat edges = new Mat();
        //Canny(Mat image, Mat edges, double threshold1, double threshold2, int apertureSize, boolean L2gradient)
        //Imgproc.Canny(mRgba, edges, 300, 600, 5, true);
        Imgproc.Canny(mRgba, edges, 50, 150, 3, true);
        //Imgproc.HoughLinesP(mRgba,lines,CV_HOUGH_STANDARD,Math.PI/180, 7);
        Imgproc.HoughLinesP(edges, lines, rho, Math.PI/180, threshold, minLineSize, lineGap);
       // Log.d(TAG, "Lines Rows: " + lines.rows());
        //Log.d(TAG, "Lines Cols: " + lines.cols());
        if(lines.rows() > 0){


            //Imprimo las lineas
            for (int i = 0; i < lines.rows(); i++) {
                double[] val = lines.get(i, 0);
                //Imgproc.line(mRgba, new Point(val[0], val[1]), new Point(val[2], val[3]), new Scalar(0, 255, 0), 4);
                Imgproc.line(result, new Point(val[0], val[1]), new Point(val[2], val[3]), new Scalar(0, 255, 0), 4);
            }
            //Calculo las intersecciones
            List<Point> corners = getCorners(lines);

            //Dibujo las intersecciones
            Log.d(TAG, "Corners #: " + corners.size());
            for (int i =0; i<corners.size(); i++){
                Imgproc.circle(result,corners.get(i),3,new Scalar(0,0,255),2);
            }

            MatOfPoint2f corners_mat = new MatOfPoint2f();
            MatOfPoint2f diego = new MatOfPoint2f();
            corners_mat.fromList(corners);

            if (corners.size() > 0 ){
                //approxPolyDP(MatOfPoint2f curve, MatOfPoint2f approxCurve, double epsilon, boolean closed)
                Imgproc.approxPolyDP(corners_mat, diego, Imgproc.arcLength(corners_mat, true) * 0.02, true);

                Log.d(TAG, "diego rows #: " + diego.rows());
                Log.d(TAG, "diego cols #: " + diego.cols());
            }

        }
        edges.release();
        lines.release();
        mRgba.release();
        return result;
    }
}
