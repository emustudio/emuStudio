/*
 * ICPUInstruction.java
 *
 * Created on Nedeľa, 2007, november 4, 8:26
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 * 
 * Interface for one row in debug window (GUI)
 */

package interfaces;

/**
 *
 * @author vbmacher
 */
public class ICPUInstruction {
    private String mnemo;
    private String operCode;
    private int nextInstruction;
    
    /** Creates a new instance of ICPUInstruction */
    public ICPUInstruction(String mnemo, String opCode, int next) {
        this.mnemo = mnemo;
        this.operCode = opCode;
        this.nextInstruction = next;
    }
    
    public String getMnemo() { return this.mnemo; }
    public String getOperCode() { return this.operCode; }
    public int getNextInstruction() { return this.nextInstruction; }
    
}
