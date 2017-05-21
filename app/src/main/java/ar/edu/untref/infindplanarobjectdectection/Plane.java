package ar.edu.untref.infindplanarobjectdectection;

import org.opencv.core.Point;

public class Plane {
    private double[] horizontalLine;
    private double[] verticalLine;
    private Point intersectPoint;


    public Plane(double[] horizontalLine, double[] verticalLine, Point intersectPoint) {
        this.horizontalLine = horizontalLine;
        this.verticalLine = verticalLine;
        this.intersectPoint = intersectPoint;
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
}
