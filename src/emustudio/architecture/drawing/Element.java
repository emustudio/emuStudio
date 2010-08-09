/*
 * Element.java
 *
 * Created on 3.7.2008, 8:26:22
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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author vbmacher
 */
public abstract class Element {
    /**
     *
     */
    public final static int MIN_LEFT_MARGIN = 5;
    public final static int MIN_TOP_MARGIN = 5;

    /**
     * Variable holds details that should be shown inside the element,
     * above the plug-in type.
     */
    private String details;

    /**
     * Element width - it is measured within the measure() method.
     */
    private int width;

    /**
     * Element height - it is measured within the measure() method.
     */
    private int height;

    /**
     * The X position of the box.
     */
    private int x;

    /**
     * The Y position of the box.
     */
    private int y;

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
     * This variable holds true, if the element size has been measured.
     * The measurement process means the computation of the width and
     * height, and the correction of the X and Y positions wihin the schema
     * canvas.
     *
     * The measurement is realized manually, by external call of the measure()
     * method.
     */
    private boolean wasMeasured;

    private Font boldFont;
    private Font italicFont;

    /**
     * Background color of the element. Each plug-in type can have different
     * background color.
     */
    private Color backColor;

    public Element(Color backColor, String details,int x, int y) {
        this.x = x;
        this.y = y;
        this.details = details;
        this.backColor = backColor;
        this.wasMeasured = false;
    }

    /**
     * This method draws this element to the graphics.
     *
     * @param g graphics, where to draw
     */
    public void draw(Graphics g)  {
        if (!wasMeasured)
            measure(g,0,0);
        g.setColor(backColor);
        g.fillRect(x, y, getWidth(), getHeight());
        g.setColor(Color.black);
        g.drawRect(x, y, getWidth(), getHeight());
        g.setFont(boldFont);
        g.drawString(getPluginType(), textX, textY);
        g.setFont(italicFont);
        g.drawString(details, detailsX, detailsY);
    }

    /**
     * Move this element to a new location.
     *
     * @param x
     * @param y
     */
    public void move(int x, int y) {
        wasMeasured = false;
        this.x = x;
        this.y = y;
    }

    /**
     * 
     * @return
     */
    public String getDetails() { return details; }

    /**
     * Get a string represetnation of plug-in type:
     *   CPU, Compiler, Memory or Device.
     *
     * @return plug-in type string
     */
    protected abstract String getPluginType();

    /**
     * Perform a measurement of the box, based on given graphics. Before
     * the element can be drawn, it has to be measured out. It means, that
     * the width and height are computed (based on font sizes that depend on
     * the Graphics object) and the x and y correction is performed (based on
     * the leftFactor and topFactor).
     *
     * @param g Graphics object
     * @param leftFactor the correction value for X
     * @param topFactor the correction value for Y
     */
    public void measure(Graphics g, int leftFactor, int topFactor) {
        if (wasMeasured)
            return;
        Font f = g.getFont();
        boldFont = f.deriveFont(Font.BOLD);
        italicFont = f.deriveFont(Font.ITALIC);
        FontMetrics fm = g.getFontMetrics(boldFont);
        Rectangle2D r = fm.getStringBounds(getPluginType(), g);

        // compute width and height
        width = (int)r.getWidth();
        height = (int)r.getHeight();
        int tW = width;
        int tH = (int)fm.getAscent();

        textY = height + 20 - tH;

        fm = g.getFontMetrics(italicFont);
        r = fm.getStringBounds(details, g);

        if (width < (int)r.getWidth())
            width = (int)r.getWidth();
        width += 20;
        height += (int)r.getHeight() + 20;

        // set starting x and y
        x -= getWidth()/2;
        y -= getHeight()/2;

        // perform the correction, "trim" empty space from the left and top
        x -= (leftFactor - getWidth()/2);
        y -= (topFactor - getHeight()/2);

        textX = x + (getWidth() - tW) / 2;

        tW = (int)r.getWidth();
        tH = (int)fm.getAscent();

        textY += y;
        detailsX = x + (getWidth() - tW) / 2;
        detailsY = y + (getHeight() - tH);
        wasMeasured = true;
    }

    /**
     * Get element box width in pixels.
     * @return width of the element
     */
    public int getWidth() { return (width == 0) ? 80 : width; }

    /**
     * Get element box height in pixels.
     * @return height of the element
     */
    public int getHeight() { return (height == 0) ? 50: height; }
    public int getX() { return x; }
    public int getY() { return y; }
}
