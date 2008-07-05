/*
 * DrawingPanel.java
 *
 * Created on 3.7.2008, 8:31:58
 * hold to: KISS, YAGNI
 *
 */

package architecture.drawing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import javax.swing.JPanel;

/**
 *
 * @author vbmacher
 */
public class DrawingPanel extends JPanel implements MouseListener,
        MouseMotionListener {
    private Point e1 = null;
    private Dimension area; // velkost kresliacej plochy
    private BasicStroke thickLine;
    
    private ArrayList<Element> shapes;
    private ArrayList<ConnectionLine> lines;
    private ArrayList<Point> points;
    private boolean useGrid; // whether should use and draw grid
    private int gridGap; // gap between vertical and horizontal grid lines
    
    private drawTool tool;
    private boolean shapeMove;

    public enum drawTool {
        shapeCPU,
        shapeMemory,
        shapeDevice,
        connectLine,
        delete,
        nothing
    }
    
    /* double buffering */
    private Image dbImage;   // second buffer
    private Graphics2D dbg;  // graphics for double buffering
    
    private Element selShape;
    private Element selShape2;
    
    private ConnectionLine selLine; // pri presuvani bodov ciary
    private int selPointIndex;
    private Point selPoint;
    private Color gridColor;
    
    public int searchGridPointX(int x_near) {
        if (gridGap <= 0) return x_near;
        int dX = (int)Math.round(x_near / (double)gridGap);
        return (dX * gridGap);
    }
    
    public int searchGridPointY(int y_near) {
        if (gridGap <= 0) return y_near;
        int dY = (int)Math.round(y_near / (double)gridGap);
        return (dY * gridGap);
    }
    
    public DrawingPanel(boolean useGrid, int gridGap) {
        this.setBackground(Color.WHITE);
        shapes = new ArrayList<Element>();
        lines = new ArrayList<ConnectionLine>();
        points = new ArrayList<Point>();
        tool = drawTool.nothing;
        area = new Dimension(0,0);
        thickLine = new BasicStroke(2);
        shapeMove = false;
        this.useGrid = useGrid;
        this.gridGap = gridGap;
        gridColor = new Color(0xBFBFBF);
    }
    
    public void setUseGrid(boolean useGrid) {
        this.useGrid = useGrid;
        repaint();
    }
    
    public void setGridGap(int gridGap) {
        this.gridGap = gridGap;
        repaint();
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
        for (int i = 0; i < shapes.size(); i++) {
            Element e = shapes.get(i);
            if (e.getX() + e.getWidth() > area.width)
                area.width = e.getX() + e.getWidth();
            if (e.getY() + e.getHeight() > area.height)
                area.height = e.getY() + e.getHeight();
        }
        for (int i = 0; i < lines.size(); i++) {
            ArrayList<Point> ps = lines.get(i).getPoints();
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
        
        // najprv mriezka
        if (useGrid) {
            g.setColor(gridColor);
            for (int xi = 0; xi < this.getWidth(); xi+=gridGap) {
                g.drawLine(xi, 0, xi, this.getHeight());
            }
            for (int yi = 0; yi < this.getHeight(); yi+= gridGap) {
                g.drawLine(0, yi, this.getWidth(), yi);
            }
        }
        for (int i = 0; i < shapes.size(); i++)
            shapes.get(i).measure(g);
        for (int i = 0; i < lines.size(); i++)
            lines.get(i).draw((Graphics2D)g);
        for (int i = 0; i < shapes.size(); i++)
            shapes.get(i).draw(g);
        // ak je oznaceny nejaky bod ciary
        if (selPoint != null) {
            int xx = (int)selPoint.getX();
            int yy = (int)selPoint.getY();
            g.setColor(Color.red);
            ((Graphics2D)g).setStroke(thickLine);
            g.drawOval(xx-4, yy-4, 8, 8);
        }
        if (tool == drawTool.connectLine && selShape != null) {
            ConnectionLine.drawSketch((Graphics2D)g, selShape, 
                    e1, points);
        }
    }

    public void setTool(drawTool tool) {
        this.tool = tool;
        shapeMove = false;
        selShape = null;
        selShape2 = null;
        points.clear();
    }
    
    
    public void clear() {
        shapes.clear();
        lines.clear();
        points.clear();
        repaint();
    }
    
    public void cancelTasks() {
        shapeMove = false;
        selShape = null;
        selShape2 = null;
        points.clear();
        repaint();
    }
    
    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e){}
    public void mouseExited(MouseEvent e){}

    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) return;
        if (tool == drawTool.nothing || tool == drawTool.delete
                || tool == drawTool.connectLine) {
            // shapeMove?
            shapeMove = false;
            for (int i = shapes.size()-1; i >= 0 ; i--) {
                Element el = shapes.get(i);
                Point p = e.getPoint();
                if ((el.getX() <= p.getX()) 
                        && (el.getX() + el.getWidth() >= p.getX())
                        && (el.getY() <= p.getY())
                        && (el.getY() + el.getHeight() >= p.getY())) {
                    if (tool == drawTool.nothing) {
                        shapeMove = true;
                        selShape = el;
                        selShape2 = null;
                    } else if (tool == drawTool.connectLine) {
                        if (selShape == null) selShape = el;
                        else selShape2 = el;
                    } else if (tool == drawTool.delete) {
                        selShape = el;
                        selShape2 = null;
                    }
                    return;
                }
            }
            if (tool == drawTool.connectLine) {
                Point p = e.getPoint();
                if (useGrid)
                    p.setLocation(searchGridPointX((int)p.getX()),
                        searchGridPointY((int)p.getY()));
                points.add(p);
            }
            return;
        }
        e1 = e.getPoint();
        if (useGrid)
            e1.setLocation(searchGridPointX((int)e1.getX()),
                    searchGridPointY((int)e1.getY()));

    }
    
    public void mouseReleased(MouseEvent e) {
        shapeMove = false;
        if (tool == drawTool.delete && selShape != null) {
            for (int i = lines.size()-1; i >= 0; i--) {
                if (lines.get(i).containsElement(selShape))
                    lines.remove(i);
            }
            shapes.remove(selShape);
            repaint();
            resizePanel();
            selShape = null;
            return;
        }
        if (tool == drawTool.nothing) {
            selShape = null;
            return;
        }
        if (tool == drawTool.shapeCPU)
            shapes.add(new CpuElement(e1, "CPU " + (shapes.size()+1)));
        else if (tool == drawTool.shapeMemory)
            shapes.add(new MemoryElement(e1, "Mem " + (shapes.size()+1)));
        else if (tool == drawTool.shapeDevice)
            shapes.add(new DeviceElement(e1, "Dev " + (shapes.size()+1)));
        else if (tool == drawTool.connectLine) {
            if (selShape != null && selShape2 != null) {
                // kontrola ci nahodou uz spojenie neexistuje
                // resp. ci nie je spojenie sam so sebou
                boolean b = false;
                for (int i = 0; i < lines.size(); i++) {
                    ConnectionLine l = lines.get(i);
                    if (l.containsElement(selShape) 
                            && l.containsElement(selShape2)) {
                        b = true;
                        break;
                    }
                }
                if (!b && (selShape != selShape2)) 
                    lines.add(new ConnectionLine(selShape, 
                        selShape2,points));
                selShape = null;
                selShape2 = null;
                points.clear();
            }
        }
        repaint();
        resizePanel();
        e1 = null;
    }

    public void mouseDragged(MouseEvent e) {
        if (selPoint != null && selLine != null) {
            Point p = e.getPoint();
            if (p.getX() < 0 || p.getY() < 0) return;
            if (useGrid)
                p.setLocation(searchGridPointX((int)p.getX()),
                        searchGridPointY((int)p.getY()));
            selLine.pointMove(selPointIndex, (int)p.getX(),
                    (int)p.getY());
            repaint();
            resizePanel();
        } else if (shapeMove && selShape != null) {
            Point p = e.getPoint();
            if (p.getX() < 0 || p.getY() < 0) return;
            if (useGrid)
                p.setLocation(searchGridPointX((int)p.getX()),
                        searchGridPointY((int)p.getY()));
            selShape.move((int)p.getX(), (int)p.getY());
            repaint();
            resizePanel();
        } 
    }

    public void mouseMoved(MouseEvent e) {
        if (shapeMove) return;
        selPoint = null;
        selPointIndex = 0;
        if (selLine != null) repaint();
        selLine = null;

        if (tool == drawTool.nothing) {
            for (int i = lines.size()-1; i >= 0 ; i--) {
                Point[]ps = lines.get(i).getPoints().toArray(new Point[0]);
                Point p = e.getPoint();
                for (int j = 0; j < ps.length; j++) {
                    double d = Math.hypot(ps[j].getX() - p.getX(), 
                            ps[j].getY() - p.getY());
                    if (d < 8) {
                        selLine = lines.get(i);
                        selPointIndex = j;
                        selPoint  = ps[j];
                        repaint();
                        break;
                    } 
                }
            }
        } else if (tool == drawTool.connectLine && selShape != null) {
            e1 = e.getPoint();
            repaint();
        }
    }

}
