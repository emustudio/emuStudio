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

import java.awt.Graphics;

/**
 *
 * @author vbmacher
 */
public abstract class Element {
    protected String details;
    protected int width;
    protected int height;
    protected int x;
    protected int y;

    public Element(String details) {
        this.details = details;
    }
    
    public String getDetails() { return details; }
    
    public abstract void measure(Graphics g);
    public abstract void draw(Graphics g);
    public int getWidth() { return (width == 0) ? 80 : width; }
    public int getHeight() { return (height == 0) ? 50: height; }
    public int getX() { return x; }
    public int getY() { return y; }
    public abstract void move(int x, int y);
}
