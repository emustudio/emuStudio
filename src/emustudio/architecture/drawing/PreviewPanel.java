/*
 * PreviewPanel.java
 *
 * Created on 9.7.2008, 12:42:32
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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.util.ArrayList;
import javax.swing.JPanel;

/**
 *
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class PreviewPanel extends JPanel {
    private Schema schema;
    
    /**
     * Left factor is a constant used in panel resizing. It is a distance
     * between panel left and the x position of the nearest point in the
     * schema.
     */
    private int leftFactor;
    /**
     * Top factor is a constant used in panel resizing. It is a distance
     * between panel top and the y position of the nearest point in the
     * schema.
     */
    private int topFactor;
    
    /* double buffering */
    private Image dbImage;   // second buffer
    private Graphics2D dbg;  // graphics for double buffering

    /**
     * Holds true when this PreviewPanel was resized, false otherwise
     */
    private boolean panelResized;

    public PreviewPanel() {
        this(null);
    }

    public PreviewPanel(Schema schema) {
        this.schema = schema;
        this.setBackground(Color.WHITE);
        leftFactor = topFactor = 0;
        panelResized = false;
        this.setDoubleBuffered(true);
    }
    
    /**
     * Override previous update method in order to implement
     * double-buffering. As a second buffer is used Image object.
     */
    @Override
    public void update(Graphics g) {
        // initialize buffer if needed
        if (dbImage == null) {
            dbImage = createImage (this.getSize().width,
                    this.getSize().height);
            dbg = (Graphics2D)dbImage.getGraphics();
        }
        // clear screen in background
        dbg.setColor(getBackground());
        dbg.fillRect (0, 0, this.getSize().width,
                this.getSize().height);

        // draw elements in background
        dbg.setColor(getForeground());
        paint(dbg);

        // draw image on the screen
        g.drawImage(dbImage, 0, 0, this);
    }
    
    private void resizePanel(Graphics g) {
        if (schema == null)
            return;
        // hladanie najvzdialenejsich elementov (alebo bodov lebo ciara
        // nemoze byt dalej ako bod)
        int width=0, height=0, minLeft = -1, minTop = -1;

        ArrayList<Element> a = schema.getAllElements();

        for (int i = 0; i < a.size(); i++) {
            Element e = a.get(i);
            e.measure(g,0,0);
        }

        for (int i = 0; i < a.size(); i++) {
            Element e = a.get(i);
            if (minLeft == -1)
                minLeft = e.getX();
            else if (minLeft > e.getX())
                minLeft = e.getX();

            if (minTop == -1)
                minTop = e.getY();
            else if (minTop > e.getY())
                minTop = e.getY();

            if (e.getX() + e.getWidth() > width)
                width = e.getX() + e.getWidth();
            if (e.getY() + e.getHeight() > height)
                height = e.getY() + e.getHeight();
        }
        leftFactor = minLeft - Element.MIN_LEFT_MARGIN;
        topFactor = minTop - Element.MIN_TOP_MARGIN;
        
        for (int i = 0; i < schema.getConnectionLines().size(); i++) {
            ArrayList<Point> ps = schema.getConnectionLines().get(i).getPoints();
            for (int j = 0; j < ps.size(); j++) {
                Point p = ps.get(j);
                if ((int)p.getX() > width)
                    width = (int)p.getX();
                if ((int)p.getY() > height)
                    height = (int)p.getY();
            }
        }
        if (width != 0 && height != 0) {
            this.setSize(width-leftFactor+Element.MIN_LEFT_MARGIN,
                    height-topFactor+Element.MIN_TOP_MARGIN);
            this.revalidate();
        }
        panelResized = true;
    }
    
    //override panel paint method to draw shapes
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (schema == null)
            return;
        boolean moved = panelResized;
        if (panelResized == false)
            resizePanel(g);
        ArrayList<Element> a = schema.getAllElements();
        if (moved == false)
            for (int i = 0; i < a.size(); i++) {
                Element e = a.get(i);
                e.move(new Point(e.getX() - (leftFactor - e.getWidth()/2),
                        e.getY() - (topFactor - e.getHeight()/2)));
            }
        for (int i = 0; i < schema.getConnectionLines().size(); i++)
            schema.getConnectionLines().get(i)
                    .draw((Graphics2D)g, leftFactor, topFactor);
        for (int i = 0; i < a.size(); i++)
            a.get(i).draw(g);
    }

    /**
     * Assign new schema to this PreviewPanel. If it is null, does nothing
     *
     * @param s new abstract schema
     */
    public void setSchema(Schema s) {
        if (s == null)
            return;
        this.schema = s;
        panelResized = false;
        this.repaint();
    }

}
