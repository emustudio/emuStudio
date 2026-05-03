/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.cpu.testsuite.Generator;
import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.zilogZ80.suite.ByteTestBuilder;
import net.emustudio.plugins.cpu.zilogZ80.suite.CpuRunnerImpl;
import net.emustudio.plugins.cpu.zilogZ80.suite.CpuVerifierImpl;
import net.emustudio.plugins.cpu.zilogZ80.suite.IntegerTestBuilder;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class ControlTest extends InstructionsTest {

    @Test
    public void testEI_DI() {
        cpuVerifierImpl.checkInterruptsAreDisabled(0);
        cpuRunnerImpl.setProgram(0xFB, 0xF3);
        cpuRunnerImpl.reset();

        cpuRunnerImpl.step();
        cpuVerifierImpl.checkInterruptsAreEnabled(0);

        cpuRunnerImpl.step();
        cpuVerifierImpl.checkInterruptsAreDisabled(0);
    }

    @Test
    public void testRepeatedEiExtendsInterruptSkipWindow() {
        cpuRunnerImpl.setProgram(0xFB, 0xFB, 0x00, 0x00);
        cpuRunnerImpl.reset();
        cpuRunnerImpl.setIntMode((byte) 1);
        cpu.getEngine().setInterruptDuration(32);
        setLevelInterrupt(cpu.getEngine(), new byte[]{0});

        cpuRunnerImpl.step(); // EI
        cpuVerifierImpl.checkPC(1);

        cpuRunnerImpl.step(); // EI
        cpuVerifierImpl.checkPC(2);

        cpuRunnerImpl.step(); // NOP, interrupt still must be skipped here
        cpuVerifierImpl.checkPC(3);

        cpuRunnerImpl.step(); // now interrupt may be accepted before executing opcode at 0x38
        cpuVerifierImpl.checkPC(0x39);
    }

    @Test
    public void testIm2InterruptWithFloatingBusFfResumesFromInstructionAfterHalt() {
        cpuRunnerImpl.setProgram(0x76, 0x00);
        cpuRunnerImpl.reset();
        cpuRunnerImpl.setIntMode((byte) 2);
        cpuRunnerImpl.setI(0x28);
        cpuRunnerImpl.enableIFF2();
        cpu.getEngine().IFF[0] = true;
        cpu.getEngine().setInterruptDuration(32);

        cpuRunnerImpl.setByte(0x28FF, 0x5C);
        cpuRunnerImpl.setByte(0x2900, 0x7E);
        cpuRunnerImpl.setByte(0x7E5C, 0xED);
        cpuRunnerImpl.setByte(0x7E5D, 0x4D);

        cpuRunnerImpl.step();
        cpuVerifierImpl.checkPC(0x0000);

        setLevelInterrupt(cpu.getEngine(), new byte[]{(byte) 0xFF});

        cpuRunnerImpl.step();
        cpuVerifierImpl.checkPC(0x0001);
        cpuVerifierImpl.checkRegisterPair(REG_SP, 0xFFFF);
    }

    @Test
    public void testIm2InterruptAcknowledgeIncrementsRefreshRegister() {
        cpuRunnerImpl.setProgram(0x76, 0x00);
        cpuRunnerImpl.reset();
        cpuRunnerImpl.setIntMode((byte) 2);
        cpuRunnerImpl.setI(0x28);
        cpuRunnerImpl.setR(0x7F);
        cpuRunnerImpl.enableIFF2();
        cpu.getEngine().IFF[0] = true;
        cpu.getEngine().setInterruptDuration(32);

        cpuRunnerImpl.setByte(0x28FF, 0x5C);
        cpuRunnerImpl.setByte(0x2900, 0x7E);
        cpuRunnerImpl.setByte(0x7E5C, 0xED);
        cpuRunnerImpl.setByte(0x7E5D, 0x5F);

        cpuRunnerImpl.step();
        cpuVerifierImpl.checkR(0x00);

        setLevelInterrupt(cpu.getEngine(), new byte[]{(byte) 0xFF});

        cpuRunnerImpl.step();
        cpuVerifierImpl.checkRegister(REG_A, 0x03);
        cpuVerifierImpl.checkR(0x03);
        cpuVerifierImpl.checkPC(0x7E5E);
    }

    @Test
    public void testIm0CallInterruptAcknowledgeIncrementsRefreshRegister() {
        cpuRunnerImpl.setProgram(0x00);
        cpuRunnerImpl.reset();
        cpuRunnerImpl.setIntMode((byte) 0);
        cpuRunnerImpl.setR(0x7F);
        cpuRunnerImpl.enableIFF2();
        cpu.getEngine().IFF[0] = true;
        cpu.getEngine().setInterruptDuration(32);
        cpuRunnerImpl.setByte(0x1234, 0x00);

        setLevelInterrupt(cpu.getEngine(), new byte[]{(byte) 0xCD, 0x34, 0x12});

        cpuRunnerImpl.step();
        cpuVerifierImpl.checkR(0x01);
        cpuVerifierImpl.checkPC(0x1235);
        cpuVerifierImpl.checkRegisterPair(REG_SP, 0xFFFD);
    }

    @Test
    public void testTakenJrAddsFiveHiddenCyclesAtDisplacementAddress() throws Exception {
        // Timing reference: JR e is 12 T-states and consumes the signed displacement byte right after the opcode.
        // https://www.z80.info/z80flag.htm
        // https://z80.info/decoding.htm
        try (CountingTestEnvironment env = newCountingTestEnvironment()) {
            ByteTestBuilder test = new ByteTestBuilder(env.cpuRunner, env.cpuVerifier)
                    .verifyPC(context -> (context.PC + context.first + 2) & 0xFFFF)
                    .verify(context -> assertReadAndPassiveCycles(env.memory, 0x0001, 1, 5));

            Generator.forSome8bitUnary(
                    test.runWithFirstOperand(0x18)
                            .injectNoOperand(runner -> env.memory.clearCounters())
            );
        }
    }

    @Test
    public void testIncBcAddsTwoHiddenRefreshCycles() throws Exception {
        // Timing reference: INC rr is 6 T-states, with the last 2 T-states spent in refresh/internal cycles.
        // https://www.z80.info/z80flag.htm
        try (CountingTestEnvironment env = newCountingTestEnvironment()) {
            ByteTestBuilder test = new ByteTestBuilder(env.cpuRunner, env.cpuVerifier)
                    .verify(context -> {
                        env.cpuVerifier.checkRegisterPair(REG_PAIR_BC, 0x0001);
                        assertReadAndPassiveCycles(env.memory, (unsigned(context.first) << 8) | 0x0001, 0, 2);
                    });

            Generator.forSome8bitUnary(
                    test.run(0x03)
                            .injectFirst((runner, value) -> {
                                runner.setI(unsigned(value));
                                runner.setR(0);
                            })
                            .injectNoOperand(runner -> env.memory.clearCounters())
            );
        }
    }

    @Test
    public void testIndexedLoadAddsFiveHiddenCyclesAtDisplacementAddress() throws Exception {
        // Timing reference: LD r,(IX+d) is a 19 T-state indexed load with an explicit displacement phase.
        // https://www.z80.info/z80flag.htm
        // https://z80.info/decoding.htm
        try (CountingTestEnvironment env = newCountingTestEnvironment()) {
            ByteTestBuilder test = new ByteTestBuilder(env.cpuRunner, env.cpuVerifier)
                    .setIX(0x4000)
                    .verifyRegister(REG_A, context -> unsigned(context.second))
                    .verify(context -> {
                        assertReadAndPassiveCycles(env.memory, 0x0002, 1, 5);
                        assertEquals(1, env.memory.getReadCount(indexedAddress(0x4000, context.first)));
                    });

            Generator.forSome8bitBinary(
                    test.runWithFirstOperand(0xDD, 0x7E)
                            .injectTwoOperands((runner, displacement, value) ->
                                    runner.setByte(indexedAddress(0x4000, displacement), unsigned(value)))
                            .injectNoOperand(runner -> env.memory.clearCounters())
            );
        }
    }

    @Test
    public void testIndexedHLoadAddsFiveHiddenCyclesAtDisplacementAddress() throws Exception {
        // Timing reference: LD H,(IX+d) follows the same 19 T-state indexed timing as other LD r,(IX+d) forms.
        // https://www.z80.info/z80flag.htm
        // https://z80.info/decoding.htm
        try (CountingTestEnvironment env = newCountingTestEnvironment()) {
            ByteTestBuilder test = new ByteTestBuilder(env.cpuRunner, env.cpuVerifier)
                    .setIX(0x4000)
                    .verifyRegister(REG_H, context -> unsigned(context.second))
                    .verify(context -> {
                        assertReadAndPassiveCycles(env.memory, 0x0002, 1, 5);
                        assertEquals(1, env.memory.getReadCount(indexedAddress(0x4000, context.first)));
                    });

            Generator.forSome8bitBinary(
                    test.runWithFirstOperand(0xDD, 0x66)
                            .injectTwoOperands((runner, displacement, value) ->
                                    runner.setByte(indexedAddress(0x4000, displacement), unsigned(value)))
                            .injectNoOperand(runner -> env.memory.clearCounters())
            );
        }
    }

    @Test
    public void testIndexedAddAddsFiveHiddenCyclesAtDisplacementAddress() throws Exception {
        // Timing reference: ADD A,(IX+d) is a 19 T-state indexed ALU read with a 5 T-state displacement phase.
        // https://www.z80.info/z80flag.htm
        // https://z80.info/decoding.htm
        try (CountingTestEnvironment env = newCountingTestEnvironment()) {
            ByteTestBuilder test = new ByteTestBuilder(env.cpuRunner, env.cpuVerifier)
                    .setIX(0x4000)
                    .setRegister(REG_A, 0x10)
                    .verifyRegister(REG_A, context -> (0x10 + unsigned(context.second)) & 0xFF)
                    .verify(context -> {
                        assertReadAndPassiveCycles(env.memory, 0x0002, 1, 5);
                        assertEquals(1, env.memory.getReadCount(indexedAddress(0x4000, context.first)));
                    });

            Generator.forSome8bitBinary(
                    test.runWithFirstOperand(0xDD, 0x86)
                            .injectTwoOperands((runner, displacement, value) ->
                                    runner.setByte(indexedAddress(0x4000, displacement), unsigned(value)))
                            .injectNoOperand(runner -> env.memory.clearCounters())
            );
        }
    }

    @Test
    public void testIndexedIncAddsHiddenCyclesAtDisplacementAndIndexedAddress() throws Exception {
        // Timing reference: INC (IX+d) is a 23 T-state indexed read-modify-write with both displacement and memory phases.
        // https://www.z80.info/z80flag.htm
        // https://z80.info/decoding.htm
        try (CountingTestEnvironment env = newCountingTestEnvironment()) {
            ByteTestBuilder test = new ByteTestBuilder(env.cpuRunner, env.cpuVerifier)
                    .setIX(0x4000)
                    .verify(context -> {
                        int indexedAddress = indexedAddress(0x4000, context.first);
                        assertReadAndPassiveCycles(env.memory, 0x0002, 1, 5);
                        assertReadAndPassiveCycles(env.memory, indexedAddress, 1, 1);
                    })
                    .verifyByte(context -> indexedAddress(0x4000, context.first),
                            context -> (unsigned(context.second) + 1) & 0xFF);

            Generator.forSome8bitBinary(
                    test.runWithFirstOperand(0xDD, 0x34)
                            .injectTwoOperands((runner, displacement, value) ->
                                    runner.setByte(indexedAddress(0x4000, displacement), unsigned(value)))
                            .injectNoOperand(runner -> env.memory.clearCounters())
            );
        }
    }

    @Test
    public void testIndexedImmediateStoreAddsTwoHiddenCyclesAtImmediateAddress() throws Exception {
        // Timing reference: LD (IX+d),n is a 19 T-state indexed store and pays the extra 2 T-states on the immediate byte.
        // https://www.z80.info/z80flag.htm
        // https://z80.info/decoding.htm
        try (CountingTestEnvironment env = newCountingTestEnvironment()) {
            ByteTestBuilder test = new ByteTestBuilder(env.cpuRunner, env.cpuVerifier)
                    .setIX(0x4000)
                    .verify(context -> assertReadAndPassiveCycles(env.memory, 0x0003, 1, 2))
                    .verifyByte(context -> indexedAddress(0x4000, context.first), context -> unsigned(context.second));

            Generator.forSome8bitBinary(
                    test.runWithFirstOperand(0xDD, 0x36)
                            .injectSecond((runner, value) -> runner.setByte(0x0003, unsigned(value)))
                            .injectNoOperand(runner -> env.memory.clearCounters())
            );
        }
    }

    @Test
    public void testRldAddsFourHiddenCyclesAtHlAddress() throws Exception {
        // Timing reference: RLD is an 18 T-state nibble-rotate instruction with extra cycles on the HL memory access.
        // https://www.z80.info/z80flag.htm
        try (CountingTestEnvironment env = newCountingTestEnvironment()) {
            ByteTestBuilder test = new ByteTestBuilder(env.cpuRunner, env.cpuVerifier)
                    .firstIsRegister(REG_A)
                    .setPair(REG_PAIR_HL, 0x4000)
                    .verify(context -> assertReadAndPassiveCycles(env.memory, 0x4000, 1, 4))
                    .verifyRegister(REG_A,
                            context -> (unsigned(context.first) & 0xF0) | ((unsigned(context.second) >>> 4) & 0x0F))
                    .verifyByte(0x4000,
                            context -> ((unsigned(context.second) << 4) & 0xF0) | (unsigned(context.first) & 0x0F));

            Generator.forSome8bitBinary(
                    test.run(0xED, 0x6F)
                            .injectSecond((runner, value) -> runner.setByte(0x4000, unsigned(value)))
                            .injectNoOperand(runner -> env.memory.clearCounters())
            );
        }
    }

    @Test
    public void testIndexedCbOpcodeFetchDoesNotIncrementRefreshRegister() throws Exception {
        // Timing/reference quirk: DDCB/FDCB instructions still increment R by 2, not 3, despite the extra opcode fetch.
        // https://z80.info/z80info.htm
        // Timing reference for BIT b,(IX+d): https://www.z80.info/z80flag.htm
        try (CountingTestEnvironment env = newCountingTestEnvironment()) {
            ByteTestBuilder test = new ByteTestBuilder(env.cpuRunner, env.cpuVerifier)
                    .setIX(0x4000)
                    .verifyRegisterR(context -> 0x02)
                    .verify(context -> {
                        int indexedAddress = indexedAddress(0x4000, context.first);
                        assertReadAndPassiveCycles(env.memory, 0x0003, 1, 2);
                        assertReadAndPassiveCycles(env.memory, indexedAddress, 1, 1);
                    });

            Generator.forSome8bitBinary(
                    test.runWithFirst8bitOperandWithOpcodeAfter(0x46, 0xDD, 0xCB)
                            .injectTwoOperands((runner, displacement, value) ->
                                    runner.setByte(indexedAddress(0x4000, displacement), unsigned(value)))
                            .injectNoOperand(runner -> env.memory.clearCounters())
            );
        }
    }

    @Test
    public void testIndexedResAddsHiddenCycleAtIndexedAddress() throws Exception {
        // Timing/reference: indexed CB RES uses the DDCB/FDCB format and the indexed memory phase is 1 passive T-state.
        // https://www.z80.info/z80undoc.htm
        // https://www.z80.info/z80flag.htm
        try (CountingTestEnvironment env = newCountingTestEnvironment()) {
            ByteTestBuilder test = new ByteTestBuilder(env.cpuRunner, env.cpuVerifier)
                    .setIX(0x4000)
                    .verify(context -> {
                        int indexedAddress = indexedAddress(0x4000, context.first);
                        assertReadAndPassiveCycles(env.memory, 0x0003, 1, 2);
                        assertReadAndPassiveCycles(env.memory, indexedAddress, 1, 1);
                    })
                    .verifyByte(context -> indexedAddress(0x4000, context.first),
                            context -> unsigned(context.second) & 0xFE);

            Generator.forSome8bitBinary(
                    test.runWithFirst8bitOperandWithOpcodeAfter(0x86, 0xDD, 0xCB)
                            .injectTwoOperands((runner, displacement, value) ->
                                    runner.setByte(indexedAddress(0x4000, displacement), unsigned(value)))
                            .injectNoOperand(runner -> env.memory.clearCounters())
            );
        }
    }

    @Test
    public void testJP__nn__AND__JP_cc__nn() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsMemoryAddressWord(0)
                .verifyPC(context -> context.first)
                .verifyPair(REG_SP, context -> 0xFFFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitUnary(3,
                test.runWithFirstOperand(0xC3),
                test.runWithFirstOperand(0xC2),
                test.setFlags(FLAG_Z).runWithFirstOperand(0xCA),
                test.runWithFirstOperand(0xD2),
                test.setFlags(FLAG_C).runWithFirstOperand(0xDA),
                test.runWithFirstOperand(0xE2),
                test.setFlags(FLAG_PV).runWithFirstOperand(0xEA),
                test.runWithFirstOperand(0xF2),
                test.setFlags(FLAG_S).runWithFirstOperand(0xFA)
        );
    }

    @Test
    public void testNegative_JP_cc__nn() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsMemoryAddressWord(0)
                .verifyPC(context -> context.PC + 3)
                .verifyPair(REG_SP, context -> 0xFFFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitUnary(3,
                test.setFlags(FLAG_Z).runWithFirstOperand(0xC2),
                test.runWithFirstOperand(0xCA),
                test.setFlags(FLAG_C).runWithFirstOperand(0xD2),
                test.runWithFirstOperand(0xDA),
                test.setFlags(FLAG_PV).runWithFirstOperand(0xE2),
                test.runWithFirstOperand(0xEA),
                test.setFlags(FLAG_S).runWithFirstOperand(0xF2),
                test.runWithFirstOperand(0xFA)
        );
    }

    @Test
    public void testJP__mHL() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsMemoryAddressWord(0)
                .firstIsPair(REG_PAIR_HL)
                .verifyPC(context -> context.first);

        Generator.forSome16bitUnary(1,
                test.run(0xE9)
        );
    }

    @Test
    public void testCALL__nn() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsMemoryAddressWord(0)
                .secondIsPair(REG_SP)
                .verifyPair(REG_SP, context -> (context.second - 2) & 0xFFFF)
                .verifyPC(context -> context.first)
                .verifyWord(context -> (context.second - 2) & 0xFFFF, context -> context.PC + 3)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(3, 5,
                test.runWithFirstOperand(0xCD)
        );
    }

    @Test
    public void testCALL_cc__nn() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsMemoryAddressWord(0)
                .secondIsPair(REG_SP)
                .verifyPair(REG_SP, context -> (context.second - 2) & 0xFFFF)
                .verifyPC(context -> context.first)
                .verifyWord(context -> (context.second - 2) & 0xFFFF, context -> context.PC + 3)
                .setFlags(FLAG_Z | FLAG_C | FLAG_PV | FLAG_S)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(3, 5,
                test.runWithFirstOperand(0xCC),
                test.runWithFirstOperand(0xDC),
                test.runWithFirstOperand(0xEC),
                test.runWithFirstOperand(0xFC)
        );
    }

    @Test
    public void testNegative_CALL_cc__nn() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsMemoryAddressWord(0)
                .secondIsPair(REG_SP)
                .verifyPair(REG_SP, context -> (context.second - 2) & 0xFFFF)
                .verifyPC(context -> context.first)
                .verifyWord(context -> (context.second - 2) & 0xFFFF, context -> context.PC + 3)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(3, 5,
                test.runWithFirstOperand(0xC4),
                test.runWithFirstOperand(0xD4),
                test.runWithFirstOperand(0xE4),
                test.runWithFirstOperand(0xF4)
        );
    }

    @Test
    public void testRET() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .firstIsPair(REG_SP)
                .verifyPC(context -> context.second)
                .verifyPair(REG_SP, context -> (context.first + 2) & 0xFFFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(1,
                test.run(0xC9)
        );
    }

    @Test
    public void testRET__cc() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .firstIsPair(REG_SP)
                .verifyPC(context -> context.second)
                .verifyPair(REG_SP, context -> (context.first + 2) & 0xFFFF)
                .setFlags(FLAG_Z | FLAG_C | FLAG_PV | FLAG_S)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(1,
                test.run(0xC8),
                test.run(0xD8),
                test.run(0xE8),
                test.run(0xF8)
        );
    }

    @Test
    public void testNegative_RET__cc() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .firstIsPair(REG_SP)
                .verifyPC(context -> context.second)
                .verifyPair(REG_SP, context -> (context.first + 2) & 0xFFFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(1,
                test.run(0xC0),
                test.run(0xD0),
                test.run(0xE0),
                test.run(0xF0)
        );
    }

    @Test
    public void testRST() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsMemoryAddressWord(0)
                .firstIsPair(REG_SP)
                .verifyPair(REG_SP, context -> (context.first - 2) & 0xFFFF)
                .verifyWord(context -> (context.first - 2) & 0xFFFF, context -> context.PC + 1)
                .keepCurrentInjectorsAfterRun()
                .clearOtherVerifiersAfterRun();

        Generator.forSome16bitUnary(3,
                test.verifyPC(context -> 0).run(0xC7),
                test.verifyPC(context -> 8).run(0xCF),
                test.verifyPC(context -> 0x10).run(0xD7),
                test.verifyPC(context -> 0x18).run(0xDF),
                test.verifyPC(context -> 0x20).run(0xE7),
                test.verifyPC(context -> 0x28).run(0xEF),
                test.verifyPC(context -> 0x30).run(0xF7),
                test.verifyPC(context -> 0x38).run(0xFF)
        );
    }

    @Test
    public void testHLT() {
        cpuRunnerImpl.setProgram(0x76);
        cpuRunnerImpl.reset();
        cpuRunnerImpl.expectRunState(CPU.RunState.STATE_STOPPED_NORMAL);
        cpuRunnerImpl.step();
    }

    @Test
    public void testInvalidInstruction() {
        cpuRunnerImpl.setProgram(0xED, 0x80);
        cpuRunnerImpl.reset();
        cpuRunnerImpl.expectRunState(CPU.RunState.STATE_STOPPED_BREAK); // Z80 ignores bad instructions
        cpuRunnerImpl.step();
    }

    @Test
    public void testLongDdFdPrefixBlockDoesNotOverflowStack() {
        final int prefixes = 12000;

        cpuRunnerImpl.ensureProgramSize(prefixes + 3);
        for (int i = 0; i < prefixes; i++) {
            cpuRunnerImpl.setByte(i, (i % 2 == 0) ? 0xDD : 0xFD);
        }
        cpuRunnerImpl.setByte(prefixes, 0x21);
        cpuRunnerImpl.setByte(prefixes + 1, 0x34);
        cpuRunnerImpl.setByte(prefixes + 2, 0x12);
        cpuRunnerImpl.reset();

        cpuRunnerImpl.step();

        cpuVerifierImpl.checkIX(0);
        cpuVerifierImpl.checkIY(0x1234);
        cpuVerifierImpl.checkPC(prefixes + 3);
    }

    @Test
    public void testNOP() {
        cpuRunnerImpl.setProgram(0x00);
        cpuRunnerImpl.reset();

        cpuRunnerImpl.step();

        cpuVerifierImpl.checkInterruptsAreDisabled(0);
        cpuVerifierImpl.checkIX(0);
        cpuVerifierImpl.checkIY(0);
        cpuVerifierImpl.checkNotFlags(FLAG_C | FLAG_H | FLAG_N | FLAG_PV | FLAG_S | FLAG_Z);
        cpuVerifierImpl.checkRegister(REG_A, 0);
        cpuVerifierImpl.checkRegister(REG_B, 0);
        cpuVerifierImpl.checkRegister(REG_C, 0);
        cpuVerifierImpl.checkRegister(REG_D, 0);
        cpuVerifierImpl.checkRegister(REG_E, 0);
        cpuVerifierImpl.checkRegister(REG_H, 0);
        cpuVerifierImpl.checkRegister(REG_L, 0);
        cpuVerifierImpl.checkRegisterPair(REG_PAIR_BC, 0);
        cpuVerifierImpl.checkRegisterPair(REG_PAIR_HL, 0);
        cpuVerifierImpl.checkRegisterPair(REG_SP, 0xFFFF);
        cpuVerifierImpl.checkPC(1);
    }

    private void checkIm(int opcode, int intModeSetting, int intModeCheck) {
        cpuRunnerImpl.setProgram(0xED, opcode);
        cpuRunnerImpl.reset();
        cpuRunnerImpl.setIntMode((byte) intModeSetting);

        cpuRunnerImpl.step();

        cpuVerifierImpl.checkIntMode(intModeCheck);
    }

    private static void setLevelInterrupt(EmulatorEngine engine, byte[] data) {
        try {
            Field levelInterrupt = EmulatorEngine.class.getDeclaredField("levelInterrupt");
            levelInterrupt.setAccessible(true);
            levelInterrupt.set(engine, data);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to prepare level interrupt for test", e);
        }
    }

    @Test
    public void testIM__0() {
        checkIm(0x46, 1, 0);
        checkIm(0x4E, 1, 0); // undocumented
        checkIm(0x66, 1, 0); // undocumented
        checkIm(0x6E, 1, 0); // undocumented
    }

    @Test
    public void testIM__1() {
        checkIm(0x56, 0, 1);
        checkIm(0x76, 0, 1); // undocumented
    }

    @Test
    public void testIM__2() {
        checkIm(0x5E, 0, 2);
        checkIm(0x7E, 0, 2); // undocumented
    }

    @Test
    public void testJR__e__AND__JR__cc() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .expandMemory(first -> cpuRunnerImpl.getPC() + first.intValue() & 0xFF)
                .verifyPC(context -> (context.PC + context.first + 2) & 0xFFFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.runWithFirstOperand(0x18), // jr *
                test.runWithFirstOperand(0x20), // jr nz, *
                test.setFlags(FLAG_Z).runWithFirstOperand(0x28),  // jr z, *
                test.runWithFirstOperand(0x30), // jr nc, *
                test.setFlags(FLAG_C).runWithFirstOperand(0x38) // jr c, *
        );
    }

    @Test
    public void testNegative__JR__e__AND__JR__cc() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .expandMemory(first -> cpuRunnerImpl.getPC() + first.intValue())
                .verifyPC(context -> (context.PC + 2) & 0xFFFF)
                .keepCurrentInjectorsAfterRun();

        Generator.forSome8bitUnary(
                test.setFlags(FLAG_Z).runWithFirstOperand(0x20),
                test.runWithFirstOperand(0x28),
                test.setFlags(FLAG_C).runWithFirstOperand(0x30),
                test.runWithFirstOperand(0x38)
        );
    }

    @Test
    public void testJP__IX() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsIX()
                .verifyPC(context -> context.first);

        Generator.forSome16bitUnary(
                test.run(0xDD, 0xE9)
        );
    }

    @Test
    public void testJP__IY() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsIY()
                .verifyPC(context -> context.first);

        Generator.forSome16bitUnary(
                test.run(0xFD, 0xE9)
        );
    }

    @Test
    public void testDJNZ__e() {
        ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .secondIsRegister(REG_B)
                .expandMemory(first -> cpuRunnerImpl.getPC() + first.intValue())
                .verifyPC(context -> {
                    if (((context.second - 1) & 0xFF) == 0) {
                        return context.PC + 2;
                    }
                    return (context.PC + context.first + 2) & 0xFFFF;
                })
                .verifyRegister(REG_B, context -> (context.second - 1) & 0xFF);

        Generator.forSome8bitBinary(
                test.runWithFirstOperand(0x10)
        );
    }

    @Test
    public void testRETI() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .firstIsPair(REG_SP)
                .verifyPC(context -> context.second)
                .verifyPair(REG_SP, context -> (context.first + 2) & 0xFFFF);

        Generator.forSome16bitBinary(2,
                test.run(0xED, 0x4D)
        );
    }

    @Test
    public void testRETN() {
        IntegerTestBuilder test = new IntegerTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                .firstIsAddressAndSecondIsMemoryWord()
                .firstIsPair(REG_SP)
                .verifyPC(context -> context.second)
                .verifyPair(REG_SP, context -> (context.first + 2) & 0xFFFF)
                .enableIFF2()
                .disableIFF1()
                .verifyIFF1isEnabled()
                .keepCurrentInjectorsAfterRun();

        Generator.forSome16bitBinary(2,
                test.run(0xED, 0x45),
                test.run(0xED, 0x55),
                test.run(0xED, 0x5D),
                test.run(0xED, 0x65),
                test.run(0xED, 0x6D),
                test.run(0xED, 0x75),
                test.run(0xED, 0x7D)
        );
    }

    private static int indexedAddress(int base, Number displacement) {
        return (base + displacement.byteValue()) & 0xFFFF;
    }

    private static int unsigned(Number value) {
        return value.intValue() & 0xFF;
    }

    private static void assertReadAndPassiveCycles(CountingByteMemoryStub memory, int address, int expectedReadCount,
                                                   int expectedPassiveCycles) {
        assertEquals(expectedReadCount, memory.getReadCount(address));
        assertEquals(expectedPassiveCycles, memory.getPassiveCycleCount(address));
    }

    private CountingTestEnvironment newCountingTestEnvironment() throws Exception {
        CountingByteMemoryStub memory = new CountingByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        Capture<Context8080> cpuContext = Capture.newInstance();
        ContextPool contextPool = EasyMock.createNiceMock(ContextPool.class);
        expect(contextPool.getMemoryContext(0, MemoryContext.class)).andReturn(memory).anyTimes();
        contextPool.register(anyLong(), capture(cpuContext), same(Context8080.class));
        expectLastCall().anyTimes();
        replay(contextPool);

        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(contextPool).anyTimes();
        replay(applicationApi);

        CpuImpl cpu = new CpuImpl(0L, applicationApi, PluginSettings.UNAVAILABLE);

        if (!cpuContext.hasCaptured()) {
            throw new AssertionError("CPU context was not captured for counting test environment");
        }

        List<FakeByteDevice> devices = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            FakeByteDevice device = new FakeByteDevice();
            devices.add(device);
            cpuContext.getValue().attachDevice(i, device);
        }

        cpu.initialize();
        return new CountingTestEnvironment(
                cpu,
                memory,
                new CpuRunnerImpl(cpu, memory, devices),
                new CpuVerifierImpl(cpu, memory, devices)
        );
    }

    private static final class CountingTestEnvironment implements AutoCloseable {
        private final CpuImpl cpu;
        private final CountingByteMemoryStub memory;
        private final CpuRunnerImpl cpuRunner;
        private final CpuVerifierImpl cpuVerifier;

        private CountingTestEnvironment(CpuImpl cpu, CountingByteMemoryStub memory, CpuRunnerImpl cpuRunner,
                                        CpuVerifierImpl cpuVerifier) {
            this.cpu = cpu;
            this.memory = memory;
            this.cpuRunner = cpuRunner;
            this.cpuVerifier = cpuVerifier;
        }

        @Override
        public void close() {
            cpu.destroy();
        }
    }

    private static final class CountingByteMemoryStub extends ByteMemoryStub implements CPUContext.PassedCyclesListener {
        private final Map<Integer, Integer> readCounts = new HashMap<>();
        private final Map<Integer, Integer> passiveCycleCounts = new HashMap<>();

        private CountingByteMemoryStub(int wordReadingStrategy) {
            super(wordReadingStrategy);
        }

        @Override
        public Byte read(int location) {
            int masked = location & 0xFFFF;
            readCounts.merge(masked, 1, Integer::sum);
            return super.read(masked);
        }

        @Override
        public Byte[] read(int location, int count) {
            Byte[] values = new Byte[count];
            for (int i = 0; i < count; i++) {
                values[i] = read(location + i);
            }
            return values;
        }

        @Override
        public void write(int location, Byte value) {
            super.write(location & 0xFFFF, value);
        }

        @Override
        public void write(int location, Byte[] values, int count) {
            for (int i = 0; i < count; i++) {
                write(location + i, values[i]);
            }
        }

        @Override
        public void passedCycles(long cyclesDelta) {
        }

        @Override
        public void passedCycles(int address, int cycles) {
            passiveCycleCounts.merge(address & 0xFFFF, cycles, Integer::sum);
        }

        @Override
        public Class<Byte> getCellTypeClass() {
            return Byte.class;
        }

        @Override
        public void clear() {
            super.clear();
            clearCounters();
        }

        private void clearCounters() {
            readCounts.clear();
            passiveCycleCounts.clear();
        }

        @Override
        public MemoryContextAnnotations annotations() {
            return null;
        }

        int getReadCount(int location) {
            return readCounts.getOrDefault(location & 0xFFFF, 0);
        }

        int getPassiveCycleCount(int location) {
            return passiveCycleCounts.getOrDefault(location & 0xFFFF, 0);
        }
    }
}
