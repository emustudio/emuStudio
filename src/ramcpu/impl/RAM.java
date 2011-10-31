/**
 * RAM.java
 * 
 *  KISS, YAGNI
 *
 * Copyright (C) 2009-2011 Peter Jakubčo <pjakubco at gmail.com>
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

import emuLib8.plugins.cpu.IDisassembler;
import interfaces.C50E67F515A7C87A67947F8FB0F82558196BE0AC7;
import interfaces.C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E;
import interfaces.C8E258161A30C508D5E8ED07CE943EEF7408CA508;

import javax.swing.JPanel;

import emuLib8.plugins.ISettingsHandler;
import emuLib8.plugins.cpu.ICPUContext;
import emuLib8.plugins.cpu.SimpleCPU;
import ramcpu.gui.RAMDisassembler;
import ramcpu.gui.RAMStatusPanel;
import emuLib8.runtime.Context;
import emuLib8.runtime.StaticDialogs;
import java.util.ArrayList;

public class RAM extends SimpleCPU {

    private C8E258161A30C508D5E8ED07CE943EEF7408CA508 mem;
    private RAMContext context;
    private RAMDisassembler dis;     // disassembler
    private int IP; // instruction position

    public RAM(Long pluginID) {
        super(pluginID);
        context = new RAMContext(this);
        if (!Context.getInstance().register(pluginID, context,
                ICPUContext.class)) {
            StaticDialogs.showErrorMessage("Error: Could not register the RAM CPU");
        }
    }

    @Override
    public String getTitle() {
        return "Random Access Machine (RAM)";
    }

    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2009-2011, P. Jakubčo";
    }

    @Override
    public String getVersion() {
        return "0.12b";
    }

    @Override
    public String getDescription() {
        return "This is an emulator of RAM machine. It requires exactly 3 tapes"
                + " (Input, Output, Storage) in order to work properly. Besides"
                + " it requires special operating memory.";
    }

    @Override
    public boolean initialize(ISettingsHandler settings) {
        super.initialize(settings);

        mem = (C8E258161A30C508D5E8ED07CE943EEF7408CA508)
                Context.getInstance().getMemoryContext(pluginID,
                C8E258161A30C508D5E8ED07CE943EEF7408CA508.class);

        if (mem == null) {
            StaticDialogs.showErrorMessage("This CPU must have access to memory");
            return false;
        }
        if (mem.getDataType() != C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.class) {
            StaticDialogs.showErrorMessage("The RAM machine doesn't support"
                    + " this kind of memory!");
            return false;
        }

        dis = new RAMDisassembler(this.mem);
        if (!context.init(pluginID))
            return false;
        return true;
    }

    // called from RAMContext after Input tape attachement
    public void loadTape(C50E67F515A7C87A67947F8FB0F82558196BE0AC7 tape) {
        ArrayList<String> data = mem.getInputs();
        if (data == null) {
            return;
        }

        int j = data.size();
        for (int i = 0; i < j; i++) {
            tape.setSymbolAt(i, data.get(i));
        }
    }

    @Override
    public JPanel getStatusGUI() {
        return new RAMStatusPanel(this, mem);
    }

    @Override
    public boolean setInstrPosition(int pos) {
        if (pos < 0) {
            return false;
        }
        IP = pos;
        return true;
    }

    @Override
    public int getInstrPosition() {
        return IP;
    }

    public String getR0() {
        if (!context.checkTapes()) {
            return "<empty>";
        }
        return context.getStorage().getSymbolAt(0);
    }

    @Override
    public void destroy() {
        run_state = RunState.STATE_STOPPED_NORMAL;
        context.destroy();
        context = null;
        breaks.clear();
        breaks = null;
    }

    @Override
    public void reset(int pos) {
        super.reset(pos);
        IP = pos;
        fireCpuRun(run_state);
        fireCpuState();

        if (context.checkTapes()) {
            loadTape(context.getInput());
        }
    }

    @Override
    public void pause() {
        run_state = RunState.STATE_STOPPED_BREAK;
        fireCpuRun(run_state);
    }

    @Override
    public void stop() {
        run_state = RunState.STATE_STOPPED_NORMAL;
        fireCpuRun(run_state);
    }

    @Override
    public void step() {
        if (run_state == RunState.STATE_STOPPED_BREAK) {
            try {
                run_state = RunState.STATE_RUNNING;
                emulateInstruction();
                if (run_state == RunState.STATE_RUNNING) {
                    run_state = RunState.STATE_STOPPED_BREAK;
                }
            } catch (IndexOutOfBoundsException e) {
                run_state = RunState.STATE_STOPPED_ADDR_FALLOUT;
            }
            fireCpuRun(run_state);
            fireCpuState();
        }
    }

    @Override
    public void showSettings() {
        // no settings
    }

    @Override
    public void run() {
        run_state = RunState.STATE_RUNNING;
        fireCpuRun(run_state);

        while (run_state == RunState.STATE_RUNNING) {
            try {
                if (getBreakpoint(IP) == true) {
                    throw new Error();
                }
                emulateInstruction();
            } catch (IndexOutOfBoundsException e) {
                run_state = RunState.STATE_STOPPED_ADDR_FALLOUT;
                break;
            } catch (Error er) {
                run_state = RunState.STATE_STOPPED_BREAK;
                break;
            }
        }
        fireCpuState();
        fireCpuRun(run_state);
    }

    private void emulateInstruction() {
        if (!context.checkTapes()) {
            run_state = RunState.STATE_STOPPED_ADDR_FALLOUT;
            return;
        }

        C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E in = (C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E) mem.read(IP++);
        if (in == null) {
            run_state = RunState.STATE_STOPPED_BAD_INSTR;
            return;
        }
        switch (in.getCode()) {
            case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.READ:
                if (in.getDirection() == 0) {
                    String input = (String) context.getInput().read();
                    context.getInput().moveRight();
                    context.getStorage().setSymbolAt((Integer) in.getOperand(),
                            input);
                    return;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt((Integer) in.getOperand()));
                        if (M < 0) {
                            break;
                        }
                        String input = (String) context.getInput().read();
                        context.getInput().moveRight();
                        context.getStorage().setSymbolAt(M, input);
                    } catch (NumberFormatException e) {
                        break;
                    }
                    return;
                }
                break;
            case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.WRITE:
                if (in.getDirection() == 0) {
                    context.getOutput().write(context.getStorage()
                            .getSymbolAt((Integer) in.getOperand()));
                    context.getOutput().moveRight();
                    return;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage()
                                .getSymbolAt((Integer) in.getOperand()));
                        if (M < 0) {
                            break;
                        }
                        context.getOutput().write(context.getStorage().getSymbolAt(M));
                        context.getOutput().moveRight();
                    } catch (NumberFormatException e) {
                        break;
                    }
                    return;
                } else if (in.getDirection() == '=') {
                    context.getOutput().write(in.getOperand());
                    context.getOutput().moveRight();
                    return;
                }
                break;
            case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.LOAD:
                if (in.getDirection() == 0) {
                    context.getStorage().setSymbolAt(0, context.getStorage().getSymbolAt((Integer) in.getOperand()));
                    return;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt((Integer) in.getOperand()));
                        if (M < 0) {
                            break;
                        }
                        context.getStorage().setSymbolAt(0, context.getStorage().getSymbolAt(M));
                    } catch (NumberFormatException e) {
                        break;
                    }
                    return;
                } else if (in.getDirection() == '=') {
                    context.getStorage().setSymbolAt(0, (String) in.getOperand());
                    return;
                }
                break;
            case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.STORE:
                if (in.getDirection() == 0) {
                    context.getStorage().setSymbolAt((Integer) in.getOperand(),
                            context.getStorage().getSymbolAt(0));
                    return;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt((Integer) in.getOperand()));
                        if (M < 0) {
                            break;
                        }
                        context.getStorage().setSymbolAt(M, context.getStorage().getSymbolAt(0));
                    } catch (NumberFormatException e) {
                        break;
                    }
                    return;
                }
                break;
            case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.ADD:
                if (in.getDirection() == 0) {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = context.getStorage().getSymbolAt((Integer) in.getOperand());
                    // first try double values
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
                        return;
                    } catch (NumberFormatException e) {
                    }
                    // then integer (if double failed)
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
                    } catch (NumberFormatException e) {
                        break;
                    }
                    return;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt(
                                (Integer) in.getOperand()));
                        if (M < 0) {
                            break;
                        }

                        String sym0 = context.getStorage().getSymbolAt(0);
                        String sym1 = context.getStorage().getSymbolAt(M);
                        // first try double values
                        try {
                            int r0 = Integer.decode(sym0);
                            int ri = Integer.decode(sym1);
                            context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
                            return;
                        } catch (NumberFormatException e) {
                        }
                        // then integer (if double failed)
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
                    } catch (NumberFormatException e) {
                        break;
                    }
                    return;
                } else if (in.getDirection() == '=') {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = (String) in.getOperand();
                    // first try double values
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
                        return;
                    } catch (NumberFormatException e) {
                    }
                    // then integer (if double failed)
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 + ri));
                    } catch (NumberFormatException e) {
                        break;
                    }
                    return;
                }
                break;
            case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.SUB:
                if (in.getDirection() == 0) {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = context.getStorage().getSymbolAt((Integer) in.getOperand());
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
                        return;
                    } catch (NumberFormatException e) {
                    }
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
                    } catch (NumberFormatException e) {
                        break;
                    }
                    return;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt(
                                (Integer) in.getOperand()));
                        if (M < 0) {
                            break;
                        }
                        String sym0 = context.getStorage().getSymbolAt(0);
                        String sym1 = context.getStorage().getSymbolAt(M);
                        try {
                            int r0 = Integer.decode(sym0);
                            int ri = Integer.decode(sym1);
                            context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
                            return;
                        } catch (NumberFormatException e) {
                        }
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
                    } catch (NumberFormatException e) {
                        break;
                    }
                    return;
                } else if (in.getDirection() == '=') {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = (String) in.getOperand();
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
                        return;
                    } catch (NumberFormatException e) {
                    }
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 - ri));
                    } catch (NumberFormatException e) {
                        break;
                    }
                    return;
                }
                break;
            case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.MUL:
                if (in.getDirection() == 0) {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = context.getStorage().getSymbolAt((Integer) in.getOperand());
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
                        return;
                    } catch (NumberFormatException e) {
                    }
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
                    } catch (NumberFormatException e) {
                        break;
                    }
                    return;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt(
                                (Integer) in.getOperand()));
                        if (M < 0) {
                            break;
                        }

                        String sym0 = context.getStorage().getSymbolAt(0);
                        String sym1 = context.getStorage().getSymbolAt(M);
                        try {
                            int r0 = Integer.decode(sym0);
                            int ri = Integer.decode(sym1);
                            context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
                            return;
                        } catch (NumberFormatException e) {
                        }
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
                    } catch (NumberFormatException e) {
                        break;
                    }
                    return;
                } else if (in.getDirection() == '=') {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = (String) in.getOperand();
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
                        return;
                    } catch (NumberFormatException e) {
                    }
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 * ri));
                    } catch (NumberFormatException e) {
                        break;
                    }
                    return;
                }
                break;
            case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.DIV:
                if (in.getDirection() == 0) {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = context.getStorage().getSymbolAt((Integer) in.getOperand());
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        if (ri == 0) {
                            break;
                        }
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
                        return;
                    } catch (NumberFormatException e) {
                        // This really works (tested) for double numbers
                    }
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        if (ri == 0) {
                            break;
                        }
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
                    } catch (NumberFormatException e) {
                        break;
                    }
                    return;
                } else if (in.getDirection() == '*') {
                    try {
                        int M = Integer.decode(context.getStorage().getSymbolAt(
                                (Integer) in.getOperand()));
                        if (M < 0) {
                            break;
                        }

                        String sym0 = context.getStorage().getSymbolAt(0);
                        String sym1 = context.getStorage().getSymbolAt(M);

                        try {
                            int r0 = Integer.decode(sym0);
                            int ri = Integer.decode(sym1);
                            if (ri == 0) {
                                break;
                            }
                            context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
                            return;
                        } catch (NumberFormatException e) {
                        }

                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        if (ri == 0) {
                            break;
                        }

                        context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
                    } catch (Exception e) {
                        break;
                    }
                    return;
                } else if (in.getDirection() == '=') {
                    String sym0 = context.getStorage().getSymbolAt(0);
                    String sym1 = (String) in.getOperand();
                    try {
                        int r0 = Integer.decode(sym0);
                        int ri = Integer.decode(sym1);
                        if (ri == 0) {
                            break;
                        }
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
                        return;
                    } catch (NumberFormatException e) {
                    }
                    try {
                        double r0 = Double.parseDouble(sym0);
                        double ri = Double.parseDouble(sym1);
                        if (ri == 0) {
                            break;
                        }
                        context.getStorage().setSymbolAt(0, String.valueOf(r0 / ri));
                    } catch (NumberFormatException e) {
                        break;
                    }
                    return;
                }
                break;
            case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.JMP:
                IP = (Integer) in.getOperand();
                return;
            case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.JZ: {
                String r0 = context.getStorage().getSymbolAt(0);
                if (r0 == null || r0.equals("")) {
                    IP = (Integer) in.getOperand();
                    return;
                }
                int rr0 = 0;
                boolean t = false;
                try {
                    rr0 = Integer.decode(r0);
                    t = true;
                } catch (NumberFormatException e) {
                }
                if (t == false) {
                    try {
                        rr0 = (int) Double.parseDouble(r0);
                    } catch (NumberFormatException e) {
                        break;
                    }
                }
                if (rr0 == 0) {
                    IP = (Integer) in.getOperand();
                    return;
                }
                return;
            }
            case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.JGTZ:
                try {
                    String r0 = context.getStorage().getSymbolAt(0);
                    int rr0 = 0;
                    boolean t = false;
                    try {
                        rr0 = Integer.decode(r0);
                        t = true;
                    } catch (NumberFormatException e) {
                    }
                    if (t == false) {
                        try {
                            rr0 = (int) Double.parseDouble(r0);
                        } catch (NumberFormatException e) {
                            break;
                        }
                    }
                    if (rr0 > 0) {
                        IP = (Integer) in.getOperand();
                        return;
                    }
                } catch (NumberFormatException e) {
                    break;
                }
                return;
            case C451E861E4A4CCDA8E08442AB068DE18DEE56ED8E.HALT:
                run_state = RunState.STATE_STOPPED_NORMAL;
                return;
        }
        run_state = RunState.STATE_STOPPED_BAD_INSTR;
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public IDisassembler getDisassembler() {
        return dis;
    }
}
