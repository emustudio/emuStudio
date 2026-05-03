/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.zilogZ80.suite.CpuRunnerImpl;
import net.emustudio.plugins.cpu.zilogZ80.suite.CpuVerifierImpl;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.FLAG_C;
import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.FLAG_PV;
import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.FLAG_S;
import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.FLAG_Z;
import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.REG_A;
import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.REG_B;
import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.REG_C;
import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * CPU-core extraction of the 48K Spectrum timing tape.
 * <p>
 * Extraction summary:
 * - The tape starts with one BASIC UI/selection loader and three turbo blocks.
 * - The turbo payloads are stored as raw bytes with checksum and use ED ED count value runs.
 * - Expanding those runs yields a direct RAM image that the BASIC harness expects at 0xC000.
 * - The lower half is mostly BASIC-facing tables, strings, and lookup data.
 * - The upper pages store opcode tables. Some are reversed triplets, for example
 * 6E FD 00 in RAM means the harness executes FD 6E 00 -> ld l,(iy+0).
 * Others are already stored in executable order, for example CB 6C -> bit 5,h.
 * <p>
 * The original tape measures Spectrum-visible effects with loops, interrupts, and a few scratch
 * cells. In z80-cpu we translate that into direct CPU-core assertions:
 * - total T-states per instruction or instruction group,
 * - refresh-register increment,
 * - stack pointer effects where the tape reports SP.
 * <p>
 * Tests 35-37 on tape differ only by Spectrum contention/display interaction. Per user request,
 * this file keeps only the CPU-invariant opcode timing and leaves contention/display behavior to
 * the ZX Spectrum bus and ULA modules.
 */
public class TimingTests48kProgramTest {
    private static final int REG_PAIR_BC = 0;
    private static final int REG_PAIR_DE = 1;
    private static final int REG_PAIR_HL = 2;
    private static final int REG_SP = 3;
    private static final int LOAD_ADDRESS = 0xC000;
    private static final String IMAGE_RESOURCE = "/net/emustudio/plugins/cpu/zilogZ80/timing_tests-48k_v1.0.bin";

    @Test
    public void extractedImageMatchesTapeRecovery() throws Exception {
        byte[] image = readImage();

        assertEquals(15821, image.length);
        assertEquals(0x00, image[0] & 0xFF);
        assertEquals(0xFD, image[1] & 0xFF);
        assertEquals("25194", new String(image, 0x462, 5, StandardCharsets.US_ASCII));
        assertEquals("48116", new String(image, 0x3EC, 5, StandardCharsets.US_ASCII));
        assertArrayEquals(new byte[]{0x6E, (byte) 0xFD, 0x00}, slice(image, 0x3800, 3));
        assertArrayEquals(new byte[]{(byte) 0xCB, 0x6C}, slice(image, 0x3A00, 2));
        assertArrayEquals(new byte[]{(byte) 0x84, (byte) 0xBB}, slice(image, 0x3C00, 2));
    }

    @Test
    public void menu01_jrIncBcLdBcIndirect() throws Exception {
        assertSequenceTiming("menu 01", 58, 6, 4, ints(0x18, 0x00, 0x03, 0xED, 0x4B, 0x00, 0x40, 0xED, 0x43, 0x02, 0x40), env -> setWord(env, 0x4000, 0x1234), null);
    }

    @Test
    public void menu02_incDecRegisterPairs() throws Exception {
        assertSequenceTiming("menu 02", 48, 8, 8, ints(0x03, 0x0B, 0x13, 0x1B, 0x23, 0x2B, 0x33, 0x3B));
    }

    @Test
    public void menu03_nopLdIncDecRegisters() throws Exception {
        assertSequenceTiming("menu 03", 16, 4, 4, ints(0x00, 0x40, 0x04, 0x05));
    }

    @Test
    public void menu04_aluRegisterAndHl() throws Exception {
        assertSequenceTiming("menu 04", 88, 16, 16, ints(0x80, 0x88, 0x90, 0x98, 0xA0, 0xA8, 0xB0, 0xB8, 0x86, 0x8E, 0x96, 0x9E, 0xA6, 0xAE, 0xB6, 0xBE), env -> {
            env.cpuRunner.setRegisterPair(REG_PAIR_HL, 0x4000);
            env.cpuRunner.setByte(0x4000, 0x22);
        }, null);
    }

    @Test
    public void menu05_exchangeInstructions() throws Exception {
        assertSequenceTiming("menu 05", 12, 3, 3, ints(0xD9, 0x08, 0xEB));
    }

    @Test
    public void menu06_daaCplCcfScf() throws Exception {
        assertSequenceTiming("menu 06", 16, 4, 4, ints(0x27, 0x2F, 0x3F, 0x37));
    }

    @Test
    public void menu07_pushPopAndExSpHl() throws Exception {
        assertSequenceTiming("menu 07", 40, 3, 3, ints(0xC5, 0xC1, 0xE3), env -> {
            env.cpuRunner.setRegisterPair(REG_PAIR_BC, 0x3456);
            env.cpuRunner.setRegisterPair(REG_PAIR_HL, 0xABCD);
            env.cpuRunner.setSP(0x4000);
            setWord(env, 0x4000, 0x1234);
        }, env -> assertEquals(0x4000, env.cpu.getEngine().SP));
    }

    @Test
    public void menu08_rotateAccumulatorAndNibbleOps() throws Exception {
        assertSequenceTiming("menu 08", 52, 8, 6, ints(0x17, 0x1F, 0x07, 0x0F, 0xED, 0x6F, 0xED, 0x67), env -> {
            env.cpuRunner.setRegister(REG_A, 0x12);
            env.cpuRunner.setRegisterPair(REG_PAIR_HL, 0x4000);
            env.cpuRunner.setByte(0x4000, 0x34);
        }, null);
    }

    @Test
    public void menu09_ldHlForms() throws Exception {
        assertSequenceTiming("menu 09", 52, 4, 4, ints(0x21, 0x34, 0x12, 0x2A, 0x00, 0x40, 0x22, 0x02, 0x40, 0x36, 0x20), env -> {
            setWord(env, 0x4000, 0x5678);
            env.cpuRunner.setRegisterPair(REG_PAIR_HL, 0x4004);
        }, null);
    }

    @Test
    public void menu10_pushPopCallAndJumpWithFlagsReset() throws Exception {
        assertInstructionTiming("push bc", 11, 1, ints(0xC5), null, null);
        assertInstructionTiming("pop bc", 10, 1, ints(0xC1), env -> {
            env.cpuRunner.setSP(0x4000);
            setWord(env, 0x4000, 0x1234);
        }, null);
        assertInstructionTiming("call nz,nn taken", 17, 1, ints(0xC4, 0x34, 0x12), env -> env.cpuRunner.resetFlags(), env -> assertEquals(0x1234, env.cpu.getEngine().PC));
        assertInstructionTiming("call z,nn not taken", 10, 1, ints(0xCC, 0x34, 0x12), env -> env.cpuRunner.resetFlags(), env -> assertEquals(0x0003, env.cpu.getEngine().PC));
        assertInstructionTiming("jp nz,nn taken", 10, 1, ints(0xC2, 0x34, 0x12), env -> env.cpuRunner.resetFlags(), env -> assertEquals(0x1234, env.cpu.getEngine().PC));
        assertInstructionTiming("jp z,nn not taken", 10, 1, ints(0xCA, 0x34, 0x12), env -> env.cpuRunner.resetFlags(), env -> assertEquals(0x0003, env.cpu.getEngine().PC));
    }

    @Test
    public void menu11_pushPopCallAndJumpWithFlagsSet() throws Exception {
        assertInstructionTiming("call z,nn taken", 17, 1, ints(0xCC, 0x34, 0x12), env -> env.cpuRunner.setFlags(FLAG_Z | FLAG_C | FLAG_PV | FLAG_S), env -> assertEquals(0x1234, env.cpu.getEngine().PC));
        assertInstructionTiming("call nz,nn not taken", 10, 1, ints(0xC4, 0x34, 0x12), env -> env.cpuRunner.setFlags(FLAG_Z | FLAG_C | FLAG_PV | FLAG_S), env -> assertEquals(0x0003, env.cpu.getEngine().PC));
        assertInstructionTiming("jp z,nn taken", 10, 1, ints(0xCA, 0x34, 0x12), env -> env.cpuRunner.setFlags(FLAG_Z | FLAG_C | FLAG_PV | FLAG_S), env -> assertEquals(0x1234, env.cpu.getEngine().PC));
        assertInstructionTiming("jp nz,nn not taken", 10, 1, ints(0xC2, 0x34, 0x12), env -> env.cpuRunner.setFlags(FLAG_Z | FLAG_C | FLAG_PV | FLAG_S), env -> assertEquals(0x0003, env.cpu.getEngine().PC));
    }

    @Test
    public void menu12_cbRotateShiftRegisterAndHl() throws Exception {
        assertSequenceTiming("menu 12", 161, 28, 14, ints(0xCB, 0x00, 0xCB, 0x09, 0xCB, 0x12, 0xCB, 0x1B, 0xCB, 0x24, 0xCB, 0x2D, 0xCB, 0x39, 0xCB, 0x06, 0xCB, 0x0E, 0xCB, 0x16, 0xCB, 0x1E, 0xCB, 0x26, 0xCB, 0x2E, 0xCB, 0x3E), env -> {
            env.cpuRunner.setRegisterPair(REG_PAIR_HL, 0x4000);
            env.cpuRunner.setByte(0x4000, 0x55);
        }, null);
    }

    @Test
    public void menu13_bitRegister() throws Exception {
        assertSequenceTiming("menu 13", 32, 8, 4, ints(0xCB, 0x6C, 0xCB, 0x79, 0xCB, 0x42, 0xCB, 0x7B));
    }

    @Test
    public void menu14_setAndResRegister() throws Exception {
        assertSequenceTiming("menu 14", 32, 8, 4, ints(0xCB, 0xC2, 0xCB, 0x82, 0xCB, 0xFF, 0xCB, 0xBF));
    }

    @Test
    public void menu15_imAndIRTransfers() throws Exception {
        assertSequenceTiming("menu 15", 60, 2, 7, ints(0xED, 0x46, 0xED, 0x56, 0xED, 0x5E, 0xED, 0x57, 0xED, 0x47, 0xED, 0x4F, 0xED, 0x5F), env -> env.cpuRunner.setRegister(REG_A, 0x33), null);
    }

    @Test
    public void menu16_ldSpHlAnd16BitMath() throws Exception {
        assertSequenceTiming("menu 16", 47, 6, 4, ints(0xF9, 0x09, 0xED, 0x4A, 0xED, 0x42), env -> {
            env.cpuRunner.setRegisterPair(REG_PAIR_HL, 0x4000);
            env.cpuRunner.setRegisterPair(REG_PAIR_BC, 0x1000);
        }, null);
    }

    @Test
    public void menu17_aluRegisterGroup0to3() throws Exception {
        assertSequenceTiming("menu 17", 16, 4, 4, ints(0x80, 0x81, 0x82, 0x83));
    }

    @Test
    public void menu18_aluRegisterGroup4to7() throws Exception {
        assertSequenceTiming("menu 18", 19, 4, 4, ints(0x84, 0x85, 0x86, 0x87), env -> {
            env.cpuRunner.setRegisterPair(REG_PAIR_HL, 0x4000);
            env.cpuRunner.setByte(0x4000, 0x10);
        }, null);
    }

    @Test
    public void menu19_loadImmediateAndAbsoluteForms() throws Exception {
        assertSequenceTiming("menu 19", 83, 8, 6, ints(0x06, 0x12, 0x01, 0x34, 0x12, 0x3A, 0x00, 0x40, 0x32, 0x02, 0x40, 0xED, 0x4B, 0x04, 0x40, 0xED, 0x43, 0x06, 0x40), env -> {
            setWord(env, 0x4000, 0xABCD);
            setWord(env, 0x4004, 0x5678);
            env.cpuRunner.setRegister(REG_A, 0x9A);
            env.cpuRunner.setRegisterPair(REG_PAIR_BC, 0x1357);
        }, null);
    }

    @Test
    public void menu20_ldViaBcAndDe() throws Exception {
        assertSequenceTiming("menu 20", 28, 4, 4, ints(0x0A, 0x1A, 0x02, 0x12), env -> {
            env.cpuRunner.setRegisterPair(REG_PAIR_BC, 0x4000);
            env.cpuRunner.setRegisterPair(REG_PAIR_DE, 0x4001);
            env.cpuRunner.setByte(0x4000, 0x11);
            env.cpuRunner.setByte(0x4001, 0x22);
        }, null);
    }

    @Test
    public void menu21_pushPopIiAndIndexedStores() throws Exception {
        assertSequenceTiming("menu 21", 67, 8, 4, ints(0xDD, 0xE5, 0xDD, 0xE1, 0xDD, 0x70, 0x00, 0xDD, 0x36, 0x00, 0x12), env -> {
            env.cpuRunner.setIX(0x4000);
            env.cpuRunner.setSP(0x4200);
            env.cpuRunner.setRegister(REG_B, 0x34);
        }, null);
    }

    @Test
    public void menu22_indexedLoadsViaIxAndIy() throws Exception {
        assertSequenceTiming("menu 22", 76, 8, 4, ints(0xDD, 0x7E, 0x00, 0xDD, 0x46, 0x00, 0xFD, 0x7E, 0x00, 0xFD, 0x46, 0x00), env -> {
            env.cpuRunner.setIX(0x4000);
            env.cpuRunner.setIY(0x4100);
            env.cpuRunner.setByte(0x4000, 0x12);
            env.cpuRunner.setByte(0x4100, 0x34);
        }, null);
    }

    @Test
    public void menu23_ixBitResSetAndLoads() throws Exception {
        assertSequenceTiming("menu 23", 104, 10, 5, ints(0xDD, 0x7E, 0x00, 0xDD, 0x46, 0x00, 0xDD, 0xCB, 0x00, 0x46, 0xDD, 0xCB, 0x00, 0x86, 0xDD, 0xCB, 0x00, 0xC6), env -> {
            env.cpuRunner.setIX(0x4000);
            env.cpuRunner.setByte(0x4000, 0x55);
        }, null);
    }

    @Test
    public void menu24_iyBitResSetAndLoads() throws Exception {
        assertSequenceTiming("menu 24", 104, 10, 5, ints(0xFD, 0x7E, 0x00, 0xFD, 0x46, 0x00, 0xFD, 0xCB, 0x00, 0x46, 0xFD, 0xCB, 0x00, 0x86, 0xFD, 0xCB, 0x00, 0xC6), env -> {
            env.cpuRunner.setIY(0x4100);
            env.cpuRunner.setByte(0x4100, 0x55);
        }, null);
    }

    @Test
    public void menu25_aluOnIndexedIxValue() throws Exception {
        assertSequenceTiming("menu 25", 152, 16, 8, ints(0xDD, 0x86, 0x00, 0xDD, 0x8E, 0x00, 0xDD, 0x96, 0x00, 0xDD, 0x9E, 0x00, 0xDD, 0xA6, 0x00, 0xDD, 0xAE, 0x00, 0xDD, 0xB6, 0x00, 0xDD, 0xBE, 0x00), env -> {
            env.cpuRunner.setIX(0x4000);
            env.cpuRunner.setByte(0x4000, 0x11);
        }, null);
    }

    @Test
    public void menu26_aluOnIndexedIyValue() throws Exception {
        assertSequenceTiming("menu 26", 152, 16, 8, ints(0xFD, 0x86, 0x00, 0xFD, 0x8E, 0x00, 0xFD, 0x96, 0x00, 0xFD, 0x9E, 0x00, 0xFD, 0xA6, 0x00, 0xFD, 0xAE, 0x00, 0xFD, 0xB6, 0x00, 0xFD, 0xBE, 0x00), env -> {
            env.cpuRunner.setIY(0x4100);
            env.cpuRunner.setByte(0x4100, 0x11);
        }, null);
    }

    @Test
    public void menu27_bitResSetIncDecOnHl() throws Exception {
        assertSequenceTiming("menu 27", 64, 8, 5, ints(0xCB, 0x46, 0xCB, 0x86, 0xCB, 0xC6, 0x34, 0x35), env -> {
            env.cpuRunner.setRegisterPair(REG_PAIR_HL, 0x4000);
            env.cpuRunner.setByte(0x4000, 0x20);
        }, null);
    }

    @Test
    public void menu28_retFamily() throws Exception {
        assertInstructionTiming("ret", 10, 1, ints(0xC9), env -> {
            env.cpuRunner.setSP(0x4000);
            setWord(env, 0x4000, 0x1234);
        }, env -> assertEquals(0x1234, env.cpu.getEngine().PC));
        assertInstructionTiming("ret z taken", 11, 1, ints(0xC8), env -> {
            env.cpuRunner.setFlags(FLAG_Z);
            env.cpuRunner.setSP(0x4000);
            setWord(env, 0x4000, 0x5678);
        }, env -> assertEquals(0x5678, env.cpu.getEngine().PC));
        assertInstructionTiming("ret nz not taken", 5, 1, ints(0xC0), env -> {
            env.cpuRunner.setFlags(FLAG_Z);
            env.cpuRunner.setSP(0x4000);
            setWord(env, 0x4000, 0x5678);
        }, env -> assertEquals(0x0001, env.cpu.getEngine().PC));
        assertInstructionTiming("reti", 14, 2, ints(0xED, 0x4D), env -> {
            env.cpuRunner.setSP(0x4000);
            setWord(env, 0x4000, 0x1357);
        }, env -> assertEquals(0x1357, env.cpu.getEngine().PC));
        assertInstructionTiming("retn", 14, 2, ints(0xED, 0x45), env -> {
            env.cpuRunner.enableIFF2();
            env.cpuRunner.disableIFF1();
            env.cpuRunner.setSP(0x4000);
            setWord(env, 0x4000, 0x2468);
        }, env -> assertEquals(0x2468, env.cpu.getEngine().PC));
    }

    @Test
    public void menu29_callCcJrCcAndDjnz() throws Exception {
        assertInstructionTiming("call nz taken", 17, 1, ints(0xC4, 0x34, 0x12), env -> env.cpuRunner.resetFlags(), env -> assertEquals(0x1234, env.cpu.getEngine().PC));
        assertInstructionTiming("call z not taken", 10, 1, ints(0xCC, 0x34, 0x12), env -> env.cpuRunner.resetFlags(), env -> assertEquals(0x0003, env.cpu.getEngine().PC));
        assertInstructionTiming("jr nz taken", 12, 1, ints(0x20, 0x00), env -> env.cpuRunner.resetFlags(), env -> assertEquals(0x0002, env.cpu.getEngine().PC));
        assertInstructionTiming("jr z not taken", 7, 1, ints(0x28, 0x00), env -> env.cpuRunner.resetFlags(), env -> assertEquals(0x0002, env.cpu.getEngine().PC));
        assertInstructionTiming("djnz taken", 13, 1, ints(0x10, 0x00), env -> env.cpuRunner.setRegister(REG_B, 0x02), env -> assertEquals(0x01, env.cpu.getEngine().regs[REG_B]));
        assertInstructionTiming("djnz final", 8, 1, ints(0x10, 0x00), env -> env.cpuRunner.setRegister(REG_B, 0x01), env -> assertEquals(0x00, env.cpu.getEngine().regs[REG_B]));
    }

    @Test
    public void menu30_ldiFamily() throws Exception {
        assertInstructionTiming("ldi", 16, 2, ints(0xED, 0xA0), env -> prepareBlockCopy(env, 0x4000, 0x4100, 0x0001), null);
        assertInstructionTiming("ldir repeat", 21, 2, ints(0xED, 0xB0), env -> prepareBlockCopy(env, 0x4000, 0x4100, 0x0002), env -> assertEquals(0x0000, env.cpu.getEngine().PC));
        assertInstructionTiming("ldir final", 16, 2, ints(0xED, 0xB0), env -> prepareBlockCopy(env, 0x4000, 0x4100, 0x0001), env -> assertEquals(0x0002, env.cpu.getEngine().PC));
        assertInstructionTiming("ldd", 16, 2, ints(0xED, 0xA8), env -> prepareBlockCopy(env, 0x4001, 0x4101, 0x0001), null);
        assertInstructionTiming("lddr repeat", 21, 2, ints(0xED, 0xB8), env -> prepareBlockCopy(env, 0x4001, 0x4101, 0x0002), env -> assertEquals(0x0000, env.cpu.getEngine().PC));
    }

    @Test
    public void menu31_cpiFamily() throws Exception {
        assertInstructionTiming("cpi", 16, 2, ints(0xED, 0xA1), env -> prepareBlockCompare(env, 0x4000, 0x0001, 0x20, 0x10), null);
        assertInstructionTiming("cpir repeat", 21, 2, ints(0xED, 0xB1), env -> prepareBlockCompare(env, 0x4000, 0x0002, 0x20, 0x10), env -> assertEquals(0x0000, env.cpu.getEngine().PC));
        assertInstructionTiming("cpd", 16, 2, ints(0xED, 0xA9), env -> prepareBlockCompare(env, 0x4001, 0x0001, 0x20, 0x10), null);
        assertInstructionTiming("cpdr repeat", 21, 2, ints(0xED, 0xB9), env -> prepareBlockCompare(env, 0x4001, 0x0002, 0x20, 0x10), env -> assertEquals(0x0000, env.cpu.getEngine().PC));
    }

    @Test
    public void menu32_iniFamily() throws Exception {
        assertInstructionTiming("ini", 16, 2, ints(0xED, 0xA2), env -> prepareBlockIoInput(env, 0x4000, 0x0101), null);
        assertInstructionTiming("inir repeat", 21, 2, ints(0xED, 0xB2), env -> prepareBlockIoInput(env, 0x4000, 0x0201), env -> assertEquals(0x0000, env.cpu.getEngine().PC));
        assertInstructionTiming("ind", 16, 2, ints(0xED, 0xAA), env -> prepareBlockIoInput(env, 0x4001, 0x0101), null);
        assertInstructionTiming("indr repeat", 21, 2, ints(0xED, 0xBA), env -> prepareBlockIoInput(env, 0x4001, 0x0201), env -> assertEquals(0x0000, env.cpu.getEngine().PC));
    }

    @Test
    public void menu33_outiFamily() throws Exception {
        assertInstructionTiming("outi", 16, 2, ints(0xED, 0xA3), env -> prepareBlockIoOutput(env, 0x4000, 0x0101), null);
        assertInstructionTiming("otir repeat", 21, 2, ints(0xED, 0xB3), env -> prepareBlockIoOutput(env, 0x4000, 0x0201), env -> assertEquals(0x0000, env.cpu.getEngine().PC));
        assertInstructionTiming("outd", 16, 2, ints(0xED, 0xAB), env -> prepareBlockIoOutput(env, 0x4001, 0x0101), null);
        assertInstructionTiming("otdr repeat", 21, 2, ints(0xED, 0xBB), env -> prepareBlockIoOutput(env, 0x4001, 0x0201), env -> assertEquals(0x0000, env.cpu.getEngine().PC));
    }

    @Test
    public void menu34_rst18() throws Exception {
        assertInstructionTiming("rst 18", 11, 1, ints(0xDF), env -> env.cpuRunner.setSP(0x4000), env -> assertEquals(0x0018, env.cpu.getEngine().PC));
    }

    @Test
    public void menu35_ioGroupCpuCoreOnly() throws Exception {
        assertSequenceTiming("menu 35", 46, 6, 4, ints(0xDB, 0x00, 0xD3, 0x00, 0xED, 0x78, 0xED, 0x41), env -> {
            env.cpuRunner.setRegister(REG_C, 0x20);
            env.cpuRunner.getDevice(0).setValue((byte) 0x44);
            env.cpuRunner.getDevice(0x20).setValue((byte) 0x55);
        }, null);
    }

    @Test
    public void menu36_ioGroupClsVariantReducedToCpuCore() throws Exception {
        assertSequenceTiming("menu 36", 46, 6, 4, ints(0xDB, 0x00, 0xD3, 0x00, 0xED, 0x78, 0xED, 0x41), env -> {
            env.cpuRunner.setRegister(REG_C, 0x20);
            env.cpuRunner.getDevice(0).setValue((byte) 0x44);
            env.cpuRunner.getDevice(0x20).setValue((byte) 0x55);
        }, null);
    }

    @Test
    public void menu37_ioGroupContendedVariantReducedToCpuCore() throws Exception {
        assertSequenceTiming("menu 37", 46, 6, 4, ints(0xDB, 0x00, 0xD3, 0x00, 0xED, 0x78, 0xED, 0x41), env -> {
            env.cpuRunner.setRegister(REG_C, 0x20);
            env.cpuRunner.getDevice(0).setValue((byte) 0x44);
            env.cpuRunner.getDevice(0x20).setValue((byte) 0x55);
        }, null);
    }

    private void assertInstructionTiming(String label, int expectedCycles, int expectedRDelta, int[] program, Consumer<TimingEnvironment> setup, Consumer<TimingEnvironment> verify) throws Exception {
        assertSequenceTiming(label, expectedCycles, expectedRDelta, 1, program, setup, verify);
    }

    private void assertSequenceTiming(String label, int expectedCycles, int expectedRDelta, int steps, int[] program) throws Exception {
        assertSequenceTiming(label, expectedCycles, expectedRDelta, steps, program, null, null);
    }

    private void assertSequenceTiming(String label, int expectedCycles, int expectedRDelta, int steps, int[] program, Consumer<TimingEnvironment> setup, Consumer<TimingEnvironment> verify) throws Exception {
        try (TimingEnvironment env = newTimingEnvironment()) {
            env.cpuRunner.setProgram(program);
            env.cpuRunner.reset();
            env.cpuRunner.setR(0);
            if (setup != null) {
                setup.accept(env);
            }
            env.memory.clearCounters();
            for (int i = 0; i < steps; i++) {
                env.cpuRunner.step();
            }
            assertEquals(label + " cycles", expectedCycles, env.memory.getTotalCycles());
            assertEquals(label + " R", expectedRDelta, env.cpu.getEngine().R & 0x7F);
            if (verify != null) {
                verify.accept(env);
            }
        }
    }

    private static void prepareBlockCopy(TimingEnvironment env, int hl, int de, int bc) {
        env.cpuRunner.setRegisterPair(REG_PAIR_HL, hl);
        env.cpuRunner.setRegisterPair(REG_PAIR_DE, de);
        env.cpuRunner.setRegisterPair(REG_PAIR_BC, bc);
        env.cpuRunner.setByte(hl, 0x5A);
    }

    private static void prepareBlockCompare(TimingEnvironment env, int hl, int bc, int a, int memoryValue) {
        env.cpuRunner.setRegisterPair(REG_PAIR_HL, hl);
        env.cpuRunner.setRegisterPair(REG_PAIR_BC, bc);
        env.cpuRunner.setRegister(REG_A, a);
        env.cpuRunner.setByte(hl, memoryValue);
    }

    private static void prepareBlockIoInput(TimingEnvironment env, int hl, int bc) {
        env.cpuRunner.setRegisterPair(REG_PAIR_HL, hl);
        env.cpuRunner.setRegisterPair(REG_PAIR_BC, bc);
        env.cpuRunner.getDevice(bc & 0xFF).setValue((byte) 0x7E);
    }

    private static void prepareBlockIoOutput(TimingEnvironment env, int hl, int bc) {
        env.cpuRunner.setRegisterPair(REG_PAIR_HL, hl);
        env.cpuRunner.setRegisterPair(REG_PAIR_BC, bc);
        env.cpuRunner.setByte(hl, 0x33);
    }

    private static void setWord(TimingEnvironment env, int address, int value) {
        env.cpuRunner.setByte(address, value & 0xFF);
        env.cpuRunner.setByte((address + 1) & 0xFFFF, (value >>> 8) & 0xFF);
    }

    private static int[] ints(int... values) {
        return values;
    }

    private static byte[] readImage() throws IOException {
        try (InputStream stream = TimingTests48kProgramTest.class.getResourceAsStream(IMAGE_RESOURCE)) {
            if (stream == null) {
                throw new IOException("Missing resource: " + IMAGE_RESOURCE);
            }
            return stream.readAllBytes();
        }
    }

    private static byte[] slice(byte[] array, int from, int length) {
        byte[] copy = new byte[length];
        System.arraycopy(array, from, copy, 0, length);
        return copy;
    }

    private TimingEnvironment newTimingEnvironment() throws Exception {
        TimingMemoryStub memory = new TimingMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
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
            throw new AssertionError("CPU context was not captured");
        }

        List<FakeByteDevice> devices = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            FakeByteDevice device = new FakeByteDevice();
            devices.add(device);
            cpuContext.getValue().attachDevice(i, device);
        }
        cpuContext.getValue().addPassedCyclesListener(memory);

        cpu.initialize();
        return new TimingEnvironment(cpu, memory, new CpuRunnerImpl(cpu, memory, devices), new CpuVerifierImpl(cpu, memory, devices));
    }

    private static final class TimingEnvironment implements AutoCloseable {
        private final CpuImpl cpu;
        private final TimingMemoryStub memory;
        private final CpuRunnerImpl cpuRunner;
        @SuppressWarnings("unused")
        private final CpuVerifierImpl cpuVerifier;

        private TimingEnvironment(CpuImpl cpu, TimingMemoryStub memory, CpuRunnerImpl cpuRunner, CpuVerifierImpl cpuVerifier) {
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

    private static final class TimingMemoryStub extends ByteMemoryStub implements CPUContext.PassedCyclesListener {
        private final Map<Integer, Integer> readCounts = new HashMap<>();
        private final Map<Integer, Integer> passiveCycleCounts = new HashMap<>();
        private long totalCycles;

        private TimingMemoryStub(int wordReadingStrategy) {
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
            totalCycles += cyclesDelta;
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

        @Override
        public MemoryContextAnnotations annotations() {
            return null;
        }

        private void clearCounters() {
            readCounts.clear();
            passiveCycleCounts.clear();
            totalCycles = 0;
        }

        long getTotalCycles() {
            return totalCycles;
        }
    }
}
