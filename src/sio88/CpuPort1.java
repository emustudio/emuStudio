/*
 * CpuPort1.java
 *
 * Created on 18.6.2008, 14:27:23
 * hold to: KISS, YAGNI
 *
 * This is the status port of 88-SIO card.
 * 
 * A write to the status port can select
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
 */

package sio88;

import java.util.EventObject;
import plugins.device.IDeviceContext;

/**
 *
 * @author vbmacher
 */
public class CpuPort1 implements IDeviceContext {
    private Mits88SIO sio;
    
    public CpuPort1(Mits88SIO sio) {
        this.sio = sio;
    }

    @Override
    public Object in(EventObject evt) { return sio.status; }
    @Override
    public void out(EventObject evt, Object val) {
    	short v = (Short)val;
        if (v == 0x03) sio.reset();
    }
    
    @Override
    public Class<?> getDataType() {
    	return Short.class;
    }
    
    @Override
    public String getID() { return "88-SIO-PORT1"; }
    
    @Override
    public String getHash() {
    	return "4a0411686e1560c765c1d6ea903a9c5f";
    }

}
