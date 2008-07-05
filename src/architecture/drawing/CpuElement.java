/*
 * CpuElement.java
 *
 * Created on 3.7.2008, 8:28:14
 * hold to: KISS, YAGNI
 *
 */

package architecture.drawing;

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
    private int x;
    private int y;
    private int width;
    private int height;
    private int tX1; // text "CPU"
    private int tY1;

    private int tX2; // details
    private int tY2;
    
    private boolean wasMeasured;
    private Font boldFont;
    private Font italicFont;
    private Font plainFont;
    
    private String text;
    private Color cpuColor;
  
    public CpuElement(Point e1, String text) {
        x = (int)e1.getX();
        y = (int)e1.getY();
        this.text = text;
        wasMeasured = false;
        cpuColor = new Color(0x6D8471);        
    }
    
    public void draw(Graphics g)  {
        if (!wasMeasured) measure(g);
        g.setColor(cpuColor);
        g.fillRect(x, y, width, height);
        g.setColor(Color.black);
        g.drawRect(x, y, width, height);
        g.setFont(boldFont);
        g.drawString("CPU", tX1, tY1);
        g.setFont(italicFont);
        g.drawString(text, tX2, tY2);
        g.setFont(plainFont);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public void move(int x, int y) {
        wasMeasured = false;
        this.x = x;
        this.y = y;
    }

    @Override
    public void measure(Graphics g) {
        if (wasMeasured) return;
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
        r = fm.getStringBounds(text, g);

        if (width < (int)r.getWidth())
            width = (int)r.getWidth();
        width += 20;
        height += (int)r.getHeight() + 20;
        x -= width/2;
        y -= height/2;

        tX1 = x + (width - tW) / 2;

        tW = (int)r.getWidth();
        tH = (int)fm.getAscent();

        tY1 += y;
        tX2 = x + (width - tW) / 2;
        tY2 = y + (height - tH);
        wasMeasured = true;
    }
    
}

