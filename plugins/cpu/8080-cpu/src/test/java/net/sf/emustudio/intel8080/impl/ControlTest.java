package net.sf.emustudio.intel8080.impl;

import emulib.emustudio.SettingsManager;
import emulib.plugins.PluginInitializationException;
import emulib.plugins.cpu.CPU;
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

import java.util.List;
import java.util.function.Consumer;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_C;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_P;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_S;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_Z;

public class ControlTest {
    private static final long PLUGIN_ID = 0L;
    public static final int REGISTER_SP = 3;
    public static final int REGISTER_PAIR_HL = 2;

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
    public void testEI_DI() throws Exception {
        cpuRunner.resetProgram(0xFB, 0xF3);

        cpu.step();
        assertTrue(cpu.getEngine().INTE);

        cpu.step();
        assertFalse(cpu.getEngine().INTE);
    }

    @Test
    public void testJMP() throws Exception {
        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyPC(context -> context.first);

        Generator.forSome16bitUnary(
                test.runB(0xC3, 0),
                test.runB(0xC2, 0),
                test.runB(0xCA, FLAG_Z),
                test.runB(0xD2, 0),
                test.runB(0xDA, FLAG_C),
                test.runB(0xE2, 0),
                test.runB(0xEA, FLAG_P),
                test.runB(0xF2, 0),
                test.runB(0xFA, FLAG_S)
        );

        test.clearVerifiers().verifyPC(context -> (context.PCbefore + 3) & 0xFFFF);
        Generator.forSome16bitUnary(
                test.runB(0xC2, FLAG_Z),
                test.runB(0xCA, 0),
                test.runB(0xD2, FLAG_C),
                test.runB(0xDA, 0),
                test.runB(0xE2, FLAG_P),
                test.runB(0xEA, 0),
                test.runB(0xF2, FLAG_S),
                test.runB(0xFA, 0)
        );
    }

    @Test
    public void testCALL() throws Exception {
        int SP = 20;

        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyPC(context -> context.first)
                .verifyWord(context -> context.PCbefore + 3, context -> SP - 2);

        Generator.forSome16bitUnary(
                test.runB(0xCD, 0, SP),
                test.runB(0xC4, 0, SP),
                test.runB(0xCC, FLAG_Z, SP),
                test.runB(0xD4, 0, SP),
                test.runB(0xDC, FLAG_C, SP),
                test.runB(0xE4, 0, SP),
                test.runB(0xEC, FLAG_P, SP),
                test.runB(0xF4, 0, SP),
                test.runB(0xFC, FLAG_S, SP)
        );

        test.clearVerifiers().verifyPC(context -> (context.PCbefore + 3) & 0xFFFF);
        Generator.forSome16bitUnary(
                test.runB(0xC4, FLAG_Z, SP),
                test.runB(0xCC, 0, SP),
                test.runB(0xD4, FLAG_C, SP),
                test.runB(0xDC, 0, SP),
                test.runB(0xE4, FLAG_P, SP),
                test.runB(0xEC, 0, SP),
                test.runB(0xF4, FLAG_S, SP),
                test.runB(0xFC, 0, SP)
        );
    }

    @Test
    public void testRET() throws Exception {
        TestBuilder.BinaryInteger test = new TestBuilder.BinaryInteger(cpuRunner, cpuVerifier)
                .verifyPair(REGISTER_SP, context -> context.first + 2)
                .verifyPC(context -> context.second);

        Generator.forSome16bitBinary(1,
                test.runM(0xC9),
                test.runM(0xC0),
                test.runM(0xC8, FLAG_Z),
                test.runM(0xD0),
                test.runM(0xD8, FLAG_C),
                test.runM(0xE0),
                test.runM(0xE8, FLAG_P),
                test.runM(0xF0),
                test.runM(0xF8, FLAG_S)
        );

        // negative tests
        test.clearVerifiers()
                .verifyPC(context -> 1)
                .verifyPair(REGISTER_SP, context -> context.first);

        Generator.forSome16bitBinary(1,
                test.runM(0xC0, FLAG_Z),
                test.runM(0xC8, 0),
                test.runM(0xD0, FLAG_C),
                test.runM(0xD8, 0),
                test.runM(0xE0, FLAG_P),
                test.runM(0xE8, 0),
                test.runM(0xF0, FLAG_S),
                test.runM(0xF8, 0)
        );

    }

    @Test
    public void testRST() throws Exception {
        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyPair(REGISTER_SP, context -> context.first - 2)
                .verifyWord(context -> 1, context -> context.SPbefore - 2);

        List<Consumer<RunnerContext<Integer>>> verifiers = test.getVerifiers();

        Generator.forSome16bitUnary(3,
                test.verifyPC(context -> 0).runPair(0xC7, REGISTER_SP)
        );

        test.clearVerifiers().verifyAll(verifiers);
        Generator.forSome16bitUnary(3,
                test.verifyPC(context -> 0x8).runPair(0xCF, REGISTER_SP)
        );

        test.clearVerifiers().verifyAll(verifiers);
        Generator.forSome16bitUnary(3,
                test.verifyPC(context -> 0x10).runPair(0xD7, REGISTER_SP)
        );

        test.clearVerifiers().verifyAll(verifiers);
        Generator.forSome16bitUnary(3,
                test.verifyPC(context -> 0x18).runPair(0xDF, REGISTER_SP)
        );

        test.clearVerifiers().verifyAll(verifiers);
        Generator.forSome16bitUnary(3,
                test.verifyPC(context -> 0x20).runPair(0xE7, REGISTER_SP)
        );

        test.clearVerifiers().verifyAll(verifiers);
        Generator.forSome16bitUnary(3,
                test.verifyPC(context -> 0x28).runPair(0xEF, REGISTER_SP)
        );

        test.clearVerifiers().verifyAll(verifiers);
        Generator.forSome16bitUnary(3,
                test.verifyPC(context -> 0x30).runPair(0xF7, REGISTER_SP)
        );

        test.clearVerifiers().verifyAll(verifiers);
        Generator.forSome16bitUnary(3,
                test.verifyPC(context -> 0x38).runPair(0xFF, REGISTER_SP)
        );
    }

    @Test
    public void testPCHL() throws Exception {
        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyPC(context -> context.first);

        Generator.forSome16bitUnary(
                test.runPair(0xE9, REGISTER_PAIR_HL)
        );
    }

    @Test
    public void testHLT() throws Exception {
        cpuRunner.resetProgram(0x76);
        cpuRunner.setExpectedRunState(CPU.RunState.STATE_STOPPED_NORMAL);
        cpuRunner.step();
    }


}
