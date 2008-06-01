/*
 * TerminalDisplay.java
 *
 * Created on Utorok, 2007, november 20, 20:15
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * This class is rewritten example from the book _Java in a Nutshell_ by David Flanagan.
 * Written by David Flanagan. Copyright (c) 1996 O'Reilly & Associates.
 *
 * Terminal can interpret ASCII codes from 0-127. Some have special functionality (0-31)
 */

package terminalImpl;

import java.awt.*;
import java.util.*;

/**
 *
 * @author vbmacher
 */
public class TerminalDisplay extends Canvas {
    private char[] video_memory;
    private int t_columns;
    private int t_rows;
    
    private int line_height; // Total height of the font
    private int line_ascent; // Font height above baseline
    private int max_width; // The width of the terminal

    private Timer cursorTimer;
    private CursorPainter cursorPainter;
    private int cursor_x = 0, cursor_y = 0;
    private int char_width = 0;
    private int start_y;

    private Image dbImage; // second buffer
    private Graphics dbg;  // graphics for double buffering

    // Here are four versions of the cosntrutor.
    // Break the label up into separate lines, and save the other info.
    public TerminalDisplay(int cols, int rows) { 
        this.t_columns = cols;
        this.t_rows = rows;
        video_memory = new char[rows * cols];
        cursorTimer = new Timer();
        cursorPainter = new CursorPainter();
        cursorTimer.scheduleAtFixedRate(cursorPainter,0, 800);
    }    
    
    public void setCursorPos(int x, int y) {
        cursor_x = x; cursor_y = y; repaint();
    }

    protected void measure() {
        FontMetrics fm = getFontMetrics(getFont());
        if (fm == null) return;
        line_height = fm.getHeight();
        line_ascent = fm.getAscent();
        char_width = fm.stringWidth("W");
        max_width = t_columns * char_width;
        
        Dimension d = getSize();
        start_y = 2*line_ascent + (d.height - t_rows * line_height) / 2;
    }

    public void destroyMe() {
        cursorPainter.stop();
        cursorTimer.cancel();
    }
    
    // Methods to set the various attributes of the component
    @Override
    public void setFont(Font f) { 
        super.setFont(f);
        measure();
        repaint();
    }
    
    @Override
    public void setForeground(Color c) {
        super.setForeground(c);
        repaint();
    }

    public void addNotify() { 
        super.addNotify();
        measure();
    }
    
    public Dimension getPreferredSize() { 
        return new Dimension(max_width, t_rows * line_height);
    }
    
    public Dimension getMinimumSize() { 
        return new Dimension(max_width, t_rows * line_height);
    }

    public void clear_screen() {
        synchronized(video_memory) {
            for (int i = 0; i < (t_rows * t_columns); i++)
                video_memory[i] = 0;
        }
        cursor_x = 0; cursor_y = 0; repaint();
    }
    
    // call from serial I/O card (OUT)
    public void sendChar(int c) {
        // if it is special char, interpret it. else just add to "video memory"
        measure();
        switch (c) {
            case 7: return; /* bell */
            case 8: back_cursor(); repaint(); return; /* backspace*/
            case 0x0A: /* line feed */
                cursor_y++; cursor_x = 0;
                if (cursor_y > (t_rows-1)) {
                    cursor_y = (t_rows-1);
                    roll_line();
                }
                repaint(); // to be sure for erasing cursor
                return; 
            case 0x0D: cursor_x = 0; return; /* carriage return */
        }
        insert_char((char)c);
        move_cursor();
        repaint();
    }

    // insert char to cursor position
    private void insert_char(char c) {
        synchronized(video_memory) {
            video_memory[cursor_y * t_columns + cursor_x] = c;
        }
    }
    
    // don't move cursor vertically
    private void back_cursor() {
        if (cursor_x <= 0) return;
        cursor_x--;
    }
    
    private void move_cursor() {
        cursor_x++;
        if (cursor_x > (t_columns-1)) {
            cursor_x = 0; cursor_y++;
            // automatic line rolling
            if (cursor_y > (t_rows-1)) {
                roll_line();
                cursor_y = (t_rows-1);
            }
        }
    }
    
    // rolls screen by 1 row up
    // hiw: moves lines from 1 in videomemory to line 0
    public void roll_line() {        
        synchronized(video_memory) {
            for (int i = t_columns; i < (t_columns * t_rows); i++)
                video_memory[i-t_columns] = video_memory[i];
            for (int i = t_columns * t_rows - t_columns
                    ; i < (t_columns * t_rows); i++)
                video_memory[i] = 0;
        }
        repaint();
    }
    
    public void update(Graphics g) {
        // initialize buffer
        if (dbImage == null) {
            dbImage = createImage (this.getSize().width, this.getSize().height);
            dbg = dbImage.getGraphics();
        }
        // clear screen in background
        dbg.setColor(getBackground());
        dbg.fillRect (0, 0, this.getSize().width, this.getSize().height);

        // draw elements in background
        dbg.setColor(getForeground());
        paint(dbg);

        // draw image on the screen
        g.drawImage(dbImage, 0, 0, this);
    }
    
    // TODO: improve speed
    public void paint(Graphics g) {
        //int t_x;
        int t_y;
        int x,y;
        int temp = 0;
        String sLine = "";

        Graphics2D g2d = (Graphics2D)g;
        // for antialiasing text
      //  g2d.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
        //        RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
        
        for (y = 0; y < t_rows; y++) {
           // t_x = 0;
            t_y = start_y + y * line_height;
            for (x = 0; x < t_columns; x++) {
                temp = y * t_columns + x;
                synchronized(video_memory) {
                    if (video_memory[temp] != 0)
                        sLine += (char)video_memory[temp];
                    else
                        sLine += " ";
                }
                //t_x = 1 + x * char_width;
            }

            g2d.drawString(sLine,1,t_y);
            sLine = "";
        }
    }
    
    private class CursorPainter extends TimerTask {
        @Override
        public void run() {
            if (!EventQueue.isDispatchThread()) {
	        EventQueue.invokeLater(this);
	    } else {
                Graphics g = getGraphics();
                if (g == null) return;
                g.setXORMode(Color.BLACK);
                g.fillRect(cursor_x * char_width, cursor_y*line_height 
                        + start_y-line_height, char_width,line_height);
                g.setPaintMode();
	    }
        }
        public void stop() {
	    this.cancel();
        } 
    }
  
}