/*
 * PreviewPanel.java
 *
 * Created on 9.7.2008, 12:42:32
 * hold to: KISS, YAGNI
 *
 */

package architecture.drawing;

import java.awt.Color;
import java.awt.Dimension;
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
    private Dimension area; // velkost kresliacej plochy
    private Schema schema;
    
    /* double buffering */
    private Image dbImage;   // second buffer
    private Graphics2D dbg;  // graphics for double buffering
    
    public PreviewPanel(Schema schema) {
        this.schema = schema;
        area = new Dimension(0,0);
        this.setBackground(Color.WHITE);
        resizePanel();
    }
    
    /**
     * Override previous update method in order to implement
     * double-buffering. As a second buffer is used Image object.
     */
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
    
    private void resizePanel() {
        // hladanie najvzdialenejsich elementov (alebo bodov lebo ciara
        // nemoze byt dalej ako bod)
        area.width=0;
        area.height=0;
        ArrayList<Element> a = schema.getAllElements();
        for (int i = 0; i < a.size(); i++) {
            Element e = a.get(i);
            if (e.getX() + e.getWidth() > area.width)
                area.width = e.getX() + e.getWidth();
            if (e.getY() + e.getHeight() > area.height)
                area.height = e.getY() + e.getHeight();
        }
        for (int i = 0; i < schema.getConnectionLines().size(); i++) {
            ArrayList<Point> ps = schema.getConnectionLines().get(i).getPoints();
            for (int j = 0; j < ps.size(); j++) {
                Point p = ps.get(j);
                if ((int)p.getX() > area.width)
                    area.width = (int)p.getX();
                if ((int)p.getY() > area.height)
                    area.height = (int)p.getY();
            }
        }
        if (area.width != 0 && area.height != 0) {
            this.setPreferredSize(area);
            this.revalidate();
        }
    }
    
    //override panel paint method to draw shapes
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        ArrayList<Element> a = schema.getAllElements();
        for (int i = 0; i < a.size(); i++)
            a.get(i).measure(g);
        for (int i = 0; i < schema.getConnectionLines().size(); i++)
            schema.getConnectionLines().get(i).draw((Graphics2D)g);
        for (int i = 0; i < a.size(); i++)
            a.get(i).draw(g);
    }


}
