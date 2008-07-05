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
    public abstract void measure(Graphics g);
    public abstract void draw(Graphics g);
    public abstract int getWidth();
    public abstract int getHeight();
    public abstract int getX();
    public abstract int getY();
    public abstract void move(int x, int y);
}
