/**
 * RAM.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 *  KISS, YAGNI
 */
package ramcpu.impl;

import java.util.EventObject;
import java.util.HashSet;

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
	private final static String KNOWN_MEM = "f15733d5fdcfe37498c7d14dd913ea24";
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
		context = new RAMContext();
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
	public String getVersion() { return "0.1b"; }

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
    			} catch(Exception e) { break; }
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
    			} catch(Exception e) { break; }
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
    			} catch(Exception e) { break; }
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
    			} catch(Exception e) { break; }
    			return;
    		}
    		break;
    	case IRAMInstruction.ADD:
    		if (in.getDirection() == 0) {
    			try {
    				int r0 = Integer.decode(context.getStorage().getSymbolAt(0));
    				int ri = Integer.decode(context.getStorage().getSymbolAt(
    						(Integer)in.getOperand()));
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
    			} catch(Exception e) { break; }
    			return;
    		} else if (in.getDirection() == '*') {
    			try {
    				int M = Integer.decode(context.getStorage().getSymbolAt(
    						(Integer)in.getOperand()));
    				int r0 = Integer.decode(context.getStorage().getSymbolAt(0));
    				int ri = Integer.decode(context.getStorage().getSymbolAt(M));

    				if (M < 0) break;
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
    			} catch(Exception e) { break; }
    			return;
    		} else if (in.getDirection() == '=') {
    			try {
    				int r0 = Integer.decode(context.getStorage().getSymbolAt(0));
    				int ri = Integer.decode((String)in.getOperand());
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
    			} catch(Exception e) { break; }
    			return;
    		}
    		break;
    	case IRAMInstruction.SUB:
    		if (in.getDirection() == 0) {
    			try {
    				int r0 = Integer.decode(context.getStorage().getSymbolAt(0));
    				int ri = Integer.decode(context.getStorage().getSymbolAt(
    						(Integer)in.getOperand()));
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
    			} catch(Exception e) { break; }
    			return;
    		} else if (in.getDirection() == '*') {
    			try {
    				int M = Integer.decode(context.getStorage().getSymbolAt(
    						(Integer)in.getOperand()));
    				int r0 = Integer.decode(context.getStorage().getSymbolAt(0));
    				int ri = Integer.decode(context.getStorage().getSymbolAt(M));

    				if (M < 0) break;
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
    			} catch(Exception e) { break; }
    			return;
    		} else if (in.getDirection() == '=') {
    			try {
    				int r0 = Integer.decode(context.getStorage().getSymbolAt(0));
    				int ri = Integer.decode((String)in.getOperand());
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
    			} catch(Exception e) { break; }
    			return;
    		}
    		break;
    	case IRAMInstruction.MUL:
    		if (in.getDirection() == 0) {
    			try {
    				int r0 = Integer.decode(context.getStorage().getSymbolAt(0));
    				int ri = Integer.decode(context.getStorage().getSymbolAt(
    						(Integer)in.getOperand()));
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
    			} catch(Exception e) { break; }
    			return;
    		} else if (in.getDirection() == '*') {
    			try {
    				int M = Integer.decode(context.getStorage().getSymbolAt(
    						(Integer)in.getOperand()));
    				int r0 = Integer.decode(context.getStorage().getSymbolAt(0));
    				int ri = Integer.decode(context.getStorage().getSymbolAt(M));

    				if (M < 0) break;
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
    			} catch(Exception e) { break; }
    			return;
    		} else if (in.getDirection() == '=') {
    			try {
    				int r0 = Integer.decode(context.getStorage().getSymbolAt(0));
    				int ri = Integer.decode((String)in.getOperand());
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
    			} catch(Exception e) { break; }
    			return;
    		}
    		break;
    	case IRAMInstruction.DIV:
    		if (in.getDirection() == 0) {
    			try {
    				int r0 = Integer.decode(context.getStorage().getSymbolAt(0));
    				int ri = Integer.decode(context.getStorage().getSymbolAt(
    						(Integer)in.getOperand()));
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
    			} catch(Exception e) { break; }
    			return;
    		} else if (in.getDirection() == '*') {
    			try {
    				int M = Integer.decode(context.getStorage().getSymbolAt(
    						(Integer)in.getOperand()));
    				int r0 = Integer.decode(context.getStorage().getSymbolAt(0));
    				int ri = Integer.decode(context.getStorage().getSymbolAt(M));

    				if (M < 0) break;
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
    			} catch(Exception e) { break; }
    			return;
    		} else if (in.getDirection() == '=') {
    			try {
    				int r0 = Integer.decode(context.getStorage().getSymbolAt(0));
    				int ri = Integer.decode((String)in.getOperand());
    				context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
    			} catch(Exception e) { break; }
    			return;
    		}
    		break;
    	case IRAMInstruction.JMP:
    		IP = (Integer)in.getOperand();
    		return;
    	case IRAMInstruction.JZ:
    		try {
    			String r0 = context.getStorage().getSymbolAt(0);
    			if (r0 == null || r0.equals("")) {
    				IP = (Integer)in.getOperand();
    				return;
    			}
    			int rr0 = Integer.decode(r0);
    			if (rr0 == 0) {
    				IP = (Integer)in.getOperand();
    				return;
    			}
    		} catch(Exception e) { break; }
    		return;
    	case IRAMInstruction.JGTZ:
    		try {
    			String r0 = context.getStorage().getSymbolAt(0);
    			int rr0 = Integer.decode(r0);
    			if (rr0 > 0) {
    				IP = (Integer)in.getOperand();
    				return;
    			}
    		} catch(Exception e) { break; }
    		return;    		
    	case IRAMInstruction.HALT:
    		run_state = ICPU.STATE_STOPPED_NORMAL;
    		return;
    	}
        run_state = ICPU.STATE_STOPPED_BAD_INSTR;
    }
}
