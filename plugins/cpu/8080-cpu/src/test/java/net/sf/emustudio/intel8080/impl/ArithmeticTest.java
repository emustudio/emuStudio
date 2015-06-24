package net.sf.emustudio.intel8080.impl;

import emulib.emustudio.SettingsManager;
import emulib.plugins.PluginInitializationException;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextNotFoundException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import net.sf.emustudio.intel8080.impl.suite.CpuRunner;
import net.sf.emustudio.intel8080.impl.suite.CpuVerifier;
import net.sf.emustudio.intel8080.impl.suite.FlagsBuilder;
import net.sf.emustudio.intel8080.impl.suite.Generator;
import net.sf.emustudio.intel8080.impl.suite.TestBuilder;
import net.sf.emustudio.intel8080.impl.suite.runners.RunnerContext;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Function;

import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_A;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_B;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_C;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_D;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_E;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_H;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_L;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class ArithmeticTest {
    private static final long PLUGIN_ID = 0L;
    public static final int REG_PAIR_BC = 0;
    public static final int REG_PAIR_DE = 1;
    public static final int REG_PAIR_HL = 2;
    public static final int REG_SP = 3;

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

    private TestBuilder.BinaryByte additionTestBuilder() {
        FlagsBuilder flagsToCheck = new FlagsBuilder().sign().zero().carry().auxCarry().parity();
        return new TestBuilder.BinaryByte(cpuRunner, cpuVerifier)
                .checkFlags(flagsToCheck)
                .verifyR(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF));
    }

    private TestBuilder.BinaryByte subtractionTestBuilder() {
        FlagsBuilder flagsToCheck = new FlagsBuilder().sign().zero().carry().auxCarry().parity();
        return new TestBuilder.BinaryByte(cpuRunner, cpuVerifier)
                .checkFlags(flagsToCheck)
                .verifyR(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF));
    }

    @Test
    public void testADD() throws Exception {
        TestBuilder.BinaryByte test = additionTestBuilder();

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0x87, REG_A)
        );
        Generator.forSome8bitBinary(
                test.run(0x80, REG_B),
                test.run(0x81, REG_C),
                test.run(0x82, REG_D),
                test.run(0x83, REG_E),
                test.run(0x84, REG_H),
                test.run(0x85, REG_L),
                test.runM(0x86, 1)
        );
    }

    @Test
    public void testADI() throws Exception {
        TestBuilder.BinaryByte test = additionTestBuilder();

        Generator.forSome8bitBinary(
                test.runB(0xC6)
        );
    }

    @Test
    public void testADC() throws Exception {
        TestBuilder.BinaryByte test = additionTestBuilder()
                .verifyR(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF) + (context.flagsBefore & 1));

        Generator.forAll8bitBinaryWhichEqual(
                test.run(0x8F, REG_A)
        );
//        Generator.forSome8bitBinary(
//                test.run(0x88, REG_B),
//                test.run(0x89, REG_C),
//                test.run(0x8A, REG_D),
//                test.run(0x8B, REG_E),
//                test.run(0x8C, REG_H),
//                test.run(0x8D, REG_L),
//                test.runM(0x8E, 1)
//        );
    }

    @Test
    public void testACI() throws Exception {
        TestBuilder.BinaryByte test = additionTestBuilder()
                .verifyR(REG_A, context -> (context.first & 0xFF) + (context.second & 0xFF) + (context.flagsBefore & 1));

        Generator.forSome8bitBinary(
                test.runB(0xCE)
        );
    }

    @Test
    public void testSUB() throws Exception {
        TestBuilder.BinaryByte test = subtractionTestBuilder();

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0x97, REG_A)
        );
        Generator.forSome8bitBinary(
                test.run(0x90, REG_B),
                test.run(0x91, REG_C),
                test.run(0x92, REG_D),
                test.run(0x93, REG_E),
                test.run(0x94, REG_H),
                test.run(0x95, REG_L),
                test.runM(0x96, 1)
        );
    }

    @Test
    public void testSUI() throws Exception {
        TestBuilder.BinaryByte test = subtractionTestBuilder();

        Generator.forSome8bitBinary(
                test.runB(0xD6)
        );
    }

    @Test
    public void testSBB() throws Exception {
        TestBuilder.BinaryByte test = subtractionTestBuilder()
                .verifyR(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF) - (context.flagsBefore & 1));

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0x9F, REG_A)
        );
        Generator.forSome8bitBinary(
                test.run(0x98, REG_B),
                test.run(0x99, REG_C),
                test.run(0x9A, REG_D),
                test.run(0x9B, REG_E),
                test.run(0x9C, REG_H),
                test.run(0x9D, REG_L),
                test.runM(0x9E, 1)
        );
    }

    @Test
    public void testSBI() throws Exception {
        TestBuilder.BinaryByte test = subtractionTestBuilder()
            .verifyR(REG_A, context -> (context.first & 0xFF) - (context.second & 0xFF) - (context.flagsBefore & 1));

        Generator.forSome8bitBinary(
                test.runB(0xDE)
        );
    }

    @Test
    public void testINR() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .checkFlags(new FlagsBuilder().sign().zero().parity().auxCarry());
        Function<RunnerContext<Byte>, Integer> verifier = context -> context.first + 1;

        Generator.forSome8bitUnary(
                test.verifyR(REG_B, verifier).run(0x04, REG_B),
                test.verifyR(REG_C, verifier).run(0x0C, REG_C),
                test.verifyR(REG_D, verifier).run(0x14, REG_D),
                test.verifyR(REG_E, verifier).run(0x1C, REG_E),
                test.verifyR(REG_H, verifier).run(0x24, REG_H),
                test.verifyR(REG_L, verifier).run(0x2C, REG_L),
                test.verifyR(REG_A, verifier).run(0x3C, REG_A),
                test.verifyM(1, verifier).runM(0x34, 1)
        );
    }

    @Test
    public void testDCR() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .checkFlags(new FlagsBuilder().sign().zero().parity().auxCarry());
        Function<RunnerContext<Byte>, Integer> verifier = context -> context.first - 1;

        Generator.forSome8bitUnary(
                test.verifyR(REG_B, verifier).run(0x05, REG_B),
                test.verifyR(REG_C, verifier).run(0x0D, REG_C),
                test.verifyR(REG_D, verifier).run(0x15, REG_D),
                test.verifyR(REG_E, verifier).run(0x1D, REG_E),
                test.verifyR(REG_H, verifier).run(0x25, REG_H),
                test.verifyR(REG_L, verifier).run(0x2D, REG_L),
                test.verifyR(REG_A, verifier).run(0x3D, REG_A),
                test.verifyM(1, verifier).runM(0x35, 1)
        );
    }

    @Test
    public void testINX() throws Exception {
        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier);
        Function<RunnerContext<Integer>, Integer> verifier = context -> context.first + 1;

        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_BC, verifier).runPair(0x03, REG_PAIR_BC),
                test.verifyPair(REG_PAIR_DE, verifier).runPair(0x13, REG_PAIR_DE),
                test.verifyPair(REG_PAIR_HL, verifier).runPair(0x23, REG_PAIR_HL),
                test.verifyPair(REG_SP, verifier).runPair(0x33, REG_SP)
        );
    }

    @Test
    public void testDCX() throws Exception {
        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier);
        Function<RunnerContext<Integer>, Integer> verifier = context -> context.first - 1;

        Generator.forSome16bitUnary(
                test.verifyPair(REG_PAIR_BC, verifier).runPair(0x0B, REG_PAIR_BC),
                test.verifyPair(REG_PAIR_DE, verifier).runPair(0x1B, REG_PAIR_DE),
                test.verifyPair(REG_PAIR_HL, verifier).runPair(0x2B, REG_PAIR_HL),
                test.verifyPair(REG_SP, verifier).runPair(0x3B, REG_SP)
        );
    }

    @Test
    public void testDAD() throws Exception {
        TestBuilder.BinaryInteger test = new TestBuilder.BinaryInteger(cpuRunner, cpuVerifier)
                .checkFlags(new FlagsBuilder().carry15())
                .verifyPair(REG_PAIR_HL, context -> context.first + context.second);

        Generator.forSome16bitBinaryWhichEqual(
                test.runHLWithPair(0x29, REG_PAIR_HL)
        );

        Generator.forSome16bitBinary(
                test.runHLWithPair(0x09, REG_PAIR_BC),
                test.runHLWithPair(0x19, REG_PAIR_DE),
                test.runHLWithPair(0x39, REG_SP)
        );
    }


}
