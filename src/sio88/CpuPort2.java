/*
 * CpuPort2.java
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
public class CpuPort2 implements IDeviceContext {
    private Mits88SIO sio;
    private IDeviceContext dev;
    private EventObject evt;
    
    public CpuPort2(Mits88SIO sio) {
        this.sio = sio;
        this.evt = new EventObject(this);
    }

    public void attachDevice(IDeviceContext device) { this.dev = device; }
    public void detachDevice() { this.dev = null; }
    public IDeviceContext getAttachedDevice() { return dev; }
    
    public void out(EventObject evt, int data) {
        if (dev == null) return;
        dev.out(evt, data);
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

    /**
     * This is communication method between device and
     * SIO. For terminal: If user pressed a key, then it is
     * sent from terminal to SIO device via this method.
     */
    public void writeBuffer(int data) {
        sio.status |= 0x01;
        sio.buffer = data;
    }
    

    public String getID() { return "88-SIO-PORT2"; }
    public int getVersionMajor() { return 1; }
    public int getVersionMinor() { return 5; }
    public String getVersionRev() { return "b1"; }

}
