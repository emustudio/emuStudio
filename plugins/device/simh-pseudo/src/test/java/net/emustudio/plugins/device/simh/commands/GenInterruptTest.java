/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class GenInterruptTest extends CommandTestBase {

    @Before
    public void setUp() {
        GenInterrupt.INS.reset(control);
    }

    @Test
    public void testStartClearsReadCommand() {
        GenInterrupt.INS.start(control);
        assertTrue(isReadCommandCleared());
    }

    @Test
    public void testWriteTwoBytesGeneratesInterrupt() {
        byte interruptData = (byte) 0xCF; // RST 1

        cpu.signalInterrupt(aryEq(new byte[]{interruptData}));
        expectLastCall();
        replay(cpu);

        GenInterrupt.INS.start(control);
        resetClearFlags();

        // First byte: interrupt vector
        GenInterrupt.INS.write((byte) 0, control);
        assertFalse(isCommandCleared());

        // Second byte: data to put on data bus
        GenInterrupt.INS.write(interruptData, control);
        assertTrue(isCommandCleared());

        verify(cpu);
    }

    @Test
    public void testResetResetsPosition() {
        GenInterrupt.INS.start(control);
        GenInterrupt.INS.write((byte) 1, control); // write first byte

        GenInterrupt.INS.reset(control);

        replay(cpu);

        GenInterrupt.INS.start(control);
        resetClearFlags();

        // Should be back at position 0, expecting vector byte
        GenInterrupt.INS.write((byte) 2, control);
        assertFalse(isCommandCleared()); // still expecting data byte
    }
}

