/*
 * ConnectionLine.java
 *
 * Created on 4.7.2008, 9:43:39
 * hold to: KISS, YAGNI
 *
 * Copyright (C) 2008-2011 Peter Jakubčo <pjakubco at gmail.com>
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
 * The line can connect two and only two different elements.
 * 
 * @author vbmacher
 */
public class ConnectionLine {
    /**
     * Tolerance radius for user point selection, in pixels
     */
    public final static int TOLERANCE = 5;
    
    /**
     * First element in the connection.
     *
     * If the connection is not bidirectional, then from this element the data
     * flow flows. It means that this element can access the other element,
     */
    private Element e1;

    /**
     * The last element in the connection.
     *
     * If the connection is not bidirectional, then into this element the data
     * flow flows. It means, that this device can only return the results, but
     * can not activate the first element.
     */
    private Element e2;
    


    private ArrayList<Point> points;
    private static BasicStroke thickLine = new BasicStroke(2);

    /**
     * Whether this line is selected by the user
     */
    private boolean selected;

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
        this.selected = false;
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
        computeElementArrow(leftFactor, topFactor, true);
        if (!bidirectional) {
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

        int eX = first ? e1.getX() - e1.getWidth()/2: e2.getX() - e2.getWidth()/2;
        int eWidth = first ? e1.getWidth() : e2.getWidth();
        int eY = first ? e1.getY() - e1.getHeight()/2 : e2.getY() - e2.getHeight()/2;
        int eHeight = first ? e1.getHeight() : e2.getHeight();

        Point lineStart = new Point (eX + eWidth/2, eY + eHeight/2);
        Point lineEnd;

        x_left = lineStart.x - eWidth/2;
        x_right = lineStart.x + eWidth/2;
        y_bottom = lineStart.y + eHeight/2;
        y_top = lineStart.y - eHeight/2;

        if (points.isEmpty())
            lineEnd = new Point ((first ? e2.getX()-e2.getWidth()/2 
                      : e1.getX()-e1.getWidth()/2)
                    + (first ? e2.getWidth() : e1.getWidth())/2,
                    (first ? e2.getY() - e2.getHeight()/2
                      : e1.getY() - e1.getHeight()/2)
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
     * Determines whether a selection area crosses or overlays this line.
     *
     * If at least one intersection is found, then true is returned.
     *
     * @param selectionStart the selection start point
     * @param selectionEnd the selection end point
     * @return true if the line is crossing the selection area
     */
    public boolean isAreaCrossing(Point selectionStart, Point selectionEnd) {
        Point lineStart = new Point(e1.getX() + arrow1.x,
                e1.getY() + arrow1.y);

        Point lineEnd;
        Point intersection = null;

        for (int i = 0; i < points.size(); i++) {
            lineEnd = points.get(i);

            // test: left side of the selection
            Point bottomLeft = new Point(selectionStart.x, selectionEnd.y);
            intersection = intersection(selectionStart, bottomLeft, lineStart,
                    lineEnd);
            if ((intersection != null) && (intersection.x == selectionStart.x)
                    && (intersection.y >= selectionStart.y)
                    && (intersection.y <= selectionEnd.y)
                    && (getCrossPointAfter(intersection,1) != -1))
                return true;

            // test: right side of the selection
            Point topRight = new Point(selectionEnd.x, selectionStart.y);
            intersection = intersection(topRight, selectionEnd, lineStart,
                    lineEnd);
            if ((intersection != null) && (intersection.x == selectionEnd.x)
                    && (intersection.y >= selectionStart.y)
                    && (intersection.y <= selectionEnd.y)
                    && (getCrossPointAfter(intersection,1) != -1))
                return true;

            // test: top side of the selection
            intersection = intersection(selectionStart, topRight, lineStart,
                    lineEnd);
            if ((intersection != null) && (intersection.y == selectionStart.y)
                    && (intersection.x >= selectionStart.x)
                    && (intersection.x <= selectionEnd.x)
                    && (getCrossPointAfter(intersection,1) != -1))
                return true;

            // test: bottom side of the selection
            intersection = intersection(bottomLeft, selectionEnd, lineStart,
                    lineEnd);
            if ((intersection != null) && (intersection.y == selectionEnd.y)
                    && (intersection.x >= selectionStart.x)
                    && (intersection.x <= selectionEnd.x)
                    && (getCrossPointAfter(intersection,1) != -1))
                return true;

            lineStart = lineEnd;
        }
        lineEnd = new Point(e2.getX() + arrow2.x,
                e2.getY() + arrow2.y);

        // test: left side of the selection
        Point bottomLeft = new Point(selectionStart.x, selectionEnd.y);
        intersection = intersection(selectionStart, bottomLeft, lineStart,
                lineEnd);
        if ((intersection != null) && (intersection.x == selectionStart.x)
                && (intersection.y >= selectionStart.y)
                && (intersection.y <= selectionEnd.y)
                && (getCrossPointAfter(intersection,1) != -1))
            return true;

        // test: right side of the selection
        Point topRight = new Point(selectionEnd.x, selectionStart.y);
        intersection = intersection(topRight, selectionEnd, lineStart,
                lineEnd);
        if ((intersection != null) && (intersection.x == selectionEnd.x)
                && (intersection.y >= selectionStart.y)
                && (intersection.y <= selectionEnd.y)
                && (getCrossPointAfter(intersection,1) != -1))
            return true;

        // test: top side of the selection
        intersection = intersection(selectionStart, topRight, lineStart,
                lineEnd);
        if ((intersection != null) && (intersection.y == selectionStart.y)
                && (intersection.x >= selectionStart.x)
                && (intersection.x <= selectionEnd.x)
                && (getCrossPointAfter(intersection,1) != -1))
            return true;

        // test: bottom side of the selection
        intersection = intersection(bottomLeft, selectionEnd, lineStart,
                lineEnd);
        if ((intersection != null) && (intersection.y == selectionEnd.y)
                && (intersection.x >= selectionStart.x)
                && (intersection.x <= selectionEnd.x)
                && (getCrossPointAfter(intersection,1) != -1))
            return true;

        // if there is no intersection, maybe the line lies inside the selection
        // it is enough if we just compare any point if it lies inside the
        // selection

        if ((lineStart.x >= selectionStart.x) && (lineStart.x <= selectionEnd.x)
                && (lineStart.y >= selectionStart.y)
                && (lineStart.y <= selectionEnd.y))
            return true;
        return false;
    }

    /**
     * Select or deselect this line.
     *
     * @param selected true if the line should be selected, false otherwise
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Get the selection status.
     *
     * @return true if the line is selected, false otherwise.
     */
    public boolean isSelected() {
        return selected;
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
        if (selected)
            g.setColor(Color.BLUE);
        else
            g.setColor(Color.black);
        int x1 = e1.getX();
        int y1 = e1.getY();
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
        x2 = e2.getX();
        y2 = e2.getY();
        g.drawLine(x1, y1, x2, y2);

        // sipky - princip dedenia v UML <---> princip toku udajov => tok!!
        if ((arrow1 != null) && (arrow1LeftEnd != null)
                && (arrow1RightEnd != null)) {
            x1 = e1.getX() - e1.getWidth()/2;
            y1 = e1.getY() - e1.getHeight()/2;
            g.drawLine(x1 + arrow1.x,
                    y1 + arrow1.y,
                    x1 + arrow1.x + arrow1LeftEnd.x,
                    y1 + arrow1.y + arrow1LeftEnd.y);
            g.drawLine(x1 + arrow1.x,
                    y1 + arrow1.y,
                    x1 + arrow1.x + arrow1RightEnd.x,
                    y1 + arrow1.y + arrow1RightEnd.y);
        }
        if ((arrow2 !=null) && (arrow2LeftEnd != null)
                && (arrow2RightEnd != null)) {
            x2 = e2.getX() - e2.getWidth()/2;
            y2 = e2.getY() - e2.getHeight()/2;
            g.drawLine(x2 + arrow2.x,
                    y2 + arrow2.y,
                    x2 + arrow2.x + arrow2LeftEnd.x,
                    y2 + arrow2.y + arrow2LeftEnd.y);
            g.drawLine(x2 + arrow2.x,
                    y2 + arrow2.y,
                    x2 + arrow2.x + arrow2RightEnd.x,
                    y2 + arrow2.y + arrow2RightEnd.y);
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
        int x1 = ee1.getX();
        int y1 = ee1.getY();
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
     * Draws a small red circle around given point
     * 
     * @param selPoint the center of the circle 
     * @param g Graphics2D object
     */
    public static void highlightPoint(Point selPoint, Graphics2D g) {
        int xx = (int) selPoint.getX();
        int yy = (int) selPoint.getY();
        g.setColor(Color.WHITE);
        ((Graphics2D) g).setStroke(thickLine);
        g.fillOval(xx - TOLERANCE - 2, yy - TOLERANCE - 2,
                (TOLERANCE + 2) * 2, (TOLERANCE + 2) * 2);
        g.setColor(Color.BLACK);
        g.drawOval(xx - TOLERANCE, yy - TOLERANCE,
                TOLERANCE * 2, TOLERANCE * 2);

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
     * @return original point object of the line, null if the test point is
     * not included
     */
    public Point containsPoint(Point p) {
        int size = points.size();
        for (int i = 0; i < size; i++) {
            Point tmp = points.get(i);
            if ((tmp.x >= (p.x-TOLERANCE)) && (tmp.x <= (p.x + TOLERANCE))
                    && (tmp.y >= (p.y-TOLERANCE)) && (tmp.y <= (p.y+TOLERANCE)))
                return tmp;
        }
        return null;
    }
    
    /**
     * Determine if two points point to each other (with some tolerance).
     * 
     * @param linePoint First point (e.g. a line point)
     * @param selPoint Second point (e.g. mouse point)
     * @return true if given points point to each other, false otherwise.
     */
    public static boolean isPointSelected(Point linePoint, Point selPoint) {
        double d = Math.hypot(linePoint.getX() - selPoint.getX(),
                linePoint.getY() - selPoint.getY());
        return (d < TOLERANCE);
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
     * Moves all points of this line to a new location. The new location is
     * computed as: old + diff.
     *
     * @param diffX The X difference between new and old location
     * @param diffY The Y difference between new and old location
     */
    public void pointMoveAll(int diffX, int diffY) {
        for (int i = points.size()-1; i >= 0; i--) {
            Point p = points.get(i);
            p.setLocation(p.x + diffX, p.y + diffY);
        }
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
     * (with some tolerance)
     * 
     * d(X, p) = abs(a*x0 + b*y0 + c)/sqrt(a^2 + b^2)
     * 
     * and if yes, return index of a point of a cross point.
     *
     * @param point point that is checked
     * @param tolerance the tolerance
     * @return If line doesn't contain any point but point[x,y] is crossing the
     * line; or if point[x,y] is crossing near the beginning of the line, then 0
     * is returned. It means that new point should be added before first point.
     * And if point[x,y] doesn't cross the line, -1 is returned. Otherwise is
     * returned index of point that crosses the line.
     */
    public int getCrossPointAfter(Point point, double tolerance) {
        int x1 = e1.getX() + ((arrow1 == null) ? 0 : arrow1.x - e1.getWidth()/2);
        int y1 = e1.getY() + ((arrow1 == null) ? 0 : arrow1.y - e1.getHeight()/2);
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
            if (d < tolerance) {
                double l1 = Math.hypot(x1-point.x, y1-point.y);
                double l2 = Math.hypot(x2-point.x, y2-point.y);
                double l = Math.hypot(x2-x1, y2-y1);
                if ((l > l1) && (l > l2)) return i;
            }
            x1 = x2;
            y1 = y2;
        }
        x2 = e2.getX() + ((arrow2 != null) ? arrow2.x - e2.getWidth()/2 : 0);
        y2 = e2.getY() + ((arrow2 != null) ? arrow2.y - e2.getHeight()/2: 0);
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
