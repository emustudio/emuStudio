/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package net.sf.emustudio.intel8080.impl;

import emulib.emustudio.SettingsManager;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import emulib.runtime.NumberUtils;
import emulib.runtime.exceptions.PluginInitializationException;
import net.sf.emustudio.cpu.testsuite.Generator;
import net.sf.emustudio.cpu.testsuite.memory.ShortMemoryStub;
import net.sf.emustudio.intel8080.impl.suite.CpuRunnerImpl;
import net.sf.emustudio.intel8080.impl.suite.CpuVerifierImpl;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;

import static org.easymock.EasyMock.*;

public class InstructionsTest {
    private static final long PLUGIN_ID = 0L;

    public static final int REG_PAIR_BC = 0;
    static final int REG_PAIR_DE = 1;
    public static final int REG_PAIR_HL = 2;
    public static final int REG_SP = 3;
    static final int REG_PSW = 3;

    private CpuImpl cpu;
    protected CpuRunnerImpl cpuRunnerImpl;
    protected CpuVerifierImpl cpuVerifierImpl;

    @Before
    public void setUp() throws PluginInitializationException {
        ShortMemoryStub memoryStub = new ShortMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);

        ContextPool contextPool = EasyMock.createNiceMock(ContextPool.class);
        expect(contextPool.getMemoryContext(0, MemoryContext.class))
            .andReturn(memoryStub)
            .anyTimes();
        replay(contextPool);

        cpu = new CpuImpl(PLUGIN_ID, contextPool);

        SettingsManager settingsManager = createNiceMock(SettingsManager.class);
        // CHANGE TO "true" FOR VERBOSE OUTPUT
        expect(settingsManager.readSetting(PLUGIN_ID, CpuImpl.PRINT_CODE)).andReturn("false").anyTimes();
        expect(settingsManager.readSetting(PLUGIN_ID, CpuImpl.PRINT_CODE_USE_CACHE)).andReturn("false").anyTimes();
        replay(settingsManager);

        // simulate emuStudio boot
        cpu.initialize(settingsManager);

        cpuRunnerImpl = new CpuRunnerImpl(cpu, memoryStub);
        cpuVerifierImpl = new CpuVerifierImpl(cpu, memoryStub);

        Generator.setRandomTestsCount(10);
    }

    @After
    public void tearDown() {
        cpu.destroy();
    }

}
