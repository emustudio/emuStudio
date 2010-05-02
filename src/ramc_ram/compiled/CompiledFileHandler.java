package ramc_ram.compiled;

import interfaces.IRAMInstruction;
import interfaces.IRAMMemoryContext;

import java.util.ArrayList;

import plugins.memory.IMemoryContext;
import ramc_ram.tree.Label;
import runtime.StaticDialogs;

/**
*
* @author vbmacher
*/
public class CompiledFileHandler {
	private ArrayList<IRAMInstruction> program;
	private String KNOWN_MEM_HASH = "894da3cf31d433afcee33c22a64d2ed9";
	
	public CompiledFileHandler() {
		program = new ArrayList<IRAMInstruction>();
	}

    public void addCode(IRAMInstruction code) {
        program.add(code);
    }
    
    /**
     * Method is similar to generateHex() method in that way, that
     * compiled program is also transformed into chunk of bytes, but
     * not to hex file but to the operating memory.
	 * 
	 * @param mem context of operating memory
	 */
	public boolean loadIntoMemory(IMemoryContext mem) {
	    if (!mem.getHash().equals(KNOWN_MEM_HASH) ||
	    		!(mem instanceof IRAMMemoryContext)) {
	        StaticDialogs.showErrorMessage("Incompatible operating memory type!"
	            + "\n\nThis compiler can't load file into this memory.");
	        return false;
	    }
	    IRAMMemoryContext rmem = (IRAMMemoryContext)mem;
	    // load labels
	    Label[] labels = CompilerEnvironment.getLabels();
	    for (int i = 0; i < labels.length; i++)
	    	rmem.addLabel(labels[i].getAddress(), labels[i].getValue());
	    
	    // load input tape
	    rmem.addInputs(CompilerEnvironment.getInputs());

	    // load program
	    for (int i = 0; i < program.size(); i++)
            rmem.write(i, program.get(i));
	    
	    // TODO: beware!
	    CompilerEnvironment.clear();
	    return true;
	}
}
