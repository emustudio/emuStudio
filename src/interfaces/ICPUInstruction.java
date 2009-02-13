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
    
    /** Creates a new instance of ICPUInstruction */
    public ICPUInstruction(String mnemo, String opCode) {
        this.mnemo = mnemo;
        this.operCode = opCode;
    }
    
    public String getMnemo() { return this.mnemo; }
    public String getOperCode() { return this.operCode; }
    
}
