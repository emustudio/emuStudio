/*
 * PreviewPanel.java
 *
 * Created on 9.7.2008, 12:42:32
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

package emustudio.architecture.drawing;

import java.awt.*;
import java.util.List;
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

    /**
     * Creates empty PreviewPanel
     */
    public PreviewPanel() {
        this(null);
    }

    /**
     * Creates PreviewPanel instance representing given schema.
     *
     * @param schema Schema of the virtual computer
     */
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
     * 
     * @param g the Graphics object. It is retyped to Graphics2D
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

        List<Element> a = schema.getAllElements();

        for (int i = a.size()-1; i >= 0; i--)
            a.get(i).measure(g,0,0);

        for (int i = a.size()-1; i >= 0; i--) {
            Element e = a.get(i);
            int eX = e.getX() - e.getWidth() /2;
            int eY = e.getY() - e.getHeight()/2;
            int eWidth = e.getWidth();
            int eHeight = e.getHeight();

            if (minLeft == -1)
                minLeft = eX;
            else if (minLeft > eX)
                minLeft = eX;

            if (minTop == -1)
                minTop = eY;
            else if (minTop > eY)
                minTop = eY;

            if (eX + eWidth > width)
                width = eX + eWidth;
            if (eY + eHeight > height)
                height = eY + eHeight;
        }
        for (int i = schema.getConnectionLines().size() -1; i >= 0; i--) {
            List<Point> ps = schema.getConnectionLines().get(i).getPoints();
            for (int j = ps.size() - 1; j >= 0; j--) {
                Point p = ps.get(j);

                if (minLeft == -1)
                    minLeft = p.x;
                else if (minLeft > p.x)
                    minLeft = p.x;

                if (minTop == -1)
                    minTop = p.y;
                else if (minTop > p.y)
                    minTop = p.y;

                if (p.x > width)
                    width = p.x;
                if (p.y > height)
                    height = p.y;
            }
        }
        leftFactor = minLeft - Schema.MIN_LEFT_MARGIN;
        topFactor = minTop - Schema.MIN_TOP_MARGIN;
        if (width != 0 && height != 0) {
            this.setSize(width-leftFactor+Schema.MIN_LEFT_MARGIN,
                    height-topFactor+Schema.MIN_TOP_MARGIN);
            this.revalidate();
        }
        panelResized = true;
    }
    
    /**
     * Override panel paint method to draw shapes.
     * 
     * @param g the Graphics object
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (schema == null)
            return;
        boolean moved = panelResized;
        if (panelResized == false)
            resizePanel(g);
        List<Element> a = schema.getAllElements();
        if (moved == false)
            for (int i = 0; i < a.size(); i++) {
                Element e = a.get(i);
                e.move(new Point(e.getX() - leftFactor, e.getY() - topFactor));
            }
        for (int i = 0; i < schema.getConnectionLines().size(); i++) {
            ConnectionLine l = schema.getConnectionLines().get(i);
            l.computeArrows(leftFactor, topFactor);
            l.draw((Graphics2D)g, leftFactor, topFactor,true);
        }
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
    
    /**
     * Clears the preview panel.
     */
    public void clearScreen() {
        this.schema = null;
        this.repaint();
    }

}
