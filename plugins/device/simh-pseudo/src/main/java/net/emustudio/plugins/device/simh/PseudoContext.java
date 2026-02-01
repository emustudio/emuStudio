/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.simh.commands.Command;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import static net.emustudio.plugins.device.simh.Commands.COMMANDS_MAP;
import static net.emustudio.plugins.device.simh.Commands.unknownCmd;

/**
 * SIMH PseudoContext
 * <p>
 * Z80 or 8080 programs communicate with the SIMH pseudo device via port 0xfe.
 * The following principles apply:
 * <p>
 * 1)  For commands that do not require parameters and do not return results
 * ld  a,<cmd>
 * out (0feh),a
 * Special case is the reset command which needs to be send 128 times to make
 * sure that the internal state is properly reset.
 * <p>
 * 2)  For commands that require parameters and do not return results
 * ld  a,<cmd>
 * out (0feh),a
 * ld  a,<p1>
 * out (0feh),a
 * ld  a,<p2>
 * out (0feh),a
 * ...
 * Note: The calling program must send all parameter bytes. Otherwise
 * the pseudo device is left in an undefined state.
 * <p>
 * 3)  For commands that do not require parameters and return results
 * ld  a,<cmd>
 * out (0feh),a
 * in  a,(0feh)    ; <A> contains first byte of result
 * in  a,(0feh)    ; <A> contains second byte of result
 * ...
 * Note: The calling program must request all bytes of the result. Otherwise
 * the pseudo device is left in an undefined state.
 * <p>
 * 4)  Commands requiring parameters and returning results do not exist currently.
 */
class PseudoContext implements Context8080.CpuPortDevice, Command.Control {
    private ByteMemoryContext memory;
    private Context8080 cpu;

    private Commands lastReadCommand = unknownCmd;
    private Commands lastWriteCommand = unknownCmd;


    @Override
    public void clearCommand() {
        lastReadCommand = unknownCmd;
        lastWriteCommand = unknownCmd;
    }

    @Override
    public void clearReadCommand() {
        lastReadCommand = unknownCmd;
    }

    @Override
    public void clearWriteCommand() {
        lastWriteCommand = unknownCmd;
    }

    @Override
    public ByteMemoryContext getMemory() {
        return memory;
    }

    void setMemory(ByteMemoryContext mem) {
        this.memory = mem;
    }

    @Override
    public Context8080 getCpu() {
        return cpu;
    }

    void setCpu(Context8080 cpu) {
        this.cpu = cpu;
    }

    void reset() {
        clearCommand();
        COMMANDS_MAP.values().forEach(c -> c.reset(this));
    }


    @Override
    public byte read(int portAddress) {
        int lastCommandOrdinal = lastReadCommand.ordinal();
        if (!COMMANDS_MAP.containsKey(lastCommandOrdinal)) {
            System.out.printf("SIMH: Unknown command (%d) to SIMH pseudo device ignored.\n", lastCommandOrdinal);
            clearCommand();
        } else {
            return COMMANDS_MAP.get(lastReadCommand.ordinal()).read(this);
        }
        return 0;
    }

    @Override
    public void write(int portAddress, byte data) {
        int lastCommandOrdinal = lastWriteCommand.ordinal();
        if (!COMMANDS_MAP.containsKey(lastCommandOrdinal)) {
            if (!COMMANDS_MAP.containsKey(data & 0xFF)) {
                System.out.printf("SIMH: Unknown command (%d) to SIMH pseudo device ignored.\n", data);
            } else {
                lastReadCommand = Commands.fromInt(data);
                lastWriteCommand = lastReadCommand;
                COMMANDS_MAP.get(lastWriteCommand.ordinal()).start(this);
            }
        } else {
            COMMANDS_MAP.get(lastCommandOrdinal).write(data, this);
        }
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        return "SIMH-pseudo context";
    }
}
