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
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_AC;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_C;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_P;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_S;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_Z;
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

public class LogicTest {
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

    @Test
    public void testANA() throws Exception {
        TestBuilder.BinaryByte test = new TestBuilder.BinaryByte(cpuRunner, cpuVerifier)
                .checkFlags(new FlagsBuilder().sign().zero().parity().carryIsReset())
                .verifyR(REG_A, context -> context.first & context.second);

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0xA7, REG_A)
        );
        Generator.forSome8bitBinary(
                test.run(0xA0, REG_B),
                test.run(0xA1, REG_C),
                test.run(0xA2, REG_D),
                test.run(0xA3, REG_E),
                test.run(0xA4, REG_H),
                test.run(0xA5, REG_L),
                test.runM(0xA6, 1)
        );
    }

    @Test
    public void testANI() throws Exception {
        TestBuilder.BinaryByte test = new TestBuilder.BinaryByte(cpuRunner, cpuVerifier)
                .checkFlags(new FlagsBuilder().sign().zero().parity().carryIsReset())
                .verifyR(REG_A, context -> context.first & context.second);

        Generator.forSome8bitBinary(
                test.runB(0xE6)
        );
    }

    @Test
    public void testXRA() throws Exception {
        TestBuilder.BinaryByte test = new TestBuilder.BinaryByte(cpuRunner, cpuVerifier)
                .checkFlags(new FlagsBuilder().sign().zero().parity().carryIsReset().auxCarry())
                .verifyR(REG_A, context -> context.first ^ context.second);

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0xAF, REG_A)
        );
        Generator.forSome8bitBinary(
                test.run(0xA8, REG_B),
                test.run(0xA9, REG_C),
                test.run(0xAA, REG_D),
                test.run(0xAB, REG_E),
                test.run(0xAC, REG_H),
                test.run(0xAD, REG_L),
                test.runM(0xAE, 1)
        );
    }

    @Test
    public void testXRI() throws Exception {
        TestBuilder.BinaryByte test = new TestBuilder.BinaryByte(cpuRunner, cpuVerifier)
                .checkFlags(new FlagsBuilder().sign().zero().parity().carryIsReset())
                .verifyR(REG_A, context -> context.first ^ context.second);

        Generator.forSome8bitBinary(
                test.runB(0xEE)
        );
    }

    @Test
    public void testORA() throws Exception {
        TestBuilder.BinaryByte test = new TestBuilder.BinaryByte(cpuRunner, cpuVerifier)
                .checkFlags(new FlagsBuilder().sign().zero().parity().carryIsReset())
                .verifyR(REG_A, context -> context.first | context.second);

        Generator.forSome8bitBinaryWhichEqual(
                test.run(0xB7, REG_A)
        );
        Generator.forSome8bitBinary(
                test.run(0xB0, REG_B),
                test.run(0xB1, REG_C),
                test.run(0xB2, REG_D),
                test.run(0xB3, REG_E),
                test.run(0xB4, REG_H),
                test.run(0xB5, REG_L),
                test.runM(0xB6, 1)
        );
    }

    @Test
    public void testORI() throws Exception {
        TestBuilder.BinaryByte test = new TestBuilder.BinaryByte(cpuRunner, cpuVerifier)
                .checkFlags(new FlagsBuilder().sign().zero().parity().carryIsReset())
                .verifyR(REG_A, context -> context.first | context.second);

        Generator.forSome8bitBinary(
                test.runB(0xF6)
        );
    }

    @Test
    public void testDAA() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .checkFlags(new FlagsBuilder().sign().zero().parity().carry().auxCarryDAA())
                .verifyR(REG_A, context -> {
                    int result = ((int)context.first)& 0xFF;
                    if (((context.flagsBefore & FLAG_AC) == FLAG_AC) || (result & 0x0F) > 9) {
                        result += 6;
                    }
                    if ((context.flagsBefore & FLAG_C) == FLAG_C || ((result >>> 4) & 0x0F) > 9) {
                        result += 0x60;
                    }
                    return result;
                });

        Generator.forSome8bitUnary(
                test.run(0x27, REG_A)
        );
    }

    @Test
    public void testCMA() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyR(REG_A, context -> (~context.first) & 0xFF);

        Generator.forSome8bitUnary(
                test.run(0x2F, REG_A)
        );
    }

    @Test
    public void testSTC() throws Exception {
        cpuRunner.resetProgram(0x37);
        cpuRunner.step();

        cpuVerifier.checkFlags(FLAG_C);
        cpuVerifier.checkNotFlags(FLAG_S | FLAG_Z | FLAG_AC | FLAG_P);
    }

    @Test
    public void testCMC() throws Exception {
        cpuRunner.resetProgram(0x3F);
        cpuRunner.setFlags(FLAG_C);
        cpuRunner.step();

        cpuVerifier.checkNotFlags(FLAG_C | FLAG_S | FLAG_Z | FLAG_AC | FLAG_P);
    }


    @Test
    public void testRLC() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .checkFlags(new FlagsBuilder().carryIsFirstOperandMSB())
                .verifyR(REG_A, context -> (context.first << 1) | ((context.first >>> 7) & 1));

        Generator.forSome8bitUnary(
                test.run(0x07, REG_A)
        );
    }

    @Test
    public void testRRC() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .checkFlags(new FlagsBuilder().carryIsFirstOperandLSB())
                .verifyR(REG_A, context -> (((context.first &0xFF) >>> 1) | ((context.first & 1) << 7)) & 0xFF);

        Generator.forSome8bitUnary(
                test.run(0x0F, REG_A)
        );
    }

    @Test
    public void testRAL() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .checkFlags(new FlagsBuilder().carryIsFirstOperandMSB())
                .verifyR(REG_A, context -> (context.first << 1) | (context.flagsBefore & 1));

        Generator.forSome8bitUnary(
                test.run(0x17, REG_A)
        );
    }

    @Test
    public void testRAR() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .checkFlags(new FlagsBuilder().carryIsFirstOperandLSB())
                .verifyR(REG_A, context -> (((context.first &0xFF) >>> 1) | ((context.flagsBefore & 1) << 7)) & 0xFF);

        Generator.forSome8bitUnary(
                test.run(0x1F, REG_A)
        );
    }

    @Test
    public void testCMP() throws Exception {
        FlagsBuilder flagsToCheck = new FlagsBuilder().sign().zero().carry().auxCarry().parity();
        TestBuilder.BinaryByte test = new TestBuilder.BinaryByte(cpuRunner, cpuVerifier)
                .checkFlags(flagsToCheck)
                .verifyR(REG_A, context -> ((Number)context.first).intValue(), (context) -> context.first - context.second);

        // 0000 0001
        // 1111 1111
        //    0 0000

        Generator.forAll8bitBinaryWhichEqual(
                test.run(0xBF, REG_A)
        );
//        Generator.forSome8bitBinary(
//                test.run(0xB8, REG_B, result),
//                test.run(0xB9, REG_C, result),
//                test.run(0xBA, REG_D, result),
//                test.run(0xBB, REG_E, result),
//                test.run(0xBC, REG_H, result),
//                test.run(0xBD, REG_L, result),
//                test.runM(0xBE, 1, result)
//        );
    }


}
