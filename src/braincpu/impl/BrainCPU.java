/**
 * BrainCPU.java
 * 
 * KISS, YAGNI
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
package braincpu.impl;

import javax.swing.JPanel;

import braincpu.gui.BrainDisassembler;
import braincpu.gui.BrainStatusPanel;
import braincpu.interfaces.CCCE9E80B38CBADCB7B61244B4DE664A0FEAAD26F;

import plugins.ISettingsHandler;
import plugins.cpu.ICPU;
import plugins.cpu.IDebugColumn;
import plugins.cpu.SimpleCPU;
import plugins.memory.IMemoryContext;
import runtime.Context;
import runtime.StaticDialogs;

public class BrainCPU extends SimpleCPU {
    private IMemoryContext mem;      // kontext pamäte
    private BrainCPUContext cpu;     // kontext procesora
    private int IP, P;               // registre procesora
    private BrainDisassembler dis;   // disassembler

    // konštruktor
    public BrainCPU(Long pluginID) {
        super(pluginID);
        cpu = new BrainCPUContext();
        if (!Context.getInstance().register(pluginID, cpu,
                CCCE9E80B38CBADCB7B61244B4DE664A0FEAAD26F.class))
            StaticDialogs.showErrorMessage("Could not register the CPU");
    }

    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2009-2010, P. Jakubčo";
    }

    @Override
    public String getDescription() {
        return "CPU for BrainDuck architecture";
    }

    @Override
    public String getTitle() {
        return "BrainCPU";
    }

    @Override
    public String getVersion() {
        return "0.13b";
    }

    @Override
    public boolean initialize(ISettingsHandler settings) {
        super.initialize(settings);

        mem = Context.getInstance().getMemoryContext(pluginID,
                IMemoryContext.class, "brainduck_memory");

        if (mem == null) {
            StaticDialogs.showErrorMessage("CPU must have access to memory");
            return false;
        }

        if (mem.getDataType() != Short.class) {
            StaticDialogs.showErrorMessage("Operating memory type is not supported"
                    + " for this kind of CPU.");
            return false;
        }
        dis = new BrainDisassembler(mem, this);
        return true;
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
     * memPos.
     * 
     * @param memPos adresa inštrukcie I1
     * @return adresa nasledujúcej inštrukcie (po I1)
     */
    @Override
    public int getInstrPosition(int pos) {
        short op = (Short) mem.read(pos);
        // LOOP || ENDL
        if (op == 7 || op == 8) {
            return pos + 1;
        } else {
            return pos + 2;
        }
    }

    @Override
    public boolean setInstrPosition(int pos) {
        if (pos < 0) {
            return false;
        }
        IP = pos;
        return true;
    }

    /**** GUI **************************/
    @Override
    public JPanel getStatusGUI() {
        return new BrainStatusPanel(this, mem);
    }

    @Override
    public void showSettings() {
        // Nemáme žiadne GUI nastavení        
    }

    /**** EMULÁCIA **************************/
    public int getP() {
        return P;
    }

    public int getIP() {
        return IP;
    }

    @Override
    public void reset(int adr) {
        super.reset(adr);
        IP = adr; // programové počítadlo na adr

        // nájdeme najbližiu "voľnú" adresu,
        // kde sa už nenachádza program
        try {
            while ((Short) mem.read(adr++) != 0);
        } catch (IndexOutOfBoundsException e) {
            // tu sa dostaneme, ak "adr"
            // už bude ukazovať na neexistujúcu
            // pamäť, čiže keď prejdeme celou pamäťou
            // bez výsledku
            adr = 0;
        }
        P = adr; // a priradíme register P
        // adresu za programom

        // oznámime zmenu stavu listenerom
        fireCpuRun(run_state);
        fireCpuState();
    }

    @Override
    public void run() {
        // zmeníme stav na "running"
        run_state = ICPU.STATE_RUNNING;
        // oznámime zmenu stavu listenerom
        fireCpuRun(run_state);

        // v podstate nekonečný cyklus, pokiaľ
        // emuláciu niečo nezastaví
        // externe: používateľ,
        // interne: chybná inštrukcia, neexistujúca pamäť
        while (run_state == ICPU.STATE_RUNNING) {
            try {
                // ak je na adrese IP nastavený breakpoint,
                // vyhodenie výnimky typu Error
                if (getBreakpoint(IP) == true) {
                    throw new Error();
                }
                emulateInstruction();
            } catch (IndexOutOfBoundsException e) {
                // tu sa dostaneme, ak IP
                // ukazuje na neexistujúcu pamäť
                run_state = ICPU.STATE_STOPPED_ADDR_FALLOUT;
                break;
            } catch (Error er) {
                // odchytenie výnimky Error - zmena
                // stavu na "breakpoint"
                run_state = ICPU.STATE_STOPPED_BREAK;
                break;
            }
        }
        fireCpuState();
        fireCpuRun(run_state);
    }

    @Override
    public void pause() {
        // zmeníme stav behu na "breakpoint"
        run_state = ICPU.STATE_STOPPED_BREAK;
        // oznámime zmenu stavu listenerom 
        fireCpuRun(run_state);
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
                if (run_state == ICPU.STATE_RUNNING) // tak zmeníme stav späť na "breakpoint"
                {
                    run_state = ICPU.STATE_STOPPED_BREAK;
                }
            } catch (IndexOutOfBoundsException e) {
                // tu sa dostaneme, ak IP
                // ukazuje na neexistujúcu pamäť
                run_state = ICPU.STATE_STOPPED_ADDR_FALLOUT;
            }
            // oznámime stav procesora listenerom
            fireCpuRun(run_state);
            fireCpuState();
        }
    }

    @Override
    public void stop() {
        // zmeníme stav behu na "stopped"
        run_state = ICPU.STATE_STOPPED_NORMAL;
        // oznámime zmenu stavu listenerom 
        fireCpuRun(run_state);
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
        OP = ((Short) mem.read(IP++)).shortValue();

        // DECODE
        switch (OP) {
            case 0: /* HALT */
                run_state = ICPU.STATE_STOPPED_NORMAL;
                return;
            case 1: /* INC */
                param = ((Short) mem.read(IP++)).shortValue();
                if (param == 0xff) {
                    P++;
                } else {
                    while (param > 0) {
                        P++;
                        param--;
                    }
                }
                return;
            case 2: /* DEC */
                param = ((Short) mem.read(IP++)).shortValue();
                if (param == 0xff) {
                    P--;
                } else {
                    while (param > 0) {
                        P--;
                        param--;
                    }
                }
                return;
            case 3: /* INCV */
                param = ((Short) mem.read(IP++)).shortValue();
                if (param == 0xff) {
                    mem.write(P, (Short) mem.read(P) + 1);
                } else {
                    while (param > 0) {
                        mem.write(P, (Short) mem.read(P) + 1);
                        param--;
                    }
                }
                return;
            case 4: /* DECV */
                param = ((Short) mem.read(IP++)).shortValue();
                if (param == 0xff) {
                    mem.write(P, (Short) mem.read(P) - 1);
                } else {
                    while (param > 0) {
                        mem.write(P, (Short) mem.read(P) - 1);
                        param--;
                    }
                }
                return;
            case 5: /* PRINT */
                param = ((Short) mem.read(IP++)).shortValue();
                if (param == 0xff) {
                    cpu.writeToDevice((Short) mem.read(P));
                } else {
                    while (param > 0) {
                        cpu.writeToDevice((Short) mem.read(P));
                        param--;
                    }
                }
                return;
            case 6: /* LOAD */
                param = ((Short) mem.read(IP++)).shortValue();
                if (param == 0xff) {
                    mem.write(P, cpu.readFromDevice());
                } else {
                    while (param > 0) {
                        mem.write(P, cpu.readFromDevice());
                        P++;
                        param--;
                    }
                }
                return;
            case 7: { /* LOOP */
                if ((Short) mem.read(P) != 0) {
                    return;
                }
                byte loop_count = 0; // počítadlo vnorenia
                // v cykle (čiže počítadlo
                // cyklov). Nerátam s viac ako
                // 127 vnoreniami
                // inak hľadáme inštrukciu "endl" na
                // aktuálnej úrovni vnorenia (podľa loop_count)
                // IP je nastavený na nasledujúcu inštrukciu
                while ((OP = (Short) mem.read(IP++)) != 0) {
                    // ak je instrukcia <0,6> tak ma parameter
                    if ((OP > 0) && (OP <= 6)) {
                        if ((OP = (Short) mem.read(IP++)) == 0) {
                            break;
                        }
                    }
                    if (OP == 7) {
                        loop_count++;
                    }
                    if (OP == 8) {
                        if (loop_count == 0) {
                            return;
                        } else {
                            loop_count--;
                        }
                    }
                }
                // tu sme už na konci programu, čiže
                // niekde je chyba
                break;
            }
            case 8: /* ENDL */
                if ((Short) mem.read(P) == 0) {
                    return;
                }
                short old_OP = 0;
                byte loop_count = 0; // počítadlo vnorenia
                // v cykle (čiže počítadlo
                // cyklov). Nerátam s viac ako
                // 127 vnoreniami
                // inak hľadáme inštrukciu "loop" hore na
                // aktuálnej úrovni vnorenia (podľa loop_count)
                IP--; // IP späť na túto inštrukciu
                while ((OP = (Short) mem.read(--IP)) != 0) {
                    if (IP - 1 >= 0) {
                        old_OP = (Short) mem.read(IP - 1);
                        if (old_OP > 0 && old_OP <= 6) {
                            OP = old_OP;
                            IP--;
                        }
                    }
                    if (OP == 8) {
                        loop_count++;
                    }
                    if (OP == 7) {
                        if (loop_count == 0) {
                            return;
                        } else {
                            loop_count--;
                        }
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

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }
}
