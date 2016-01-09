package sk.tuke.emustudio.rasp.memory;

import emulib.plugins.memory.MemoryContext;

public interface RASPMemoryContext extends MemoryContext<RASPInstruction> {

    public void addLabel(int pos, String label);

    public String getLabel(int pos);
    
    public void destroy();

}
