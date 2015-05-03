/*
 * KISS, YAGNI, DRY
 * (c) Copyright 2015, Peter Jakubčo
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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Element used by abstract schema of virtual computer.
 *
 * It is a graphical object representing a plug-in.
 */
public abstract class Element {
    public final static int MIN_WIDTH = 80;
    public final static int MIN_HEIGHT = 50;

    public final static int MOUSE_TOLERANCE = 5;

    private final Properties myProperties;
    private final String pluginName;

    private int width;
    private int height;

    private int x;
    private int leftX;
    private int y;
    private int topY;

    private int textX;
    private int textY;

    private int detailsX;
    private int detailsY;

    /**
     * This variable holds true, if the element size has been measured. The measurement process means the computation of
     * the width and height, and the correction of the X and Y positions wihin the schema canvas.
     *
     * The measurement is realized manually, by external call of the measure() method.
     */
    private boolean wasMeasured;

    private Font boldFont;
    private Font italicFont;

    private final Color backColor;
    private final Color foreColor = new Color(0x909090);

    private GradientPaint gradient;
    protected boolean selected = false;

    protected Schema schema;

    public Element(String pluginName, Properties settings, Color backColor, Schema schema) throws NumberFormatException {
        this.pluginName = (pluginName == null || pluginName.isEmpty()) ? "unknown" : pluginName;
        this.schema = Objects.requireNonNull(schema);
        this.backColor = Objects.requireNonNull(backColor);

        this.gradient = new GradientPaint(x, y, Color.WHITE, x, y + height, this.backColor, false);
        this.myProperties = settings;

        refreshSettings();
    }

    public Element(String pluginName, Point location, Color backColor, Schema schema) {
        this.pluginName = (pluginName == null || pluginName.isEmpty()) ? "unknown" : pluginName;
        this.schema = Objects.requireNonNull(schema);
        this.backColor = Objects.requireNonNull(backColor);

        this.gradient = new GradientPaint(x, y, Color.WHITE, x, y + height, this.backColor, false);
        this.myProperties = new Properties();

        this.x = location.x;
        this.y = location.y;
        this.wasMeasured = false;
    }

    private void refreshSettings() throws NumberFormatException {
        x = Integer.parseInt(myProperties.getProperty("point.x", "0"));
        y = Integer.parseInt(myProperties.getProperty("point.y", "0"));
        width = Integer.parseInt(myProperties.getProperty("width", "0"));
        height = Integer.parseInt(myProperties.getProperty("height", "0"));
        this.wasMeasured = false;
    }
    
    public final void refreshSettings(Map properties) throws NumberFormatException {
        myProperties.clear();
        updateProperties();
        myProperties.putAll(properties);
        refreshSettings();
    }

    private void updateProperties() {
        myProperties.put("point.x", String.valueOf(x));
        myProperties.put("point.y", String.valueOf(y));
        myProperties.put("width", String.valueOf(getWidth()));
        myProperties.put("height", String.valueOf(getHeight()));
    }

    public Properties getProperties() {
        // actualize internal properties
        updateProperties();
        return new Properties(myProperties);
    }
    
    public Map<String, String> getPropertiesWithoutSchema() {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry entry : myProperties.entrySet()) {
            String key = (String)entry.getKey();
            if (key.startsWith("point")) {
                continue;
            }
            if (key.equals("width") || key.equals("height")) {
                continue;
            }
            map.put(key, (String)entry.getValue());
        }
        return map;
    }

    /**
     * Saves all the element's settings into given properties.
     *
     * @param properties properties of virtual computer
     * @param elementName key setting name of this element within the properies
     */
    public void saveProperties(Properties properties, String elementName) {
        updateProperties();
        properties.put(elementName, pluginName);

        Enumeration keys = myProperties.keys();
        while (keys.hasMoreElements()) {
            String elementSetting = (String) keys.nextElement();
            properties.put(elementName + "." + elementSetting, myProperties.getProperty(elementSetting));
        }
    }

    public void draw(Graphics2D g) {
        if (!wasMeasured) {
            measure(g);
        }
        g.setPaint(gradient);
        g.fillRect(leftX, topY, getWidth(), getHeight());
        if (selected) {
            g.setColor(Color.BLUE);
        } else {
            g.setColor(foreColor);
        }
        g.draw3DRect(leftX, topY, getWidth(), getHeight(), true);
        g.setFont(boldFont);
        if (!selected) {
            g.setColor(Color.BLACK);
        }
        g.drawString(getPluginType(), textX, textY);
        g.setFont(italicFont);
        g.drawString(pluginName, detailsX, detailsY);
    }

    public boolean move(Point p) {
        return move(p.x, p.y);
    }

    public boolean move(int x, int y) {
        if (!wasMeasured) {
            return false;
        }
        if (!schema.canMoveElement(x, y, this)) {
            return false;
        }
        int diffX = this.x - x;
        int diffY = this.y - y;

        this.x = x;
        this.y = y;

        // do not break internal state of the element
        leftX += diffX;
        topY += diffY;

        textX += diffX;
        textY += diffY;

        detailsX += diffX;
        detailsY += diffY;
        gradient = new GradientPaint(leftX, topY, Color.WHITE, leftX, topY + getHeight(), backColor, true);

        return true;
    }

    public void setSize(int width, int height) {
        wasMeasured = false;
        this.width = (width <= MIN_WIDTH) ? MIN_WIDTH : width;
        this.height = (height <= MIN_HEIGHT) ? MIN_HEIGHT : height;
    }

    public String getPluginName() {
        return pluginName;
    }

    protected abstract String getPluginType();

    /**
     * Perform a measurement of the box, based on given graphics. Before the element can be drawn, it has to be measured
     * out. It means, that the width and height are computed (based on font sizes that depend on the Graphics object).
     *
     * @param g Graphics object
     */
    public void measure(Graphics g) {
        Font f = g.getFont();
        boldFont = f.deriveFont(Font.BOLD);
        italicFont = f.deriveFont(Font.PLAIN);

        // First measure width and height of text
        FontMetrics fm = g.getFontMetrics(boldFont);
        Rectangle2D r = fm.getStringBounds(getPluginType(), g);
        int tW1 = (int) r.getWidth();
        int tH1 = (int) r.getHeight();
        int tA1 = (int) fm.getAscent();

        FontMetrics fm1 = g.getFontMetrics(italicFont);
        Rectangle2D r1 = fm1.getStringBounds(pluginName, g);
        int tW2 = (int) r1.getWidth();
        int tH2 = (int) r1.getHeight();
        int tA2 = (int) fm1.getAscent();

        // text width, text height, text ascent
        int tW = (tW1 > tW2) ? tW1 : tW2;
        int tH = (tH1 > tH2) ? tH1 : tH2;

        // compute width and height
        if (width == 0) {
            width = tW + 20;
        }

        if (height == 0) {
            height = 2 * tH + 20;
        }

        textY = height / 2 + 10 - tA1;
        // set starting x and y
        leftX = x - getWidth() / 2;
        topY = y - getHeight() / 2;

        gradient = new GradientPaint(leftX, topY, Color.WHITE, leftX, topY + getHeight(), backColor, true);

        textX = leftX + (getWidth() - tW) / 2;
        textY += topY;

        detailsX = leftX + (getWidth() - tW) / 2;
        detailsY = topY + (getHeight() - tA2);
        wasMeasured = true;
    }

    public void setDefaultSize() {
        width = 0;
        height = 0;
        wasMeasured = false;
    }

    public int getWidth() {
        return (width == 0) ? MIN_WIDTH : width;
    }

    public int getHeight() {
        return (height == 0) ? MIN_HEIGHT : height;
    }

    public Point getPoint() {
        return new Point(x, y);
    }

    public Rectangle getRectangle() {
        return new Rectangle(x, y, getWidth(), getHeight());
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    /**
     * Determines whether a selection area crosses or overlays this element. It is assumed that the element is measured
     * already.
     *
     * The following four conditions must be true: 1. selection.Xleft <= element.Xright 2. selection.Xright >=
     * element.Xleft 3. selection.Ytop <= element.Ybottom 4. selection.Ybottom >= element.Ytop
     *
     * @param selectionStart the selection start point
     * @param selectionEnd the selection end point
     * @return true if the element is crossing
     */
    public boolean crossesArea(Point selectionStart, Point selectionEnd) {
        if (!wasMeasured) {
            return false;
        }
        int xR = leftX + getWidth();
        int yB = topY + getHeight();
        return (selectionStart.x <= xR) && (selectionEnd.x >= leftX)
                && (selectionStart.y <= yB) && (selectionEnd.y >= topY);
    }

    /**
     * Determine if a point crosses north (top) border of this element.
     *
     * Used for resizing. It uses a tolerance.
     *
     * @param borderPoint the point for the testing
     * @return true if mouse is crossing a top border of this element, false otherwise.
     */
    public boolean crossesTopBorder(Point borderPoint) {
        if ((!wasMeasured) || (borderPoint == null)) {
            return false;
        }
        int xR = leftX + getWidth();
        int yB = topY + MOUSE_TOLERANCE;
        return ((borderPoint.x >= leftX) && (borderPoint.x <= xR) && (borderPoint.y <= yB) && (borderPoint.y >= topY - MOUSE_TOLERANCE));
    }

    /**
     * Determine if a point crosses south (bottom) border of this element.
     *
     * Used for resizing. It uses a tolerance.
     *
     * @param borderPoint the point for the testing
     * @return true if mouse is crossing a bottom border of this element, false otherwise.
     */
    public boolean crossesBottomBorder(Point borderPoint) {
        if (!wasMeasured || (borderPoint == null)) {
            return false;
        }
        int xR = leftX + getWidth();
        int yT = topY + getHeight() - MOUSE_TOLERANCE;
        int yB = topY + getHeight() + MOUSE_TOLERANCE;
        return ((borderPoint.x >= leftX) && (borderPoint.x <= xR) && (borderPoint.y <= yB) && (borderPoint.y >= yT));
    }

    /**
     * Determine if a point crosses west (left) border of this element.
     *
     * Used for resizing. It uses a tolerance.
     *
     * @param borderPoint the point for the testing
     * @return true if mouse is crossing a left border of this element; false otherwise.
     */
    public boolean crossesLeftBorder(Point borderPoint) {
        if ((!wasMeasured) || (borderPoint == null)) {
            return false;
        }
        int xR = leftX + MOUSE_TOLERANCE;
        int yB = topY + getHeight();
        return ((borderPoint.x >= leftX - MOUSE_TOLERANCE) && (borderPoint.x <= xR) && (borderPoint.y <= yB) && (borderPoint.y >= topY));
    }

    /**
     * Determine if a point crosses east (right) border of this element.
     *
     * Used for resizing. It uses a tolerance.
     *
     * @param borderPoint the point for the testing
     * @return true if mouse is crossing a right border of this element; false otherwise.
     */
    public boolean crossesRightBorder(Point borderPoint) {
        if ((!wasMeasured) || (borderPoint == null)) {
            return false;
        }
        int xL = leftX + getWidth() - MOUSE_TOLERANCE;
        int xR = leftX + getWidth() + MOUSE_TOLERANCE;
        int yB = topY + getHeight();
        return ((borderPoint.x >= xL) && (borderPoint.x <= xR) && (borderPoint.y <= yB) && (borderPoint.y >= topY));
    }

}
