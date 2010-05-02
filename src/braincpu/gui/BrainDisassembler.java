/**
 *  BrainDisassembler.java
 * 
 *  KISS, YAGNI
 *
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package braincpu.gui;

import braincpu.impl.BrainCPU;
import plugins.cpu.IDebugColumn;
import plugins.memory.IMemoryContext;

public class BrainDisassembler {
	private IMemoryContext mem;     // pamäť bude potrebná pre
	                                // čítanie bytov pre dekódovanie
	                                // inštrukcií
	private BrainCPU cpu;           // procesor je potrebný pre
	                                // zistenie breakpointu na danej
	                                // adrese
    private IDebugColumn[] columns; // stĺpce okna debuggera
	
    /**
     * V konštruktore vytvorím stĺpce ako objekty
     * triedy ColumnInfo.
     * 
     * @param mem  kontext operačnej pamäte, ktorý bude
     *             potrebný pre dekódovanie inštrukcií
     */
	public BrainDisassembler(IMemoryContext mem, BrainCPU cpu) {
		this.mem = mem;
		this.cpu = cpu;
        columns = new IDebugColumn[4];
        IDebugColumn c1 = new ColumnInfo("breakpoint", Boolean.class,true);
        IDebugColumn c2 = new ColumnInfo("address", String.class,false);
        IDebugColumn c3 = new ColumnInfo("mnemonics", String.class,false);
        IDebugColumn c4 = new ColumnInfo("opcode", String.class,false);
        
        columns[0] = c1;columns[1] = c2;columns[2] = c3;columns[3] = c4;
	}
	
	/**
	 * Metóda vráti stĺpce pre okno debuggera ako pole.
	 *  
	 * @return pole stĺpcov
	 */
    public IDebugColumn[] getDebugColumns() { return columns; }

    /**
     * Metóda vráti hodnotu pre okno debuggera, pre daný 
     * riadok a stĺpec. Riadok sa berie ako
     * <strong>adresa</strong>. O odovzdanie správneho parametra
     * adresy sa stará hlavný modul.
     * 
     * @param row  Riadok okna debuggera (adresa v OP) 
     * @param col  Stĺpec v okne debuggera
     * @return hodnota pre daný riadok a stĺpec
     */
    public Object getDebugColVal(int row, int col) {
        try {
        	// najprv musíme disassemblovať inštrukciu
        	// na danej adrese (row)
            CPUInstruction instr = cpuDecode(row);
            switch (col) {
                case 0: /* stĺpec č.0 je breakpoint */
                	return cpu.getBreakpoint(row);
                case 1: /* stĺpec č.1 je adresa */
                	return String.format("%04Xh", row);
                case 2: /* stĺpec č.2 je mnemonický tvar inštr. */ 
                	return instr.getMnemo();
                case 3: /* stĺpec č.3 je operačný kód */
                	return instr.getOperCode(); // operacny kod
                default: return "";
            }
        } catch(IndexOutOfBoundsException e) {
            // Tu sa dostaneme iba v prípade, ak používateľ manuálne
            // zmenil hodnotu operačnej pamäte tak, že vyjadruje
            // inštrukciu s viacerými bytami, ktoré sa už nezmestili
            // do operačnej pamäte a teda vznikla výnimka pri
        	// disassemblovaní. Pre architektúru BrainDuck sa tu
        	// nedostaneme nikdy, ale uvádzam tento kód len pre
        	// ilustráciu.
            switch (col) {
                case 0: return cpu.getBreakpoint(row);
                case 1: return String.format("%04Xh", row);
                case 2: return "incomplete instruction";
                case 3: return String.format("%X", (Short)mem.read(row));
                default: return "";
            }
        }
    }

    /**
     * Metóda je volaná, ak používateľ v okne debuggera zmenil
     * hodnotu na pozícii riadka row a stĺpca col. Pre stĺpce,
     * ktorých riadky nie sú editovateľé, sa táto metóda
     * nezavolá.
     *  
     * @param row   Riadok v okne debuggera
     * @param col   Stĺpec v okne debuggera
     * @param value Nová hodnota
     */
    public void setDebugColVal(int row, int col, Object value) {
        if (col != 0) return;
        if (value.getClass() != Boolean.class) return;
        
        boolean v = Boolean.valueOf(value.toString());
        cpu.setBreakpoint(row,v);
    }
	
    /**
     * Disassembler. Vykoná dekódovanie jednej inštrukcie
     * začínajúcej na adrese memPos.
     * 
     * @param memPos  adresa, kde začína inštrukcia
     * @return dekódovaná inštrukcia
     */
    private CPUInstruction cpuDecode(int memPos) {
        short val,param;
        CPUInstruction instr;
        String mnemo, oper;

        val = ((Short)mem.read(memPos++)).shortValue();
        oper = String.format("%02X",val);
        
        switch (val) {
        	case 0: mnemo = "halt"; break;
            case 1: 
            	mnemo = "inc";
            	param = ((Short)mem.read(memPos++)).shortValue();
        		oper += String.format(" %02X", param);
            	if (param != 0xFF)
            		mnemo += String.format(" %02X", param);
            	break;
            case 2:
            	mnemo = "dec";
            	param = ((Short)mem.read(memPos++)).shortValue();
        		oper += String.format(" %02X", param);
            	if (param != 0xFF)
            		mnemo += String.format(" %02X", param);
            	break;
            case 3:
            	mnemo = "incv";
            	param = ((Short)mem.read(memPos++)).shortValue();
        		oper += String.format(" %02X", param);
            	if (param != 0xFF)
            		mnemo += String.format(" %02X", param);
            	break;
            case 4:
            	mnemo = "decv";
            	param = ((Short)mem.read(memPos++)).shortValue();
        		oper += String.format(" %02X", param);
            	if (param != 0xFF)
            		mnemo += String.format(" %02X", param);
            	break;
            case 5:
            	mnemo = "print";
            	param = ((Short)mem.read(memPos++)).shortValue();
        		oper += String.format(" %02X", param);
            	if (param != 0xFF)
            		mnemo += String.format(" %02X", param);
            	break;
            case 6:
            	mnemo = "load";
            	param = ((Short)mem.read(memPos++)).shortValue();
        		oper += String.format(" %02X", param);
        		if (param != 0xFF)
            		mnemo += String.format(" %02X", param);
            	break;
            case 7:	mnemo = "loop";	break;
            case 8:	mnemo = "endl";	break;
            default: mnemo = "unknown";
        }
        instr = new CPUInstruction(mnemo,oper);
        return instr;
    }
	
}
