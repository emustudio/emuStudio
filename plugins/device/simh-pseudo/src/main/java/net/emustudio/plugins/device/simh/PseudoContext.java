/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.device.simh;

import net.emustudio.emulib.plugins.device.DeviceContext;
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
class PseudoContext implements DeviceContext<Byte>, Command.Control {
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

    @Override
    public DeviceContext<Byte> getDevice() {
        return this;
    }

    void reset() {
        clearCommand();
        COMMANDS_MAP.values().forEach(c -> c.reset(this));
    }


    @Override
    public Byte readData() {
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
    public void writeData(Byte data) {
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
    public Class<Byte> getDataType() {
        return Byte.class;
    }
}
