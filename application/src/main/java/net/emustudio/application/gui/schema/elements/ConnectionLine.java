/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.gui.schema.elements;

import net.emustudio.application.settings.PluginConnection;
import net.emustudio.application.settings.SchemaPoint;
import net.emustudio.application.gui.P;
import net.emustudio.application.gui.schema.Schema;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The connection line within the abstract schemas.
 * <p>
 * The line can connect two and only two different elements.
 */
public class ConnectionLine {
    private final static int ARROW_ARM_LENGTH = 10;
    private final static int SELECTION_TOLERANCE = 5;

    private final static BasicStroke thickLine = new BasicStroke(2);

    private final Color lineColor = new Color(0x333333);

    private Element elementFrom;
    private Element elementTo;
    private final List<P> points;
    private final boolean bidirectional;

    /**
     * Starting arrow point for elementFrom. The x and y values are relative to the element middle-point.
     */
    private P arrow1;
    private P arrow1LeftEnd;
    private P arrow1RightEnd;
    private final int[] xx1 = new int[4];
    private final int[] yy1 = new int[4];

    /**
     * Starting arrow point for elementTo. The x and y values are relative to the element middle-point.
     */
    private P arrow2;
    private P arrow2LeftEnd;
    private P arrow2RightEnd;
    private final int[] xx2 = new int[4];
    private final int[] yy2 = new int[4];

    private boolean selected = false;


    public ConnectionLine(Element from, Element to, List<P> points, boolean bidirectional) {
        this.elementFrom = Objects.requireNonNull(from);
        this.elementTo = Objects.requireNonNull(to);
        this.points = new ArrayList<>(Objects.requireNonNull(points));
        this.bidirectional = bidirectional;
    }

    private static P intersection(P l1s, P l1e, P l2s, P l2e) {
        double div;
        double p_x, p_y;

        div = (l1s.x - l1e.x) * (l2s.y - l2e.y) - (l1s.y - l1e.y) * (l2s.x - l2e.x);

        // there is no intersection - lines are parallel
        if (div == 0) {
            return null;
        }

        p_x = ((l1s.x * l1e.y - l1s.y * l1e.x) * (l2s.x - l2e.x) - (l1s.x - l1e.x) * (l2s.x * l2e.y - l2s.y * l2e.x)) / div;
        p_y = ((l1s.x * l1e.y - l1s.y * l1e.x) * (l2s.y - l2e.y) - (l1s.y - l1e.y) * (l2s.x * l2e.y - l2s.y * l2e.x)) / div;

        return P.of(p_x, p_y);
    }

    private void computeArrows() {
        computeElementArrow(elementTo, elementFrom);
        computeElementArrow(elementFrom, elementTo);
        if (!bidirectional) {
            arrow1LeftEnd = null;
            arrow1RightEnd = null;
        }
    }

    /**
     * This method computes relative start points of "arrows" on line end.
     * <p>
     * The direction is meant to be firstE -> secondE.
     *
     * @param firstE  first (starting) element
     * @param secondE second (ending) element
     */
    private void computeElementArrow(Element firstE, Element secondE) {
        P p, arrow;
        double x_left, x_right, y_bottom, y_top;

        P lineStart = P.of(firstE.getX(), firstE.getY());
        P lineEnd;

        int widthHalf = firstE.getWidth() / 2;
        int heightHalf = firstE.getHeight() / 2;

        x_left = lineStart.x - widthHalf;
        x_right = lineStart.x + widthHalf;
        y_bottom = lineStart.y + heightHalf;
        y_top = lineStart.y - heightHalf;

        if (points.isEmpty()) {
            lineEnd = P.of(secondE.getX(), secondE.getY());
        } else {
            lineEnd = (firstE == elementFrom) ? points.get(0) : points.get(points.size() - 1);
            double x2 = lineEnd.x;
            double y2 = lineEnd.y;

            if (x2 < Schema.MIN_LEFT_MARGIN) {
                x2 = Schema.MIN_LEFT_MARGIN;
            }
            if (y2 < Schema.MIN_TOP_MARGIN) {
                y2 = Schema.MIN_TOP_MARGIN;
            }

            lineEnd = P.of(x2, y2);
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
        p = intersection(P.of(x_left, y_bottom), P.of(x_right, y_bottom), lineStart, lineEnd);
        arrow = null;
        if ((p != null) && (lineEnd.y > p.y) && (p.y == y_bottom) && (p.x >= x_left) && (p.x <= x_right)) {
            arrow = p.copy();
        }

        if (arrow == null) {
            // test: top line of element1
            p = intersection(P.of(x_left, y_top), P.of(x_right, y_top), lineStart, lineEnd);
            if ((p != null) && (lineEnd.y < p.y) && (p.y == y_top) && (p.x >= x_left) && (p.x <= x_right)) {
                arrow = p.copy();
            }
        }
        if (arrow == null) {
            // test: left line of element1
            p = intersection(P.of(x_left, y_bottom), P.of(x_left, y_top), lineStart, lineEnd);
            if ((p != null) && (lineEnd.x < p.x) && (p.x == x_left) && (p.y >= y_top) && (p.y <= y_bottom)) {
                arrow = p.copy();
            }
        }
        if (arrow == null) {
            // test: right line of element1
            p = intersection(P.of(x_right, y_bottom), P.of(x_right, y_top), lineStart, lineEnd);
            if ((p != null) && (lineEnd.x > p.x) && (p.x == x_right) && (p.y >= y_top) && (p.y <= y_bottom)) {
                arrow = p.copy();
            }
        }
        if (firstE == elementFrom) {
            arrow1 = arrow;
        } else {
            arrow2 = arrow;
        }
        computeArrowEnds(lineStart, lineEnd, firstE);
    }

    /**
     * This method computes the positions of the arrows end lines.
     * <p>
     * The direction is meant to be firstE -> secondE.
     *
     * @param lineStart start point of this line
     * @param lineEnd   end point of this line
     * @param firstE    first (starting) element
     */
    private void computeArrowEnds(P lineStart, P lineEnd, Element firstE) {

        double delta = Math.atan2(lineEnd.x - lineStart.x, lineEnd.y - lineStart.y);

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

        P a_left = P.of(ARROW_ARM_LENGTH * Math.sin(radians), ARROW_ARM_LENGTH * Math.cos(radians));
        P a_right = P.of(ARROW_ARM_LENGTH * Math.sin(mRadians), ARROW_ARM_LENGTH * Math.cos(mRadians));

        if (firstE == elementFrom) {
            arrow1LeftEnd = a_left;
            arrow1RightEnd = a_right;
        } else {
            arrow2LeftEnd = a_left;
            arrow2RightEnd = a_right;
        }
    }

    /**
     * Determines whether a selection area crosses or overlays this line.
     * <p>
     * If at least one intersection is found, then true is returned.
     * <p>
     * The algorithm is based on:
     * http://stackoverflow.com/questions/4497841/optimal-algorithm-if-line-intersects-convex-polygon
     *
     * @param selectionStart the selection start point
     * @param selectionEnd   the selection end point
     * @return true if the line is crossing the selection area
     */
    public boolean isAreaCrossing(Point selectionStart, Point selectionEnd) {
        P v1 = P.of(selectionEnd.x, selectionStart.y);
        P v3 = P.of(selectionStart.x, selectionEnd.y);

        if ((arrow1 == null) || (arrow2 == null)) {
            computeArrows();
        }
        if ((arrow1 == null) || (arrow2 == null)) {
            return false;
        }
        P lineStart = arrow1.copy();
        P lineEnd;
        P intersection;

        P pselectionStart = P.of(selectionStart);
        P pselectionEnd = P.of(selectionEnd);

        for (P p : points) {
            lineEnd = p;

            intersection = intersection(pselectionStart, pselectionEnd, lineStart, lineEnd);
            if (checkValidIntersection(pselectionStart, v1, pselectionEnd, v3, lineStart, lineEnd, intersection)) {
                return true;
            }
            lineStart = lineEnd;
        }
        lineEnd = arrow2.copy();

        intersection = intersection(pselectionStart, pselectionEnd, lineStart, lineEnd);
        return checkValidIntersection(pselectionStart, v1, pselectionEnd, v3, lineStart, lineEnd, intersection);
    }

    /**
     * Determine if some line point crosses given area.
     *
     * @param selectionStart Left-top point of the area
     * @param selectionEnd   Bottom-right point of the area
     * @return true if the area covers some line point, false otherwise (or if line doesn't have points)
     */
    public boolean isAreaCrossingPoint(Point selectionStart, Point selectionEnd) {
        for (P p : points) {
            if (p.isInRectangle(selectionStart, selectionEnd)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if given point crosses given area.
     *
     * @param selectionStart Left-top point of the area
     * @param selectionEnd   Bottom-right point of the area
     * @param point          The point
     * @return true if the area covers the point, false otherwise
     */
    public static boolean isAreaCrossingPoint(Point selectionStart, Point selectionEnd, Point point) {
        return P.of(point).isInRectangle(selectionStart, selectionEnd);
    }

    private static double dot(P v0, P v1) {
        return (v0.x * v1.x) + (v0.y * v1.y);
    }

    private static boolean inTriangle(P point, P pa, P pb, P pc) {
        // Compute vectors
        P v0 = pc.minus(pa);
        P v1 = pb.minus(pa);
        P v2 = point.minus(pa);

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
     * @param lineStart start point of the line segment
     * @param lineEnd   end point of the line segment
     * @param pa        first point of the triangle
     * @param pb        second point of the triangle
     * @param pc        third point of the triangle
     * @return true if the line segment crosses the triangle; false otherwise
     */
    private static boolean inTriangle(P lineStart, P lineEnd, P pa, P pb, P pc) {
        double a = lineEnd.y - lineStart.y;
        double b = lineStart.x - lineEnd.x;
        double c = -a * lineStart.x - b * lineStart.y; // general line equation

        double result = a * pa.x + b * pa.y + c;
        int sign = Integer.signum((int) result);

        result = a * pb.x + b * pb.y + c;
        boolean is = isCrossing(sign, result);
        if (!is) {
            result = a * pc.x + b * pc.y + c;
            is = isCrossing(sign, result);
        }

        if (is) {
            // now we know that line is crossing the triangle.
            // we must check if the line segment crosses the triangle

            // A point is always left-top, B is always right, C is always under A
            return (Math.min(lineStart.y, lineEnd.y) <= pc.y) && (Math.max(lineStart.y, lineEnd.y) >= pa.y)
                && (Math.min(lineStart.x, lineEnd.x) <= pb.x) && (Math.max(lineStart.x, lineEnd.x) >= pa.x);
        } else {
            // last check if the line segment lies inside the triangle and does not cross it
            return inTriangle(lineStart, pa, pb, pc) && inTriangle(lineEnd, pa, pb, pc);
        }

    }

    private static boolean isCrossing(int sign, double result) {
        if ((result > 0) && (sign != 1)) {
            return true;
        } else if ((result == 0) && (sign != 0)) {
            return true;
        } else return (result < 0) && (sign != -1);
    }

    private static boolean checkValidIntersection(P v0, P v1, P v2, P v3, P lineStart, P lineEnd, P intersection) {
        if (intersection != null) {
            if ((intersection.x < v0.x) || (intersection.x > v2.x)
                || (intersection.y < v0.y) || (intersection.y > v2.y)) {
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
            if (inTriangle(lineStart, lineEnd, v0, v2, v3)) {
                return true;
            }
            // possible crossing (or overlay) can be on triangle (v0, v1, v2)
            return inTriangle(lineStart, lineEnd, v0, v1, v2);
        } else {
            return true;
        }
    }

    /**
     * Determines whether a selection area crosses or overlays a line segment.
     * <p>
     * If at least one intersection is found, then true is returned.
     *
     * @param lineStart      Start point of the line segment
     * @param lineEnd        End point of the line segment
     * @param selectionStart the selection start point
     * @param selectionEnd   the selection end point
     * @return true if the line segment is crossing the selection area; false otherwise
     */
    public static boolean isAreaCrossing(Point lineStart, Point lineEnd, Point selectionStart, Point selectionEnd) {
        P v1 = P.of(selectionEnd.x, selectionStart.y);
        P v3 = P.of(selectionStart.x, selectionEnd.y);

        P pselectionStart = P.of(selectionStart);
        P pselectionEnd = P.of(selectionEnd);
        P plineEnd = P.of(lineEnd);
        P plineStart = P.of(lineStart);

        P intersection = intersection(pselectionStart, pselectionEnd, plineStart, plineEnd);
        return checkValidIntersection(pselectionStart, v1, pselectionEnd, v3, plineStart, plineEnd, intersection);
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
     * @param g       Graphics2D object, where to draw the line
     * @param preview whether to draw line points
     */
    public void draw(Graphics2D g, boolean preview) {
        if (selected) {
            g.setColor(Color.BLUE);
        } else {
            g.setColor(lineColor);
        }
        int x1 = elementFrom.getX();
        int y1 = elementFrom.getY();
        int x2, y2;

        int widthHalf1 = elementFrom.getWidth() / 2;
        int heightHalf1 = elementFrom.getHeight() / 2;

        Stroke ss = g.getStroke();
        g.setStroke(thickLine);
        int j = points.size() - 1;
        for (int i = 0; i <= j; i++) {
            P p = points.get(i);
            x2 = p.ix();
            y2 = p.iy();

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
                g.fill3DRect(x2 - 1, y2 - 5, 2, 10, false); // .fillOval(x2 - 4, y2 - 4, 8, 8);
                g.fill3DRect(x2 - 5, y2 - 1, 10, 2, false);
                if (!selected) {
                    g.setColor(lineColor);
                }
            }
            g.drawLine(x1, y1, x2, y2);
            x1 = x2;
            y1 = y2;
        }
        x2 = elementTo.getX();
        y2 = elementTo.getY();

        int widthHalf2 = elementTo.getWidth() / 2;
        int heightHalf2 = elementTo.getHeight() / 2;

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
            xx1[0] = arrow1.ix();
            xx1[1] = arrow1.ix() + arrow1LeftEnd.ix();
            xx1[2] = arrow1.ix() + arrow1RightEnd.ix();
            xx1[3] = arrow1.ix();
            yy1[0] = arrow1.iy();
            yy1[1] = arrow1.iy() + arrow1LeftEnd.iy();
            yy1[2] = arrow1.iy() + arrow1RightEnd.iy();
            yy1[3] = arrow1.iy();
            g.fillPolygon(xx1, yy1, 4);
        }
        if ((arrow2 != null) && (arrow2LeftEnd != null) && (arrow2RightEnd != null)) {
            xx2[0] = arrow2.ix();
            xx2[1] = arrow2.ix() + arrow2LeftEnd.ix();
            xx2[2] = arrow2.ix() + arrow2RightEnd.ix();
            xx2[3] = arrow2.ix();
            yy2[0] = arrow2.iy();
            yy2[1] = arrow2.iy() + arrow2LeftEnd.iy();
            yy2[2] = arrow2.iy() + arrow2RightEnd.iy();
            yy2[3] = arrow2.iy();
            g.fillPolygon(xx2, yy2, 4);
        }
        g.setStroke(ss);
    }

    /**
     * This method draws a "sketch" line - in the process when user tries
     * to draw a connection line. It is based on fixed first element ee1, where
     * the line begins, and it continues through the points defined in the
     * ppoints list. The last point is defined by the point ee2.
     *
     * @param g       graphics object, where to draw the sketch line.
     * @param ee1     first element
     * @param ee2     last point
     * @param ppoints list of middle-points
     */
    public static void drawSketch(Graphics2D g, Element ee1, Point ee2, List<P> ppoints) {
        Stroke ss = g.getStroke();
        g.setStroke(thickLine);
        g.setColor(Color.GRAY);
        int x1 = ee1.getX();
        int y1 = ee1.getY();
        int x2, y2;

        for (P p : ppoints) {
            x2 = p.ix();
            y2 = p.iy();

            if (x2 < Schema.MIN_LEFT_MARGIN) {
                x2 = Schema.MIN_LEFT_MARGIN;
            }
            if (y2 < Schema.MIN_TOP_MARGIN) {
                y2 = Schema.MIN_TOP_MARGIN;
            }

            g.drawLine(x1, y1, x2, y2);
            g.fillOval(x2 - 3, y2 - 3, 6, 6);
            x1 = x2;
            y1 = y2;
        }
        if (ee2 != null) {
            x2 = (int) ee2.getX();
            y2 = (int) ee2.getY();
            g.drawLine(x1, y1, x2, y2);
        }
        g.setStroke(ss);
    }

    /**
     * Draws a small red circle around given point
     *
     * @param point the center of the circle
     * @param g     Graphics2D object
     */
    public static void highlightPoint(P point, Graphics2D g) {
        int xx = point.ix();
        int yy = point.iy();
        g.setColor(Color.WHITE);
        g.setStroke(thickLine);
        g.fillOval(xx - SELECTION_TOLERANCE - 2, yy - SELECTION_TOLERANCE - 2, (SELECTION_TOLERANCE + 2) * 2, (SELECTION_TOLERANCE + 2) * 2);
        // TODO:     if (selected)
        //               g.setColor(Color.BLUE);
        g.setColor(Color.BLACK);
        g.drawOval(xx - SELECTION_TOLERANCE, yy - SELECTION_TOLERANCE, SELECTION_TOLERANCE * 2, SELECTION_TOLERANCE * 2);

    }

    /**
     * Adds a middle-point to this line.
     *
     * @param before index of the point before that a new point will be added
     * @param point  the point that will be added
     */
    public void addPoint(int before, P point) {
        points.add(before, point);
    }

    /**
     * Removes a middle-point if exists.
     *
     * @param p the point that will be removed
     */
    public void removePoint(P p) {
        points.remove(p);
    }

    /**
     * Get all middle-points within this line.
     *
     * @return list of all middle-points
     */
    public List<P> getPoints() {
        return points;
    }

    /**
     * Test if given point is included within this line (with some tolerance).
     * If it is, return the original Point object, null otherwise.
     *
     * @param clickPoint Point to test
     * @return original point object of the line, null if the test point is not included
     */
    public P findPoint(Point clickPoint) {
        for (P point : points) {
            if ((point.x >= (clickPoint.x - SELECTION_TOLERANCE)) && (point.x <= (clickPoint.x + SELECTION_TOLERANCE))
                && (point.y >= (clickPoint.y - SELECTION_TOLERANCE)) && (point.y <= (clickPoint.y + SELECTION_TOLERANCE))) {
                return point;
            }
        }
        return null;
    }

    /**
     * Moves specific line point to a new location.
     * <p>
     * If the point is not found, nothing is done.
     *
     * @param originalPoint the point that will be moved
     * @param newLocation   new location of the point
     */
    public void movePoint(P originalPoint, P newLocation) {
        int i = points.indexOf(originalPoint);
        if (i != -1) {
            points.get(i).move(newLocation);
        }
        computeArrows();
    }

    /**
     * Moves all points of this line to a new location. The new location is
     * computed as: old + diff.
     *
     * @param diffX The X difference between new and old location
     * @param diffY The Y difference between new and old location
     */
    public void moveAllPoints(int diffX, int diffY, Function<P, P> searchGridPoint) {
        for (P point : points) {
            P movedPoint = searchGridPoint.apply(point.diff(diffX, diffY));
            point.move(movedPoint);
        }
        computeArrows();
    }

    /**
     * Checks, whether this line is connected (from any side) to the specific
     * element.
     *
     * @param e element to what the connection is checked
     * @return true, if the line is connected to the element; false otherwise
     */
    public boolean containsElement(Element e) {
        return elementFrom == e || elementTo == e;
    }

    /**
     * If the line is connected (from any side) to the first element, then
     * this method will replace it with new element.
     *
     * @param origin      first, origin element
     * @param replacement replacement for the first element
     */
    public void replaceElement(Element origin, Element replacement) {
        if (this.elementFrom == origin) {
            this.elementFrom = replacement;
        }
        if (this.elementTo == origin) {
            this.elementTo = replacement;
        }
    }

    /**
     * Method determines whether point[x,y] crosses with this line.
     * <p>
     * If yes, return index of a nearest cross point. If new point is
     * going to be added, it should be added to replace returned point index.
     *
     * @param clickPoint     point that is checked
     * @return 0 - if line doesn't contain any point, but point[x,y] is crossing the
     * line; or if point[x,y] crosses the line near the beginning of it
     * <p>
     * -1 - if point[x,y] doesn't cross the line at all
     * <p>
     * points count - if point crosses line after last point, or before ending point of the line.
     * <p>
     * Nearest point index of the line that the point crosses otherwise.
     */
    public int findCrossingPoint(Point clickPoint) {
        if (clickPoint == null) {
            return -1;
        }
        if ((arrow1 == null) || (arrow2 == null)) {
            computeArrows();
        }
        if ((arrow1 == null) || (arrow2 == null)) {
            return -1;
        }
        P X1 = arrow1.copy();
        P X2;

        int pointsSize = points.size();
        for (int i = 0; i < pointsSize; i++) {
            X2 = points.get(i);
            double len = Math.hypot(X2.x - X1.x, X2.y - X1.y);
            Point2D.Double vector1 = new Point2D.Double((X2.x - X1.x) / len, (X2.y - X1.y) / len); // normalized vector
            Point2D.Double vector2 = new Point2D.Double((clickPoint.x - X1.x) / len, (clickPoint.y - X1.y) / len);

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
            if ((Math.abs(crossproduct) * len <= SELECTION_TOLERANCE)
                && (Math.min(X1.x - SELECTION_TOLERANCE, X2.x - SELECTION_TOLERANCE) <= clickPoint.x)
                && (clickPoint.x <= Math.max(X1.x + SELECTION_TOLERANCE, X2.x + SELECTION_TOLERANCE))
                && (Math.min(X1.y - SELECTION_TOLERANCE, X2.y - SELECTION_TOLERANCE) <= clickPoint.y)
                && (clickPoint.y <= Math.max(X1.y + SELECTION_TOLERANCE, X2.y + SELECTION_TOLERANCE))) {
                return i;
            }
            X1 = X2;
        }
        X2 = arrow2.copy();
        double len = Math.hypot(X2.x - X1.x, X2.y - X1.y);
        Point2D.Double vector1 = new Point2D.Double((X2.x - X1.x) / len, (X2.y - X1.y) / len); // normalized vector
        Point2D.Double vector2 = new Point2D.Double((clickPoint.x - X1.x) / len, (clickPoint.y - X1.y) / len);
        double crossProduct = vector2.y * vector1.x - vector2.x * vector1.y;
        if ((Math.abs(crossProduct) * len <= SELECTION_TOLERANCE)
            && (Math.min(X1.x - SELECTION_TOLERANCE, X2.x - SELECTION_TOLERANCE) <= clickPoint.x)
            && (clickPoint.x <= Math.max(X1.x + SELECTION_TOLERANCE, X2.x + SELECTION_TOLERANCE))
            && (Math.min(X1.y - SELECTION_TOLERANCE, X2.y - SELECTION_TOLERANCE) <= clickPoint.y)
            && (clickPoint.y <= Math.max(X1.y + SELECTION_TOLERANCE, X2.y + SELECTION_TOLERANCE))) {
            return pointsSize; // as new point index
        }
        return -1;
    }

    public PluginConnection toPluginConnection() {
        List<SchemaPoint> schemaPoints = points.stream().map(P::toSchemaPoint).collect(Collectors.toList());

        return PluginConnection.create(
            elementFrom.getPluginId(),
            elementTo.getPluginId(),
            bidirectional,
            schemaPoints
        );
    }
}
