/*
 * Copyright (C) 2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.emustudio.zilogZ80.impl;

import emulib.emustudio.SettingsManager;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import net.sf.emustudio.cpu.testsuite.MemoryStub;
import net.sf.emustudio.cpu.testsuite.RunStateListenerStub;
import net.sf.emustudio.intel8080.ExtendedContext;
import net.sf.emustudio.zilogZ80.impl.suite.CpuRunnerImpl;
import net.sf.emustudio.zilogZ80.impl.suite.CpuVerifierImpl;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertTrue;

public class InstructionsTest {
    private static final long PLUGIN_ID = 0L;

    public static final int REG_PAIR_BC = 0;
    public static final int REG_PAIR_DE = 1;
    public static final int REG_PAIR_HL = 2;
    public static final int REG_SP = 3;
    public static final int REG_PSW = 3;

    private CpuImpl cpu;
    protected CpuRunnerImpl cpuRunnerImpl;
    protected CpuVerifierImpl cpuVerifierImpl;
    protected final List<FakeDevice> devices = new ArrayList<>();


    @Before
    public void setUp() throws Exception {
        MemoryStub memoryStub = new MemoryStub();

        Capture<ExtendedContext> cpuContext = new Capture<>();
        ContextPool contextPool = EasyMock.createNiceMock(ContextPool.class);
        expect(contextPool.getMemoryContext(0, MemoryContext.class))
                .andReturn(memoryStub)
                .anyTimes();
        contextPool.register(anyLong(), capture(cpuContext), same(ExtendedContext.class));
        expectLastCall().anyTimes();
        replay(contextPool);

        RunStateListenerStub runStateListener = new RunStateListenerStub();
        cpu = new CpuImpl(PLUGIN_ID, contextPool);
        cpu.addCPUListener(runStateListener);

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
    }

    @After
    public void tearDown() {
        cpu.destroy();
    }

}
