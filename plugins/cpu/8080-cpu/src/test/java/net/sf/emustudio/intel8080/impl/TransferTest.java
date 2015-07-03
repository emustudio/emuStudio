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
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

public class TransferTest {
    private static final long PLUGIN_ID = 0L;
    public static final int REGISTER_SP = 3;
    public static final int REGISTER_PAIR_HL = 2;
    public static final int REGISTER_PAIR_BC = 0;
    public static final int REGISTER_PAIR_DE = 1;

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
    public void testMVI() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier);

        Generator.forSome8bitUnary(
                test.verifyRegister(REG_A, context -> context.first & 0xFF).runB(0x3E)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_B).runB(0x06)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_C).runB(0x0E)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_D).runB(0x16)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_E).runB(0x1E)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_H).runB(0x26)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyRegister(REG_L).runB(0x2E)
        );

        test.clearVerifiers();
        Generator.forSome8bitUnary(
                test.verifyByte(0x20, context -> context.first & 0xFF).runHL(0x36, 0x20)
        );
    }

    @Test
    public void testMOV_A() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> context.first & 0xFF);

        Generator.forSome8bitUnary(
                test.run(0x7F, REG_A),
                test.run(0x78, REG_B),
                test.run(0x79, REG_C),
                test.run(0x7A, REG_D),
                test.run(0x7B, REG_E),
                test.run(0x7C, REG_H),
                test.run(0x7D, REG_L),
                test.runM(0x7E, 0x20)
        );
    }

    @Test
    public void testMOV_B() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_B, context -> context.first & 0xFF);

        Generator.forSome8bitUnary(
                test.run(0x47, REG_A),
                test.run(0x40, REG_B),
                test.run(0x41, REG_C),
                test.run(0x42, REG_D),
                test.run(0x43, REG_E),
                test.run(0x44, REG_H),
                test.run(0x45, REG_L),
                test.runM(0x46, 0x20)
        );
    }

    @Test
    public void testMOV_C() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_C, context -> context.first & 0xFF);

        Generator.forSome8bitUnary(
                test.run(0x4F, REG_A),
                test.run(0x48, REG_B),
                test.run(0x49, REG_C),
                test.run(0x4A, REG_D),
                test.run(0x4B, REG_E),
                test.run(0x4C, REG_H),
                test.run(0x4D, REG_L),
                test.runM(0x4E, 0x20)
        );
    }

    @Test
    public void testMOV_D() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_D, context -> context.first & 0xFF);

        Generator.forSome8bitUnary(
                test.run(0x57, REG_A),
                test.run(0x50, REG_B),
                test.run(0x51, REG_C),
                test.run(0x52, REG_D),
                test.run(0x53, REG_E),
                test.run(0x54, REG_H),
                test.run(0x55, REG_L),
                test.runM(0x56, 0x20)
        );
    }

    @Test
    public void testMOV_E() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_E, context -> context.first & 0xFF);

        Generator.forSome8bitUnary(
                test.run(0x5F, REG_A),
                test.run(0x58, REG_B),
                test.run(0x59, REG_C),
                test.run(0x5A, REG_D),
                test.run(0x5B, REG_E),
                test.run(0x5C, REG_H),
                test.run(0x5D, REG_L),
                test.runM(0x5E, 0x20)
        );
    }

    @Test
    public void testMOV_H() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_H, context -> context.first & 0xFF);

        Generator.forSome8bitUnary(
                test.run(0x67, REG_A),
                test.run(0x60, REG_B),
                test.run(0x61, REG_C),
                test.run(0x62, REG_D),
                test.run(0x63, REG_E),
                test.run(0x64, REG_H),
                test.run(0x65, REG_L),
                test.runM(0x66, 0x20)
        );
    }

    @Test
    public void testMOV_L() throws Exception {
        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyRegister(REG_L, context -> context.first & 0xFF);

        Generator.forSome8bitUnary(
                test.run(0x6F, REG_A),
                test.run(0x68, REG_B),
                test.run(0x69, REG_C),
                test.run(0x6A, REG_D),
                test.run(0x6B, REG_E),
                test.run(0x6C, REG_H),
                test.run(0x6D, REG_L),
                test.runM(0x6E, 0x20)
        );
    }

    @Test
    public void testMOV_M_r() throws Exception {
        final int address = 0x35;

        TestBuilder.UnaryByte test = new TestBuilder.UnaryByte(cpuRunner, cpuVerifier)
                .verifyByte(address, context -> context.first & 0xFF);

        Generator.forAll8bitUnary(
                test.runHL(0x77, address, REG_A),
                test.runHL(0x70, address, REG_B),
                test.runHL(0x71, address, REG_C),
                test.runHL(0x72, address, REG_D),
                test.runHL(0x73, address, REG_E)
        );
        test.runHL(0x74, address, REG_H).accept((byte)0);
        test.runHL(0x75, address, REG_L).accept((byte)address);
    }

    @Test
    public void testLDAX() throws Exception {
        final int value = 0x25;

        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyRegister(REG_A, context -> value);

        Generator.forSome16bitUnary(
                test.runPair(0x0A, REGISTER_PAIR_BC, value),
                test.runPair(0x1A, REGISTER_PAIR_DE, value)
        );
    }

    @Test
    public void testSTAX() throws Exception {
        final int value = 0x25;

        TestBuilder.UnaryInteger test = new TestBuilder.UnaryInteger(cpuRunner, cpuVerifier)
                .verifyByte(context -> context.first, context -> value);

        Generator.forSome16bitUnary(
                test.runPair(0x02, REGISTER_PAIR_BC, REG_A, value),
                test.runPair(0x12, REGISTER_PAIR_DE, REG_A, value)
        );
    }


}
