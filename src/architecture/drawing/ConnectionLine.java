/*
 * ConnectionLine.java
 *
 * Created on 4.7.2008, 9:43:39
 * hold to: KISS, YAGNI
 *
 */

package architecture.drawing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.util.ArrayList;

/**
 *
 * @author vbmacher
 */
public class ConnectionLine {
    private Element e1;
    private Element e2;
    private ArrayList<Point> points;
    private BasicStroke thickLine;
    
    public ConnectionLine(Element e1, Element e2,
            ArrayList<Point> points) {
        this.e1 = e1;
        this.e2 = e2;
        this.points = new ArrayList<Point>();
        if (points != null)
            this.points.addAll(points);
        this.thickLine = new BasicStroke(2);
    }
    
    public void draw(Graphics2D g) {
        g.setColor(Color.black);
        int x1 = e1.getX() + e1.getWidth()/2;
        int y1 = e1.getY() + e1.getHeight()/2;
        int x2, y2;
        
        Stroke ss = g.getStroke();
        g.setStroke(thickLine);
        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            x2 = (int)p.getX();
            y2 = (int)p.getY();
            g.drawLine(x1, y1, x2, y2);
            x1 = x2;
            y1 = y2;
        }
        x2 = e2.getX() + e2.getWidth()/2;
        y2 = e2.getY() + e2.getHeight()/2;
        g.drawLine(x1, y1, x2, y2);
        g.setStroke(ss);
    }
    
    public static void drawSketch(Graphics2D g,Element ee1, Point ee2,
            ArrayList<Point> ppoints) {
        g.setColor(Color.black);
        int x1 = ee1.getX() + ee1.getWidth()/2;
        int y1 = ee1.getY() + ee1.getHeight()/2;
        int x2, y2;
        
        for (int i = 0; i < ppoints.size(); i++) {
            Point p = ppoints.get(i);
            x2 = (int)p.getX();
            y2 = (int)p.getY();
            g.drawLine(x1, y1, x2, y2);
            x1 = x2;
            y1 = y2;
        }
        if (ee2 != null) {
            x2 = (int)ee2.getX();
            y2 = (int)ee2.getY();
            g.drawLine(x1, y1, x2, y2);
        }
    }

    public void addPoint(int before, Point p) {
        points.add(before+1, p);
    }
    
    /**
     * Dont use, only if necessary (for loading configuration)
     */
    public void addPoint(Point p) {
        points.add(p);
    }
    
    public void removePoint(int index) {
        points.remove(index);
    }
    
    public ArrayList<Point> getPoints() {
        return points;
    }
    
    public void pointMove(int index, int x, int y) {
        points.get(index).setLocation(x, y);
    }
    
    public boolean containsElement(Element e) {
        if (e1 == e || e2 == e) return true;
        return false;
    }
    
    /**
     * Method determines whether point[x,y] crosses with this line
     * with some tolerance (5)
     * 
     * d(X, p) = abs(a*x0 + b*y0 + c)/sqrt(a^2 + b^2)
     * 
     * and if yes, return index of a point of a cross point.
     * 
     * @return If line doesn't contain any point but point[x,y] is crossing the
     * line; or if point[x,y] is crossing near the beginning of the line, then 0
     * is returned. It means that new point should be added before first point.
     * And if point[x,y] doesn't cross the line, -1 is returned. Otherwise is
     * returned index of point that crosses the line.
     */
    public int getCrossPointAfter(int x, int y) {
        int x1 = e1.getX() + e1.getWidth()/2;
        int y1 = e1.getY() + e1.getHeight()/2;
        int x2, y2;
        double a,b,c,d;
        
        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            x2 = (int)p.getX();
            y2 = (int)p.getY();
            
            // ciel: vseobecna rovnica priamky
            // smerovy vektor: s = (-b,a)
            // -b = x2-x1
            // a = y2-y1
            a = y2-y1;
            b = x1-x2;
            c = -a*x1-b*y1; // mam rovnicu
            d = Math.abs(a*x + b*y + c)/Math.hypot(a, b);
            if (d < 5) {
                double l1 = Math.hypot(x1-x, y1-y);
                double l2 = Math.hypot(x2-x, y2-y);
                double l = Math.hypot(x2-x1, y2-y1);
                if ((l > l1) && (l > l2)) return i;
            }
            x1 = x2;
            y1 = y2;
        }
        x2 = e2.getX() + e2.getWidth()/2;
        y2 = e2.getY() + e2.getHeight()/2;
        a = y2-y1;
        b = x1-x2;
        c = -a*x1-b*y1; // mam rovnicu
        d = Math.abs(a*x + b*y + c)/Math.hypot(a, b);
        if (d < 5) {
            double l1 = Math.hypot(x1-x, y1-y);
            double l2 = Math.hypot(x2-x, y2-y);
            double l = Math.hypot(x2-x1, y2-y1);
            if ((l > l1) && (l > l2)) return points.size();
        }
        return -1;
    }
    
    public Element getJunc0() { return e1; }
    public Element getJunc1() { return e2; }
}
