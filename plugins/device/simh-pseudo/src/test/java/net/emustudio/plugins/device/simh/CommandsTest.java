/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh;

import org.junit.Test;

import static org.junit.Assert.*;

public class CommandsTest {

    @Test
    public void testFromIntReturnsCorrectCommand() {
        assertEquals(Commands.printTimeCmd, Commands.fromInt(0));
        assertEquals(Commands.startTimerCmd, Commands.fromInt(1));
        assertEquals(Commands.stopTimerCmd, Commands.fromInt(2));
        assertEquals(Commands.getSIMHVersionCmd, Commands.fromInt(6));
        assertEquals(Commands.genInterruptCmd, Commands.fromInt(33));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromIntThrowsForInvalidValue() {
        Commands.fromInt(999);
    }

    @Test
    public void testAllCommandsHaveMappings() {
        for (Commands cmd : Commands.values()) {
            if (cmd != Commands.unknownCmd) {
                assertTrue(
                        "Command " + cmd.name() + " (ordinal=" + cmd.ordinal() + ") should have a mapping",
                        Commands.COMMANDS_MAP.containsKey(cmd.ordinal())
                );
            }
        }
    }

    @Test
    public void testUnknownCommandDoesNotHaveMapping() {
        assertFalse(Commands.COMMANDS_MAP.containsKey(Commands.unknownCmd.ordinal()));
    }

    @Test
    public void testCommandOrdinalsAreSequential() {
        Commands[] values = Commands.values();
        for (int i = 0; i < values.length; i++) {
            assertEquals(i, values[i].ordinal());
        }
    }
}

