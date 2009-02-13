/**
 * CPUInstruction.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KISS, YAGNI
 */
package braincpu.gui;

public class CPUInstruction {
    private String mnemo;
    private String operCode;
    
    public CPUInstruction(String mnemo, String opCode) {
        this.mnemo = mnemo;
        this.operCode = opCode;
    }
    
    public String getMnemo() { return this.mnemo; }
    public String getOperCode() { return this.operCode; }
}