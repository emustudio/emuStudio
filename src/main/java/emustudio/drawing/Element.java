/*
 * Element.java
 *
 * Created on 3.7.2008, 8:26:22
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2008-2012, Peter Jakubčo
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
import java.util.Properties;

/**
 * Element used by abstract schema of virtual computer.
 *
 * It is a graphical object representing a plug-in.
 *
 * @author vbmacher
 */
public abstract class Element {

    /**
     * Minimum width
     */
    public final static int MIN_WIDTH = 80;

    /**
     * Minimum height
     */
    public final static int MIN_HEIGHT = 50;

    /**
     * Mouse tolerance of element border
     */
    public final static int TOLERANCE = 5;

    /**
     * Element's actual properties.
     */
    private final Properties myProperties;

    /**
     * Variable holds plug-in file name that is shown inside the element, below the plug-in type.
     */
    private String pluginName;

    /**
     * Element width - it is measured within the measure() method, or set by user.
     */
    private int width;

    /**
     * Element height - it is measured within the measure() method, or set by user.
     */
    private int height;

    /**
     * The center X position of the box.
     */
    private int x;
    private int leftX;

    /**
     * The center Y position of the box.
     */
    private int y;
    private int topY;

    /**
     * The X position of the plug-in type text
     */
    private int textX;

    /**
     * The Y position of the plug-in type text
     */
    private int textY;

    /**
     * The X position of the details text
     */
    private int detailsX;

    /**
     * The Y position of the details text
     */
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

    /**
     * Background color of the element. Each plug-in type can have different background color.
     */
    private final Color backColor;

    /**
     * Foreground color of the element box. Line color.
     */
    private final Color foreColor;

    /**
     * Element will be drawn with linear gradient from white to backColor
     */
    private GradientPaint gradient;

    /**
     * Holds true, when this element is selected by user. False otherwise.
     */
    protected boolean selected;

    protected Schema schema;

    /**
     * Creates the Element instance. The element has a background color, location and details that represent the plug-in
     * name.
     *
     * @param pluginName file name of this plug-in, without '.jar' extension.
     * @param settings settings of this element
     * @param backColor Background color
     * @param schema Schema object of the element
     * @throws NumberFormatException when plug-in with settingsName does not exist in the schema
     */
    public Element(String pluginName, Properties settings, Color backColor, Schema schema) throws NumberFormatException {
        this.pluginName = pluginName;
        this.backColor = backColor;
        this.selected = false;
        this.foreColor = new Color(0x909090);
        gradient = new GradientPaint(x, y, Color.WHITE, x, y + height, this.backColor, false);
        this.myProperties = settings;
        this.schema = schema;
        refreshSettings();
    }

    /**
     * Creates the Element instance. The element has a background color, all other parameters are created automatically.
     *
     * @param pluginName file name of this plug-in, without '.jar' extension.
     * @param location Location of this element in the abstract schema
     * @param backColor Background color
     * @param schema Schema object of the element
     */
    public Element(String pluginName, Point location, Color backColor, Schema schema) {
        this.pluginName = pluginName;
        this.x = location.x;
        this.y = location.y;
        this.backColor = backColor;
        this.wasMeasured = false;
        this.selected = false;
        this.foreColor = new Color(0x909090);
        this.myProperties = new Properties();
        this.schema = schema;
        gradient = new GradientPaint(x, y, Color.WHITE, x, y + height, this.backColor, false);
    }

    /**
     * Updates settings of the element from internal properties.
     *
     * @throws NumberFormatException when some properties are not well parseable.
     */
    public final void refreshSettings() throws NumberFormatException {
        if ((pluginName == null) || (pluginName.isEmpty())) {
            pluginName = "unknown";
        }

        x = Integer.parseInt(myProperties.getProperty("point.x", "0"));
        y = Integer.parseInt(myProperties.getProperty("point.y", "0"));
        width = Integer.parseInt(myProperties.getProperty("width", "0"));
        height = Integer.parseInt(myProperties.getProperty("height", "0"));
        this.wasMeasured = false;
    }

    /**
     * Update internal properties
     */
    private void updateProperties() {
        myProperties.put("point.x", String.valueOf(x));
        myProperties.put("point.y", String.valueOf(y));
        myProperties.put("width", String.valueOf(getWidth()));
        myProperties.put("height", String.valueOf(getHeight()));
    }

    /**
     * Get actual properties of this element.
     *
     * @return properties for this element
     */
    public Properties getProperties() {
        // actualize internal properties
        updateProperties();
        return myProperties;
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

    /**
     * This method draws this element to the graphics.
     *
     * @param g graphics, where to draw
     */
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

    /**
     * Move this element to a new location.
     *
     * @param p new point location
     * @return true if new location is not out of the canvas (less than minimal margins), false otherwise
     */
    public boolean move(Point p) {
        return move(p.x, p.y);
    }

    /**
     * Move this element to a new location.
     *
     * @param x new X location
     * @param y new Y location
     * @return true if the element moved; false if it couldn't either due to new location is out of the canvas (less
     * than minimal margins), or other elements/line points are in the way.
     */
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

    /**
     * Set new size of this element.
     *
     * @param width new width
     * @param height new height
     */
    public void setSize(int width, int height) {
        wasMeasured = false;
        this.width = (width <= MIN_WIDTH) ? MIN_WIDTH : width;
        this.height = (height <= MIN_HEIGHT) ? MIN_HEIGHT : height;
    }

    /**
     * Get plug-in name of this element.
     *
     * @return plug-in name string
     */
    public String getPluginName() {
        return pluginName;
    }

    /**
     * Get a string represetnation of plug-in type: CPU, Compiler, Memory or Device.
     *
     * @return plug-in type string
     */
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

    /**
     * Set the default size according to text width and height.
     */
    public void setDefaultSize() {
        width = 0;
        height = 0;
        wasMeasured = false;
    }

    /**
     * Get element box width in pixels.
     *
     * @return width of the element
     */
    public int getWidth() {
        return (width == 0) ? MIN_WIDTH : width;
    }

    /**
     * Get element box height in pixels.
     *
     * @return height of the element
     */
    public int getHeight() {
        return (height == 0) ? MIN_HEIGHT : height;
    }

    public Point getPoint() {
        return new Point(x, y);
    }

    public Rectangle getRectangle() {
        return new Rectangle(x, y, getWidth(), getHeight());
    }

    /**
     * Get the center X location of the element.
     *
     * @return X location of the element
     */
    public int getX() {
        return x;
    }

    /**
     * Get the center Y location of the element.
     *
     * @return Y location of the element
     */
    public int getY() {
        return y;
    }

    /**
     * Select/deselect this element.
     *
     * @param selected if it is true, the element will be selected. If false, the element will be deselected.
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Determine whether this element is selected.
     *
     * @return true, if this element is selected, false otherwise.
     */
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
        int yB = topY + TOLERANCE;
        return ((borderPoint.x >= leftX) && (borderPoint.x <= xR) && (borderPoint.y <= yB) && (borderPoint.y >= topY - TOLERANCE));
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
        int yT = topY + getHeight() - TOLERANCE;
        int yB = topY + getHeight() + TOLERANCE;
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
        int xR = leftX + TOLERANCE;
        int yB = topY + getHeight();
        return ((borderPoint.x >= leftX - TOLERANCE) && (borderPoint.x <= xR) && (borderPoint.y <= yB) && (borderPoint.y >= topY));
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
        int xL = leftX + getWidth() - TOLERANCE;
        int xR = leftX + getWidth() + TOLERANCE;
        int yB = topY + getHeight();
        return ((borderPoint.x >= xL) && (borderPoint.x <= xR) && (borderPoint.y <= yB) && (borderPoint.y >= topY));
    }

}
