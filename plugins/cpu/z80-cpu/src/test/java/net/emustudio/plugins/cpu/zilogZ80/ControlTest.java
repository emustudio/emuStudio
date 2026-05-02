/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.cpu.testsuite.Generator;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.plugins.cpu.zilogZ80.suite.ByteTestBuilder;
import net.emustudio.plugins.cpu.zilogZ80.suite.IntegerTestBuilder;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;

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
    public void testTakenJrAddsFiveHiddenCyclesAtDisplacementAddress() throws Exception {
        CountingMemory memory = new CountingMemory();
        ContextZ80Impl context = new ContextZ80Impl();
        EmulatorEngine engine = new EmulatorEngine(memory, context);
        context.setEngine(engine);

        memory.write(0x0000, (byte) 0x18);
        memory.write(0x0001, (byte) 0x00);

        engine.reset(0x0000);
        engine.step();

        org.junit.Assert.assertEquals(6, memory.getReadCount(0x0001));
        org.junit.Assert.assertEquals(0x0002, engine.PC);
    }

    @Test
    public void testIncBcAddsTwoHiddenRefreshCycles() throws Exception {
        CountingMemory memory = new CountingMemory();
        ContextZ80Impl context = new ContextZ80Impl();
        EmulatorEngine engine = new EmulatorEngine(memory, context);
        context.setEngine(engine);

        memory.write(0x0000, (byte) 0x03);

        engine.reset(0x0000);
        engine.I = 0x40;
        engine.step();

        org.junit.Assert.assertEquals(2, memory.getReadCount(0x4001));
        org.junit.Assert.assertEquals(0x0001, (engine.regs[REG_B] << 8) | engine.regs[REG_C]);
    }

    @Test
    public void testIndexedLoadAddsFiveHiddenCyclesAtDisplacementAddress() throws Exception {
        CountingMemory memory = new CountingMemory();
        ContextZ80Impl context = new ContextZ80Impl();
        EmulatorEngine engine = new EmulatorEngine(memory, context);
        context.setEngine(engine);

        memory.write(0x0000, (byte) 0xDD);
        memory.write(0x0001, (byte) 0x7E);
        memory.write(0x0002, (byte) 0x00);
        memory.write(0x4000, (byte) 0x5A);

        engine.reset(0x0000);
        engine.IX = 0x4000;
        engine.step();

        org.junit.Assert.assertEquals(6, memory.getReadCount(0x0002));
        org.junit.Assert.assertEquals(1, memory.getReadCount(0x4000));
        org.junit.Assert.assertEquals(0x5A, engine.regs[REG_A]);
    }

    @Test
    public void testIndexedHLoadAddsFiveHiddenCyclesAtDisplacementAddress() throws Exception {
        CountingMemory memory = new CountingMemory();
        ContextZ80Impl context = new ContextZ80Impl();
        EmulatorEngine engine = new EmulatorEngine(memory, context);
        context.setEngine(engine);

        memory.write(0x0000, (byte) 0xDD);
        memory.write(0x0001, (byte) 0x66);
        memory.write(0x0002, (byte) 0x00);
        memory.write(0x4000, (byte) 0x5A);

        engine.reset(0x0000);
        engine.IX = 0x4000;
        engine.step();

        org.junit.Assert.assertEquals(6, memory.getReadCount(0x0002));
        org.junit.Assert.assertEquals(1, memory.getReadCount(0x4000));
        org.junit.Assert.assertEquals(0x5A, engine.regs[REG_H]);
    }

    @Test
    public void testIndexedAddAddsFiveHiddenCyclesAtDisplacementAddress() throws Exception {
        CountingMemory memory = new CountingMemory();
        ContextZ80Impl context = new ContextZ80Impl();
        EmulatorEngine engine = new EmulatorEngine(memory, context);
        context.setEngine(engine);

        memory.write(0x0000, (byte) 0xDD);
        memory.write(0x0001, (byte) 0x86);
        memory.write(0x0002, (byte) 0x00);
        memory.write(0x4000, (byte) 0x05);

        engine.reset(0x0000);
        engine.IX = 0x4000;
        engine.regs[REG_A] = 0x10;
        engine.step();

        org.junit.Assert.assertEquals(6, memory.getReadCount(0x0002));
        org.junit.Assert.assertEquals(1, memory.getReadCount(0x4000));
        org.junit.Assert.assertEquals(0x15, engine.regs[REG_A]);
    }

    @Test
    public void testIndexedIncAddsHiddenCyclesAtDisplacementAndIndexedAddress() throws Exception {
        CountingMemory memory = new CountingMemory();
        ContextZ80Impl context = new ContextZ80Impl();
        EmulatorEngine engine = new EmulatorEngine(memory, context);
        context.setEngine(engine);

        memory.write(0x0000, (byte) 0xDD);
        memory.write(0x0001, (byte) 0x34);
        memory.write(0x0002, (byte) 0x00);
        memory.write(0x4000, (byte) 0x05);

        engine.reset(0x0000);
        engine.IX = 0x4000;
        engine.step();

        org.junit.Assert.assertEquals(6, memory.getReadCount(0x0002));
        org.junit.Assert.assertEquals(2, memory.getReadCount(0x4000));
        org.junit.Assert.assertEquals(0x06, memory.read(0x4000) & 0xFF);
    }

    @Test
    public void testIndexedImmediateStoreAddsTwoHiddenCyclesAtImmediateAddress() throws Exception {
        CountingMemory memory = new CountingMemory();
        ContextZ80Impl context = new ContextZ80Impl();
        EmulatorEngine engine = new EmulatorEngine(memory, context);
        context.setEngine(engine);

        memory.write(0x0000, (byte) 0xDD);
        memory.write(0x0001, (byte) 0x36);
        memory.write(0x0002, (byte) 0x00);
        memory.write(0x0003, (byte) 0x34);

        engine.reset(0x0000);
        engine.IX = 0x4000;
        engine.step();

        org.junit.Assert.assertEquals(3, memory.getReadCount(0x0003));
        org.junit.Assert.assertEquals(0x34, memory.read(0x4000) & 0xFF);
    }

    @Test
    public void testRldAddsFourHiddenCyclesAtHlAddress() throws Exception {
        CountingMemory memory = new CountingMemory();
        ContextZ80Impl context = new ContextZ80Impl();
        EmulatorEngine engine = new EmulatorEngine(memory, context);
        context.setEngine(engine);

        memory.write(0x0000, (byte) 0xED);
        memory.write(0x0001, (byte) 0x6F);
        memory.write(0x4000, (byte) 0x12);

        engine.reset(0x0000);
        engine.regs[REG_H] = 0x40;
        engine.regs[REG_L] = 0x00;
        engine.regs[REG_A] = 0x34;
        engine.step();

        org.junit.Assert.assertEquals(5, memory.getReadCount(0x4000));
        org.junit.Assert.assertEquals(0x24, memory.read(0x4000) & 0xFF);
        org.junit.Assert.assertEquals(0x31, engine.regs[REG_A]);
    }

    @Test
    public void testIndexedCbOpcodeFetchDoesNotIncrementRefreshRegister() throws Exception {
        CountingMemory memory = new CountingMemory();
        ContextZ80Impl context = new ContextZ80Impl();
        EmulatorEngine engine = new EmulatorEngine(memory, context);
        context.setEngine(engine);

        memory.write(0x0000, (byte) 0xDD);
        memory.write(0x0001, (byte) 0xCB);
        memory.write(0x0002, (byte) 0x00);
        memory.write(0x0003, (byte) 0x46);
        memory.write(0x4000, (byte) 0x01);

        engine.reset(0x0000);
        engine.IX = 0x4000;
        engine.step();

        org.junit.Assert.assertEquals(0x02, engine.R);
        org.junit.Assert.assertEquals(3, memory.getReadCount(0x0003));
        org.junit.Assert.assertEquals(2, memory.getReadCount(0x4000));
    }

    @Test
    public void testIndexedResAddsHiddenCycleAtIndexedAddress() throws Exception {
        CountingMemory memory = new CountingMemory();
        ContextZ80Impl context = new ContextZ80Impl();
        EmulatorEngine engine = new EmulatorEngine(memory, context);
        context.setEngine(engine);

        memory.write(0x0000, (byte) 0xDD);
        memory.write(0x0001, (byte) 0xCB);
        memory.write(0x0002, (byte) 0x00);
        memory.write(0x0003, (byte) 0x86);
        memory.write(0x4000, (byte) 0xFF);

        engine.reset(0x0000);
        engine.IX = 0x4000;
        engine.step();

        org.junit.Assert.assertEquals(3, memory.getReadCount(0x0003));
        org.junit.Assert.assertEquals(2, memory.getReadCount(0x4000));
        org.junit.Assert.assertEquals(0xFE, memory.read(0x4000) & 0xFF);
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

    private static final class CountingMemory extends AbstractMemoryContext<Byte> {
        private final byte[] data = new byte[0x10000];
        private final Map<Integer, Integer> readCounts = new HashMap<>();

        @Override
        public Byte read(int location) {
            int masked = location & 0xFFFF;
            readCounts.merge(masked, 1, Integer::sum);
            return data[masked];
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
            data[location & 0xFFFF] = value;
        }

        @Override
        public void write(int location, Byte[] values, int count) {
            for (int i = 0; i < count; i++) {
                write(location + i, values[i]);
            }
        }

        @Override
        public Class<Byte> getCellTypeClass() {
            return Byte.class;
        }

        @Override
        public void clear() {
            Arrays.fill(data, (byte) 0);
            readCounts.clear();
        }

        @Override
        public int getSize() {
            return data.length;
        }

        @Override
        public MemoryContextAnnotations annotations() {
            return null;
        }

        int getReadCount(int location) {
            return readCounts.getOrDefault(location & 0xFFFF, 0);
        }
    }
}
