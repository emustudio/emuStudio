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
package net.sf.emustudio.zilogZ80.impl;

import emulib.emustudio.SettingsManager;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import emulib.runtime.NumberUtils;
import net.sf.emustudio.cpu.testsuite.Generator;
import net.sf.emustudio.cpu.testsuite.memory.ShortMemoryStub;
import net.sf.emustudio.intel8080.api.ExtendedContext;
import net.sf.emustudio.zilogZ80.impl.suite.CpuRunnerImpl;
import net.sf.emustudio.zilogZ80.impl.suite.CpuVerifierImpl;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;

public class InstructionsTest {
    private static final long PLUGIN_ID = 0L;

    static final int REG_PAIR_BC = 0;
    static final int REG_PAIR_DE = 1;
    static final int REG_PAIR_HL = 2;
    static final int REG_SP = 3;
    public static final int REG_PSW = 3;

    private CpuImpl cpu;
    CpuRunnerImpl cpuRunnerImpl;
    CpuVerifierImpl cpuVerifierImpl;
    private final List<FakeDevice> devices = new ArrayList<>();


    @Before
    public void setUp() throws Exception {
        ShortMemoryStub memoryStub = new ShortMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);

        Capture<ExtendedContext> cpuContext = Capture.newInstance();
        ContextPool contextPool = EasyMock.createNiceMock(ContextPool.class);
        expect(contextPool.getMemoryContext(0, MemoryContext.class))
            .andReturn(memoryStub)
            .anyTimes();
        contextPool.register(anyLong(), capture(cpuContext), same(ExtendedContext.class));
        expectLastCall().anyTimes();
        replay(contextPool);

        cpu = new CpuImpl(PLUGIN_ID, contextPool);

        assertTrue(cpuContext.hasCaptured());

        for (int i = 0; i < 256; i++) {
            FakeDevice device = new FakeDevice();
            devices.add(device);
            cpuContext.getValue().attachDevice(device, i);
        }

        SettingsManager settingsManager = createNiceMock(SettingsManager.class);
//        expect(settingsManager.readSetting(PLUGIN_ID, CpuImpl.PRINT_CODE)).andReturn("true").anyTimes();
//        expect(settingsManager.readSetting(PLUGIN_ID, CpuImpl.PRINT_CODE_USE_CACHE)).andReturn("false").anyTimes();
        replay(settingsManager);

        // simulate emuStudio boot
        cpu.initialize(settingsManager);

        cpuRunnerImpl = new CpuRunnerImpl(cpu, memoryStub, devices);
        cpuVerifierImpl = new CpuVerifierImpl(cpu, memoryStub, devices);

        Generator.setRandomTestsCount(10);
    }

    @After
    public void tearDown() {
        cpu.destroy();
    }

}
