package terminal.gui.utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;

import terminal.TerminalDisplay;
import terminal.TerminalFemale;

public class TerminalKeyboard extends java.awt.event.KeyAdapter implements ContainerListener {
	private TerminalFemale female;
    private boolean halfDuplex = false;
    private Object keyLock; // monitor for key pressing
    private int keyCode = 0;
    private TerminalDisplay terminal;
    
	public TerminalKeyboard(TerminalDisplay terminal,TerminalFemale female) {
        keyLock = new Object();
        this.terminal = terminal;
		this.female = female;
	}

    public void setHalfDuplex(boolean hd) { halfDuplex = hd; }
	
    public int getChar() {
        if (keyCode != 0) {
            int v = keyCode;
            keyCode = 0;
            return v;
        }
        synchronized(keyLock) {
	    try { keyLock.wait(); }
            catch (InterruptedException ex) {
                int v = keyCode;
                keyCode = 0;
                return v;
            }
        }
        return 0;
    }    

    public void addListenerRecursively(Component c) {
        c.addKeyListener(this);
        if(c instanceof Container) {
            Container cont = (Container)c;
            cont.addContainerListener(this);
            Component[] children = cont.getComponents();
            for(int i = 0; i < children.length; i++)
                addListenerRecursively(children[i]);
        }
     }
    
    private void removeListenerRecursively(Component c) {
        c.removeKeyListener(this);
        if(c instanceof Container) {
            Container cont = (Container)c;
            cont.removeContainerListener(this);
            Component[] children = cont.getComponents();
            for(int i = 0; i < children.length; i++)
                removeListenerRecursively(children[i]);
        }
    }    
    
    @Override
    public void keyPressed(KeyEvent evt) {
        if (evt.isControlDown()) {
            switch (evt.getKeyCode()) {
                case (int)'@': keyCode = 0; break;
                case (int)'A': keyCode = 1; break;
                case (int)'B': keyCode = 2; break;
                case (int)'C': keyCode = 3; break;
                case (int)'D': keyCode = 4; break;
                case (int)'E': keyCode = 5; break;
                case (int)'F': keyCode = 6; break;
                case (int)'G': keyCode = 7; break;
                case (int)'H': keyCode = 8; break;
                case (int)'I': keyCode = 9; break;
                case (int)'J': keyCode = 10; break;
                case (int)'K': keyCode = 11; break;
                case (int)'L': keyCode = 12; break;
                case (int)'M': keyCode = 13; break;
                case (int)'N': keyCode = 14; break;
                case (int)'O': keyCode = 15; break;
                case (int)'P': keyCode = 16; break;
                case (int)'Q': keyCode = 17; break;
                case (int)'R': keyCode = 18; break;
                case (int)'S': keyCode = 19; break;
                case (int)'T': keyCode = 20; break;
                case (int)'U': keyCode = 21; break;
                case (int)'V': keyCode = 22; break;
                case (int)'W': keyCode = 23; break;
                case (int)'X': keyCode = 24; break;
                case (int)'Y': keyCode = 25; break;
                case (int)'Z': keyCode = 26; break;
                case (int)'[': keyCode = 27; break;
                case (int)'*': keyCode = 28; break;
                case (int)']': keyCode = 29; break;
                case (int)'^': keyCode = 30; break;
                default: return;
            }
        } else {
            int kC = evt.getKeyCode();
            if (kC == KeyEvent.VK_DOWN) keyCode = 10;
            else if (kC == KeyEvent.VK_UP) keyCode = 11; 
            else if (kC == KeyEvent.VK_RIGHT) keyCode = 12;
            else if (kC == KeyEvent.VK_LEFT) keyCode = 8;
            else if (kC == KeyEvent.VK_ENTER) keyCode = 13;
            else {
                keyCode = (int)evt.getKeyChar();
                if (keyCode > 254) {
                    keyCode = 0;
                    return;
                }
            }
        }
        if (halfDuplex == true) {
            if (keyCode == 13) terminal.out(evt,(short)10);
            terminal.out(evt,(short)keyCode);
        }
        female.out(evt,(short)keyCode);
        synchronized(keyLock) {
            keyLock.notifyAll();
        }
    }

    @Override
    public void componentAdded(ContainerEvent e) {
        addListenerRecursively(e.getChild());
    }

    @Override
    public void componentRemoved(ContainerEvent e) {
        removeListenerRecursively(e.getChild());
    }
}
