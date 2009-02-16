/**
 * BrainCPU.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KISS, YAGNI
 */
package braincpu.impl;

import java.util.HashSet;
import javax.swing.JPanel;

import braincpu.gui.BrainDisassembler;
import braincpu.gui.BrainStatusPanel;

import plugins.ISettingsHandler;
import plugins.cpu.ICPU;
import plugins.cpu.ICPUContext;
import plugins.cpu.IDebugColumn;
import plugins.memory.IMemoryContext;
import runtime.StaticDialogs;

public class BrainCPU implements ICPU, Runnable {
    private final static String BRAIN_MEMCONTEXT = "949fe1a163b65ae72a06aeb09976cb47";
    
    private long hash;
    @SuppressWarnings("unused")
	private ISettingsHandler settings;

    private IMemoryContext mem;      // kontext pamäte
    private BrainCPUContext cpu;     // kontext procesora
    private int run_state;           // uchovanie stavu behu
    private HashSet<Integer> breaks; // zoznam breakpointov
    
    private int IP, P;               // registre procesora
    private Thread cpuThread;        // vlákno pre permanentný
                                     // beh procesora
    private BrainDisassembler dis;   // disassembler

    // konštruktor
    public BrainCPU(Long hash) {
        this.hash = hash;
        cpuThread = null;
        cpu = new BrainCPUContext();
        run_state = ICPU.STATE_STOPPED_NORMAL;
        breaks = new HashSet<Integer>();
    }
    
    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2009, P. Jakubčo";
    }

    @Override
    public String getDescription() {
        return "CPU for BrainDuck architecture";
    }

    @Override
    public String getTitle() { return "BrainCPU"; }

    @Override
    public String getVersion() { return "0.12b"; }
    
    @Override
    public long getHash() { return hash; }

    @Override
    public ICPUContext getContext() {
        return cpu;
    }
    
    @Override
    public boolean initialize(IMemoryContext mem, ISettingsHandler settings) {
        if (mem == null)
            throw new java.lang.NullPointerException("CPU must have access to memory");
        if (!mem.getID().equals("brainduck_memory")
                || !mem.getHash().equals(BRAIN_MEMCONTEXT)
                || (mem.getDataType() != Short.class)) {
            StaticDialogs.showErrorMessage("Operating memory type is not supported"
                    + " for this kind of CPU.");
            return false;
        }
        this.mem = mem;
        this.settings = settings;
        dis = new BrainDisassembler(mem,this);
        return true;
    }

    /**** BREAKPOINTY **************************/
    
    @Override
    public boolean isBreakpointSupported() {
        return true;
    }

    @Override
    public void setBreakpoint(int pos, boolean set) {
        if (set) breaks.add(pos);
        else breaks.remove(pos);
    }
    
    @Override
    public boolean getBreakpoint(int pos) {
        return breaks.contains(pos);
    }

    /**** OKNO DEBUGGERA **************************/

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
    
    /**** POZÍCIA INŠTRUKCIE A REGISTRA IP **************************/

    @Override
    public int getInstrPosition() {
        return IP;
    }

    /**
     * Vráti adresu nasledujúcej inštruckie od adresy
     * memPos. Vyu
     * 
     * @param memPos adresa inštrukcie I1
     * @return adresa nasledujúcej inštrukcie (po I1)
     */
    @Override
    public int getInstrPosition(int pos) {
    	short op = (Short)mem.read(pos);
    	// LOOP || ENDL
    	if (op == 7 || op == 8)
    		return pos+1;
    	else return pos+2;
    }

    @Override
    public boolean setInstrPosition(int pos) {
        if (pos < 0) return false;
        IP = pos;
        return true;
    }

    /**** GUI **************************/
    
    @Override
    public JPanel getStatusGUI() {
        return new BrainStatusPanel(this,mem);
    }

    @Override
    public void showSettings() {
        // Nemáme žiadne GUI nastavení        
    }
    
    /**** EMULÁCIA **************************/
    public int getP()  { return P; }
    public int getIP() { return IP; }
    
    @Override
    public void reset() { reset(0); }

    @Override
    public void reset(int adr) {
        IP = adr; // programové počítadlo na adr
        
        // nájdeme najbližiu "voľnú" adresu,
        // kde sa už nenachádza program
        try {
        	while((Short)mem.read(adr++) != 0)
        		;
        } catch(IndexOutOfBoundsException e) {
        	// tu sa dostaneme, ak "adr"
        	// už bude ukazovať na neexistujúcu
        	// pamäť, čiže keď prejdeme celou pamäťou
        	// bez výsledku
        	adr = 0;
        }
        P = adr; // a priradíme register P
                 // adresu za programom

        // zmeníme stav behu na "breakpoint"
        run_state = ICPU.STATE_STOPPED_BREAK;
        cpuThread = null;
        
        // oznámime zmenu stavu listenerom
        cpu.fireCpuRun(run_state);
        cpu.fireCpuState();
    }
    
    @Override
    public void execute() {
        cpuThread = new Thread(this, "BrainCPU");
        cpuThread.start();
    }

    @Override
    public void run() {
    	// zmeníme stav na "running"
        run_state = ICPU.STATE_RUNNING;
        // oznámime zmenu stavu listenerom
        cpu.fireCpuRun(run_state);

        // v podstate nekonečný cyklus, pokiaľ
        // emuláciu niečo nezastaví
        // externe: používateľ,
        // interne: chybná inštrukcia, neexistujúca pamäť
        while(run_state == ICPU.STATE_RUNNING) {
            try {
            	// ak je na adrese IP nastavený breakpoint,
            	// vyhodenie výnimky typu Error
                if (getBreakpoint(IP) == true)
                    throw new Error();
                emulateInstruction();
            } catch (IndexOutOfBoundsException e) {
            	// tu sa dostaneme, ak IP
            	// ukazuje na neexistujúcu pamäť
                run_state = ICPU.STATE_STOPPED_ADDR_FALLOUT;
                break;
            }
            catch (Error er) {
            	// odchytenie výnimky Error - zmena
            	// stavu na "breakpoint"
                run_state = ICPU.STATE_STOPPED_BREAK;
                break;
            }
        }
        cpu.fireCpuState();
        cpu.fireCpuRun(run_state); 
    }
    
    @Override
    public void pause() { 
    	// zmeníme stav behu na "breakpoint"
        run_state = ICPU.STATE_STOPPED_BREAK;
        // oznámime zmenu stavu listenerom 
        cpu.fireCpuRun(run_state);
    }

    @Override
    public void step() { 
    	// ak je stav "breakpoint"
        if (run_state == ICPU.STATE_STOPPED_BREAK) {
            try {
            	// zmeníme stav na "running"
                run_state = ICPU.STATE_RUNNING;
                emulateInstruction();
                // ak by emulácia sponntánne
                // pokračovala (ak ju externe nič
                // nezastavilo)
                if (run_state == ICPU.STATE_RUNNING)
                	// tak zmeníme stav späť na "breakpoint"
                    run_state = ICPU.STATE_STOPPED_BREAK;
            }
            catch (IndexOutOfBoundsException e) {
            	// tu sa dostaneme, ak IP
            	// ukazuje na neexistujúcu pamäť
                run_state = ICPU.STATE_STOPPED_ADDR_FALLOUT;
            }
            // oznámime stav procesora listenerom
            cpu.fireCpuRun(run_state);
            cpu.fireCpuState();
        }
    }

    @Override
    public void stop() { 
    	// zmeníme stav behu na "stopped"
        run_state = ICPU.STATE_STOPPED_NORMAL;
        // oznámime zmenu stavu listenerom 
        cpu.fireCpuRun(run_state);
    }

    @Override
    public void destroy() {
        run_state = ICPU.STATE_STOPPED_NORMAL;
    }

    /**
     * Metóda emuluje jednu inštrukciu.
     */
    private void emulateInstruction() {
    	short OP, param;
    	
    	// FETCH
        OP = ((Short)mem.read(IP++)).shortValue();
        
        // DECODE
        switch(OP) {
        	case 0: /* HALT */
        		run_state = ICPU.STATE_STOPPED_NORMAL;
        		return;
        	case 1: /* INC */
        		param = ((Short)mem.read(IP++)).shortValue();
        		if (param == 0xff) P++;
        		else while (param > 0) { 
        			P++;
        			param--;
        		}
        		return;
        	case 2: /* DEC */
        		param = ((Short)mem.read(IP++)).shortValue();
        		if (param == 0xff) P--;
        		else while (param > 0) {
        			P--;
        			param--;
        		}
        		return;
        	case 3: /* INCV */
        		param = ((Short)mem.read(IP++)).shortValue();
        		if (param == 0xff) mem.write(P, (Short)mem.read(P) + 1);
        		else while (param > 0) {
        			mem.write(P, (Short)mem.read(P) + 1);
        			param--;
        		}
        		return;
        	case 4: /* DECV */
        		param = ((Short)mem.read(IP++)).shortValue();
        		if (param == 0xff) mem.write(P, (Short)mem.read(P) - 1);
        		else while (param > 0) {
        			mem.write(P, (Short)mem.read(P) - 1);
        			param--;
        		}
        		return;
        	case 5: /* PRINT */
        		param = ((Short)mem.read(IP++)).shortValue();
        		if (param == 0xff) cpu.writeToDevice((Short)mem.read(P));
        		else while (param > 0) {
        			cpu.writeToDevice((Short)mem.read(P));
        			param--;
        		}
        		return;
        	case 6: /* LOAD */
        		param = ((Short)mem.read(IP++)).shortValue();
        		if (param == 0xff) mem.write(P, cpu.readFromDevice());
        		else while (param > 0) {
        			mem.write(P, cpu.readFromDevice());
        			P++;
        			param--;
        		}
        		return;
        	case 7: { /* LOOP */
        		if ((Short)mem.read(P) != 0)
        			return;
        		byte loop_count = 0; // počítadlo vnorenia
        		                     // v cykle (čiže počítadlo
        		                     // cyklov). Nerátam s viac ako
        		                     // 127 vnoreniami
        		// inak hľadáme inštrukciu "endl" na
        		// aktuálnej úrovni vnorenia (podľa loop_count)
        		// IP je nastavený na nasledujúcu inštrukciu
        		while ((OP = (Short)mem.read(IP++)) != 0) {
        			// ak je instrukcia <0,6> tak ma parameter
        			if ((OP > 0) && (OP <= 6))
        				if ((OP = (Short)mem.read(IP++)) == 0)
        					break;
        			if (OP == 7) loop_count++;
        			if (OP == 8) {
        				if (loop_count == 0)
        					return;
        				else
        					loop_count--;
        			}
        		}
        		// tu sme už na konci programu, čiže
        		// niekde je chyba
        		break;
        	}
        	case 8: /* ENDL */
        		if ((Short)mem.read(P) == 0)
        			return;
        		short old_OP = 0;
        		byte loop_count = 0; // počítadlo vnorenia
        		                     // v cykle (čiže počítadlo
        		                     // cyklov). Nerátam s viac ako
        		                     // 127 vnoreniami
        		// inak hľadáme inštrukciu "loop" hore na
        		// aktuálnej úrovni vnorenia (podľa loop_count)
        		IP--; // IP späť na túto inštrukciu
        		while ((OP = (Short)mem.read(--IP)) != 0) {
        			if (IP - 1 >= 0) {
        				old_OP = (Short)mem.read(IP-1);
        				if (old_OP > 0 && old_OP <= 6) {
        					OP = old_OP; IP--;
        				}
        			}
        			if (OP == 8) loop_count++;
        			if (OP == 7) {
        				if (loop_count == 0)
        					return;
        				else
        					loop_count--;
        			}
        		}
        		// tu sme už na konci programu, čiže
        		// niekde je chyba
        		break;
        	default: /* chybná inštrukcia*/
        		break;
        }
        run_state = ICPU.STATE_STOPPED_BAD_INSTR;
    }
}
