/*
 * TerminalDisplay.java
 *
 * Created on Utorok, 2007, november 20, 20:15
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * This class is rewritten example from the book _Java in a Nutshell_
 * by David Flanagan.
 * Written by David Flanagan. Copyright (c) 1996 O'Reilly & Associates.
 *
 * Terminal can interpret ASCII codes from 0-127. Some have special
 * functionality (0-31)
 * 
 * THIS IS a MALE PLUG!!!
 */

package terminal;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EventObject;
import java.util.Timer;
import java.util.TimerTask;
import plugins.device.IDeviceContext;


/**
 * This class provides emulation of CRT display. It supports double buffering
 * and offers anti-aliasing. For painting it uses Graphics2D class.
 *
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class TerminalDisplay extends Canvas implements IDeviceContext {
	private final static String VERBOSE_FILE_NAME = "terminalADM-3A.out";
	
    private char[] video_memory;
    private int col_count; // column count in CRT
    private int row_count; // row count in CRT
    
    private int line_height; // height of the font = height of the line
    private int line_ascent; // Font height above baseline
    private int max_width;   // The width of the terminal
    private int max_height;  // The height of the terminal

    private Timer cursorTimer;
    private CursorPainter cursorPainter;
    private int cursor_x = 0, cursor_y = 0; // position of cursor
    private int char_width = 0;
    private int start_y;

    /* double buffering */
    private Image dbImage;   // second buffer
    private Graphics2D dbg;  // graphics for double buffering
    
    private boolean antiAliasing; // use antialiasing?

    // verbose mode = output to a file
    private boolean verbose =false;
    private FileWriter outw= null;


    public TerminalDisplay(int cols, int rows) {
        this.col_count = cols;
        this.row_count = rows;
        video_memory = new char[rows * cols];
        clear_screen();
        cursorTimer = new Timer();
        cursorPainter = new CursorPainter();
        cursorTimer.scheduleAtFixedRate(cursorPainter,0, 800);
        antiAliasing = false;
    }    
    
    /**
     * Set verbose mode. If verbose mode is set, the output
     * is redirected also to a file.
     * 
     * @param verbose set/unset verbose mode
     */
    public void setVerbose(boolean verbose) {
    	if (verbose) {
    		File f = new File(VERBOSE_FILE_NAME);
    		try {
				outw = new FileWriter(f);
			} catch (IOException e) {}
    	} else if (outw != null) {
    		try {outw.close();} catch (IOException e) {}
    		outw = null;
    	}
    	this.verbose = verbose;
    }

    public boolean isAntiAliasing() { return antiAliasing; }
    public void setAntiAliasing(boolean val) { 
        antiAliasing = val;
        repaint();
    }
    
    public void setCursorPos(int x, int y) {
        cursor_x = x; cursor_y = y; repaint();
    }

    protected void measure() {
        FontMetrics fm = null;
        try { fm = getFontMetrics(getFont()); } catch(Exception e) { return; }
        if (fm == null) return;
        line_height = fm.getHeight();
        line_ascent = fm.getAscent();
        char_width = fm.stringWidth("W");
        
        max_width = col_count * char_width;
        max_height = row_count * line_height;
        
        Dimension d = getSize();
        start_y = 2*line_ascent + (d.height - max_height) / 2;
    }

    public void destroyMe() {
        cursorPainter.stop();
        cursorTimer.cancel();
    	setVerbose(false);
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

    @Override
    public void addNotify() { 
        super.addNotify();
        measure();
    }
    
    @Override
    public Dimension getPreferredSize() { 
        return new Dimension(max_width, max_height);
    }
    
    @Override
    public Dimension getMinimumSize() { 
        return new Dimension(max_width, max_height);
    }

    /**
     * Method clears screen of emulated CRT
     */
    public void clear_screen() {
        synchronized(video_memory) {
            for (int i = 0; i < (row_count * col_count); i++)
                video_memory[i] = ' ';
        }
        cursor_x = 0; cursor_y = 0; repaint();
    }
    
    /**
     * Method inserts char to cursor position. Doesn't move cursor.
     * @param c char to insert
     */
    private void insert_char(char c) {
        synchronized(video_memory) {
            video_memory[cursor_y * col_count + cursor_x] = c;
        }
    }
    
    /**
     * Moves cursor backward in one position. Don't move cursor vertically.
     */
    private void back_cursor() {
        if (cursor_x <= 0) return;
        cursor_x--;
    }
    
    /**
     * Move cursor foreward in one position, also vertically if needed.
     */
    private void move_cursor() {
        cursor_x++;
        if (cursor_x > (col_count-1)) {
            cursor_x = 0; cursor_y++;
            // automatic line rolling
            if (cursor_y > (row_count-1)) {
                roll_line();
                cursor_y = (row_count-1);
            }
        }
    }
    
    /**
     * Rolls screen by 1 row vertically up.
     * The principle: moves all lines beginning from 1 in videomemory into
     * line 0, and previous value of line 0 will be lost.
     */
    public void roll_line() {        
        synchronized(video_memory) {
            for (int i = col_count; i < (col_count * row_count); i++)
                video_memory[i-col_count] = video_memory[i];
            for (int i = col_count * row_count - col_count
                    ; i < (col_count * row_count); i++)
                video_memory[i] = ' ';
        }
        repaint();
    }
    
    /**
     * Override previous update method in order to implement double-buffering.
     * As a second buffer is used Image object.
     */
    @Override
    public void update(Graphics g) {
        // initialize buffer if needed
        if (dbImage == null) {
            dbImage = createImage (this.getSize().width, this.getSize().height);
            dbg = (Graphics2D)dbImage.getGraphics();
        }
        // for antialiasing text (hope it wont be turned on if antiAliasing
        // = false)
        if (antiAliasing) { 
            dbg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
            dbg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            dbg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_OFF);
            dbg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
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
    

    /**
     * This overridement paints video memory into screen. Every char
     * in videomemory represents ASCII form of the char.
     */
    @Override
    public void paint(Graphics g) {
        int t_y;
        int x,y;
        int temp = 0;
        String sLine = "";

        Graphics2D g2d = (Graphics2D)g;
        for (y = 0; y < row_count; y++) {
            t_y = start_y + y * line_height;
            temp = y * col_count;
            for (x = 0; x < col_count; x++) {
                synchronized(video_memory) {
                    sLine += (char)video_memory[temp + x];
                }
            }
            g2d.drawString(sLine,1,t_y);
            sLine = "";
        }
    }
    
    private class CursorPainter extends TimerTask {
        public void run() {
            Graphics g = getGraphics();
            if (g == null) return;
            g.setXORMode(Color.BLACK);
            g.fillRect(cursor_x * char_width, cursor_y*line_height 
                    + start_y-line_height, char_width,line_height);
            g.setPaintMode();
        }
        public void stop() {
	    this.cancel();
        } 
    }

    /**
     * Input from the device is everytime 0, because everything new is
     * sent immediately to the device, so internal buffer of terminal is
     * everytime empty (in the implementation also doesn't exist).
     * @return 0
     */
    @Override
    public Object in(EventObject evt) { return 0; }

    private void verbose_char(short val) {
        if (verbose && (outw != null)) {
        	try {
				outw.write((char)val);
				outw.flush();
			} catch (IOException e) {}
        }    	
    }
    
    /**
     * This method is called from serial I/O card (by OUT instruction)
     */
    @Override
    public void out(EventObject evt, Object value) {
    	short val = (Short)value; 
        measure();

        verbose_char(val);
        /*
         * if it is special char, interpret it. else just add
         * to "video memory"
         */
        switch (val) {
            case 7:
            	return; /* bell */
            case 8: 
            	back_cursor(); repaint(); return; /* backspace*/
            case 0x0A: /* line feed */
                cursor_y++; cursor_x = 0;
                if (cursor_y > (row_count-1)) {
                    cursor_y = (row_count-1);
                    roll_line();
                }
                repaint(); // to be sure for erasing cursor
                return; 
            case 0x0D: cursor_x = 0; return; /* carriage return */
        }
    	insert_char((char)val);
    	move_cursor();
    	repaint();
    }
    
    @Override
    public String getID() { return "ADM-3A"; }

	@Override
	public Class<?> getDataType() {
		return Short.class;
	}

	@Override
	public String getHash() {
		return "4a0411686e1560c765c1d6ea903a9c5f";
	}

  
}