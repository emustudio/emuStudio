/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080;

import net.emustudio.emulib.plugin.cpu.CPUContext;

import net.emustudio.cpu.testsuite.Generator;
import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugin.PluginInitializationException;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.ContextPool;
import net.emustudio.emulib.Numbers;
import net.emustudio.emulib.application.settings.PluginSettings;
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
        ByteMemoryStub memoryStub = new ByteMemoryStub(Numbers.Strategy.LITTLE_ENDIAN);

        ContextPool contextPool = createNiceMock(ContextPool.class);
        expect(contextPool.getMemory(0, Byte.class))
                .andReturn(memoryStub)
                .anyTimes();
        replay(contextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        CPUContext execution = createNiceMock(CPUContext.class);
        replay(execution);
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
