package net.sf.emustudio.intel8080.impl;

import emulib.emustudio.SettingsManager;
import emulib.plugins.PluginInitializationException;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextNotFoundException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import net.sf.emustudio.intel8080.impl.suite.CpuRunner;
import net.sf.emustudio.intel8080.impl.suite.CpuVerifier;
import net.sf.emustudio.intel8080.impl.suite.Generator;
import net.sf.emustudio.intel8080.impl.suite.TestBuilder;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Function;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;


public class StackTest {
    private static final long PLUGIN_ID = 0L;
    public static final int REG_PAIR_BC = 0;
    public static final int REG_PAIR_DE = 1;
    public static final int REG_PAIR_HL = 2;
    public static final int REG_PSW = 3;

    private CpuImpl cpu;
    private CpuRunner cpuRunner;
    private CpuVerifier cpuVerifier;

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

        cpuRunner = new CpuRunner(cpu, memoryStub);
        cpuVerifier = new CpuVerifier(cpu, memoryStub);
    }

    @After
    public void tearDown() {
        cpu.destroy();
    }

    @Test
    public void testPUSH() throws Exception {
        TestBuilder.BinaryInteger test = new TestBuilder.BinaryInteger(cpuRunner, cpuVerifier)
                .verifyWord(context -> context.second, context -> context.first - 2);

        Generator.forSome16bitBinary(2,
                test.runSPWithPairAndPSW(0xC5, REG_PAIR_BC),
                test.runSPWithPairAndPSW(0xD5, REG_PAIR_DE),
                test.runSPWithPairAndPSW(0xE5, REG_PAIR_HL)
        );

        test.verifyWord(context -> context.second & 0xFFD7 | 2, context -> context.first - 2);
        Generator.forSome16bitBinary(2,
                test.runSPWithPairAndPSW(0xF5, REG_PSW)
        );
    }

    @Test
    public void testPOP() throws Exception {
        TestBuilder.BinaryInteger test = new TestBuilder.BinaryInteger(cpuRunner, cpuVerifier);
        Function<RunnerContext<Integer>, Integer> verifier = context -> context.second;

        test.clearVerifiers();
        Generator.forSome16bitBinary(2,
                test.verifyPandPSW(verifier, REG_PAIR_BC).runM(0xC1)
        );
        test.clearVerifiers();
        Generator.forSome16bitBinary(2,
                test.verifyPandPSW(verifier, REG_PAIR_DE).runM(0xD1)
        );
        test.clearVerifiers();
        Generator.forSome16bitBinary(2,
                test.verifyPandPSW(verifier, REG_PAIR_HL).runM(0xE1)
        );
        test.clearVerifiers();
        Generator.forSome16bitBinary(2,
                test.verifyPandPSW(context -> context.second & 0xFFD7 | 2, REG_PSW).runM(0xF1)
        );
    }


}
