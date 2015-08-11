package net.sf.emustudio.zilogZ80.impl;

import emulib.emustudio.SettingsManager;
import emulib.plugins.PluginInitializationException;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextNotFoundException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import net.sf.emustudio.cpu.testsuite.MemoryStub;
import net.sf.emustudio.cpu.testsuite.RunStateListenerStub;
import net.sf.emustudio.zilogZ80.impl.suite.CpuRunnerImpl;
import net.sf.emustudio.zilogZ80.impl.suite.CpuVerifierImpl;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class InstructionsTTest {
    private static final long PLUGIN_ID = 0L;

    public static final int REG_PAIR_BC = 0;
    public static final int REG_PAIR_DE = 1;
    public static final int REG_PAIR_HL = 2;
    public static final int REG_SP = 3;
    public static final int REG_PSW = 3;

    private CpuImpl cpu;
    protected CpuRunnerImpl cpuRunnerImpl;
    protected CpuVerifierImpl cpuVerifierImpl;

    @Before
    public void setUp() throws ContextNotFoundException, InvalidContextException, PluginInitializationException {
        MemoryStub memoryStub = new MemoryStub();

        ContextPool contextPool = EasyMock.createNiceMock(ContextPool.class);
        expect(contextPool.getMemoryContext(0, MemoryContext.class))
                .andReturn(memoryStub)
                .anyTimes();
        replay(contextPool);

        RunStateListenerStub runStateListener = new RunStateListenerStub();
        cpu = new CpuImpl(PLUGIN_ID, contextPool);
        cpu.addCPUListener(runStateListener);

        SettingsManager settingsManager = createNiceMock(SettingsManager.class);
        expect(settingsManager.readSetting(PLUGIN_ID, CpuImpl.PRINT_CODE)).andReturn("true").anyTimes();
        expect(settingsManager.readSetting(PLUGIN_ID, CpuImpl.PRINT_CODE_USE_CACHE)).andReturn("false").anyTimes();
        replay(settingsManager);

        // simulate emuStudio boot
        cpu.initialize(settingsManager);

        cpuRunnerImpl = new CpuRunnerImpl(cpu, memoryStub);
        cpuVerifierImpl = new CpuVerifierImpl(cpu, memoryStub);
    }

    @After
    public void tearDown() {
        cpu.destroy();
    }

}
