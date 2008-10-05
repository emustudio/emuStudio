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
    private Schema schema;
    
    private ArrayList<Point> points;
    private boolean useGrid; // whether should use and draw grid
    private int gridGap; // gap between vertical and horizontal grid lines
    
    private drawTool tool;
    private String newText;
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
    
    public DrawingPanel(Schema schema, boolean useGrid, int gridGap) {
        this.setBackground(Color.WHITE);
        this.schema = schema;
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
    
    private void paintGrid(Graphics g) {
        g.setColor(gridColor);
        for (int xi = 0; xi < this.getWidth(); xi+=gridGap)
            g.drawLine(xi, 0, xi, this.getHeight());
        for (int yi = 0; yi < this.getHeight(); yi+= gridGap)
            g.drawLine(0, yi, this.getWidth(), yi);
    }
    
    //override panel paint method to draw shapes
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        ArrayList<Element> a = schema.getAllElements();
        
        // najprv mriezka
        if (useGrid) paintGrid(g);
        
        for (int i = 0; i < a.size(); i++)
            a.get(i).measure(this.getGraphics());
        for (int i = 0; i < schema.getConnectionLines().size(); i++)
            schema.getConnectionLines().get(i).draw((Graphics2D)g);
        for (int i = 0; i < a.size(); i++)
            a.get(i).draw(g);
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

    public void setTool(drawTool tool, String text) {
        this.tool = tool;
        this.newText = text;
        shapeMove = false;
        selShape = null;
        selShape2 = null;
        points.clear();
    }
    
    
    public void clear() {
        schema.setCpuElement(null);
        schema.setMemoryElement(null);
        schema.getDeviceElements().clear();
        schema.getConnectionLines().clear();
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
        if (tool == drawTool.nothing || tool == drawTool.delete
                || tool == drawTool.connectLine) {
            // shapeMove?
            shapeMove = false;
            ArrayList<Element> a = schema.getAllElements();
            Point p = e.getPoint();
            for (int i = a.size()-1; i >= 0 ; i--) {
                Element shape = a.get(i);
                if ((shape.getX() <= p.getX()) 
                        && (shape.getX() + shape.getWidth() >= p.getX())
                        && (shape.getY() <= p.getY())
                        && (shape.getY() + shape.getHeight() >= p.getY())) {
                    if (e.getButton() != MouseEvent.BUTTON1) return;
                    if (tool == drawTool.nothing) {
                        // move a shape
                        shapeMove = true;
                        selShape = shape;
                        selShape2 = null;
                    } else if (tool == drawTool.connectLine) {
                        // draw a line
                        if (selShape == null) selShape = shape;
                        else selShape2 = shape;
                    } else if (tool == drawTool.delete) {
                        // delete shape
                        selShape = shape;
                        selShape2 = null;
                    }
                    return;
                }
            }
            if (tool == drawTool.delete) {
                if (e.getButton() != MouseEvent.BUTTON1) return;
                // delete line ?
                for (int i = schema.getConnectionLines().size()-1; i >= 0 ; i--) {
                    ConnectionLine l = schema.getConnectionLines().get(i);
                    // bug?
                    if (l.getCrossPointAfter((int)p.getX(), (int)p.getY()) != -1) {
                        schema.getConnectionLines().remove(i);
                        return;
                    }
                }
            } else if (tool == drawTool.connectLine) {
                // user doesn't clicked on shape, but on drawing area
                if (e.getButton() != MouseEvent.BUTTON1) return;
                if (useGrid)
                    p.setLocation(searchGridPointX((int)p.getX()),
                        searchGridPointY((int)p.getY()));
                points.add(p);
            } else if (tool == drawTool.nothing) {
                // add/remove a point to line ?

                if (selPoint != null) return;
                if (e.getButton() != MouseEvent.BUTTON1) return;
                for (int i = schema.getConnectionLines().size()-1; i >= 0 ; i--) {
                    ConnectionLine l = schema.getConnectionLines().get(i);
                    int pi = -1;
                    if ((pi = l.getCrossPointAfter((int)p.getX(), (int)p.getY()))
                            != -1) {
                        if (useGrid)
                            p.setLocation(searchGridPointX((int)p.getX()),
                                searchGridPointY((int)p.getY()));
                        l.addPoint(pi-1,p);
                        selPointIndex = pi;
                        selPoint = p;
                        selLine = l;
                        repaint();
                        break;
                    }
                }
            }
        }
    }
    
    public void mouseReleased(MouseEvent e) {
        shapeMove = false;
        if (tool == drawTool.delete && selShape != null) {
            if (e.getButton() != MouseEvent.BUTTON1) return;
            for (int i = schema.getConnectionLines().size()-1; i >= 0; i--) {
                if (schema.getConnectionLines().get(i).containsElement(selShape))
                    schema.getConnectionLines().remove(i);
            }
            if (selShape instanceof CpuElement) schema.setCpuElement(null);
            else if (selShape instanceof MemoryElement) 
                schema.setMemoryElement(null);
            else if (selShape instanceof DeviceElement)
                schema.getDeviceElements().remove(selShape);
            repaint();
            resizePanel();
            selShape = null;
            return;
        }
        if (tool == drawTool.nothing) {
            selShape = null;
            // if a point is selected, remove line if user pressed
            // right mouse button
            if (selLine != null && selPoint != null) {
                if (e.getButton() != MouseEvent.BUTTON3) return;
                selLine.removePoint(selPointIndex);
                selPoint = null;
                selLine = null;
                selPointIndex = -1;
                repaint();
            }
            return;
        }
        Point shapePoint = e.getPoint();
        if (useGrid)
            shapePoint.setLocation(searchGridPointX((int)e.getX()),
                    searchGridPointY((int)e.getY()));        
        if (tool == drawTool.shapeCPU)
            schema.setCpuElement(new CpuElement(shapePoint, newText));
        else if (tool == drawTool.shapeMemory)
            schema.setMemoryElement(new MemoryElement(shapePoint, newText));
        else if (tool == drawTool.shapeDevice)
            schema.addDeviceElement(new DeviceElement(shapePoint, newText));
        else if (tool == drawTool.connectLine) {
            if (selShape != null && selShape2 != null) {
                // kontrola ci nahodou uz spojenie neexistuje
                // resp. ci nie je spojenie sam so sebou
                boolean b = false;
                for (int i = 0; i < schema.getConnectionLines().size(); i++) {
                    ConnectionLine l = schema.getConnectionLines().get(i);
                    if (l.containsElement(selShape) 
                            && l.containsElement(selShape2)) {
                        b = true;
                        break;
                    }
                }
                if (!b && (selShape != selShape2)) 
                    schema.getConnectionLines().add(new ConnectionLine(selShape, 
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
            for (int i = schema.getConnectionLines().size()-1; i >= 0 ; i--) {
                Point[]ps = schema.getConnectionLines().get(i).getPoints().toArray(new Point[0]);
                Point p = e.getPoint();
                for (int j = 0; j < ps.length; j++) {
                    double d = Math.hypot(ps[j].getX() - p.getX(), 
                            ps[j].getY() - p.getY());
                    if (d < 8) {
                        selLine = schema.getConnectionLines().get(i);
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
