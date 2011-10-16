/*
 * ElementBox.java
 * 
 * Copyright (C) 2011 vbmacher
 * 
 * KISS, YAGNI
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package emustudio.architecture.drawing;

import java.awt.Color;
import java.awt.Font;
import no.geosoft.cc.graphics.GObject;
import no.geosoft.cc.graphics.GSegment;
import no.geosoft.cc.graphics.GStyle;

/**
 *
 * @author vbmacher
 */
public class ElementBox extends GObject {

    private double x0_, y0_, width_, height_; // left,top
    private GSegment border_;
    // Complete the graphics representation of the box
    private double[] xy;

    /**
     * Drawing style of the box.
     */
    private GStyle style;
    
    public ElementBox(double x0, double y0, double width, double height,
            Color backColor) {
        // Store the abstract representation of the box
        x0_ = x0;
        y0_ = y0;
        width_ = width;
        height_ = height;

        xy = new double[]{
            x0_, y0_,
            x0_ + width_, y0_,
            x0_ + width_, y0_ + height_,
            x0_, y0_ + height_,
            x0_, y0_
        };
        
        style = new GStyle();
        style.setBackgroundColor(backColor);
        style.setForegroundColor(Color.BLACK);
        style.setFont(new Font ("Dialog", Font.BOLD, 11));
        style.setLineWidth(2);

        // Prepare the graphics representation of the box
        border_ = new GSegment();
        addSegment(border_);
    }

    @Override
    public void draw() {
        border_.setGeometry(xy);
    }
    
    public double getX() { return x0_; }
    public double getY() { return y0_; }
    public double getWidth() { return width_; }
    public double getHeight() { return height_; }
    
    /**
     * Move this element to a new location.
     *
     * @param x new X location
     * @param y new Y location
     * @param width new width
     * @param height new height
     */
    public void setGemoetry(double x, double y, double width, double height) {
        this.x0_ = x;
        this.y0_ = y;
        this.width_ = width;
        this.height_ = height;
        xy = new double[]{
            x0_, y0_,
            x0_ + width_, y0_,
            x0_ + width_, y0_ + height_,
            x0_, y0_ + height_,
            x0_, y0_
        };
    }

    /**
     * Move this box to a new location.
     *
     * @param x new X location
     * @param y new Y location
     */
    public void move(int x, int y) {
        this.setGemoetry(x, y, width_, height_);
    }

    /**
     * Move this box to a new location.
     *
     * @param x new X location
     * @param y new Y location
     */
    public void setWidth(int width) {
        this.setGemoetry(x0_, y0_, width, height_);
    }

}