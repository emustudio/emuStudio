package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.plugins.cpu.zilogZ80.suite.ByteTestBuilder;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;
import static net.emustudio.plugins.cpu.zilogZ80.Z80Tests.InstrTest.mk;
import static net.emustudio.plugins.cpu.zilogZ80.suite.Utils.crc16;
import static org.junit.Assert.assertEquals;

// inspired by:
// http://www.z80.info/decoding.htm
// https://github.com/raxoft/z80test
public class Z80Tests extends InstructionsTest {

    private final static int[] TEST_VALUES = new int[]{
            0, 1, 0x7F, 0x80, 0x81, 0xFF, 0x28, 0x88
    };

    static class InstrTest {
        int bc = 0xBBCC;
        int de = 0xDDEE;
        int hl = 0x4411;
        int ix = 0xDD88;
        int iy = 0xFD77;
        int sp = 0xC000;
        int hl_mem = 0;
        int instr_y_from = 0;
        int instr_y_to = 0;
        int instr_z_from = 0;
        int instr_z_to = 0;
        int instr_p_from = 0;
        int instr_p_to = 0;

        final int opcode;
        final int crc;

        InstrTest(int opcode, int crc) {
            this.opcode = opcode;
            this.crc = crc;
        }

        InstrTest y(int from, int to) {
            instr_y_from = from;
            instr_y_to = to;
            return this;
        }

        InstrTest z(int from, int to) {
            instr_z_from = from;
            instr_z_to = to;
            return this;
        }

        InstrTest all_p() {
            instr_p_from = 0;
            instr_p_to = 3;
            return this;
        }

        InstrTest hl_mem(int value) {
            hl_mem = value;
            return this;
        }

        InstrTest hl_to_ix() {
            ix = hl;
            return this;
        }

        InstrTest hl_to_iy() {
            iy = hl;
            return this;
        }

        InstrTest hl_to_bc() {
            bc = hl;
            return this;
        }

        InstrTest hl_to_de() {
            de = hl;
            return this;
        }

        static InstrTest mk(int opcode, int crc) {
            return new InstrTest(opcode, crc);
        }

        @Override
        public String toString() {
            return "opcode=" + Integer.toHexString(opcode);
        }
    }

    private final static List<InstrTest> testData = Arrays.asList(
            mk(0x00, 0x4f43b66a), // NOP
            mk(0x08, 0x71be4764), // EX AF, AF'
            mk(0x0210, 0xfcace674), // DJNZ 2
            mk(0x0218, 0xad876a20), // JR 2
            mk(0x0220, 0x77bd64ba).y(4, 7), // JR cc[y-4], 2
            mk(0x441101, 0xe931407a).all_p().hl_mem(0x1234), // LD rp[p], 0x4020
            mk(0x09, 0x71104d77).all_p(), // ADD HL, rp[p]
            mk(0x02, 0x81f2817c).hl_to_bc(), // LD (BC), A
            mk(0x12, 0xdb06c840).hl_to_de(), // LD (DE), A
            mk(0x0A, 0xf9d52bc1).hl_to_bc().hl_mem(0x1234), // LD A, (BC)
            mk(0x1A, 0xa32162fd).hl_to_de().hl_mem(0x1234), // LD A, (DE)
            mk(0x441122, 0x9f8737bc), // LD (0x4411), HL
            mk(0x441132, 0xd4053ee8), // LD (0x4411), A
            mk(0x44112A, 0xc28558a5).hl_mem(0x1234), // LD HL, (0x4411)
            mk(0x44113A, 0x7c7f7e07), // LD A, (0x4411)
            mk(0x03, 0xc597e625).all_p(), // INC rp[p]
            mk(0x0B, 0x7660db66).all_p(), // DEC rp[p]
            mk(0x04, 0xab3517e).y(0, 7), // INC r[y]
            mk(0x05, 0x499dc529).y(0, 7), // DEC r[y]
            mk(0x2006, 0xcdd91784).y(0, 7), // LD r[y], 0x20
            mk(0x07, 0x244d0ca2), // RLCA
            mk(0x0F, 0xa9a239b7), // RRCA
            mk(0x17, 0xf6e55742), // RLA
            mk(0x1F, 0x811179f2), // RRA
            mk(0x27, 0x27ae1abd), // DAA
            mk(0x2F, 0x5148ce04), // CPL
            mk(0x37, 0x49b08399), // SCF
            mk(0x3F, 0x67dcdcb), // CCF
            mk(0x40, 0xcb9bb6eb).y(0, 5).z(0, 7), // LD r[y], r[z]
            mk(0x40, 0x38184ac8).y(7, 7).z(0, 7), // LD r[y], r[z]
            mk(0x40, 0x6ca8fc02).y(0, 7).z(0, 5), // LD r[y], r[z]
            mk(0x40, 0x8dcdede9).y(0, 7).z(7, 7), // LD r[y], r[z]
            mk(0x80, 0x75a3c6e0).hl_mem(0x20).y(0, 7).z(0, 7), // ALU[y] r[z]
            mk(0xC0, 0x750da6d8).y(0, 7), // RET cc[y]
            mk(0xC1, 0xc425c2f2).all_p(), // POP rp2[p]
            mk(0xC9, 0x44fe3276), // RET
            mk(0xD9, 0x4c7da35d), // EXX
            mk(0xE9, 0xb898651b), // JP (HL)
            mk(0xF9, 0x21a9c873), // LD SP, HL
            mk(0x4020C2, 0xc8433837).y(0, 7), // JP cc[y], 0x2040
            mk(0x4020C3, 0x9f7eba7c), // JP 0x2040
            mk(0xE3, 0x57421926), // EX (SP), HL
            mk(0xEB, 0x295b7243), // EX DE, HL
            mk(0xF3, 0x4f43b66a), // DI
            mk(0xFB, 0x4f43b66a), // EI
            mk(0x20D3, 0x34fd8b43), // OUT (0x20), A
            mk(0x20DB, 0x4e98bcbb), // IN A, (0x20)
            mk(0x4020C4, 0x9a1c9e4a).y(0, 7), // CALL cc[y], 0x2040
            mk(0xC5, 0xca10f000).all_p(), // PUSH rp2[p]
            mk(0x4020CD, 0xdbc967ee), // CALL 0x2040
            mk(0x20C6, 0x83bda0dc).y(0, 7), // ALU[y], 0x20
            mk(0xC7, 0x8f156d7d).y(0, 7), // RST y*8

            mk(0x00CB, 0x698143a3).y(0, 7).z(0, 7), // ROT[y] r[z]
            mk(0x40CB, 0xeab2f4b0).y(0, 7).z(0, 7), // BIT y, r[z]
            mk(0x80CB, 0x55ef5e5f).y(0, 7).z(0, 7), // RES y, r[z]
            mk(0xC0CB, 0x8b940665).y(0, 7).z(0, 7), // SET y, r[z]

            mk(0x40ED, 0x750b0ff6).y(0, 7), // IN r[y], (C)
            mk(0x41ED, 0x3a3319c8).y(0, 7), // OUT (C), r[y]
            mk(0x42ED, 0x8594fc6f).all_p(), // SBC HL, rp[p]
            mk(0x4AED, 0x799953a7).all_p(), // ADC HL, rp[p]
            mk(0x204043ED, 0xeb62b193).all_p(), // LD (0x4020), rp[p]
            mk(0x20404BED, 0xcaf555af).all_p(), // LD rp[p], (0x4020)
            mk(0x44ED, 0x98e9e83f), // NEG
            mk(0x45ED, 0x44fe3276), // RETN
            mk(0x45ED, 0xde97cc1e).y(2, 7), // RETN
            mk(0x4DED, 0x44fe3276), // RETI
            mk(0x46ED, 0x3a3319c8).y(0, 7), // IM im[y]
            mk(0x47ED, 0x34fd8b43), // LD I, A
            mk(0x4FED, 0x34fd8b43), // LD R, A
            mk(0x57ED, 0x9f4b7aeb), // LD A, I
            mk(0x5FED, 0x6a66dfb1), // LD A, R
            mk(0x67ED, 0x7d7eb93d).hl_mem(0x2F), // RRD
            mk(0x6FED, 0x8cd2dc37).hl_mem(0x2F), // RLD
            mk(0x77ED, 0x34fd8b43), // NOP
            mk(0x7FED, 0x34fd8b43), // NOP
            mk(0x80ED, 0xe5555767).hl_mem(0x2F).y(4, 7).z(0, 3), // bli[y, z]

            mk(0x00DD, 0x34fd8b43), // NOP
            mk(0x08DD, 0xa007a4d), // EX AF, AF'
            mk(0x0210DD, 0xa48e8209), // DJNZ 2
            mk(0x0218DD, 0xf5a50e5d), // JR 2
            mk(0x0220DD, 0x8b9bcfc5).y(4, 7), // JR cc[y-4], 2
            mk(0x204001DD, 0x4ef83004).all_p(), // LD rpx[p], 0x4020
            mk(0x09DD, 0xc5a4aaf5).all_p(), // ADD IX, rpx[p]
            mk(0x02DD, 0xfa4cbc55).hl_to_bc(), // LD (BC), A
            mk(0x12DD, 0xa0b8f569).hl_to_de(), // LD (DE), A
            mk(0x0ADD, 0x826b16e8).hl_to_bc().hl_mem(0x1234), // LD A, (BC)
            mk(0x1ADD, 0xd89f5fd4).hl_to_de().hl_mem(0x1234), // LD A, (DE)
            mk(0x441122DD, 0x6181e97c), // LD (0x4411), IX
            mk(0x441132DD, 0x155dbbf6), // LD (0x4411), A
            mk(0x44112ADD, 0x8584a86f).hl_mem(0x1234), // LD IX, (0x4411)
            mk(0x44113ADD, 0x6d7a114b).hl_mem(0x1234), // LD A, (0x4411)
            mk(0x03DD, 0x3e100736).all_p(), // INC rpx[p]
            mk(0x0BDD, 0x8de73a75).all_p(), // DEC rpx[p]
            mk(0x04DD, 0xfa2233d6).y(0, 7), // INC rx[y]
            mk(0x05DD, 0x84ef4132).y(0, 7), // DEC rx[y]
            mk(0x2006DD, 0x2db8dd4b).y(0, 5), // LD rx[y], N
            mk(0x203EDD, 0x11251bb8), // LD a, N
            mk(0x200036DD, 0x3fd402c).hl_to_ix().hl_mem(0x3F), // LD (IX+0), 0x20
            mk(0x07DD, 0x5ff3318b), // RLCA
            mk(0x0FDD, 0xd21c049e), // RRCA
            mk(0x17DD, 0x8d5b6a6b), // RLA
            mk(0x1FDD, 0xfaaf44db), // RRA
            mk(0x27DD, 0x5c102794), // DAA
            mk(0x2FDD, 0x2af6f32d), // CPL
            mk(0x37DD, 0x320ebeb0), // SCF
            mk(0x3FDD, 0x7dc3f0e2), // CCF
            mk(0x40DD, 0xb424694a).y(0, 5).z(0, 5), // LD rx[y], rx[z]
            mk(0x40DD, 0x6e4e6fc).y(7, 7).z(0, 5), // LD rx[y], rx[z]
            mk(0x40DD, 0xb8737a6b).y(0, 5).z(7, 7), // LD rx[y], rx[z]
            mk(0x4000DD, 0x67dc5aac).hl_to_ix().hl_mem(0x3F).y(6, 6).z(0, 5), // LD (IX+0), rx[z]
            mk(0x4000DD, 0x55d7a4a).hl_to_ix().hl_mem(0x3F).y(6, 6).z(7, 7), // LD (IX+0), A
            mk(0x4000DD, 0x67dc5aac).hl_to_ix().hl_mem(0x3F).y(0, 5).z(6, 6), // LD rx[y], (IX+0)
            mk(0x4000DD, 0x55d7a4a).hl_to_ix().hl_mem(0x3F).y(7, 7).z(6, 6), // LD A, (IX+0)
            mk(0x80DD, 0xa660387e).hl_mem(0x20).y(0, 7).z(0, 5), // ALU[y], rx[z]
            mk(0x80DD, 0x3931e915).hl_mem(0x20).y(0, 7).z(7, 7), // ALU[y], A
            mk(0x8000DD, 0xa4e7c045).hl_to_ix().hl_mem(0x20).y(0, 7).z(6, 6), // ALU[y], (IX+0)
            mk(0xC0DD, 0xa4c0ec82).y(0, 7), // RET cc[y]
            mk(0xC1DD, 0x1b5c411e).all_p(), // POP rp2x[p]
            mk(0xC9DD, 0x44fe3276), // RET
            mk(0xD9DD, 0x37c39e74), // EXX
            mk(0xE9DD, 0x75b2a18), // JP (IX)
            mk(0xF9DD, 0xe2b9e4bc), // LD SP, IX
            mk(0x4020C2DD, 0x6740e7e4).y(0, 7), // JP cc[y], 0x2040
            mk(0x4020C3DD, 0x9f7eba7c), // JP 0x2040
            mk(0xE3DD, 0xce792a84), // EX (SP), IX
            mk(0xEBDD, 0x52e54f6a), // EX DE, HL
            mk(0xF3DD, 0x34fd8b43), // DI
            mk(0xFBDD, 0x34fd8b43), // EI
            mk(0x20D3DD, 0x6cdfef3e), // OUT (0x20), A
            mk(0x20DBDD, 0x16bad8c6), // IN A, (0x20)
            mk(0x4020C4DD, 0x351f4199).y(0, 7), // CALL cc[y], 0x2040
            mk(0xC5DD, 0x15a7524b).all_p(), // PUSH rp2x[p]
            mk(0x4020CDDD, 0xdbc967ee), // CALL 0x2040
            mk(0x20C6DD, 0xad3982c0).y(0, 7), // ALU[y], 0x20
            mk(0xC7DD, 0x8f156d7d).y(0, 7), // RST y*8

            mk(0x00FD, 0x34fd8b43), // NOP
            mk(0x08FD, 0xa007a4d), // EX AF, AF'
            mk(0x0210FD, 0xa48e8209), // DJNZ 2
            mk(0x0218FD, 0xf5a50e5d), // JR 2
            mk(0x0220FD, 0x8b9bcfc5).y(4, 7), // JR cc[y-4], 2
            mk(0x204001FD, 0x4ef83004).all_p(), // LD rpy[p], 0x4020
            mk(0x09FD, 0x3e95b0dd).all_p(), // ADD IY, rpy[p]
            mk(0x02FD, 0xfa4cbc55).hl_to_bc().hl_mem(0x20), // LD (BC), A
            mk(0x12FD, 0xa0b8f569).hl_to_de().hl_mem(0x20), // LD (DE), A
            mk(0x0AFD, 0x16ad4a47).hl_to_bc().hl_mem(0x20), // LD A, (BC)
            mk(0x1AFD, 0x4c59037b).hl_to_de().hl_mem(0x20), // LD A, (DE)
            mk(0x441122FD, 0x9c46b95a), // LD (0x4411), IY
            mk(0x441132FD, 0x155dbbf6), // LD (0x4411), A
            mk(0x44112AFD, 0xc8da9877).hl_mem(0x1234), // LD IY, (0x4411)
            mk(0x44113AFD, 0x6d7a114b).hl_mem(0x1234), // LD A, (0x4411)
            mk(0x03FD, 0x903b0268).all_p(), // INC rpy[p]
            mk(0x0BFD, 0x23cc3f2b).all_p(), // DEC rpy[p]
            mk(0x04FD, 0xb685baf).y(0, 7), // INC ry[y]
            mk(0x05FD, 0xc4ef58b5).y(0, 7), // DEC ry[y]
            mk(0x2006FD, 0x2db8dd4b).y(0, 5), // LD ry[y], N
            mk(0x203EFD, 0x11251bb8), // LD a, N
            mk(0x200036FD, 0x92b41b2d).hl_to_iy().hl_mem(0x3F), // LD (IY+0), 0x20
            mk(0x07FD, 0x5ff3318b), // RLCA
            mk(0x0FFD, 0xd21c049e), // RRCA
            mk(0x17FD, 0x8d5b6a6b), // RLA
            mk(0x1FFD, 0xfaaf44db), // RRA
            mk(0x27FD, 0x5c102794), // DAA
            mk(0x2FFD, 0x2af6f32d), // CPL
            mk(0x37FD, 0x320ebeb0), // SCF
            mk(0x3FFD, 0x7dc3f0e2), // CCF
            mk(0x40FD, 0x39c76988).y(0, 5).z(0, 5), // LD ry[y], ry[z]
            mk(0x40FD, 0xbc8753bd).y(7, 7).z(0, 5), // LD ry[y], ry[z]
            mk(0x40FD, 0x474fdbe8).y(0, 5).z(7, 7), // LD ry[y], ry[z]
            mk(0x4000FD, 0x25039149).hl_to_iy().hl_mem(0x3F).y(6, 6).z(0, 5), // LD (IY+0), ry[z]
            mk(0x4000FD, 0x9414214b).hl_to_iy().hl_mem(0x3F).y(6, 6).z(7, 7), // LD (IY+0), A
            mk(0x4000FD, 0x25039149).hl_to_iy().hl_mem(0x3F).y(0, 5).z(6, 6), // LD ry[y], (IY+0)
            mk(0x4000FD, 0x9414214b).hl_to_iy().hl_mem(0x3F).y(7, 7).z(6, 6), // LD A, (IY+0)
            mk(0x80FD, 0x808ba661).hl_mem(0x20).y(0, 7).z(0, 5), // ALU[y], ry[z]
            mk(0x80FD, 0x3931e915).hl_mem(0x20).y(0, 7).z(7, 7), // ALU[y], A
            mk(0x8000FD, 0x4fb2a057).hl_to_iy().hl_mem(0x20).y(0, 7).z(6, 6), // ALU[y], (IY+0)
            mk(0xC0FD, 0xa4c0ec82).y(0, 7), // RET cc[y]
            mk(0xC1FD, 0x38906d13).all_p(), // POP rp2y[p]
            mk(0xC9FD, 0x44fe3276), // RET
            mk(0xD9FD, 0x37c39e74), // EXX
            mk(0xE9FD, 0x6c06b1fa), // JP (IY)
            mk(0xF9FD, 0xa09c9fdb), // LD SP, IY
            mk(0x4020C2FD, 0x6740e7e4).y(0, 7), // JP cc[y], 0x2040
            mk(0x4020C3FD, 0x9f7eba7c), // JP 0x2040
            mk(0xE3FD, 0xc20e919e), // EX (SP), IY
            mk(0xEBFD, 0x52e54f6a), // EX DE, HL
            mk(0xF3FD, 0x34fd8b43), // DI
            mk(0xFBFD, 0x34fd8b43), // EI
            mk(0x20D3FD, 0x6cdfef3e), // OUT (0x20), A
            mk(0x20DBFD, 0x16bad8c6), // IN A, (0x20)
            mk(0x4020C4FD, 0x351f4199).y(0, 7), // CALL cc[y], 0x2040
            mk(0xC5FD, 0x15a7524b).all_p(), // PUSH rp2y[p]
            mk(0x4020CDFD, 0xdbc967ee), // CALL 0x2040
            mk(0x20C6FD, 0xad3982c0).y(0, 7), // ALU[y], 0x20
            mk(0xC7FD, 0x8f156d7d).y(0, 7), // RST y*8

            mk(0x0000CBDD, 0x4074cb3).hl_to_ix().hl_mem(0x20).y(0, 7).z(0, 5),  // LD r[z], rot[y] (IX+0)
            mk(0x0000CBDD, 0xb5e1b6e5).hl_to_ix().hl_mem(0x20).y(0, 7).z(7, 7),  // LD r[z], rot[y] (IX+0)
            mk(0x0600CBDD, 0xdd8ba49f).hl_to_ix().hl_mem(0x20).y(0, 7),  // rot[y] (IX+0)
            mk(0x4000CBDD, 0xefa606ad).hl_to_ix().hl_mem(0xAA).y(0, 7).z(0, 7), // BIT y, (IX+0)
            mk(0x8000CBDD, 0x9999c088).hl_to_ix().hl_mem(0xAA).y(0, 7).z(0, 5), // LD r[z], RES y, (IX+0)
            mk(0x8700CBDD, 0xbcfac0c8).hl_to_ix().hl_mem(0xAA).y(0, 7), // LD A, RES y, (IX+0)
            mk(0x8600CBDD, 0x9135f49b).hl_to_ix().hl_mem(0xAA).y(0, 7), // RES y, (IX+0)
            mk(0xC000CBDD, 0x891c1517).hl_to_ix().y(0, 7).z(0, 5), // LD r[z], SET y, (IX+0)
            mk(0xC600CBDD, 0x82698d7e).hl_to_ix().y(0, 7), // SET y, (IX+d)
            mk(0xC700CBDD, 0x47fd2fab).hl_to_ix().y(0, 7), // LD A, SET y, (IX+0)

            mk(0x0000CBFD, 0x4118ee7).hl_to_iy().hl_mem(0x20).y(0, 7).z(0, 5),  // LD r[z], rot[y] (IX+0)
            mk(0x0000CBFD, 0x5eb4d6f7).hl_to_iy().hl_mem(0x20).y(0, 7).z(7, 7),  // LD r[z], rot[y] (IX+0)
            mk(0x0600CBFD, 0x36dec48d).hl_to_iy().hl_mem(0x20).y(0, 7),  // rot[y] (IX+0)
            mk(0x4000CBFD, 0x75efe92).hl_to_iy().hl_mem(0xAA).y(0, 7).z(0, 7), // BIT y, (IX+0)
            mk(0x8000CBFD, 0x92e9f47d).hl_to_iy().hl_mem(0xAA).y(0, 7).z(0, 5), // LD r[z], RES y, (IX+0)
            mk(0x8700CBFD, 0x57afa0da).hl_to_iy().hl_mem(0xAA).y(0, 7), // LD A, RES y, (IX+0)
            mk(0x8600CBFD, 0x7a609489).hl_to_iy().hl_mem(0xAA).y(0, 7), // RES y, (IX+0)
            mk(0xC000CBFD, 0x826c21e2).hl_to_iy().y(0, 7).z(0, 5), // LD r[z], SET y, (IX+0)
            mk(0xC600CBFD, 0x693ced6c).hl_to_iy().y(0, 7), // SET y, (IX+d)
            mk(0xC700CBFD, 0xaca84fb9).hl_to_iy().y(0, 7) // LD A, SET y, (IX+0)
    );

    @Test
    public void testInstructions() {
        testData.forEach(this::testInstruction);
    }

    public void testInstruction(InstrTest instrTest) {
        // reg A is repeated with TEST_VALUES
        // flags are flipped from 0 and 0xFF
        final AtomicInteger crc = new AtomicInteger();
        for (int a : TEST_VALUES) {
            ByteTestBuilder test = new ByteTestBuilder(cpuRunnerImpl, cpuVerifierImpl)
                    .firstIsFlags()
                    .setRegister(REG_A, a)
                    .setPair(REG_PAIR_BC, instrTest.bc)
                    .setPair(REG_PAIR_DE, instrTest.de)
                    .setPair(REG_PAIR_HL, instrTest.hl)
                    .setIX(instrTest.ix)
                    .setIY(instrTest.iy)
                    .setSP(instrTest.sp)
                    .setMemoryByteAt(instrTest.hl, (byte) instrTest.hl_mem)
                    .verify(context -> crc.set(updateCrc(crc.get(), instrTest.hl)))
                    .keepCurrentInjectorsAfterRun()
                    .clearOtherVerifiersAfterRun(); // keeps current ones

            int y_from = instrTest.instr_y_from;
            int y_to = instrTest.instr_y_to;
            int y_mask = 7;
            if (instrTest.instr_p_to > 0) {
                y_from = instrTest.instr_p_from << 1;
                y_to = instrTest.instr_p_to << 1;
                y_mask = 6;
            }

            int[] opcodes = new int[5];
            int opcode = instrTest.opcode;
            int lastOpcodeIndex = 0;
            while (opcode != 0) {
                opcodes[lastOpcodeIndex++] = opcode & 0xFF;
                opcode = opcode >>> 8;
            }
            if (lastOpcodeIndex > 0) {
                lastOpcodeIndex--;
            }

            for (int y = y_from; y <= y_to; y++) {
                for (int z = instrTest.instr_z_from; z <= instrTest.instr_z_to; z++) {
                    int yreal = y & y_mask;
                    if (yreal > 0) {
                        opcodes[lastOpcodeIndex] = (opcodes[lastOpcodeIndex] & 0xC7) | ((yreal & 7) << 3);
                    }
                    if (z > 0) {
                        opcodes[lastOpcodeIndex] = (opcodes[lastOpcodeIndex] & 0xF8) | (z & 7);
                    }
                    test.run(opcodes[0], opcodes[1], opcodes[2], opcodes[3], opcodes[4]).accept((byte) 0, (byte) 0); // flags 00
                    test.run(opcodes[0], opcodes[1], opcodes[2], opcodes[3], opcodes[4]).accept((byte) 0xFF, (byte) 0); // flags 0xFF
                }
            }
        }
   //     System.out.println("0x" + Integer.toHexString(crc.get()));
        assertEquals(instrTest.crc, crc.get());
    }

    public int updateCrc(int crc, int rHL) {
        EmulatorEngine engine = cpu.getEngine();

        int rrAF = (engine.regs[REG_A] << 8) | engine.flags;
        int rrBC = (engine.regs[REG_B] << 8) | engine.regs[REG_C];
        int rrDE = (engine.regs[REG_D] << 8) | engine.regs[REG_E];
        int rrHL = (engine.regs[REG_H] << 8) | engine.regs[REG_L];
        int rrIX = engine.IX;
        int rrIY = engine.IY;
        int rrPC = engine.PC;
        int rrSP = engine.SP;
        int rrMemHL = memory.read(rHL) & 0xFF;

        return (int) crc16(new byte[]{
                (byte) ((crc >>> 8) & 0xFF),
                (byte) ((crc) & 0xFF),
                (byte) ((rrAF >>> 8) & 0xFF),
                (byte) ((rrAF) & 0xFF),
                (byte) ((rrBC >>> 8) & 0xFF),
                (byte) ((rrBC) & 0xFF),
                (byte) ((rrDE >>> 8) & 0xFF),
                (byte) ((rrDE) & 0xFF),
                (byte) ((rrHL >>> 8) & 0xFF),
                (byte) ((rrHL) & 0xFF),
                (byte) ((rrIX >>> 8) & 0xFF),
                (byte) ((rrIX) & 0xFF),
                (byte) ((rrIY >>> 8) & 0xFF),
                (byte) ((rrIY) & 0xFF),
                (byte) ((rrPC >>> 8) & 0xFF),
                (byte) ((rrPC) & 0xFF),
                (byte) ((rrSP >>> 8) & 0xFF),
                (byte) ((rrSP) & 0xFF),
                (byte) ((rrMemHL) & 0xFF)
        });
    }

}
