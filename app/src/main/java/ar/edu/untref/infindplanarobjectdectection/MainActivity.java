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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG="MainActivity";
    private int MY_PERMISSIONS_REQUEST_CAMERA;
    int frameNumber;
    List<double[]> horizontalLineList;
    List<double[]> verticalLineList;
    JavaCameraView javaCameraView;
    Mat mRgba;
    Mat result;
    Scalar[] triangleColours;
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
        javaCameraView.enableFpsMeter();
        triangleColours = new Scalar[3];
        triangleColours[0] = new Scalar(255,0,255); //violeta
        triangleColours[1] = new Scalar(153, 255, 102); //verde claro
        triangleColours[2] = new Scalar(102, 102, 255); //azul claro
        frameNumber = 0;
        horizontalLineList = new ArrayList<>();
        verticalLineList = new ArrayList<>();
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

    private boolean isSamePoint(Point a, Point b){
        double delta = 1;

        double difX = Math.abs(a.x - b.x);
        double difY = Math.abs(a.y - b.y);

        if(difX <= delta && difY <= delta){
            return true;
        }
        else {
            return false;
        }
    }

    private List<Point> refineCorners(List<Point> corners){
        List<Point> refinedCorners = new ArrayList<>();

        for (int i=0; i< corners.size(); i++){
            for (int j=i+1; j<corners.size();j++){
                if(isSamePoint(corners.get(i),corners.get(j))){
                    if(refinedCorners.contains(corners.get(i))){
                        refinedCorners.remove(corners.get(i));
                    }
                    refinedCorners.add(corners.get(j));
                }
            }
        }

        return refinedCorners;
    }

    private List<double[]> getLinesContainingPoint(Point p, Mat lineMatrix){
        List<double[]> lineList = new ArrayList<>();
        //recorrer todas las lineas, y ver si el punto pertenece, si pertene lo agregamos a la lista
        for (int i = 0; i < lineMatrix.rows(); i++) {
            double[] line = lineMatrix.get(i, 0);
            double x1 = line[0];
            double y1 = line[1];
            double x2 = line[2];
            double y2 = line[3];
            double x = p.x;
            double y = p.y;
            //Si son iguales el punto pertence a la recta
            if(((y - y1)/(x-x1)) == (y2-y1)/(x2-x1)){
                lineList.add(line);
            }
        }

        //devolvemos la lista
        return lineList;
    }


    private void separateHorizontalAndVerticalLines(Mat lineMatrix){
        double delta = 10;
        for (int i = 0; i < lineMatrix.rows(); i++) {
            double[] line = lineMatrix.get(i, 0);
            double x1 = line[0];
            double y1 = line[1];
            double x2 = line[2];
            double y2 = line[3];
            double angle = Math.atan2(y2 - y1, x2 - x1) * 180.0 / Math.PI;

            Log.d(TAG, "Lineas - Angulo : " + angle);

            if(Math.abs(Math.abs(angle) - 90) < delta){
                verticalLineList.add(line);
            }

            if(Math.abs(angle) < delta){
                horizontalLineList.add(line);
            }
        }

    }

    private Point getLineMiddlePoint(double[] line){
        Point middlePoint = new Point();
        double x1 = line[0];
        double y1 = line[1];
        double x2 = line[2];
        double y2 = line[3];
        middlePoint.x  = (x1 + x2) / 2;
        middlePoint.y = (y1 + y2) / 2;

        return middlePoint;
    }


    private void drawLines(Mat lines, Scalar color){
        for (int i = 0; i < lines.rows(); i++) {
            double[] val = lines.get(i, 0);
            Imgproc.line(result, new Point(val[0], val[1]), new Point(val[2], val[3]), color, 4);
        }
    }


    private void drawLines(List<double[]> lineList, Scalar color){
        for (int i = 0; i < lineList.size(); i++) {
            double[] val = lineList.get(i);
            Imgproc.line(result, new Point(val[0], val[1]), new Point(val[2], val[3]), color, 4);
        }
    }

    private void drawLine(double[] line, Scalar color){
            Imgproc.line(result, new Point(line[0], line[1]), new Point(line[2], line[3]), color, 4);
    }


    private void drawCircles(List<Point> points, Scalar color){
        for (int i =0; i<points.size(); i++){
            Imgproc.circle(result,points.get(i),3,color,2);
        }
    }

    private void drawCircle(Point point, Scalar color){
        Imgproc.circle(result,point,3,color,2);
    }



    private List<Plane> calculatePlanes(List<double[]> horizontalLineList, List<double[]> verticalLineList){
        List<Plane> planeList = new ArrayList<>();

        for(int i=0; i<horizontalLineList.size(); i++){
            for (int j = 0; j<verticalLineList.size(); j++){
                Point intersectPoint  = computeIntersect(horizontalLineList.get(i),verticalLineList.get(j));

                if(intersectPoint.x > 0 && intersectPoint.y > 0){
                    planeList.add(new Plane(horizontalLineList.get(i),verticalLineList.get(j),intersectPoint));
                    //Remuevo la linea vertical de la lista para que otra horizontal que la cruce no se cuente como plano
                    //Es para intentar evitar falsos positivos
                    verticalLineList.remove(j);
                    break;
                }
            }
        }
        return planeList;
    }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if (frameNumber == 0){
            mRgba = inputFrame.rgba();
            result = mRgba.clone();
            int threshold = 80;
            int minLineSize = 250;
            int lineGap = 50;
            //double rho = CV_HOUGH_STANDARD;
            double rho = 1;
            Mat lines = new Mat();
            //la transformo a escala de grises
            Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_RGB2GRAY);
            //Aplico medianBlur para remover el ruido de la imagen
            Imgproc.medianBlur(mRgba, mRgba, 5);
            Mat edges = new Mat();
            //Canny(Mat image, Mat edges, double threshold1, double threshold2, int apertureSize, boolean L2gradient)
            //Imgproc.Canny(mRgba, edges, 300, 600, 5, true);
            Imgproc.Canny(mRgba, edges, 100, 300, 3, true);
            //A partir de este punto, mRgba no se usa mas, libero memoria
            //mRgba.release();
            //Imgproc.HoughLinesP(mRgba,lines,CV_HOUGH_STANDARD,Math.PI/180, 7);
            Imgproc.HoughLinesP(edges, lines, rho, Math.PI/180, threshold, minLineSize, lineGap);
            //A partir de este punto, edges no se usa mas, libero memoria
            //edges.release();

            //Evito que explote si no tenemos ni una sola linea
            if(lines.rows() > 0){
                drawLines(lines,new Scalar(0, 255, 153));
                //Separo las lineas horizontales y verticales
                verticalLineList.clear();
                horizontalLineList.clear();
                separateHorizontalAndVerticalLines(lines);
                Log.d(TAG, "Lineas Horizontales #: " + horizontalLineList.size());
                Log.d(TAG, "Lineas Verticales #: " + verticalLineList.size());


                //Veo si entre las lineas horizontales y verticales tengo planos
                List<Plane> planeList = calculatePlanes(horizontalLineList,verticalLineList);
                int amountOfPlanes = planeList.size();
                Log.d(TAG, "Planos #: " + planeList.size());




                for (int i=0; i<planeList.size(); i++){

                    //Imprimo las lineas que componen los planos y su punto de interseccion
                    drawCircle(planeList.get(i).getIntersectPoint(), new Scalar(0,0,255));
                    drawLine(planeList.get(i).getHorizontalLine(),new Scalar(0, 255, 0));
                    drawLine(planeList.get(i).getVerticalLine(),new Scalar(255, 0, 0));


                    //Creo un poligono con 3 puntos
                    //Pto 1 = Pto de interseccion
                    //Pto 2 = Mitad de la linea Horizontal
                    //Pto 3 = Mitad de la linea Vertical

                    //Creo una lista para guardar los vertices del triangulo (poligono)
                    List<Point> listOfPoints = new ArrayList<>();

                    listOfPoints.add(planeList.get(i).getIntersectPoint());
                    listOfPoints.add(getLineMiddlePoint(planeList.get(i).getHorizontalLine()));
                    listOfPoints.add(getLineMiddlePoint(planeList.get(i).getVerticalLine()));

                    //Creo esta matriz de puntos a partir de la lista de puntos porque el metodo fillPoly que se usa para dibujar
                    //El triangulo lo necesita en ese formato
                    MatOfPoint matOfPoints = new MatOfPoint();
                    matOfPoints.fromList(listOfPoints);
                    List<MatOfPoint> matOfPointList = new ArrayList<>();
                    matOfPointList.add(matOfPoints);


                    //Dibujo el Poligono (triangulo)
                    //fillPoly(Mat img, java.util.List<MatOfPoint> pts,Scalar)
                    Imgproc.fillPoly(result,matOfPointList,triangleColours[i]);

                    //break;
                    //Por enunciado, solo dibujo hasta 3
                    if(i > 2){
                        break;
                    }
                }
            }
            lines.release();

        }
        frameNumber++;
        if(frameNumber == 10){
            frameNumber = 0;
        }
        return result;
    }
}
