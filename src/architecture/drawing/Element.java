/*
 * Element.java
 *
 * Created on 3.7.2008, 8:26:22
 * hold to: KISS, YAGNI
 *
 */

package architecture.drawing;

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
