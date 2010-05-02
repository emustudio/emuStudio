/*
 * ICPUInstruction.java
 *
 * Created on Nedeľa, 2007, november 4, 8:26
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 * 
 * Interface for one row in debug window (GUI)
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
