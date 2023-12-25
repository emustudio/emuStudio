/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.gui.P;
import net.emustudio.application.settings.PluginConfig;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

/**
 * Element used by abstract schema of virtual computer.
 * <p>
 * It is a graphical object representing a plug-in.
 */
public abstract class Element {
    final static int MOUSE_TOLERANCE = 5;
    private final static int MIN_WIDTH = 80;
    private final static int MIN_HEIGHT = 50;
    private final static Font PLUGIN_NAME_FONT = new Font(Font.DIALOG, Font.BOLD, 13);
    private final static Font PLUGIN_TYPE_FONT = new Font(Font.MONOSPACED, Font.ITALIC, 12);

    private final Color backColor;
    private final Color foreColor = new Color(0x909090);

    private final String pluginId;
    private final PLUGIN_TYPE pluginType;
    private final String pluginName;
    private final String pluginFileName;
    private final Config pluginSettings;

    protected boolean selected = false;

    /**
     * This variable holds true, if the element size has been measured. The measurement process means the computation of
     * the width and height, and the correction of the X and Y positions within the schema canvas.
     * <p>
     * The measurement is realized manually, by external call of the measure() method.
     */
    private boolean wasMeasured = false;
    private GradientPaint gradient;

    private P schemaPoint;

    private int width;
    private int height;

    private int leftX;
    private int topY;

    private int textX;
    private int textY;

    private int detailsX;
    private int detailsY;

    public Element(Color backColor, P schemaPoint, String pluginId, PLUGIN_TYPE pluginType, String pluginName,
                   String pluginFileName, Config pluginSettings) {
        this.pluginId = Objects.requireNonNull(pluginId);
        this.pluginType = Objects.requireNonNull(pluginType);
        this.backColor = Objects.requireNonNull(backColor);
        this.schemaPoint = Objects.requireNonNull(schemaPoint);
        this.pluginName = Objects.requireNonNull(pluginName);
        this.pluginFileName = Objects.requireNonNull(pluginFileName);
        this.pluginSettings = Objects.requireNonNull(pluginSettings);

        int x = schemaPoint.ix();
        int y = schemaPoint.iy();

        this.gradient = new GradientPaint(x, y, Color.WHITE, x, y + height, this.backColor, false);
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
        g.setFont(PLUGIN_NAME_FONT);
        if (!selected) {
            g.setColor(Color.BLACK);
        }
        g.drawString(pluginName, textX, textY);
        g.setFont(PLUGIN_TYPE_FONT);
        g.drawString(pluginType.name(), detailsX, detailsY);
    }

    public void move(P newLocation) {
        if (wasMeasured) {
            int diffX = newLocation.ix() - schemaPoint.ix();
            int diffY = newLocation.iy() - schemaPoint.iy();

            schemaPoint = newLocation;

            // do not break internal state of the element
            leftX += diffX;
            topY += diffY;

            textX += diffX;
            textY += diffY;

            detailsX += diffX;
            detailsY += diffY;
            gradient = new GradientPaint(leftX, topY, Color.WHITE, leftX, getBottomY(), backColor, true);
        }
    }

    public void setSize(int width, int height) {
        wasMeasured = false;
        this.width = Math.max(width, MIN_WIDTH);
        this.height = Math.max(height, MIN_HEIGHT);
    }

    /**
     * Perform a measurement of the box, based on given graphics. Before the element can be drawn, it has to be measured
     * out. It means, that the width and height are computed (based on font sizes that depend on the Graphics object).
     *
     * @param g Graphics object
     */
    public void measure(Graphics g) {
        // First measure width and height of text
        FontMetrics fm = g.getFontMetrics(PLUGIN_NAME_FONT);
        Rectangle2D r = fm.getStringBounds(pluginName, g);
        FontMetrics fm1 = g.getFontMetrics(PLUGIN_TYPE_FONT);
        Rectangle2D r1 = fm1.getStringBounds(pluginType.name(), g);

        int tW = (int) Math.max(r.getWidth(), r1.getWidth());
        int tH = (int) Math.max(r.getHeight(), r1.getHeight());

        // compute width and height
        Rectangle2D wrect = fm.getStringBounds("w", g);

        width = tW + 2 * (int) wrect.getWidth();
        height = 2 * tH + 2 * (int) wrect.getHeight();

        textY = height / 2 + 10 - fm.getAscent();
        // set starting x and y

        leftX = schemaPoint.ix() - getWidth() / 2;
        topY = schemaPoint.iy() - getHeight() / 2;

        gradient = new GradientPaint(leftX, topY, Color.WHITE, leftX, getBottomY(), backColor, true);

        textX = leftX + (getWidth() - tW) / 2;
        textY += topY;

        detailsX = leftX + (getWidth() - tW) / 2;
        detailsY = topY + (getHeight() - fm1.getAscent());
        wasMeasured = true;
    }

    public int getWidth() {
        return (width == 0) ? MIN_WIDTH : width;
    }

    public int getHeight() {
        return (height == 0) ? MIN_HEIGHT : height;
    }


    public int getX() {
        return schemaPoint.ix();
    }

    public int getY() {
        return schemaPoint.iy();
    }

    public P getSchemaPoint() {
        return schemaPoint;
    }

    private int getBottomY() {
        return topY + getHeight();
    }

    private int getRightX() {
        return leftX + getWidth();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Determines whether a selection area crosses or overlays this element. It is assumed that the element is measured
     * already.
     * <p>
     * The following four conditions must be true: 1. selection.Xleft <= element.Xright 2. selection.Xright >=
     * element.Xleft 3. selection.Ytop <= element.Ybottom 4. selection.Ybottom >= element.Ytop
     *
     * @param selectionStart the selection start point
     * @param selectionEnd   the selection end point
     * @return true if the element is crossing
     */
    public boolean crossesArea(Point selectionStart, Point selectionEnd) {
        if (!wasMeasured) {
            return false;
        }
        int xR = getRightX();
        int yB = getBottomY();

        return (selectionStart.x <= xR) && (selectionEnd.x >= leftX)
                && (selectionStart.y <= yB) && (selectionEnd.y >= topY);
    }

    /**
     * Determine if a point crosses north (top) border of this element.
     * <p>
     * Used for resizing. It uses a tolerance.
     *
     * @param borderPoint the point for the testing
     * @return true if mouse is crossing a top border of this element, false otherwise.
     */
    public boolean crossesTopBorder(Point borderPoint) {
        if ((!wasMeasured) || (borderPoint == null)) {
            return false;
        }
        int xR = getRightX();
        int yB = topY + MOUSE_TOLERANCE;
        return ((borderPoint.x >= leftX) && (borderPoint.x <= xR) && (borderPoint.y <= yB) && (borderPoint.y >= topY - MOUSE_TOLERANCE));
    }

    /**
     * Determine if a point crosses south (bottom) border of this element.
     * <p>
     * Used for resizing. It uses a tolerance.
     *
     * @param borderPoint the point for the testing
     * @return true if mouse is crossing a bottom border of this element, false otherwise.
     */
    public boolean crossesBottomBorder(Point borderPoint) {
        if (!wasMeasured || (borderPoint == null)) {
            return false;
        }
        int xR = getRightX();
        int yT = getBottomY() - MOUSE_TOLERANCE;
        int yB = getBottomY() + MOUSE_TOLERANCE;
        return ((borderPoint.x >= leftX) && (borderPoint.x <= xR) && (borderPoint.y <= yB) && (borderPoint.y >= yT));
    }

    /**
     * Determine if a point crosses west (left) border of this element.
     * <p>
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
        int yB = getBottomY();
        return ((borderPoint.x >= leftX - MOUSE_TOLERANCE) && (borderPoint.x <= xR) && (borderPoint.y <= yB) && (borderPoint.y >= topY));
    }

    /**
     * Determine if a point crosses east (right) border of this element.
     * <p>
     * Used for resizing. It uses a tolerance.
     *
     * @param borderPoint the point for the testing
     * @return true if mouse is crossing a right border of this element; false otherwise.
     */
    public boolean crossesRightBorder(Point borderPoint) {
        if ((!wasMeasured) || (borderPoint == null)) {
            return false;
        }
        int xL = getRightX() - MOUSE_TOLERANCE;
        int xR = getRightX() + MOUSE_TOLERANCE;
        int yB = getBottomY();
        return ((borderPoint.x >= xL) && (borderPoint.x <= xR) && (borderPoint.y <= yB) && (borderPoint.y >= topY));
    }

    public Rectangle getRectangle() {
        return new Rectangle(getX() - getWidth() / 2, getY() - getHeight() / 2, getWidth(), getHeight());
    }

    public String getPluginId() {
        return pluginId;
    }

    public PluginConfig save() {
        return PluginConfig.create(pluginId, pluginType, pluginName, pluginFileName, schemaPoint, pluginSettings);
    }

    @Override
    public String toString() {
        return pluginName + "[x=" + schemaPoint.x + ", y=" + schemaPoint.y + ", rect=" + getRectangle() + ']';
    }
}
