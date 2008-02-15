/*
 * ICPU.java
 *
 * Created on Pondelok, 2007, august 6, 9:00
 *
 * KEEP IT SIMPLE STUPID
 * sometimes just: YOU AREN'T GONNA NEED IT
 *
 * This interface is used for communication between CPU and main module
 * Plugin can interact with user by own GUI.
 */

package plugins.cpu;

import java.util.*;
import javax.swing.*;

import plugins.memory.*;
import plugins.device.*;
import plugins.IPlugin;

/**
 * Core CPU interface for communication between CPU and main module
 * @author vbmacher
 */
public interface ICPU extends IPlugin {
        /* DOWN: CPU core control */
        public void init(IMemory mem);
        public void reset(int programStart);
        public void execute();
        public void pause(); // breakpoint
        public void stop();
        // runs CPU for 1 step
        public void step();

        /**
         * External interrupt: device should call this
         * @param b1 - rst instruction
         * @param b1,b2,b3 call instruction (b1,b2,b3)
         */
        public void interrupt(short b1, short b2, short b3);
        
        // reflects CPU state changes
        public enum stateEnum { stoppedNormal, stoppedBreak, 
        stoppedAdrFallout , stoppedBadInstr, runned }
        
        public interface ICPUListener extends EventListener {
            /**
             * Triggers when CPU is runned or stopped (by any reason)
             * @param evt event object (CPU)
             * @param state current CPU state
             */
            public void cpuRunChanged(EventObject evt, stateEnum state);
            /**
             * Triggers after every instruction execution (by step() function)
             * or after whole run (execute() function) in order to update
             * GUIs, etc...
             * @param evt event object (CPU)
             */
            public void cpuStateUpdated(EventObject evt);
            /**
             * Triggers only in running state, when computation of real
             * CPU frequency was updated. If run-time frequency computation
             * is turned off, then it is not triggerred at all.
             * @param evt event object (CPU)
             * @param frequency new run-time frequency
             */
            public void frequencyChanged(EventObject evt, float frequency);
        }
        /**
         * Adds listener that implements ICPUListener to internal
         * list of listeners
         * @param listener Listener to add
         */
        public void addCPUListener(ICPUListener listener);
        /**
         * remove listener from listeners list
         * @param listener Listener to remove
         */
        public void removeCPUListener(ICPUListener listener);
        
        
        /* DOWN: GUI interaction */
        /**
         * Get column names and types for debug window in main module.
         * It is a try to be general for debugging.
         * @return debug window column values and types
         */
        public IDebugColumns[] getDebugColumns(); 
        
        /**
         * User set a value to a unique identified cell by row
         * (usually address) and column. This function just import this
         * value and interpret is for CPU behavior.
         * @param index model row value(usually address)
         * @param col column
         * @param value set value
         */
        public void setDebugValue(int index, int col, Object value);
        
        /**
         * Gets a value for unique identified cell in debug window in
         * main module.
         * @param index model row (usually address)
         * @param col columnt
         * @return value for debug window
         */
        public Object getDebugValue(int index, int col);
        
        /**
         * Determine if is breakpoint supported
         * @return true if breakpoint is supported
         */
        public boolean isBreakpointSupported();
        
        /**
         * Set/unset breakpoint to/from specified address
         * @param adr address to which toggle the breakpoint
         * @param set if set or unsed breakpoint from specified address
         */
        public void setBreakpoint(int adr, boolean set);
        
        /**
         * Test if on specified address is set a breakpoint
         * @param adr address of breakpoint test
         * @return true if breakpoint is set
         */
        public boolean getBreakpoint(int adr);
        
        /**
         * Gets a GUI JPanel for status window (will be rendered in
         * main module)
         * @return status GUI window
         */
        public JPanel getStatusGUI();
        
        /* DOWN: Device interaction */
        /**
         * Attach device to CPU
         * @param device Device to attach
         * @param port I/O CPU port
         * @return true if device was successfully attached, false if not
         */
        public boolean attachDevice(IDevice.IDevListener device, int port);
        /**
         * Disconnect device (disattach) from a port
         * @param port disattach a device in that port
         */
        public void disattachDevice(int port);        
        
        /* DOWN: CPU Context control */
        public int getPC();
        public boolean setPC(int memPos);
        public int getNextPC(int memPos);
}
