/*
 * CpuElement.java
 *
 * Created on 3.7.2008, 8:28:14
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
import java.awt.Point;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author vbmacher
 */
public class CpuElement extends Element {
    private int tX1; // pozicia textu "CPU"
    private int tY1;

    private int tX2; // pozicia details
    private int tY2;
    
    private boolean wasMeasured;
    private Font boldFont;
    private Font italicFont;
    private Font plainFont;
    
    private Color cpuColor;

    public CpuElement(int x, int y, String text) {
        super(text);
        this.x = x;
        this.y = y;
        wasMeasured = false;
        cpuColor = new Color(0x6D8471);        
    }

    public CpuElement(Point e1, String text) {
        this((int)e1.getX(),(int)e1.getY(), text);
    }

    @Override
    public void draw(Graphics g)  {
        if (!wasMeasured)
            measure(g,0,0);
        g.setColor(cpuColor);
        g.fillRect(x, y, width, height);
        g.setColor(Color.black);
        g.drawRect(x, y, width, height);
        g.setFont(boldFont);
        g.drawString("CPU", tX1, tY1);
        g.setFont(italicFont);
        g.drawString(details, tX2, tY2);
        g.setFont(plainFont);
    }


    @Override
    public void move(int x, int y) {
        wasMeasured = false;
        this.x = x;
        this.y = y;
    }

    @Override
    public void measure(Graphics g, int leftFactor, int topFactor) {
        if (wasMeasured)
            return;
        Font f = g.getFont();
        boldFont = f.deriveFont(Font.BOLD);
        italicFont = f.deriveFont(Font.ITALIC);
        plainFont = f.deriveFont(Font.PLAIN);            
        FontMetrics fm = g.getFontMetrics(boldFont);
        Rectangle2D r = fm.getStringBounds("CPU", g);
        width = (int)r.getWidth();
        height = (int)r.getHeight();
        int tW = width;
        int tH = (int)fm.getAscent();

        tY1 = height + 20 - tH;

        fm = g.getFontMetrics(italicFont);
        r = fm.getStringBounds(details, g);

        if (width < (int)r.getWidth())
            width = (int)r.getWidth();
        width += 20;
        height += (int)r.getHeight() + 20;
        x -= width/2;
        y -= height/2;

        leftFactor -= width/2;
        topFactor -= height/2;

        performCorrection(leftFactor, topFactor);

        tX1 = x + (width - tW) / 2;

        tW = (int)r.getWidth();
        tH = (int)fm.getAscent();

        tY1 += y;
        tX2 = x + (width - tW) / 2;
        tY2 = y + (height - tH);
        wasMeasured = true;
    }
    
}

