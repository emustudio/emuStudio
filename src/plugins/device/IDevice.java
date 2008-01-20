/*
 * IDevice.java
 *
 * Created on Utorok, 2007, august 7, 20:01
 *
 * KEEP IT SIMPLE STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */
package plugins.device;

import java.util.*;

import plugins.cpu.*;
import plugins.memory.*;
import plugins.IPlugin;

/**
 *
 * @author vbmacher
 */
public interface IDevice extends IPlugin {
    public void init(ICPU cpu, IMemory mem);

    // visual
    public void showGUI();

    // device mapping
    public interface IDevListener extends EventListener {
        public void devOUT(EventObject evt, int data);
        public int devIN(EventObject evt);
    }
}
