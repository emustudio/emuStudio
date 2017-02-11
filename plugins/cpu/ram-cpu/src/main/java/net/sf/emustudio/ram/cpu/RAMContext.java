package net.sf.emustudio.ram.cpu;

import emulib.plugins.cpu.CPUContext;
import net.sf.emustudio.devices.abstracttape.api.AbstractTapeContext;

public interface RAMContext extends CPUContext {

    AbstractTapeContext getStorage();

    AbstractTapeContext getInput();

    AbstractTapeContext getOutput();


}
