/*
 * Element.java
 *
 * Created on 3.7.2008, 8:26:22
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

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import no.geosoft.cc.graphics.GObject;
import no.geosoft.cc.graphics.GPosition;
import no.geosoft.cc.graphics.GSegment;
import no.geosoft.cc.graphics.GStyle;
import no.geosoft.cc.graphics.GText;

/**
 * Element within the schema
 * @author vbmacher
 */
public abstract class Element extends GObject {
    public final static int MIN_WIDTH = 80;
    public final static int MIN_HEIGHT = 50;

    /**
     * Minimal left margin for all elements within the schema
     */
    public final static int MIN_LEFT_MARGIN = 5;

    /**
     * Minimal top margin for all elements within the schema
     */
    public final static int MIN_TOP_MARGIN = 5;
    
    private GSegment text;
    private GSegment details;

    private GStyle boldStyle;
    private GStyle italicStyle;

    private ElementBox box;
    

    /**
     * Creates the Element instance. The element has a background color,
     * location, text and details that represent the plug-in name, etc.
     *
     * @param text plug-in type, ie. CPU, Device, Memory, Compiler
     * @param details details text, the plug-in name, description or author(s)
     * @param backColor The background color
     * @param x the center X location of the element
     * @param y the center Y location of the element
     */
    public Element(String text, String details, Color backColor, int x, int y) {
        GText ttext = new GText(text, GPosition.CENTER | GPosition.MIDDLE);
        
        boldStyle = new GStyle();
        boldStyle.setForegroundColor(Color.BLACK);
        boldStyle.setFont(new Font ("Dialog", Font.BOLD, 11));
        ttext.setStyle(boldStyle);
        
        GText ddetails = new GText(details, GPosition.CENTER | GPosition.BOTTOM);
        italicStyle = new GStyle();
        italicStyle.setForegroundColor(Color.BLACK);
        italicStyle.setFont(new Font ("Dialog", Font.ITALIC, 11));
        ddetails.setStyle(italicStyle);
        
        // create the box
        box = new ElementBox(x,y,MIN_WIDTH,MIN_HEIGHT,backColor);
        this.add(box);

        // add text
        this.text = new GSegment();
        this.text.setText(ttext);
        this.addSegment(this.text);
        
        // add details
        this.details = new GSegment();
        this.details.setText(ddetails);
        this.addSegment(this.details);
    }

    /**
     * This method draws this element.
     */
    @Override
    public void draw()  {
    }

    /**
     * Move this element to a new location.
     *
     * @param p new point location
     */
    public void move(Point p) {
        move (p.x, p.y);
    }

    /**
     * Move this element to a new location.
     *
     * @param x new X location
     * @param y new Y location
     */
    public void move(int x, int y) {
        box.move(x, y);
    }

    /**
     * Get text of this element.
     * 
     * @return plug-in type text (CPU, memory, compiler, device)
     */
//    public String getText() { return text.getText(); }
    
    /**
     * Get details of this element.
     * 
     * @return plug-in details
     */
  //  public String getDetails() { return details.getText(); }

    /**
     * Get a string represetnation of plug-in type:
     *   CPU, Compiler, Memory or Device.
     *
     * @return plug-in type string
     */
    protected abstract String getPluginType();

    /**
     * Get element box width in pixels.
     * @return width of the element
     */
    public int getWidth() { return (int)box.getWidth(); }

    /**
     * Get element box height in pixels.
     * @return height of the element
     */
    public int getHeight() { return (int)box.getHeight(); }

    /**
     * Get the center X location of the element.
     *
     * @return X location of the element
     */
    public int getX() { return (int)box.getX(); }

    /**
     * Get the center Y location of the element.
     *
     * @return Y location of the element
     */
    public int getY() { return (int)box.getY(); }

}
