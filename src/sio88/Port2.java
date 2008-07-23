/*
 * Port2.java
 *
 * Created on 18.6.2008, 14:30:59
 * hold to: KISS, YAGNI
 *
 * This is the data port of 88-SIO card.
 *
 * A read to the data port gets the buffered character, a write
 * to the data port writes the character to the device.
 *
 */

package sio88;

import java.util.EventObject;
import plugins.device.IDeviceContext;

/**
 *
 * @author vbmacher
 */
public class Port2 implements IDeviceContext {
    private Mits88SIO sio;
    
    public Port2(Mits88SIO sio) {
        this.sio = sio;
    }
    
    public void out(EventObject evt, int data) {
        if (sio.gui == null) return;
        sio.gui.sendChar((char)data);
    }

    public int in(EventObject evt) {
    //    if (buffer == 0 && gui != null) {
      //      // get key from terminal (polling)
        //    buffer = gui.getChar();
       // }
        int v = sio.buffer;
        sio.status &= 0xFE;
        sio.buffer = 0;
        return v;
    }

    public boolean attachDevice(IDeviceContext device) { return false; }
    public void detachDevice(IDeviceContext device) { }

    public String getID() { return "88-SIO-PORT2"; }
    public int getVersionMajor() { return 1; }
    public int getVersionMinor() { return 5; }
    public String getVersionRev() { return "b1"; }

}
