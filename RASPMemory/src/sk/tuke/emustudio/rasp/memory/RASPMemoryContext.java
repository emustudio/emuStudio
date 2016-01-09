package sk.tuke.emustudio.rasp.memory;

import emulib.plugins.memory.MemoryContext;
import java.util.List;

public interface RASPMemoryContext extends MemoryContext<RASPInstruction> {

    public void addLabel(int pos, String label);

    public String getLabel(int pos);

    public void addInputs(List<String> inputs);

    public List<String> getInputs();
    
    public void destroy();

}
