/**
 * RAM.java
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
package ramcpu.impl;

import java.util.EventObject;
import java.util.HashSet;
import java.util.Vector;

import interfaces.IAbstractTapeContext;
import interfaces.IRAMInstruction;
import interfaces.IRAMMemoryContext;

import javax.swing.JPanel;

import plugins.ISettingsHandler;
import plugins.cpu.ICPU;
import plugins.cpu.ICPUContext;
import plugins.cpu.IDebugColumn;
import plugins.memory.IMemoryContext;
import ramcpu.gui.RAMDisassembler;
import ramcpu.gui.RAMStatusPanel;
import runtime.StaticDialogs;

public class RAM implements ICPU, Runnable {
	private final static String KNOWN_MEM = "894da3cf31d433afcee33c22a64d2ed9";
	private long hash;
	@SuppressWarnings("unused")
	private ISettingsHandler settings;
	private IRAMMemoryContext mem;
	private RAMContext context;
    private HashSet<Integer> breaks; // list of breakpoints
    private int run_state;           // store of cpu's run state 
    private RAMDisassembler dis;     // disassembler
    
    private int IP; // instruction position
    private Thread cpuThread;
    private EventObject evt;
    
	public RAM(Long hash) {
		this.hash = hash;
		context = new RAMContext(this);
        cpuThread = null;
        breaks = new HashSet<Integer>();
        run_state = ICPU.STATE_STOPPED_NORMAL;
        evt = new EventObject(this);
	}

	@Override
	public String getTitle() {
		return "Random Access Machine (RAM)";
	}

	@Override
	public String getCopyright() {
		return "\u00A9 Copyright 2009, P. Jakubčo";
	}

	@Override
	public String getVersion() { return "0.1-rc1"; }

	@Override
	public String getDescription() {
		return "This is an emulator of RAM machine. It requires exactly 3 tapes" +
				" (Input, Output, Storage) in order to work properly. Besides" +
				" it requires special operating memory.";
	}
	
	@Override
	public boolean initialize(IMemoryContext mem, ISettingsHandler settings) {
        if (mem == null)
            throw new java.lang.NullPointerException("CPU must have access to memory");
		if (!mem.getHash().equals(KNOWN_MEM) || 
				mem.getDataType() != IRAMInstruction.class ||
				!(mem instanceof IRAMMemoryContext)) {
			StaticDialogs.showErrorMessage("The RAM machine doesn't support this kind of" +
					"memory!");
			return false;
		}
		this.mem = (IRAMMemoryContext)mem;
		this.settings = settings;
        dis = new RAMDisassembler(this.mem,this);
		return true;
	}

	// called from RAMContext after Input tape attachement
	public void loadTape(IAbstractTapeContext tape) {
		Vector<String> data = mem.getInputs();
		if (data == null) return;
		int j = data.size();
		for (int i = 0; i < j; i++)
			tape.setSymbolAt(i, data.elementAt(i));
	}
	
	@Override
	public IDebugColumn[] getDebugColumns() {
		return dis.getDebugColumns();
	}

	@Override
	public Object getDebugValue(int row, int col) {
		return dis.getDebugColVal(row, col);
    }

	@Override
	public void setDebugValue(int row, int col, Object val) {
        dis.setDebugColVal(row, col, val);
	}
	
	@Override
	public JPanel getStatusGUI() {
        return new RAMStatusPanel(this,mem);
	}
	
	@Override
	public boolean isBreakpointSupported() { return true; }
	
	@Override
	public void setBreakpoint(int pos, boolean set) {
        if (set) breaks.add(pos);
        else breaks.remove(pos);
	}
	
	@Override
	public boolean getBreakpoint(int pos) {
        return breaks.contains(pos);
	}

	@Override
	public ICPUContext getContext() { return context; }
	
	@Override
	public boolean setInstrPosition(int pos) {
        if (pos < 0) return false;
		IP = pos;
		return true;
	}

	@Override
	public int getInstrPosition() {
		return IP;
    }

	@Override
	public int getInstrPosition(int pos) {
		return pos + 1;
	}

	public String getR0() {
		if (!context.checkTapes()) return "<empty>";
		return context.getStorage().getSymbolAt(0);
	}

	@Override
	public void destroy() {
		run_state = ICPU.STATE_STOPPED_NORMAL;
		context.destroy();
		context = null;
		breaks.clear();
		breaks = null;
	}

	@Override
	public long getHash() {	return hash; }

	@Override
	public void reset() {
		reset(0);
	}
	
	@Override
	public void reset(int pos) {
		IP = pos;
		run_state = ICPU.STATE_STOPPED_BREAK;
        cpuThread = null;
        context.fireCpuRun(run_state);
        context.fireCpuState();
        
        if (context.checkTapes())
        	loadTape(context.getInput());
	}

	@Override
	public void pause() {
        run_state = ICPU.STATE_STOPPED_BREAK;
        context.fireCpuRun(run_state);
	}

	@Override
	public void stop() {
        run_state = ICPU.STATE_STOPPED_NORMAL;
        context.fireCpuRun(run_state);
	}
	
	@Override
	public void step() {
        if (run_state == ICPU.STATE_STOPPED_BREAK) {
            try {
                run_state = ICPU.STATE_RUNNING;
                emulateInstruction();
                if (run_state == ICPU.STATE_RUNNING)
                    run_state = ICPU.STATE_STOPPED_BREAK;
            }
            catch (IndexOutOfBoundsException e) {
                run_state = ICPU.STATE_STOPPED_ADDR_FALLOUT;
            }
            context.fireCpuRun(run_state);
            context.fireCpuState();
        }
	}
	
	@Override
	public void execute() {
        cpuThread = new Thread(this, "RAMCPU");
        cpuThread.start();
	}


	@Override
	public void showSettings() {
		// no settings
	}

	@Override
	public void run() {
        run_state = ICPU.STATE_RUNNING;
        context.fireCpuRun(run_state);

        while(run_state == ICPU.STATE_RUNNING) {
            try {
                if (getBreakpoint(IP) == true)
                    throw new Error();
                emulateInstruction();
            } catch (IndexOutOfBoundsException e) {
                run_state = ICPU.STATE_STOPPED_ADDR_FALLOUT;
                break;
            }
            catch (Error er) {
                run_state = ICPU.STATE_STOPPED_BREAK;
                break;
            }
        }
        context.fireCpuState();
        context.fireCpuRun(run_state); 
	}

    private void emulateInstruction() {
    	if (!context.checkTapes()) {
        	run_state = ICPU.STATE_STOPPED_ADDR_FALLOUT;
    		return;
    	}
    	
    	IRAMInstruction in = (IRAMInstruction)mem.read(IP++);
    	if (in == null) {
    		run_state = ICPU.STATE_STOPPED_BAD_INSTR;
    		return;
    	}
    	switch (in.getCode()) {
    	case IRAMInstruction.READ:
    		if (in.getDirection() == 0) {
    			String input = (String)context.getInput().in(evt);
    			context.getInput().moveRight();
    			context.getStorage().setSymbolAt((Integer)in.getOperand(), 
    					input);
            	return;
    		} else if (in.getDirection() == '*') {
    			try {
    				int M = Integer.decode(context.getStorage()
    						.getSymbolAt((Integer)in.getOperand()));
    				if (M < 0) break;
    				String input = (String)context.getInput().in(evt);
        			context.getInput().moveRight();
        			context.getStorage().setSymbolAt(M, input);
    			} catch(NumberFormatException e) { break; }
    			return;
    		}
    		break;
    	case IRAMInstruction.WRITE:
    		if (in.getDirection() == 0) {
    			context.getOutput().out(evt, context.getStorage()
    					.getSymbolAt((Integer)in.getOperand()));
    			context.getOutput().moveRight();
    			return;
    		} else if (in.getDirection() == '*') {
    			try {
    				int M = Integer.decode(context.getStorage()
    						.getSymbolAt((Integer)in.getOperand()));
    				if (M < 0) break;
    				context.getOutput().out(evt, 
    						context.getStorage().getSymbolAt(M));
    				context.getOutput().moveRight();
    			} catch(NumberFormatException e) { break; }
    			return;
    		} else if (in.getDirection() == '=') {
    			context.getOutput().out(evt, in.getOperand());
    			context.getOutput().moveRight();
    			return;
    		}
    		break;
    	case IRAMInstruction.LOAD:
    		if (in.getDirection() == 0) {
    			context.getStorage().setSymbolAt(0, context.getStorage()
    					.getSymbolAt((Integer)in.getOperand()));
    			return;
    		} else if (in.getDirection() == '*') {
    			try {
    				int M = Integer.decode(context.getStorage()
    						.getSymbolAt((Integer)in.getOperand()));
    				if (M < 0) break;
    				context.getStorage().setSymbolAt(0, context.getStorage()
    						.getSymbolAt(M));
    			} catch(NumberFormatException e) { break; }
    			return;
    		} else if (in.getDirection() == '=') {
    			context.getStorage().setSymbolAt(0, (String)in.getOperand());
    			return;
    		}
    		break;
    	case IRAMInstruction.STORE:
    		if (in.getDirection() == 0) {
    			context.getStorage().setSymbolAt((Integer)in.getOperand(),
    					context.getStorage().getSymbolAt(0));
    			return;
    		} else if (in.getDirection() == '*') {
    			try {
    				int M = Integer.decode(context.getStorage()
    						.getSymbolAt((Integer)in.getOperand()));
    				if (M < 0) break;
    				context.getStorage().setSymbolAt(M, context.getStorage()
    						.getSymbolAt(0));
    			} catch(NumberFormatException e) { break; }
    			return;
    		}
    		break;
    	case IRAMInstruction.ADD:
    		if (in.getDirection() == 0) {
				String sym0 = context.getStorage().getSymbolAt(0);
				String sym1 = context.getStorage().getSymbolAt((Integer)in.getOperand());
   				// first try double values
   				try { 
       				int r0 = Integer.decode(sym0);
    				int ri = Integer.decode(sym1);
   					context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
   					return;
   				} catch(NumberFormatException e) {}
   				// then integer (if double failed)
       			try {
   					double r0 = Double.parseDouble(sym0);
   					double ri = Double.parseDouble(sym1);
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
    			} catch(NumberFormatException e) { break; }
    			return;
    		} else if (in.getDirection() == '*') {
    			try {
    				int M = Integer.decode(context.getStorage().getSymbolAt(
    						(Integer)in.getOperand()));
    				if (M < 0) break;
    				
    				String sym0 = context.getStorage().getSymbolAt(0);
    				String sym1 = context.getStorage().getSymbolAt(M);
       				// first try double values
       				try { 
        				int r0 = Integer.decode(sym0);
        				int ri = Integer.decode(sym1);
       					context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
       					return;
       				} catch(NumberFormatException e) {}
       				// then integer (if double failed)
   					double r0 = Double.parseDouble(sym0);
   					double ri = Double.parseDouble(sym1);
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
    			} catch(NumberFormatException e) { break; }
    			return;
    		} else if (in.getDirection() == '=') {
    			String sym0 = context.getStorage().getSymbolAt(0);
    			String sym1 = (String)in.getOperand();
   				// first try double values
   				try { 
    				int r0 = Integer.decode(sym0);
    				int ri = Integer.decode(sym1);
   					context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
   					return;
   				} catch(NumberFormatException e) {}
   				// then integer (if double failed)
    			try {
   					double r0 = Double.parseDouble(sym0);
   					double ri = Double.parseDouble(sym1);
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
    			} catch(NumberFormatException e) { break; }
    			return;
    		}
    		break;
    	case IRAMInstruction.SUB:
    		if (in.getDirection() == 0) {
    			String sym0 = context.getStorage().getSymbolAt(0);
    			String sym1 = context.getStorage().getSymbolAt((Integer)in.getOperand());
   				try { 
    				int r0 = Integer.decode(sym0);
    				int ri = Integer.decode(sym1);
   					context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
   					return;
   				} catch(NumberFormatException e) {}
    			try {
   					double r0 = Double.parseDouble(sym0);
   					double ri = Double.parseDouble(sym1);
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
    			} catch(NumberFormatException e) { break; }
    			return;
    		} else if (in.getDirection() == '*') {
    			try {
    				int M = Integer.decode(context.getStorage().getSymbolAt(
    						(Integer)in.getOperand()));
    				if (M < 0) break;
    				String sym0 = context.getStorage().getSymbolAt(0);
    				String sym1 = context.getStorage().getSymbolAt(M);
       				try { 
        				int r0 = Integer.decode(sym0);
        				int ri = Integer.decode(sym1);
       					context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
       					return;
       				} catch(NumberFormatException e) {}
   					double r0 = Double.parseDouble(sym0);
   					double ri = Double.parseDouble(sym1);
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
    			} catch(NumberFormatException e) { break; }
    			return;
    		} else if (in.getDirection() == '=') {
    			String sym0 = context.getStorage().getSymbolAt(0);
    			String sym1 = (String)in.getOperand();  
   				try { 
    				int r0 = Integer.decode(sym0);
    				int ri = Integer.decode(sym1);
   					context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
   					return;
   				} catch(NumberFormatException e) {}
    			try {
   					double r0 = Double.parseDouble(sym0);
   					double ri = Double.parseDouble(sym1);
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
    			} catch(NumberFormatException e) { break; }
    			return;
    		}
    		break;
    	case IRAMInstruction.MUL:
    		if (in.getDirection() == 0) {
    			String sym0 = context.getStorage().getSymbolAt(0);
    			String sym1 = context.getStorage().getSymbolAt((Integer)in.getOperand());
   				try { 
    				int r0 = Integer.decode(sym0);
    				int ri = Integer.decode(sym1);
   					context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
   					return;
   				} catch(NumberFormatException e) {}
    			try {
   					double r0 = Double.parseDouble(sym0);
   					double ri = Double.parseDouble(sym1);
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
    			} catch(NumberFormatException e) { break; }
    			return;
    		} else if (in.getDirection() == '*') {
    			try {
    				int M = Integer.decode(context.getStorage().getSymbolAt(
    						(Integer)in.getOperand()));
    				if (M < 0) break;

    				String sym0 = context.getStorage().getSymbolAt(0);
    				String sym1 = context.getStorage().getSymbolAt(M);
       				try { 
        				int r0 = Integer.decode(sym0);
        				int ri = Integer.decode(sym1);
       					context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
       					return;
       				} catch(NumberFormatException e) {}
   					double r0 = Double.parseDouble(sym0);
   					double ri = Double.parseDouble(sym1);
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
    			} catch(NumberFormatException e) { break; }
    			return;
    		} else if (in.getDirection() == '=') {
    			String sym0 = context.getStorage().getSymbolAt(0);
    			String sym1 = (String)in.getOperand();
   				try { 
    				int r0 = Integer.decode(sym0);
    				int ri = Integer.decode(sym1);
   					context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
   					return;
   				} catch(NumberFormatException e) {}
    			try {
   					double r0 = Double.parseDouble(sym0);
   					double ri = Double.parseDouble(sym1);
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
    			} catch(NumberFormatException e) { break; }
    			return;
    		}
    		break;
    	case IRAMInstruction.DIV:
    		if (in.getDirection() == 0) {
    			String sym0 = context.getStorage().getSymbolAt(0);
    			String sym1 = context.getStorage().getSymbolAt((Integer)in.getOperand());
   				try { 
    				int r0 = Integer.decode(sym0);
    				int ri = Integer.decode(sym0);
   					if (ri == 0) break;
   					context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
   					return;
   				} catch(NumberFormatException e) {}
    			try {
   					double r0 = Double.parseDouble(sym0);
   					double ri = Double.parseDouble(sym1);
    				if (ri == 0) break;
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
    			} catch(NumberFormatException e) { break; }
    			return;
    		} else if (in.getDirection() == '*') {
    			try {
    				int M = Integer.decode(context.getStorage().getSymbolAt(
    						(Integer)in.getOperand()));
    				if (M < 0) break;

    				String sym0 = context.getStorage().getSymbolAt(0);
    				String sym1 = context.getStorage().getSymbolAt(M);
    				
       				try { 
        				int r0 = Integer.decode(sym0);
        				int ri = Integer.decode(sym1);
       					if (ri == 0) break;
       					context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
       					return;
       				} catch(NumberFormatException e) {}
    				
   					double r0 = Double.parseDouble(sym0);
   					double ri = Double.parseDouble(sym1);
    				if (ri == 0) break;

    				context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
    			} catch(Exception e) { break; }
    			return;
    		} else if (in.getDirection() == '=') {
    			String sym0 = context.getStorage().getSymbolAt(0);
    			String sym1 = (String)in.getOperand();
   				try { 
    				int r0 = Integer.decode(sym0);
    				int ri = Integer.decode(sym1);
   					if (ri == 0) break;
   					context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
   					return;
   				} catch(NumberFormatException e) {}
    			try {
   					double r0 = Double.parseDouble(sym0);
   					double ri = Double.parseDouble(sym1);
    				if (ri == 0) break;
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
    			} catch(NumberFormatException e) { break; }
    			return;
    		}
    		break;
    	case IRAMInstruction.JMP:
    		IP = (Integer)in.getOperand();
    		return;
    	case IRAMInstruction.JZ: {
    		String r0 = context.getStorage().getSymbolAt(0);
    		if (r0 == null || r0.equals("")) {
    			IP = (Integer)in.getOperand();
    			return;
    		}
    		int rr0 = 0;
    		boolean t = false;
        	try {
    			rr0 = Integer.decode(r0);
        		t = true;
        	} catch(NumberFormatException e) { }
        	if (t == false) {
        		try {
            		rr0 = (int)Double.parseDouble(r0);
        		} catch(NumberFormatException e) { break; }
        	}
        	if (rr0 == 0) {
        		IP = (Integer)in.getOperand();
        		return;
        	}
    		return;
    	}
    	case IRAMInstruction.JGTZ:
    		try {
    			String r0 = context.getStorage().getSymbolAt(0);
        		int rr0 = 0;
        		boolean t = false;
            	try {
            		rr0 = Integer.decode(r0);
            		t = true;
            	} catch(NumberFormatException e) { }
            	if (t == false) {
            		try {
            			rr0 = (int)Double.parseDouble(r0);
            		} catch(NumberFormatException e) { break; }
            	}
    			if (rr0 > 0) {
    				IP = (Integer)in.getOperand();
    				return;
    			}
    		} catch(NumberFormatException e) { break; }
    		return;    		
    	case IRAMInstruction.HALT:
    		run_state = ICPU.STATE_STOPPED_NORMAL;
    		return;
    	}
        run_state = ICPU.STATE_STOPPED_BAD_INSTR;
    }
}
