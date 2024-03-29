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
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.cpu.testsuite.Generator;
import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.zilogZ80.suite.CpuRunnerImpl;
import net.emustudio.plugins.cpu.zilogZ80.suite.CpuVerifierImpl;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;

public class InstructionsTest {
    static final int REG_PAIR_BC = 0;
    static final int REG_PAIR_DE = 1;
    static final int REG_PAIR_HL = 2;
    static final int REG_SP = 3;
    private static final long PLUGIN_ID = 0L;
    private final List<FakeByteDevice> devices = new ArrayList<>();
    CpuRunnerImpl cpuRunnerImpl;
    CpuVerifierImpl cpuVerifierImpl;
    protected CpuImpl cpu;
    protected ByteMemoryStub memory;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        Capture<Context8080> cpuContext = Capture.newInstance();
        ContextPool contextPool = EasyMock.createNiceMock(ContextPool.class);
        expect(contextPool.getMemoryContext(0, MemoryContext.class)).andReturn(memory).anyTimes();
        contextPool.register(anyLong(), capture(cpuContext), same(Context8080.class));
        expectLastCall().anyTimes();
        replay(contextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        cpu = new CpuImpl(PLUGIN_ID, applicationApi, PluginSettings.UNAVAILABLE);

        assertTrue(cpuContext.hasCaptured());

        for (int i = 0; i < 256; i++) {
            FakeByteDevice device = new FakeByteDevice();
            devices.add(device);
            cpuContext.getValue().attachDevice(i, device);
        }

        cpu.initialize();

        cpuRunnerImpl = new CpuRunnerImpl(cpu, memory, devices);
        cpuVerifierImpl = new CpuVerifierImpl(cpu, memory, devices);

        Generator.setRandomTestsCount(10);
    }

    @After
    public void tearDown() {
        cpu.destroy();
    }
}
