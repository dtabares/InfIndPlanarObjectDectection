package ar.edu.untref.infindplanarobjectdectection;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

public class Plane {
    private double[] horizontalLine;
    private double[] verticalLine;
    private double[] hypotenuse;
    private Point intersectPoint;
    private List<double[]> lineList;



    public Plane(double[] horizontalLine, double[] verticalLine, Point intersectPoint) {
        this.horizontalLine = horizontalLine;
        this.verticalLine = verticalLine;
        this.intersectPoint = intersectPoint;
        this.hypotenuse = new double[4];
        this.hypotenuse[0] = horizontalLine[2];
        this.hypotenuse[1] = horizontalLine[3];
        this.hypotenuse[2] = verticalLine[2];
        this.hypotenuse[3] = verticalLine[3];
        this.lineList = new ArrayList<>();
        this.lineList.add(this.horizontalLine);
        this.lineList.add(this.verticalLine);
        this.lineList.add(this.hypotenuse);
    }

    public List<double[]> getLineList(){
        return this.lineList;
    }

    public double[] getHorizontalLine() {
        return horizontalLine;
    }

    public void setHorizontalLine(double[] horizontalLine) {
        this.horizontalLine = horizontalLine;
    }

    public double[] getVerticalLine() {
        return verticalLine;
    }

    public void setVerticalLine(double[] verticalLine) {
        this.verticalLine = verticalLine;
    }

    public Point getIntersectPoint() {
        return intersectPoint;
    }

    public void setIntersectPoint(Point intersectPoint) {
        this.intersectPoint = intersectPoint;
    }

    public double[] getHypotenuse() {
        return hypotenuse;
    }

    public void setHypotenuse(double[] hypotenuse) {
        this.hypotenuse = hypotenuse;
    }
}
