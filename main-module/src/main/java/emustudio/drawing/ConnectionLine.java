/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
package emustudio.drawing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The connection line within the abstract schemas.
 *
 * The line can connect two and only two different elements.
 */
public class ConnectionLine {
    /**
     * The length of arrow arm
     */
    private final static int ARROW_LENGTH = 10;

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

    private final List<Point> points;

    private static final BasicStroke thickLine = new BasicStroke(2);

    private final Color lineColor;

    private boolean selected;

    private boolean bidirectional;

    /**
     * Begining arrow point for element1. The x and y values are relative
     * to the element middle-point.
     */
    private Point arrow1;
    private Point arrow1LeftEnd;
    private Point arrow1RightEnd;
    private final int[] xx1, yy1;

    /**
     * Begining arrow point for element2. The x and y values are relative
     * to the element middle-point.
     */
    private Point arrow2;
    private Point arrow2LeftEnd;
    private Point arrow2RightEnd;
    private final int[] xx2, yy2;

    private final Schema schema;

    public ConnectionLine(Element e1, Element e2, List<Point> points, Schema schema) {
        this.e1 = e1;
        this.e2 = e2;
        this.points = new ArrayList<>();
        if (points != null) {
            this.points.addAll(points);
        }

        this.lineColor = new Color(0x333333);
        this.bidirectional = true;
        this.selected = false;

        xx1 = new int[4];
        yy1 = new int[4];
        xx2 = new int[4];
        yy2 = new int[4];

        this.schema = schema;
    }

    private static Point intersection(Point l1s, Point l1e, Point l2s, Point l2e) {
        int div;
        int p_x, p_y;

        div = (l1s.x - l1e.x) * (l2s.y - l2e.y) - (l1s.y - l1e.y) * (l2s.x - l2e.x);

        // there is no intersection - lines are parallel
        if (div == 0) {
            return null;
        }

        p_x = ((l1s.x * l1e.y - l1s.y * l1e.x) * (l2s.x - l2e.x)- (l1s.x - l1e.x) * (l2s.x * l2e.y - l2s.y * l2e.x))/div;
        p_y = ((l1s.x * l1e.y - l1s.y * l1e.x) * (l2s.y - l2e.y)- (l1s.y - l1e.y) * (l2s.x * l2e.y - l2s.y * l2e.x))/div;

        return new Point(p_x, p_y);
    }

    private void computeArrows() {
        computeElementArrow(e2, e1);
        computeElementArrow(e1, e2);
        if (!bidirectional) {
            arrow1LeftEnd = null;
            arrow1RightEnd = null;
        }
    }

    /**
     * This method computes relative start points of "arrows" on line end.
     *
     * The direction is meant to be firstE -> secondE.
     *
     * @param firstE first (starting) element
     * @param secondE second (ending) element
     */
    private void computeElementArrow(Element firstE, Element secondE) {
        Point p, arrow;
        int x_left, x_right, y_bottom, y_top;

        Point lineStart = new Point (firstE.getX(), firstE.getY());
        Point lineEnd;

        int widthHalf = firstE.getWidth()/2;
        int heightHalf = firstE.getHeight()/2;

        x_left = lineStart.x -  widthHalf;
        x_right = lineStart.x + widthHalf;
        y_bottom = lineStart.y + heightHalf;
        y_top = lineStart.y - heightHalf;

        if (points.isEmpty()) {
            lineEnd = new Point (secondE.getX(), secondE.getY());
        } else {
            lineEnd = (firstE == e1) ? points.get(0) : points.get(points.size()-1);
            int x2 = lineEnd.x;
            int y2 = lineEnd.y;

            if (x2 < Schema.MIN_LEFT_MARGIN) {
                x2 = Schema.MIN_LEFT_MARGIN;
            }
            if (y2 < Schema.MIN_TOP_MARGIN) {
                y2 = Schema.MIN_TOP_MARGIN;
            }

            lineEnd = new Point(x2, y2);
            // if the line end is near element, modify line start
            if (lineEnd.x >= x_left && (lineEnd.x <= x_right)) {
                lineStart.x = lineEnd.x;
            }
            if (lineEnd.y >= y_top && (lineEnd.y <= y_bottom)) {
                lineStart.y = lineEnd.y;
            }
            x_left = lineStart.x - widthHalf;
            x_right = lineStart.x + widthHalf;
            y_bottom = lineStart.y + heightHalf;
            y_top = lineStart.y - heightHalf;
        }

        // test: bottom line of element1
        p = intersection(new Point(x_left,y_bottom), new Point(x_right, y_bottom), lineStart, lineEnd);
        arrow = null;
        if ((p != null) && (lineEnd.y > p.y) && (p.y == y_bottom) && (p.x >= x_left) && (p.x <= x_right)) {
            arrow = new Point(p.x, p.y);
        }

        if (arrow == null) {
            // test: top line of element1
            p = intersection(new Point(x_left,y_top), new Point(x_right, y_top), lineStart, lineEnd);
            if ((p != null) && (lineEnd.y < p.y) && (p.y == y_top) && (p.x >= x_left) && (p.x <= x_right)) {
                arrow = new Point(p.x, p.y);
            }
        }
        if (arrow == null) {
            // test: left line of element1
            p = intersection(new Point(x_left,y_bottom), new Point(x_left, y_top), lineStart, lineEnd);
            if ((p != null) && (lineEnd.x < p.x) && (p.x == x_left) && (p.y >= y_top) && (p.y <= y_bottom)) {
                arrow = new Point(p.x, p.y);
            }
        }
        if (arrow == null) {
            // test: right line of element1
            p = intersection(new Point(x_right,y_bottom), new Point(x_right, y_top), lineStart, lineEnd);
            if ((p != null) && (lineEnd.x > p.x) && (p.x == x_right) && (p.y >= y_top) && (p.y <= y_bottom)) {
                arrow = new Point(p.x, p.y);
            }
        }
        if (firstE == e1) {
            arrow1 = arrow;
        } else {
            arrow2 = arrow;
        }
        computeArrowEnds(lineStart, lineEnd, firstE);
    }

    /**
     * This method computes the positions of the arrows end lines.
     *
     * The direction is meant to be firstE -> secondE.
     *
     * @param lineStart start point of this line
     * @param lineEnd end point of this line
     * @param firstE first (starting) element
     */
    private void computeArrowEnds(Point lineStart, Point lineEnd, Element firstE) {

        double delta = Math.atan2(lineEnd.x - lineStart.x, lineEnd.y-lineStart.y);

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

        Point a_left = new Point((int)(ARROW_LENGTH * Math.sin(radians)), (int)(ARROW_LENGTH * Math.cos(radians)));
        Point a_right = new Point((int)(ARROW_LENGTH * Math.sin(mRadians)), (int)(ARROW_LENGTH * Math.cos(mRadians)));

        if (firstE == e1) {
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
    boolean isAreaCrossing(Point selectionStart, Point selectionEnd) {
        return isAreaCrossing(selectionStart, selectionEnd, 0);
    }

    /**
     * Determine if some line point crosses given area.
     *
     * @param selectionStart Left-top point of the area
     * @param selectionEnd Bottom-right point of the area
     * @return true if the area covers some line point, false otherwise (or if line doesn't have points)
     */
    boolean isAreaCrossingPoint(Point selectionStart, Point selectionEnd) {
        if (points.isEmpty()) {
            return false;
        }
        for (Point p : points) {
            if ((p.x >= selectionStart.x) && (p.x <= selectionEnd.x)
                    && (p.y >= selectionStart.y) && (p.y <= selectionEnd.y)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if given point crosses given area.
     *
     * @param selectionStart Left-top point of the area
     * @param selectionEnd Bottom-right point of the area
     * @param point The point
     * @return true if the area covers the point, false otherwise
     */
    static boolean isAreaCrossingPoint(Point selectionStart, Point selectionEnd, Point point) {
        return ((point.x >= selectionStart.x) && (point.x <= selectionEnd.x)
                && (point.y >= selectionStart.y) && (point.y <= selectionEnd.y));
    }

    private static double dot(Point v0, Point v1) {
        return (v0.x * v1.x) + (v0.y * v1.y);
    }

    private static boolean liesInTriangle(Point P, Point A, Point B, Point C) {
        // Compute vectors
        Point v0 = new Point(C.x - A.x, C.y - A.y);
        Point v1 = new Point(B.x - A.x, B.y - A.y);
        Point v2 = new Point(P.x - A.x, P.y - A.y);

        // Compute dot products
        double dot00 = dot(v0, v0);
        double dot01 = dot(v0, v1);
        double dot02 = dot(v0, v2);
        double dot11 = dot(v1, v1);
        double dot12 = dot(v1, v2);

        // Compute barycentric coordinates
        double invDenom = 1 / (dot00 * dot11 - dot01 * dot01);
        double u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        double v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        // Check if point is in triangle
        return (u >= 0) && (v >= 0) && (u + v < 1);
    }


    /**
     * Determine whether a line segment P1P2 crosses a triangle ABC.
     *
     * @param P1 start point of the line segment
     * @param P2 end point of the line segment
     * @param A first point of the triangle
     * @param B second point of the triangle
     * @param C third point of the triangle
     * @return true if the line segment crosses the triangle; false otherwise
     */
    private static boolean liesInTriangle(Point P1, Point P2, Point A, Point B, Point C) {
        int a = P2.y - P1.y;
        int b = P1.x - P2.x;
        int c = -a * P1.x - b * P1.y; // general line equation

        byte sign;
        boolean is = false;

        int result = a * A.x + b * A.y + c;
        if (result > 0) {
            sign = 1;
        } else if (result == 0) {
            sign = 0;
        } else {
            sign = -1;
        }
        result = a * B.x + b * B.y + c;
        is = isCrossing(sign, is, result);
        if (!is) {
            result = a * C.x + b * C.y + c;
            is = isCrossing(sign, is, result);
        }

        if (is) {
            // now we know that line is crossing the triangle.
            // we must check if the line segment crosses the triangle

            // A point is always left-top, B is always right, C is always under A
            if ((Math.min(P1.y, P2.y) <= C.y) && (Math.max(P1.y, P2.y) >= A.y)
                    && (Math.min(P1.x, P2.x) <= B.x) && (Math.max(P1.x, P2.x) >= A.x)) {
                return true;
            }
        } else {
            // last check if the line segment lies inside the triangle and does not cross it
            if (liesInTriangle(P1, A, B, C) && liesInTriangle(P2, A, B, C)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isCrossing(byte sign, boolean is, int result) {
        if ((result > 0) && (sign != 1)) {
            is = true;
        } else if ((result == 0) && (sign != 0)) {
            is = true;
        } else if ((result < 0) && (sign != -1)) {
            is = true;
        }
        return is;
    }

    /**
     * Determines whether a selection area crosses or overlays this line.
     *
     * If at least one intersection is found, then true is returned.
     * The algorithm is based on:
     * http://stackoverflow.com/questions/4497841/optimal-algorithm-if-line-intersects-convex-polygon
     *
     * @param selectionStart the selection start point
     * @param selectionEnd the selection end point
     * @param tolerance Tolerance of boundaries
     * @return true if the line is crossing the selection area
     */
    private boolean isAreaCrossing(Point selectionStart, Point selectionEnd, int tolerance) {
        Point v0 = selectionStart;
        Point v1 = new Point(selectionEnd.x, selectionStart.y);
        Point v2 = selectionEnd;
        Point v3 = new Point(selectionStart.x, selectionEnd.y);

        if ((arrow1 == null) || (arrow2 == null)) {
            computeArrows();
        }
        if ((arrow1 == null) || (arrow2 == null)) {
            return false;
        }
        Point lineStart = new Point(arrow1.x, arrow1.y);
        Point lineEnd;
        Point intersection;

        for (Point p : points) {
            lineEnd = p;

            intersection = intersection(v0, v2, lineStart, lineEnd);
            if (checkValidIntersection(v0, v1, v2, v3, lineStart, lineEnd, intersection)) {
                return true;
            }
            lineStart = lineEnd;
        }
        lineEnd = new Point(arrow2.x, arrow2.y);

        intersection = intersection(v0, v2, lineStart, lineEnd);
        return checkValidIntersection(v0, v1, v2, v3, lineStart, lineEnd, intersection);
    }

    private static boolean checkValidIntersection(Point v0, Point v1, Point v2, Point v3, Point lineStart, Point lineEnd, Point intersection) {
        if (intersection != null) {
            if ((intersection.x < v0.x) || (intersection.x > v2.x)
                    || (intersection.y < v0.y) || (intersection.y > v2.y)){
                intersection = null;
            } else if (intersection.x < Math.min(lineStart.x, lineEnd.x)
                    || (intersection.x > Math.max(lineStart.x, lineEnd.x))
                    || (intersection.y < Math.min(lineStart.y, lineEnd.y))
                    || (intersection.y > Math.max(lineStart.y, lineEnd.y))) {
                intersection = null;
            }
        }
        if (intersection == null) {
            // possible crossing (or overlay) can be on triangle (v0, v2, v3)
            if (liesInTriangle(lineStart, lineEnd, v0, v2, v3)) {
                return true;
            }
            // possible crossing (or overlay) can be on triangle (v0, v1, v2)
            if (liesInTriangle(lineStart, lineEnd, v0, v1, v2)) {
                return true;
            }
        } else {
            return true;
        }
        return false;
    }

    /**
     * Determines whether a selection area crosses or overlays a line segment.
     *
     * If at least one intersection is found, then true is returned.
     *
     * @param lineStart Start point of the line segment
     * @param lineEnd End point of the line segment
     * @param selectionStart the selection start point
     * @param selectionEnd the selection end point
     * @return true if the line segment is crossing the selection area; false otherwise
     */
    public static boolean isAreaCrossing(Point lineStart, Point lineEnd, Point selectionStart, Point selectionEnd) {
        Point v0 = selectionStart;
        Point v1 = new Point(selectionEnd.x, selectionStart.y);
        Point v2 = selectionEnd;
        Point v3 = new Point(selectionStart.x, selectionEnd.y);

        Point intersection = intersection(v0, v2, lineStart, lineEnd);
        return checkValidIntersection(v0, v1, v2, v3, lineStart, lineEnd, intersection);
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
     * @param preview whether to draw line points
     */
    public void draw(Graphics2D g, boolean preview) {
        if (selected) {
            g.setColor(Color.BLUE);
        } else {
            g.setColor(lineColor);
        }
        int x1 = e1.getX();
        int y1 = e1.getY();
        int x2, y2;

        int widthHalf1 = e1.getWidth()/2;
        int heightHalf1 = e1.getHeight()/2;

        Stroke ss = g.getStroke();
        g.setStroke(thickLine);
        int j = points.size()-1;
        for (int i = 0; i <= j; i++) {
            Point p = points.get(i);
            x2 = (int)p.getX();
            y2 = (int)p.getY();

            if (x2 < Schema.MIN_LEFT_MARGIN) {
                x2 = Schema.MIN_LEFT_MARGIN;
            }
            if (y2 < Schema.MIN_TOP_MARGIN) {
                y2 = Schema.MIN_TOP_MARGIN;
            }

            // if the line end is near element, modify line start in a wish
            // that the line was straight
            if (i == 0) {
                if (x2 >= (x1 - widthHalf1) && (x2 <= (x1 + widthHalf1))) {
                    x1 = x2;
                }
                if (y2 >= (y1 - heightHalf1) && (y2 <= (y1 + heightHalf1))) {
                    y1 = y2;
                }
            }

            if (!preview) {
                if (!selected) {
                    g.setColor(Color.LIGHT_GRAY);
                }
                g.fill3DRect(x2-1, y2-5, 2, 10, false); // .fillOval(x2 - 4, y2 - 4, 8, 8);
                g.fill3DRect(x2-5, y2-1, 10, 2, false);
                if (!selected) {
                    g.setColor(lineColor);
                }
            }
            g.drawLine(x1, y1, x2, y2);
            x1 = x2;
            y1 = y2;
        }
        x2 = e2.getX();
        y2 = e2.getY();

        int widthHalf2 = e2.getWidth()/2;
        int heightHalf2 = e2.getHeight()/2;

        if (j >= 0) { // only if line contains points, modify the line end in a
                      // wish that the line was straight
            if (x1 >= (x2 - widthHalf2) && (x1 <= (x2 + widthHalf2))) {
                x2 = x1;
            }
            if (y1 >= (y2 - heightHalf2) && (y1 <= (y2 + heightHalf2))) {
                y2 = y1;
            }
        }
        g.drawLine(x1, y1, x2, y2);

        // sipky - princip dedenia v UML <---> princip toku udajov => tok!!
        computeArrows();
        if ((arrow1 != null) && (arrow1LeftEnd != null) && (arrow1RightEnd != null)) {
            // nice arrow ends, filled triangles
            xx1[0] = arrow1.x;
            xx1[1] = arrow1.x + arrow1LeftEnd.x;
            xx1[2] = arrow1.x + arrow1RightEnd.x;
            xx1[3] = arrow1.x;
            yy1[0] = arrow1.y;
            yy1[1] = arrow1.y + arrow1LeftEnd.y;
            yy1[2] = arrow1.y + arrow1RightEnd.y;
            yy1[3] = arrow1.y;
            g.fillPolygon(xx1, yy1, 4);
        }
        if ((arrow2 !=null) && (arrow2LeftEnd != null) && (arrow2RightEnd != null)) {
            xx2[0] = arrow2.x;
            xx2[1] = arrow2.x + arrow2LeftEnd.x;
            xx2[2] = arrow2.x + arrow2RightEnd.x;
            xx2[3] = arrow2.x;
            yy2[0] = arrow2.y;
            yy2[1] = arrow2.y + arrow2LeftEnd.y;
            yy2[2] = arrow2.y + arrow2RightEnd.y;
            yy2[3] = arrow2.y;
            g.fillPolygon(xx2, yy2, 4);
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
    public static void drawSketch(Graphics2D g,Element ee1, Point ee2, List<Point> ppoints) {
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

            if (x2 < Schema.MIN_LEFT_MARGIN) {
                x2 = Schema.MIN_LEFT_MARGIN;
            }
            if (y2 < Schema.MIN_TOP_MARGIN) {
                y2 = Schema.MIN_TOP_MARGIN;
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
        g.setStroke(thickLine);
        g.fillOval(xx - TOLERANCE - 2, yy - TOLERANCE - 2, (TOLERANCE + 2) * 2, (TOLERANCE + 2) * 2);
  // TODO:     if (selected)
  //               g.setColor(Color.BLUE);
        g.setColor(Color.BLACK);
        g.drawOval(xx - TOLERANCE, yy - TOLERANCE, TOLERANCE * 2, TOLERANCE * 2);

    }

    /**
     * Adds a middle-point to this line.
     *
     * @param before index of the point before that a new point will be added
     * @param p the point that will be added
     */
    public void addPoint(int before, Point p) {
        points.add(before, p);
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
    public List<Point> getPoints() {
        return points;
    }

    /**
     * Test if given point is included within this line (with some tolerance).
     * If it is, return the original Point object, null otherwise.
     *
     * @param p Point to test
     * @param tolerance Tolerance
     * @return original point object of the line, null if the test point is
     * not included
     */
    public Point containsPoint(Point p, int tolerance) {
        int size = points.size();
        for (int i = 0; i < size; i++) {
            Point tmp = points.get(i);
            if ((tmp.x >= (p.x-tolerance)) && (tmp.x <= (p.x + tolerance))
                    && (tmp.y >= (p.y-tolerance)) && (tmp.y <= (p.y+tolerance))) {
                return tmp;
            }
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
    public static boolean doPointsEqual(Point linePoint, Point selPoint) {
        double d = Math.hypot(linePoint.getX() - selPoint.getX(),
                linePoint.getY() - selPoint.getY());
        return (d < TOLERANCE);
    }

    /**
     * Moves specific line point to a new location.
     *
     * If the point is not found, nothing is done.
     *
     * @param p the point that will be moved
     * @param newLocation new location of the point
     * @return true if the point was moved; false if the position was not valid or line point was not found.
     */
    public boolean movePoint(Point p, Point newLocation) {
        if (!schema.canMovePoint(newLocation.x, newLocation.y)) {
            return false;
        }

        int i = points.indexOf(p);
        if (i == -1) {
            return false;
        }
        points.get(i).setLocation(newLocation);
        return true;
    }

    /**
     * Moves all points of this line to a new location. The new location is
     * computed as: old + diff.
     *
     * @param diffX The X difference between new and old location
     * @param diffY The Y difference between new and old location
     */
    public void moveAllPoints(int diffX, int diffY) {
        if (!canMoveAllPoints(diffX, diffY)) {
            return;
        }
        for (Point point : points) {
            point.setLocation(point.x + diffX, point.y + diffY);
        }
        computeArrows();
    }

    /**
     * Determines if all points of this line can be moved to a new location.
     * The new location is computed as: old + diff.
     *
     * @param diffX The X difference between new and old location
     * @param diffY The Y difference between new and old location
     * @return true if all the points can be moved; false otherwise
     */
    public boolean canMoveAllPoints(int diffX, int diffY) {
        for (Point point : points) {
            if (!schema.canMovePoint(point.x + diffX, point.y + diffY)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks, whether this line is connected (from any side) to the specific
     * element.
     *
     * @param e element to what the connection is checked
     * @return true, if the line is connected to the element; false otherwise
     */
    public boolean containsElement(Element e) {
        return e1 == e || e2 == e;
    }

    /**
     * If the line is connected (from any side) to the first element, then
     * this method will replace it with new element.
     *
     * @param origin first, origin element
     * @param replacement replacement for the first element
     */
    public void replaceElement(Element origin, Element replacement) {
        if (this.e1 == origin) {
            this.e1 = replacement;
        }
        if (this.e2 == origin) {
            this.e2 = replacement;
        }
    }

    /**
     * Method determines whether point[x,y] crosses with this line.
     *
     * If yes, return index of a nearest cross point. If new point is
     * going to be added, it should be added to replace returned point index.
     *
     * @param point point that is checked
     * @param tolerance the tolerance
     * @return
     *     0 - if line doesn't contain any point, but point[x,y] is crossing the
     *         line; or if point[x,y] crosses the line near the beginning of it
     *
     *    -1 - if point[x,y] doesn't cross the line at all
     *
     *    points count - if point crosses line after last point, or before ending point of the line.
     *
     * Nearest point index of the line that the point crosses otherwise.
     */
    public int getCrossPoint(Point point, int tolerance) {
        if (point == null) {
            return -1;
        }
        if ((arrow1 == null) || (arrow2 == null)) {
            computeArrows();
        }
        if ((arrow1 == null) || (arrow2 == null)) {
            return -1;
        }
        Point X1 = new Point(arrow1.x, arrow1.y);
        Point X2;

        int pointsSize = points.size();
        for (int i = 0; i < pointsSize; i++) {
            X2 = points.get(i);
            double len = Math.hypot(X2.x - X1.x, X2.y - X1.y);
            Point2D.Double vector1 = new Point2D.Double((X2.x - X1.x) / len, (X2.y - X1.y) / len); // normalized vector
            Point2D.Double vector2 = new Point2D.Double((point.x - X1.x) / len, (point.y - X1.y) / len);

            /*
             * Cross product is an area of parallelogram with sides vector1 and vector2
             * The area is similar to area of original vectors.
             *
             * S  = a  * b  * sin(alfa)
             * S' = a' * b' * sin(alfa)
             *
             * a' = k * a
             * b' = k * b
             * S' = k^2 * a * b * sin(alfa)
             * S' = k^2 * S
             * ============================
             * b*sin(alfa) = S/a = S' / (k^2 * a)
             *
             * but k = 1/a, therefore
             *
             * b*sin(alfa) = distance = S * a (= crossproduct * len)
             *
             */
            double crossproduct = vector2.y * vector1.x - vector2.x * vector1.y;
            if ((Math.abs(crossproduct) * len <= tolerance)
                    && (Math.min(X1.x - tolerance, X2.x - tolerance) <= point.x)
                    && (point.x <= Math.max(X1.x + tolerance, X2.x + tolerance))
                    && (Math.min(X1.y - tolerance, X2.y - tolerance) <= point.y)
                    && (point.y <= Math.max(X1.y + tolerance, X2.y + tolerance))) {
                return i;
            }
            X1 = X2;
        }
        X2 = new Point(arrow2.x, arrow2.y);
        double len = Math.hypot(X2.x - X1.x, X2.y - X1.y);
        Point2D.Double vector1 = new Point2D.Double((X2.x - X1.x) / len, (X2.y - X1.y) / len); // normalized vector
        Point2D.Double vector2 = new Point2D.Double((point.x - X1.x) / len, (point.y - X1.y) / len);
        double crossproduct = vector2.y * vector1.x - vector2.x * vector1.y;
        if ((Math.abs(crossproduct) * len <= tolerance)
                && (Math.min(X1.x - tolerance, X2.x - tolerance) <= point.x)
                && (point.x <= Math.max(X1.x + tolerance, X2.x + tolerance))
                && (Math.min(X1.y - tolerance, X2.y - tolerance) <= point.y)
                && (point.y <= Math.max(X1.y + tolerance, X2.y + tolerance))) {
            return pointsSize; // as new point index
        }
        return -1;
    }

    /**
     * Get the first connection element of the line
     *
     * @return first connection element
     */
    public Element getJunc0() {
        return e1;
    }

    /**
     * Get the last connection element of the line
     *
     * @return last connection element
     */
    public Element getJunc1() {
        return e2;
    }

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
        if (isBidirectional()) {
            return;
        }
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
