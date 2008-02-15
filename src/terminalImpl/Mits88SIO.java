/*
 * Mits88SIO_2.java
 *
 * Created on Utorok, 2007, november 13, 17:01
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 *
 * These functions support a simulated MITS 2SIO interface card.
 * The card had two physical I/O ports which could be connected
 * to any serial I/O device that would connect to a current loop,
 * RS232, or TTY interface.  Available baud rates were jumper
 * selectable for each port from 110 to 9600.
 *
 * All I/O is via programmed I/O.  Each each has a status port
 * and a data port.  A write to the status port can select
 * some options for the device (0x03 will reset the port).
 * A read of the status port gets the port status:
 *
 *  +---+---+---+---+---+---+---+---+
 *  | X   X   X   X   X   X   O   I |
 *  +---+---+---+---+---+---+---+---+
 *
 * I - A 1 in this bit position means a character has been received
 *     on the data port and is ready to be read.
 * O - A 1 in this bit means the port is ready to receive a character
 *     on the data port and transmit it out over the serial line.
 *
 * A read to the data port gets the buffered character, a write
 * to the data port writes the character to the device.
 *
 * Meaning of all bits:
 * 7. 0 - Output device ready (a ready pulse was sent from device) also causes
 *        a hardware interrupt if is enabled
 *    1 - not ready
 * 6. not used
 * 5. 0 -
 *    1 - data available (a word of data is in the buffer on the I/O board)
 * 4. 0 -
 *    1 - data overflow (a new word of data has been received before the previous
 *        word was inputed to the accumulator)
 * 3. 0 -
 *    1 - framing error (data bit has no valid stop bit)
 * 2. 0 -
 *    1 - parity error (received parity does not agree with selected parity)
 * 1. 0 - 
 *    1 - X-mitter buffer empty (the previous data word has been X-mitted and a new data
 *        word may be outputted
 * 0. 0 - Input device ready (a ready pulse has been sent from the device)
 * 
 * From: http://www.altair32.com/Altair32specs.htm
 * The standard I/O addresses assigned by MITS was 20Q-21Q  for the first port and 22Q-23Q
 * for the second. The second port of the 2SIO is "connected" to a virtual line printer and
 * the paper tape reader/punch for support under CP/M.
 */

package terminalImpl;

import plugins.device.*;
import plugins.cpu.*;
import plugins.memory.*;

import java.util.*;

/**
 *
 * @author vbmacher
 */
public class Mits88SIO implements IDevice {
    private int buffer;
    private short status;
    private boolean attached = false;

    private ICPU cpu = null;
    private frmGUI gui = null;
    
    /** Creates a new instance of Mits88SIO_2 */
    public Mits88SIO() {}

    public String getDescription() {
        return "Recomended to use as a part of MITS Altair8800 computer. It is"
                + "implemented using MITS SIO serial interface. It have 2 IO ports:"
                + " 0x10(status), 0x11(data). For SIO programming see "
                + "http://www.classiccmp.org/dunfield/s100c/mits/88sio_1.pdf";
    }
    public String getVersion() { return "0.09b"; }
    public String getName() { return "ADM-3A Interactive display terminal"; }
    public String getCopyright() { return "\u00A9 Copyright 2007-2008, Peter Jakubčo"; }

    public static void showErrorMessage(String message) {
        javax.swing.JOptionPane.showMessageDialog(null,
                message,"Error",javax.swing.JOptionPane.ERROR_MESSAGE);
    }

    
    /* Reset routine */
    private void Reset() {
        buffer = 0;    /* Data */
        status = 0x02; /* Status */
    }
    
    // if user press key
    public void writeBuffer(int data) {
        status |= 0x01;
        buffer = data;
    }
    /*  I/O instruction handlers, called from the CPU module when an IN or OUT
     *  instruction is issued.
     *  Each function is passed an 'io' flag, where 0 means a read from
     *  the port, and 1 means a write to the port.  On input, the actual
     *  input is passed as the return value, on output, 'data' is written
     *  to the device.
     */
    public void init(ICPU cpu, IMemory mem) {
        this.cpu = cpu;
        this.attached = false;
        Reset();
        // attach IO ports
        if (cpu.attachDevice(new IDevListener() {
            public void devOUT(EventObject evt,  int data) {
                if (data == 0x03) Reset();
            }
            public int devIN(EventObject evt) {
                return status;
            }
        }, 0x10) == false) {
            showErrorMessage("Error: this device can't be attached (maybe there is a hardware conflict )");
            return;
        }
        
        if (cpu.attachDevice(new IDevListener() {
            public void devOUT(EventObject evt, int data) {
                if (gui == null) return;
                gui.sendChar((char)data);
            }
            public int devIN(EventObject evt) {
                if (buffer == 0 && gui != null) {
                    // get key from terminal (polling)
                    buffer = gui.getChar();
                }
                int v = buffer;
                status &= 0xFE;
                buffer = 0;
                return v;
            }
        },0x11) == false) {
            showErrorMessage("Error: this device can't be attached (maybe there is a hardware conflict)");
            cpu.disattachDevice(0x10);
            return;
        }
        this.attached = true;
    }

    public void showGUI() {
        if (gui == null) gui = new frmGUI(this);
        gui.setVisible(true);
    }
    
    public void destroy() {
        cpu.disattachDevice(0x10);
        cpu.disattachDevice(0x11);
        this.attached = false;
        if (gui != null) {
            gui.destroyMe();
            gui.dispose();
        }
        gui = null;
    }
    
    public boolean isAttached() { return attached; }
}
