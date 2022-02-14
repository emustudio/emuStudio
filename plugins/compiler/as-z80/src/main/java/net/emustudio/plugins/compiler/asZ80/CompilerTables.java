package net.emustudio.plugins.compiler.asZ80;

import java.util.HashMap;
import java.util.Map;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;

//http://www.z80.info/decoding.htm
public class CompilerTables {

    public final static Map<Integer, Integer> registers = new HashMap<>();
    public final static Map<Integer, Integer> regPairs = new HashMap<>();
    public final static Map<Integer, Integer> regPairsII = new HashMap<>();
    public final static Map<Integer, Integer> regPairs2 = new HashMap<>();
    public final static Map<Integer, Integer> conditions = new HashMap<>();
    public final static Map<Integer, Integer> alu = new HashMap<>();
    public final static Map<Integer, Integer> rot = new HashMap<>();
    public final static Map<Integer, Integer> im = new HashMap<>();
    public final static Map<Integer, Pair<Integer, Integer>> block = new HashMap<>();
    public final static Map<Integer, Integer> prefix = Map.of(
        REG_IX, 0xDD,
        REG_IXH, 0xDD,
        REG_IXL, 0xDD,
        REG_IY, 0xFD,
        REG_IYH, 0xFD,
        REG_IYL, 0xFD
    );

    static {
        registers.put(REG_B, 0);
        registers.put(REG_C, 1);
        registers.put(REG_D, 2);
        registers.put(REG_E, 3);
        registers.put(REG_H, 4);
        registers.put(REG_IXH, 4);
        registers.put(REG_IYH, 4);
        registers.put(REG_L, 5);
        registers.put(REG_IXL, 5);
        registers.put(REG_IYL, 5);
        registers.put(REG_HL, 6);
        registers.put(REG_A, 7);

        regPairs.put(REG_BC, 0);
        regPairs.put(REG_DE, 1);
        regPairs.put(REG_HL, 2);
        regPairs.put(REG_SP, 3);

        regPairsII.put(REG_BC, 0);
        regPairsII.put(REG_DE, 1);
        regPairsII.put(REG_IX, 2);
        regPairsII.put(REG_IY, 2);
        regPairsII.put(REG_SP, 3);

        regPairs2.put(REG_BC, 0);
        regPairs2.put(REG_DE, 1);
        regPairs2.put(REG_HL, 2);
        regPairs2.put(REG_AF, 3);

        conditions.put(COND_NZ, 0);
        conditions.put(COND_Z, 1);
        conditions.put(COND_NC, 2);
        conditions.put(COND_C, 3);
        conditions.put(COND_PO, 4);
        conditions.put(COND_PE, 5);
        conditions.put(COND_P, 6);
        conditions.put(COND_M, 7);

        alu.put(OPCODE_ADD, 0);
        alu.put(OPCODE_ADC, 1);
        alu.put(OPCODE_SUB, 2);
        alu.put(OPCODE_SBC, 3);
        alu.put(OPCODE_AND, 4);
        alu.put(OPCODE_XOR, 5);
        alu.put(OPCODE_OR, 6);
        alu.put(OPCODE_CP, 7);

        rot.put(OPCODE_RLC, 0);
        rot.put(OPCODE_RRC, 1);
        rot.put(OPCODE_RL, 2);
        rot.put(OPCODE_RR, 3);
        rot.put(OPCODE_SLA, 4);
        rot.put(OPCODE_SRA, 5);
        rot.put(OPCODE_SLL, 6);
        rot.put(OPCODE_SRL, 7);

        im.put(IM_0, 0);
        im.put(IM_01, 1);
        im.put(IM_1, 2);
        im.put(IM_2, 3);

        block.put(OPCODE_LDI, Pair.of(4, 0));
        block.put(OPCODE_LDD, Pair.of(5, 0));
        block.put(OPCODE_LDIR, Pair.of(6, 0));
        block.put(OPCODE_LDDR, Pair.of(7, 0));
        block.put(OPCODE_CPI, Pair.of(4, 1));
        block.put(OPCODE_CPD, Pair.of(5, 1));
        block.put(OPCODE_CPIR, Pair.of(6, 1));
        block.put(OPCODE_CPDR, Pair.of(7, 1));
        block.put(OPCODE_INI, Pair.of(4, 2));
        block.put(OPCODE_IND, Pair.of(5, 2));
        block.put(OPCODE_INIR, Pair.of(6, 2));
        block.put(OPCODE_INDR, Pair.of(7, 2));
        block.put(OPCODE_OUTI, Pair.of(4, 3));
        block.put(OPCODE_OUTD, Pair.of(5, 3));
        block.put(OPCODE_OTIR, Pair.of(6, 3));
        block.put(OPCODE_OTDR, Pair.of(7, 3));
    }


}
