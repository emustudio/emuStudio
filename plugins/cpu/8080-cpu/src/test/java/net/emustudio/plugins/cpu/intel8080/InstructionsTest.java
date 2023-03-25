/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.cpu.intel8080;

import net.emustudio.cpu.testsuite.Generator;
import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.suite.CpuRunnerImpl;
import net.emustudio.plugins.cpu.intel8080.suite.CpuVerifierImpl;
import org.junit.After;
import org.junit.Before;

import static org.easymock.EasyMock.*;

public class InstructionsTest {
    public static final int REG_PAIR_BC = 0;
    public static final int REG_PAIR_HL = 2;
    public static final int REG_SP = 3;
    static final int REG_PAIR_DE = 1;
    static final int REG_PSW = 3;
    private static final long PLUGIN_ID = 0L;
    protected CpuRunnerImpl cpuRunnerImpl;
    protected CpuVerifierImpl cpuVerifierImpl;
    private CpuImpl cpu;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws PluginInitializationException {
        ByteMemoryStub memoryStub = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);

        ContextPool contextPool = createNiceMock(ContextPool.class);
        expect(contextPool.getMemoryContext(0, MemoryContext.class))
                .andReturn(memoryStub)
                .anyTimes();
        replay(contextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        cpu = new CpuImpl(PLUGIN_ID, applicationApi, PluginSettings.UNAVAILABLE);
        cpu.initialize();

        cpuRunnerImpl = new CpuRunnerImpl(cpu, memoryStub);
        cpuVerifierImpl = new CpuVerifierImpl(cpu, memoryStub);

        Generator.setRandomTestsCount(10);
    }

    @After
    public void tearDown() {
        cpu.destroy();
    }
}
