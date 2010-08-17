/*
 * ConnectionLine.java
 *
 * Created on 4.7.2008, 9:43:39
 * hold to: KISS, YAGNI
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package emustudio.architecture.drawing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.util.ArrayList;

/**
 * The connection line within the abstract schemas.
 * 
 * @author vbmacher
 */
public class ConnectionLine {
    private Element e1;
    private Element e2;

    private ArrayList<Point> points;
    private static BasicStroke thickLine = new BasicStroke(2);

    /**
     * Holds true, if the line connection is bidirectional, false otherwise.
     */
    private boolean bidirectional;

    /**
     * The length of arrow arm
     */
    private final static int arrow_length = 10;

    /**
     * Begining arrow point for element1. The x and y values are relative
     * to the element middle-point.
     */
    private Point arrow1;
    private Point arrow1LeftEnd;
    private Point arrow1RightEnd;

    /**
     * Begining arrow point for element2. The x and y values are relative
     * to the element middle-point.
     */
    private Point arrow2;
    private Point arrow2LeftEnd;
    private Point arrow2RightEnd;

    /**
     * Create new ConnectionLine object.
     *
     * @param e1 first connection element
     * @param e2 last connection element
     * @param points middle-ponits arraylist
     */
    public ConnectionLine(Element e1, Element e2,
            ArrayList<Point> points) {
        this.e1 = e1;
        this.e2 = e2;
        this.points = new ArrayList<Point>();
        if (points != null)
            this.points.addAll(points);

        this.bidirectional = true;
    }

    /**
     * This method computes an intersection of two lines (not line segments)
     * 
     * @param l1s Point of the first point of the first line
     * @param l1e Point of the ending point of the first line
     * @param l2s Point of the first point of the second line
     * @param l2e Point of the ending point of the second line
     * @return the intersection point of two lines if they have one; null
     * instead
     */
    private Point intersection(Point l1s, Point l1e, Point l2s, Point l2e) {
        int div;
        int p_x, p_y;

        div = (l1s.x - l1e.x) * (l2s.y - l2e.y)
                - (l1s.y - l1e.y) * (l2s.x - l2e.x);

        // there is no intersection - lines are parallel
        if (div == 0)
            return null;

        p_x = ((l1s.x * l1e.y - l1s.y * l1e.x) * (l2s.x - l2e.x)
                - (l1s.x - l1e.x) * (l2s.x * l2e.y - l2s.y * l2e.x))/div;
        p_y = ((l1s.x * l1e.y - l1s.y * l1e.x) * (l2s.y - l2e.y)
                - (l1s.y - l1e.y) * (l2s.x * l2e.y - l2s.y * l2e.x))/div;

        return new Point(p_x, p_y);
    }

    /**
     * This method computes one or bidirectional arrows positions for this 
     * line ends.
     * 
     * @param leftFactor x coordinate correction
     * @param topFactor  y coordinate correction
     */
    public void computeArrows(int leftFactor, int topFactor) {
        computeElementArrow(leftFactor, topFactor, false);
        if (bidirectional)
            computeElementArrow(leftFactor, topFactor, true);
        else {
            arrow1 = null;
            arrow1LeftEnd = null;
            arrow1RightEnd = null;
        }
    }

    /**
     * This method computes relative start points of "arrows" on line end.
     *
     * @param leftFactor x coordinate correction
     * @param topFactor  y coordinate correction
     * @param first whether to compute first direction (e1->e2),
     * or the other direction (e2->e1).
     */
    public void computeElementArrow(int leftFactor, int topFactor, boolean first) {
        Point p, arrow;
        int x_left, x_right, y_bottom, y_top;

        int eX = first ? e1.getX(): e2.getX();
        int eWidth = first ? e1.getWidth() : e2.getWidth();
        int eY = first ? e1.getY() : e2.getY();
        int eHeight = first ? e1.getHeight() : e2.getHeight();

        Point lineStart = new Point (eX + eWidth/2, eY + eHeight/2);
        Point lineEnd;

        x_left = lineStart.x - eWidth/2;
        x_right = lineStart.x + eWidth/2;
        y_bottom = lineStart.y + eHeight/2;
        y_top = lineStart.y - eHeight/2;

        if (points.isEmpty())
            lineEnd = new Point ((first ? e2.getX() : e1.getX())
                    + (first ? e2.getWidth() : e1.getWidth())/2,
                    (first ? e2.getY(): e1.getY())
                    + (first ? e2.getHeight() : e1.getHeight())/2);
        else {
            lineEnd = first ? points.get(0) : points.get(points.size()-1);
            int x2 = lineEnd.x  - leftFactor;
            int y2 = lineEnd.y  - topFactor;

            if (x2 < Element.MIN_LEFT_MARGIN)
                x2 = Element.MIN_LEFT_MARGIN;
            if (y2 < Element.MIN_TOP_MARGIN)
                y2 = Element.MIN_TOP_MARGIN;

            lineEnd = new Point(x2,y2);
        }

        // test: bottom line of element1
        p = intersection(new Point(x_left,y_bottom), new Point(x_right, y_bottom),
                 lineStart, lineEnd);
        arrow = null;
        if ((p != null) && (lineEnd.y > p.y) && (p.y == y_bottom)
                && (p.x >= x_left) && (p.x <= x_right))
            arrow = new Point(p.x-eX, p.y - eY);

        if (arrow == null) {
            // test: top line of element1
            p = intersection(new Point(x_left,y_top), new Point(x_right, y_top),
                     lineStart, lineEnd);
            if ((p != null) && (lineEnd.y < p.y) && (p.y == y_top)
                    && (p.x >= x_left) && (p.x <= x_right))
                arrow = new Point(p.x-eX, p.y - eY);
        }
        if (arrow == null) {
            // test: left line of element1
            p = intersection(new Point(x_left,y_bottom), new Point(x_left, y_top),
                     lineStart, lineEnd);
            if ((p != null) && (lineEnd.x < p.x) && (p.x == x_left)
                    && (p.y >= y_top) && (p.y <= y_bottom))
                arrow = new Point(p.x-eX, p.y - eY);
        }
        if (arrow == null) {
            // test: right line of element1
            p = intersection(new Point(x_right,y_bottom), new Point(x_right, y_top),
                     lineStart, lineEnd);
            if ((p != null) && (lineEnd.x > p.x) && (p.x == x_right)
                    && (p.y >= y_top) && (p.y <= y_bottom))
                arrow = new Point(p.x-eX, p.y - eY);
        }
        if (first)
            arrow1 = arrow;
        else
            arrow2 = arrow;
        computeArrowEnds(lineStart, lineEnd, first);
    }

    /**
     * This method computes the positions of the arrows end lines.
     *
     * @param lineStart start point of this line
     * @param lineEnd end point of this line
     * @param first whether to compute the position of the first direction arrow
     * (e1->e2), or the other direction arrow (e2->e1)
     */
    private void computeArrowEnds(Point lineStart,
           Point lineEnd, boolean first) {

        double delta = Math.atan2(lineEnd.x - lineStart.x,
                lineEnd.y-lineStart.y);

        /*
           |*
           | *
         h |de*
           |   *
           |----*
             w

           tg(de) =  w/h;
           de = tg-1(w/h)
        */
        // line formula: y - y1 = m * (x - x1)

        // circle: x = r * sin(phi)
        //         y = r * cos(phi)

        double radians = Math.toRadians(30) + delta;
        double mRadians = Math.toRadians(-30) + delta;

        Point a_left = new Point((int)(arrow_length * Math.sin(radians)),
                (int)(arrow_length * Math.cos(radians)));
        Point a_right = new Point((int)(arrow_length * Math.sin(mRadians)),
                (int)(arrow_length * Math.cos(mRadians)));

        if (first) {
            arrow1LeftEnd = a_left;
            arrow1RightEnd = a_right;
        } else {
            arrow2LeftEnd = a_left;
            arrow2RightEnd = a_right;
        }
    }

    /**
     * Draws this connection line.
     *
     * @param g Graphics2D object, where to draw the line
     */
    public void draw(Graphics2D g) {
        draw(g,0,0);
    }

    /**
     * Draws this connection line.
     *
     * @param g Graphics2D object, where to draw the line
     * @param leftFactor correction of the line (line will be moved backward to
     *        the leftFactor value, in the X coordinate)
     * @param topFactor correction of the line (line will be moved upward to
     *        the topFactor value, in the Y coordinate)
     */
    public void draw(Graphics2D g, int leftFactor, int topFactor) {
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

            x2 -= leftFactor;
            y2 -= topFactor;

            if (x2 < Element.MIN_LEFT_MARGIN) {
                x2 = Element.MIN_LEFT_MARGIN;
            }
            if (y2 < Element.MIN_TOP_MARGIN) {
                y2 = Element.MIN_TOP_MARGIN;
            }

            g.drawLine(x1, y1, x2, y2);
            g.fillOval(x2-3, y2-3, 6, 6);
            x1 = x2;
            y1 = y2;
        }
        x2 = e2.getX() + e2.getWidth()/2;
        y2 = e2.getY() + e2.getHeight()/2;
        g.drawLine(x1, y1, x2, y2);

        // sipky - princip dedenia v UML <---> princip toku udajov => tok!!
        if (arrow1 != null) {
            g.drawLine(e1.getX() + arrow1.x,
                    e1.getY() + arrow1.y,
                    e1.getX() + arrow1.x + arrow1LeftEnd.x,
                    e1.getY() + arrow1.y + arrow1LeftEnd.y);
            g.drawLine(e1.getX() + arrow1.x,
                    e1.getY() + arrow1.y,
                    e1.getX() + arrow1.x + arrow1RightEnd.x,
                    e1.getY() + arrow1.y + arrow1RightEnd.y);
        }
        if (arrow2 != null) {
            g.drawLine(e2.getX() + arrow2.x,
                    e2.getY() + arrow2.y,
                    e2.getX() + arrow2.x + arrow2LeftEnd.x,
                    e2.getY() + arrow2.y + arrow2LeftEnd.y);
            g.drawLine(e2.getX() + arrow2.x,
                    e2.getY() + arrow2.y,
                    e2.getX() + arrow2.x + arrow2RightEnd.x,
                    e2.getY() + arrow2.y + arrow2RightEnd.y);
        }
        g.setStroke(ss);
    }

    /**
     * This method draws a "sketch" line - in the process when user tries
     * to draw a connection line. It is based on fixed first element ee1, where
     * the line begins, and it continues through the points defined in the
     * ppoints arraylist. The last point is defined by the point ee2.
     *
     * @param g graphics object, where to draw the sketch line.
     * @param ee1 first element
     * @param ee2 last point
     * @param ppoints array of middle-points
     */
    public static void drawSketch(Graphics2D g,Element ee1, Point ee2,
            ArrayList<Point> ppoints) {
        Stroke ss = g.getStroke();
        g.setStroke(thickLine);
        g.setColor(Color.GRAY);
        int x1 = ee1.getX() + ee1.getWidth()/2;
        int y1 = ee1.getY() + ee1.getHeight()/2;
        int x2, y2;
        
        for (int i = 0; i < ppoints.size(); i++) {
            Point p = ppoints.get(i);
            x2 = (int)p.getX();
            y2 = (int)p.getY();

            if (x2 < Element.MIN_LEFT_MARGIN) {
                x2 = Element.MIN_LEFT_MARGIN;
            }
            if (y2 < Element.MIN_TOP_MARGIN) {
                y2 = Element.MIN_TOP_MARGIN;
            }

            g.drawLine(x1, y1, x2, y2);
            g.fillOval(x2-3, y2-3, 6, 6);
            x1 = x2;
            y1 = y2;
        }
        if (ee2 != null) {
            x2 = (int)ee2.getX();
            y2 = (int)ee2.getY();
            g.drawLine(x1, y1, x2, y2);
        }
        g.setStroke(ss);
    }

    /**
     * Adds a middle-point to this line.
     *
     * @param before index of the point before that a new point will be added
     * @param p the point that will be added
     */
    public void addPoint(int before, Point p) {
        points.add(before+1, p);
    }
    
    /**
     * Adds a middle-point to this line.
     *
     * Dont use, only if necessary (for loading configuration)
     *
     * @param p the point that will be added
     */
    public void addPoint(Point p) {
        points.add(p);
    }

    /**
     * Removes a middle-point if exists.
     *
     * @param p the point that will be removed
     */
    public void removePoint(Point p) {
        points.remove(p);
    }

    /**
     * Get all middle-points within this line.
     *
     * @return arraylist of all middle-points
     */
    public ArrayList<Point> getPoints() {
        return points;
    }

    /**
     * Test if given point is included within this line (with some tolerance).
     * If it is, return the original Point object, null otherwise.
     *
     * @param p Point to test
     * @param tolerance tolerance radius
     * @return original point object of the line, null if the test point is
     * not included
     */
    public Point containsPoint(Point p, int tolerance) {
        int size = points.size();
        for (int i = 0; i < size; i++) {
            Point tmp = points.get(i);
            if ((tmp.x >= (p.x-tolerance)) && (tmp.x <= (p.x + tolerance))
                    && (tmp.y >= (p.y-tolerance)) && (tmp.y <= (p.y+tolerance)))
                return tmp;
        }
        return null;
    }

    /**
     * Moves specific middle-point to a new location.
     *
     * If the point is not found, nothing is done.
     *
     * @param p the point that will be moved
     * @param newLocation new location of the point
     */
    public void pointMove(Point p, Point newLocation) {
        int i = points.indexOf(p);
        if (i == -1)
            return;
        points.get(i).setLocation(newLocation);
    }

    /**
     * Checks, whether this line is connected (from any side) to the specific
     * element.
     *
     * @param e element to what the connection is checked
     * @return true, if the line is connected to the element; false otherwise
     */
    public boolean containsElement(Element e) {
        if (e1 == e || e2 == e) return true;
        return false;
    }

    /**
     * If the line is connected (from any side) to the first element, then
     * this method will replace it with new element.
     *
     * @param e1 first, origin element
     * @param e2 replacement for the first element
     */
    public void replaceElement(Element e1, Element e2) {
        if (this.e1 == e1)
            this.e1 = e1;
        if (this.e2 == e2)
            this.e2 = e2;
    }

    /**
     * Method determines whether point[x,y] crosses with this line
     * with some tolerance (5)
     * 
     * d(X, p) = abs(a*x0 + b*y0 + c)/sqrt(a^2 + b^2)
     * 
     * and if yes, return index of a point of a cross point.
     *
     * @param point point that is checked
     * @return If line doesn't contain any point but point[x,y] is crossing the
     * line; or if point[x,y] is crossing near the beginning of the line, then 0
     * is returned. It means that new point should be added before first point.
     * And if point[x,y] doesn't cross the line, -1 is returned. Otherwise is
     * returned index of point that crosses the line.
     */
    public int getCrossPointAfter(Point point) {
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
            d = Math.abs(a*point.x + b*point.y + c)/Math.hypot(a, b);
            if (d < 5) {
                double l1 = Math.hypot(x1-point.x, y1-point.y);
                double l2 = Math.hypot(x2-point.x, y2-point.y);
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
        d = Math.abs(a*point.x + b*point.y + c)/Math.hypot(a, b);
        if (d < 5) {
            double l1 = Math.hypot(x1-point.x, y1-point.y);
            double l2 = Math.hypot(x2-point.x, y2-point.y);
            double l = Math.hypot(x2-x1, y2-y1);
            if ((l > l1) && (l > l2)) return points.size();
        }
        return -1;
    }

    /**
     * Get the first connection element of the line
     *
     * @return first connection element
     */
    public Element getJunc0() { return e1; }

    /**
     * Get the last connection element of the line
     *
     * @return last connection element
     */
    public Element getJunc1() { return e2; }

    /**
     * Returns the direction of the elements connection. The connection
     * can be the one-directional, or bidirectional.
     *
     * If the connection is one-directional, then the connection direction is
     * getJunc0() -> getJunc1(). If there is a need to switch the direction,
     * use the switchDirection() method.
     *
     * If the connection is bidirectional, then the direction is in the form:
     * getJunc0 <-> getJunc1().
     *
     * @return true if the connection is bidirectional; false otherwise
     */
    public boolean isBidirectional() {
        return bidirectional;
    }

    /**
     * This method switches the direction of the connection. If the connection
     * is bidirectional, it has no implications.
     *
     * Otherwise, it switches a value returned from a call to getJunc0() method
     * with the value returned from a call to getJunc1() method.
     */
    public void switchDirection() {
        if (isBidirectional())
            return;
        Element e = e1;
        e1 = e2;
        e2 = e;
    }

    /**
     * Sets the connection direction. If the parameter is true, the connection
     * will be bidirectional. Otherwise, it will be one-directional.
     *
     * @param bidi whether the connection should be bidirectional
     */
    public void setBidirectional(boolean bidi) {
        bidirectional = bidi;
    }
}
