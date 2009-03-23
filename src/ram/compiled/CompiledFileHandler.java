package ram.compiled;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import RAMmemory.impl.RAMInstruction;

import plugins.memory.IMemoryContext;
import runtime.StaticDialogs;

/**
*
* @author vbmacher
*/
public class CompiledFileHandler {
	private ArrayList<CompiledInstruction> program;
	
	public CompiledFileHandler() {
		program = new ArrayList<CompiledInstruction>();
	}

    public void addInstruction(CompiledInstruction instr) {
        program.add(instr);
    }
    
    /**
     * Method is similar to generateHex() method in that way, that
     * compiled program is also transformed into chunk of bytes, but
     * not to hex file but to the operating memory.
	 * 
	 * @param mem context of operating memory
	 */
	public boolean loadIntoMemory(IMemoryContext mem) {
	    if (mem.getDataType() != RAMInstruction.class) {
	        StaticDialogs.showErrorMessage("Incompatible operating memory type!"
	            + "\n\nThis compiler can't load file into this memory.");
	        return false;
	    }
	    for (int i = 0; i < program.size(); i++) {
	    	RAMInstruction in = program.get(i).getInstr();
            mem.write(0, in);
	    }
	    return true;
	}
	
    public void generateFile(String filename) throws java.io.IOException{
	    filename = filename.substring(0,filename.lastIndexOf(".")) + ".ramc"; // chyba.
	    File file = new File(filename);
		FileOutputStream file_output = new FileOutputStream (file);
        DataOutputStream data_out = new DataOutputStream (file_output);

        data_out.writeChars("RAMC");
        for (int i = 0; i < program.size(); i++) {
        	ArrayList<Integer> code = program.get(i).getCode();
        	for (int j = 0; j < code.size(); j++)
        		data_out.writeInt(code.get(j));
        }
		file_output.close();
    }

}
