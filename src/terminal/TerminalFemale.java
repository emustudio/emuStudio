/*
 * TerminalFemale.java
 *
 * Created on 28.7.2008, 21:38:02
 * hold to: KISS, YAGNI
 * 
 * Female plug
 *
 */

package terminal;

import java.util.EventObject;
import plugins.device.IDeviceContext;

/**
 *
 * @author vbmacher
 */
public class TerminalFemale implements IDeviceContext {
    private TerminalDisplay terminal;
    private IDeviceContext dev;
    
    public TerminalFemale(TerminalDisplay terminal) {
        this.terminal = terminal;
    }
    
    public void attachDevice(IDeviceContext device) { this.dev = device; }
    public void detachDevice() { this.dev = null; }

    public int in(EventObject evt) { return 0; }

    public void out(EventObject evt, int val) {
        if (dev == null) return;
        dev.out(evt, val);
    }

    public String getID() { return "ADM-3A"; }
    public int getVersionMajor() { return 1; }
    public int getVersionMinor() { return 2; }
    public String getVersionRev() { return "b1"; }

}
