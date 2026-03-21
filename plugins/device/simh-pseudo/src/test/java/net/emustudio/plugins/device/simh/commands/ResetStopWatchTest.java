/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ResetStopWatchTest extends CommandTestBase {

    @Before
    public void setUp() {
        ReadStopWatch.INS.reset(control);
    }

    @Test
    public void testStartSetsStopWatchNow() {
        long before = System.currentTimeMillis();
        ResetStopWatch.INS.start(control);
        long after = System.currentTimeMillis();

        assertTrue(ReadStopWatch.INS.stopWatchNow >= before);
        assertTrue(ReadStopWatch.INS.stopWatchNow <= after);
        assertTrue(isCommandCleared());
    }
}

