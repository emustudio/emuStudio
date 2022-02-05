package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.emulib.runtime.io.IntelHEX;
import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.Program;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDB;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDS;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDW;
import net.emustudio.plugins.compiler.asZ80.ast.instr.*;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoMacroCall;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoOrg;
import org.junit.Test;

import java.util.Map;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;
import static org.junit.Assert.assertEquals;

public class GenerateCodeVisitorTest {

    @Test
    public void testCodeGeneration() {
        Program program = new Program();
        program
            .addChild(new DataDB(0, 0)
                .addChild(new Evaluated(0, 0, 255))
                .addChild(new InstrExpr(0, 0, OPCODE_RST)
                    .addChild(new Evaluated(0, 0, 4))))
            .addChild(new DataDW(0, 0)
                .addChild(new Evaluated(0, 0, 1)))
            .addChild(new DataDS(0, 0)
                .addChild(new Evaluated(0, 0, 5)))
            .addChild(new PseudoMacroCall(0, 0, "x")
                .addChild(new InstrRegPairExpr(0, 0, OPCODE_LXI, REG_B)
                    .addChild(new Evaluated(0, 0, 0xFEAB)))
                .addChild(new PseudoMacroCall(0, 0, "y")
                    .addChild(new InstrRegPairExpr(0, 0, OPCODE_LXI, REG_D)
                        .addChild(new Evaluated(0, 0, 1))))
                .addChild(new InstrRegPairExpr(0, 0, OPCODE_LXI, REG_H)
                    .addChild(new Evaluated(0, 0, 0x1234))));

        IntelHEX hex = new IntelHEX();
        GenerateCodeVisitor visitor = new GenerateCodeVisitor(hex);
        visitor.visit(program);
        Map<Integer, Byte> code = hex.getCode();

        assertEquals((byte) 255, code.get(0).byteValue());
        assertEquals((byte) 0xe7, code.get(1).byteValue());
        assertEquals(1, code.get(2).byteValue()); // dw - lower byte
        assertEquals(0, code.get(3).byteValue()); // dw - upper byte
        assertEquals(0, code.get(4).byteValue());
        assertEquals(0, code.get(5).byteValue());
        assertEquals(0, code.get(6).byteValue());
        assertEquals(0, code.get(7).byteValue());
        assertEquals(0, code.get(8).byteValue());
        assertEquals(1, code.get(9).byteValue()); // lxi b
        assertEquals((byte) 0xAB, code.get(10).byteValue());
        assertEquals((byte) 0xFE, code.get(11).byteValue());
        assertEquals(0x11, code.get(12).byteValue()); // lxi d
        assertEquals(1, code.get(13).byteValue());
        assertEquals(0, code.get(14).byteValue());
        assertEquals(0x21, code.get(15).byteValue()); // lxi h
        assertEquals(0x34, code.get(16).byteValue());
        assertEquals(0x12, code.get(17).byteValue());
    }

    @Test
    public void testPseudoOrg() {
        Program program = new Program();
        program
            .addChild(new PseudoOrg(0, 0)
                .addChild(new Evaluated(0, 0, 5)))
            .addChild(new InstrExpr(0, 0, OPCODE_CNZ)
                .addChild(new Evaluated(0, 0, 0x400)))
            .addChild(new PseudoOrg(0, 0)
                .addChild(new Evaluated(0, 0, 0)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_XCHG));

        IntelHEX hex = new IntelHEX();
        GenerateCodeVisitor visitor = new GenerateCodeVisitor(hex);
        visitor.visit(program);
        Map<Integer, Byte> code = hex.getCode();

        assertEquals((byte) 0xeb, code.get(0).byteValue());
        assertEquals((byte) 0xc4, code.get(5).byteValue());
        assertEquals(0, code.get(6).byteValue());
        assertEquals(4, code.get(7).byteValue());
    }

    @Test
    public void testGenerateInstructions() {
        Program program = new Program();
        program
            .addChild(new InstrNoArgs(0, 0, OPCODE_NOP))
            .addChild(new InstrRegPairExpr(0, 0, OPCODE_LXI, REG_B)
                .addChild(new Evaluated(0, 0, 0x1234)))
            .addChild(new InstrRegPair(0, 0, OPCODE_STAX, REG_B))
            .addChild(new InstrRegPair(0, 0, OPCODE_INX, REG_B))
            .addChild(new InstrReg(0, 0, OPCODE_INR, REG_B))
            .addChild(new InstrReg(0, 0, OPCODE_DCR, REG_B))
            .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_B)
                .addChild(new Evaluated(0, 0, 7)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_RLC))
            .addChild(new InstrRegPair(0, 0, OPCODE_DAD, REG_B))
            .addChild(new InstrRegPair(0, 0, OPCODE_LDAX, REG_B))
            .addChild(new InstrRegPair(0, 0, OPCODE_DCX, REG_B))
            .addChild(new InstrReg(0, 0, OPCODE_INR, REG_C))
            .addChild(new InstrReg(0, 0, OPCODE_DCR, REG_C))
            .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_C)
                .addChild(new Evaluated(0, 0, 8)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_RRC))
            .addChild(new InstrRegPairExpr(0, 0, OPCODE_LXI, REG_D)
                .addChild(new Evaluated(0, 0, 0x2345)))
            .addChild(new InstrRegPair(0, 0, OPCODE_STAX, REG_D))
            .addChild(new InstrRegPair(0, 0, OPCODE_INX, REG_D))
            .addChild(new InstrReg(0, 0, OPCODE_INR, REG_D))
            .addChild(new InstrReg(0, 0, OPCODE_DCR, REG_D))
            .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_D)
                .addChild(new Evaluated(0, 0, 9)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_RAL))
            .addChild(new InstrRegPair(0, 0, OPCODE_DAD, REG_D))
            .addChild(new InstrRegPair(0, 0, OPCODE_LDAX, REG_D))
            .addChild(new InstrRegPair(0, 0, OPCODE_DCX, REG_D))
            .addChild(new InstrReg(0, 0, OPCODE_INR, REG_E))
            .addChild(new InstrReg(0, 0, OPCODE_DCR, REG_E))
            .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_E)
                .addChild(new Evaluated(0, 0, 10)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_RAR))
            .addChild(new InstrRegPairExpr(0, 0, OPCODE_LXI, REG_H)
                .addChild(new Evaluated(0, 0, 0x2345)))
            .addChild(new InstrExpr(0, 0, OPCODE_SHLD)
                .addChild(new Evaluated(0, 0, 0x2345)))
            .addChild(new InstrRegPair(0, 0, OPCODE_INX, REG_H))
            .addChild(new InstrReg(0, 0, OPCODE_INR, REG_H))
            .addChild(new InstrReg(0, 0, OPCODE_DCR, REG_H))
            .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_H)
                .addChild(new Evaluated(0, 0, 0x28)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_DAA))
            .addChild(new InstrRegPair(0, 0, OPCODE_DAD, REG_H))
            .addChild(new InstrExpr(0, 0, OPCODE_LHLD)
                .addChild(new Evaluated(0, 0, 0x2345)))
            .addChild(new InstrRegPair(0, 0, OPCODE_DCX, REG_H))
            .addChild(new InstrReg(0, 0, OPCODE_INR, REG_L))
            .addChild(new InstrReg(0, 0, OPCODE_DCR, REG_L))
            .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_L)
                .addChild(new Evaluated(0, 0, 10)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_CMA))
            .addChild(new InstrRegPairExpr(0, 0, OPCODE_LXI, REG_SP)
                .addChild(new Evaluated(0, 0, 0x2345)))
            .addChild(new InstrExpr(0, 0, OPCODE_STA)
                .addChild(new Evaluated(0, 0, 0x2345)))
            .addChild(new InstrRegPair(0, 0, OPCODE_INX, REG_SP))
            .addChild(new InstrReg(0, 0, OPCODE_INR, REG_M))
            .addChild(new InstrReg(0, 0, OPCODE_DCR, REG_M))
            .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_M)
                .addChild(new Evaluated(0, 0, 7)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_STC))
            .addChild(new InstrRegPair(0, 0, OPCODE_DAD, REG_SP))
            .addChild(new InstrExpr(0, 0, OPCODE_LDA)
                .addChild(new Evaluated(0, 0, 0x2345)))
            .addChild(new InstrRegPair(0, 0, OPCODE_DCX, REG_SP))
            .addChild(new InstrReg(0, 0, OPCODE_INR, REG_A))
            .addChild(new InstrReg(0, 0, OPCODE_DCR, REG_A))
            .addChild(new InstrRegExpr(0, 0, OPCODE_MVI, REG_A)
                .addChild(new Evaluated(0, 0, -128)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_CMC))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_B, REG_B))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_B, REG_C))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_B, REG_D))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_B, REG_E))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_B, REG_H))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_B, REG_L))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_B, REG_M))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_B, REG_A))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_C, REG_B))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_C, REG_C))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_C, REG_D))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_C, REG_E))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_C, REG_H))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_C, REG_L))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_C, REG_M))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_C, REG_A))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_D, REG_B))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_D, REG_C))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_D, REG_D))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_D, REG_E))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_D, REG_H))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_D, REG_L))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_D, REG_M))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_D, REG_A))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_E, REG_B))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_E, REG_C))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_E, REG_D))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_E, REG_E))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_E, REG_H))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_E, REG_L))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_E, REG_M))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_E, REG_A))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_H, REG_B))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_H, REG_C))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_H, REG_D))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_H, REG_E))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_H, REG_H))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_H, REG_L))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_H, REG_M))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_H, REG_A))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_L, REG_B))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_L, REG_C))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_L, REG_D))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_L, REG_E))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_L, REG_H))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_L, REG_L))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_L, REG_M))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_L, REG_A))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_M, REG_B))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_M, REG_C))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_M, REG_D))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_M, REG_E))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_M, REG_H))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_M, REG_L))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_M, REG_M)) // HLT
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_M, REG_A))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_A, REG_B))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_A, REG_C))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_A, REG_D))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_A, REG_E))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_A, REG_H))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_A, REG_L))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_A, REG_M))
            .addChild(new InstrRegReg(0, 0, OPCODE_MOV, REG_A, REG_A))
            .addChild(new InstrReg(0, 0, OPCODE_ADD, REG_B))
            .addChild(new InstrReg(0, 0, OPCODE_ADD, REG_C))
            .addChild(new InstrReg(0, 0, OPCODE_ADD, REG_D))
            .addChild(new InstrReg(0, 0, OPCODE_ADD, REG_E))
            .addChild(new InstrReg(0, 0, OPCODE_ADD, REG_H))
            .addChild(new InstrReg(0, 0, OPCODE_ADD, REG_L))
            .addChild(new InstrReg(0, 0, OPCODE_ADD, REG_M))
            .addChild(new InstrReg(0, 0, OPCODE_ADD, REG_A))
            .addChild(new InstrReg(0, 0, OPCODE_ADC, REG_B))
            .addChild(new InstrReg(0, 0, OPCODE_ADC, REG_C))
            .addChild(new InstrReg(0, 0, OPCODE_ADC, REG_D))
            .addChild(new InstrReg(0, 0, OPCODE_ADC, REG_E))
            .addChild(new InstrReg(0, 0, OPCODE_ADC, REG_H))
            .addChild(new InstrReg(0, 0, OPCODE_ADC, REG_L))
            .addChild(new InstrReg(0, 0, OPCODE_ADC, REG_M))
            .addChild(new InstrReg(0, 0, OPCODE_ADC, REG_A))
            .addChild(new InstrReg(0, 0, OPCODE_SUB, REG_B))
            .addChild(new InstrReg(0, 0, OPCODE_SUB, REG_C))
            .addChild(new InstrReg(0, 0, OPCODE_SUB, REG_D))
            .addChild(new InstrReg(0, 0, OPCODE_SUB, REG_E))
            .addChild(new InstrReg(0, 0, OPCODE_SUB, REG_H))
            .addChild(new InstrReg(0, 0, OPCODE_SUB, REG_L))
            .addChild(new InstrReg(0, 0, OPCODE_SUB, REG_M))
            .addChild(new InstrReg(0, 0, OPCODE_SUB, REG_A))
            .addChild(new InstrReg(0, 0, OPCODE_SBB, REG_B))
            .addChild(new InstrReg(0, 0, OPCODE_SBB, REG_C))
            .addChild(new InstrReg(0, 0, OPCODE_SBB, REG_D))
            .addChild(new InstrReg(0, 0, OPCODE_SBB, REG_E))
            .addChild(new InstrReg(0, 0, OPCODE_SBB, REG_H))
            .addChild(new InstrReg(0, 0, OPCODE_SBB, REG_L))
            .addChild(new InstrReg(0, 0, OPCODE_SBB, REG_M))
            .addChild(new InstrReg(0, 0, OPCODE_SBB, REG_A))
            .addChild(new InstrReg(0, 0, OPCODE_ANA, REG_B))
            .addChild(new InstrReg(0, 0, OPCODE_ANA, REG_C))
            .addChild(new InstrReg(0, 0, OPCODE_ANA, REG_D))
            .addChild(new InstrReg(0, 0, OPCODE_ANA, REG_E))
            .addChild(new InstrReg(0, 0, OPCODE_ANA, REG_H))
            .addChild(new InstrReg(0, 0, OPCODE_ANA, REG_L))
            .addChild(new InstrReg(0, 0, OPCODE_ANA, REG_M))
            .addChild(new InstrReg(0, 0, OPCODE_ANA, REG_A))
            .addChild(new InstrReg(0, 0, OPCODE_XRA, REG_B))
            .addChild(new InstrReg(0, 0, OPCODE_XRA, REG_C))
            .addChild(new InstrReg(0, 0, OPCODE_XRA, REG_D))
            .addChild(new InstrReg(0, 0, OPCODE_XRA, REG_E))
            .addChild(new InstrReg(0, 0, OPCODE_XRA, REG_H))
            .addChild(new InstrReg(0, 0, OPCODE_XRA, REG_L))
            .addChild(new InstrReg(0, 0, OPCODE_XRA, REG_M))
            .addChild(new InstrReg(0, 0, OPCODE_XRA, REG_A))
            .addChild(new InstrReg(0, 0, OPCODE_ORA, REG_B))
            .addChild(new InstrReg(0, 0, OPCODE_ORA, REG_C))
            .addChild(new InstrReg(0, 0, OPCODE_ORA, REG_D))
            .addChild(new InstrReg(0, 0, OPCODE_ORA, REG_E))
            .addChild(new InstrReg(0, 0, OPCODE_ORA, REG_H))
            .addChild(new InstrReg(0, 0, OPCODE_ORA, REG_L))
            .addChild(new InstrReg(0, 0, OPCODE_ORA, REG_M))
            .addChild(new InstrReg(0, 0, OPCODE_ORA, REG_A))
            .addChild(new InstrReg(0, 0, OPCODE_CMP, REG_B))
            .addChild(new InstrReg(0, 0, OPCODE_CMP, REG_C))
            .addChild(new InstrReg(0, 0, OPCODE_CMP, REG_D))
            .addChild(new InstrReg(0, 0, OPCODE_CMP, REG_E))
            .addChild(new InstrReg(0, 0, OPCODE_CMP, REG_H))
            .addChild(new InstrReg(0, 0, OPCODE_CMP, REG_L))
            .addChild(new InstrReg(0, 0, OPCODE_CMP, REG_M))
            .addChild(new InstrReg(0, 0, OPCODE_CMP, REG_A))
            .addChild(new InstrNoArgs(0, 0, OPCODE_RNZ))
            .addChild(new InstrRegPair(0, 0, OPCODE_POP, REG_B))
            .addChild(new InstrExpr(0, 0, OPCODE_JNZ)
                .addChild(new Evaluated(0, 0, 2)))
            .addChild(new InstrExpr(0, 0, OPCODE_JMP)
                .addChild(new Evaluated(0, 0, 0x200)))
            .addChild(new InstrExpr(0, 0, OPCODE_CNZ)
                .addChild(new Evaluated(0, 0, 0xF0A0)))
            .addChild(new InstrRegPair(0, 0, OPCODE_PUSH, REG_B))
            .addChild(new InstrExpr(0, 0, OPCODE_ADI)
                .addChild(new Evaluated(0, 0, 0xF0)))
            .addChild(new InstrExpr(0, 0, OPCODE_RST)
                .addChild(new Evaluated(0, 0, 0)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_RZ))
            .addChild(new InstrNoArgs(0, 0, OPCODE_RET))
            .addChild(new InstrExpr(0, 0, OPCODE_JZ)
                .addChild(new Evaluated(0, 0, 0x1234)))
            .addChild(new InstrExpr(0, 0, OPCODE_CZ)
                .addChild(new Evaluated(0, 0, 0x1234)))
            .addChild(new InstrExpr(0, 0, OPCODE_CALL)
                .addChild(new Evaluated(0, 0, 0x1234)))
            .addChild(new InstrExpr(0, 0, OPCODE_ACI)
                .addChild(new Evaluated(0, 0, 0xF0)))
            .addChild(new InstrExpr(0, 0, OPCODE_RST)
                .addChild(new Evaluated(0, 0, 1)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_RNC))
            .addChild(new InstrRegPair(0, 0, OPCODE_POP, REG_D))
            .addChild(new InstrExpr(0, 0, OPCODE_JNC)
                .addChild(new Evaluated(0, 0, 0x1234)))
            .addChild(new InstrExpr(0, 0, OPCODE_OUT)
                .addChild(new Evaluated(0, 0, 0xF0)))
            .addChild(new InstrExpr(0, 0, OPCODE_CNC)
                .addChild(new Evaluated(0, 0, 0x1234)))
            .addChild(new InstrRegPair(0, 0, OPCODE_PUSH, REG_D))
            .addChild(new InstrExpr(0, 0, OPCODE_SUI)
                .addChild(new Evaluated(0, 0, 0xF0)))
            .addChild(new InstrExpr(0, 0, OPCODE_RST)
                .addChild(new Evaluated(0, 0, 2)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_RC))
            .addChild(new InstrExpr(0, 0, OPCODE_JC)
                .addChild(new Evaluated(0, 0, 0x1234)))
            .addChild(new InstrExpr(0, 0, OPCODE_IN)
                .addChild(new Evaluated(0, 0, 0xF0)))
            .addChild(new InstrExpr(0, 0, OPCODE_CC)
                .addChild(new Evaluated(0, 0, 0x1234)))
            .addChild(new InstrExpr(0, 0, OPCODE_SBI)
                .addChild(new Evaluated(0, 0, 0xF0)))
            .addChild(new InstrExpr(0, 0, OPCODE_RST)
                .addChild(new Evaluated(0, 0, 3)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_RPO))
            .addChild(new InstrRegPair(0, 0, OPCODE_POP, REG_H))
            .addChild(new InstrExpr(0, 0, OPCODE_JPO)
                .addChild(new Evaluated(0, 0, 0x1234)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_XTHL))
            .addChild(new InstrExpr(0, 0, OPCODE_CPO)
                .addChild(new Evaluated(0, 0, 0x1234)))
            .addChild(new InstrRegPair(0, 0, OPCODE_PUSH, REG_H))
            .addChild(new InstrExpr(0, 0, OPCODE_ANI)
                .addChild(new Evaluated(0, 0, 0xF0)))
            .addChild(new InstrExpr(0, 0, OPCODE_RST)
                .addChild(new Evaluated(0, 0, 4)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_RPE))
            .addChild(new InstrNoArgs(0, 0, OPCODE_PCHL))
            .addChild(new InstrExpr(0, 0, OPCODE_JPE)
                .addChild(new Evaluated(0, 0, 0x1234)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_XCHG))
            .addChild(new InstrExpr(0, 0, OPCODE_CPE)
                .addChild(new Evaluated(0, 0, 0x1234)))
            .addChild(new InstrExpr(0, 0, OPCODE_XRI)
                .addChild(new Evaluated(0, 0, 0xF0)))
            .addChild(new InstrExpr(0, 0, OPCODE_RST)
                .addChild(new Evaluated(0, 0, 5)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_RP))
            .addChild(new InstrRegPair(0, 0, OPCODE_POP, REG_PSW))
            .addChild(new InstrExpr(0, 0, OPCODE_JP)
                .addChild(new Evaluated(0, 0, 0x200)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_DI))
            .addChild(new InstrExpr(0, 0, OPCODE_CP)
                .addChild(new Evaluated(0, 0, 0x200)))
            .addChild(new InstrRegPair(0, 0, OPCODE_PUSH, REG_PSW))
            .addChild(new InstrExpr(0, 0, OPCODE_ORI)
                .addChild(new Evaluated(0, 0, 0xF0)))
            .addChild(new InstrExpr(0, 0, OPCODE_RST)
                .addChild(new Evaluated(0, 0, 6)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_RM))
            .addChild(new InstrNoArgs(0, 0, OPCODE_SPHL))
            .addChild(new InstrExpr(0, 0, OPCODE_JM)
                .addChild(new Evaluated(0, 0, 0x200)))
            .addChild(new InstrNoArgs(0, 0, OPCODE_EI))
            .addChild(new InstrExpr(0, 0, OPCODE_CM)
                .addChild(new Evaluated(0, 0, 0x200)))
            .addChild(new InstrExpr(0, 0, OPCODE_CPI)
                .addChild(new Evaluated(0, 0, 0xF0)))
            .addChild(new InstrExpr(0, 0, OPCODE_RST)
                .addChild(new Evaluated(0, 0, 7)));

        IntelHEX hex = new IntelHEX();
        GenerateCodeVisitor visitor = new GenerateCodeVisitor(hex);
        visitor.visit(program);
        Map<Integer, Byte> code = hex.getCode();

        assertEquals(0, code.get(0).byteValue()); // NOP
        assertEquals(1, code.get(1).byteValue()); // LXI B,D16
        assertEquals(0x34, code.get(2).byteValue());
        assertEquals(0x12, code.get(3).byteValue());
        assertEquals(2, code.get(4).byteValue()); // STAX B
        assertEquals(3, code.get(5).byteValue()); // INX B
        assertEquals(4, code.get(6).byteValue()); // INR B
        assertEquals(5, code.get(7).byteValue()); // DCR B
        assertEquals(6, code.get(8).byteValue()); // MVI B
        assertEquals(7, code.get(9).byteValue());
        assertEquals(7, code.get(10).byteValue()); // RLC
        assertEquals(9, code.get(11).byteValue()); // DAD B
        assertEquals(0x0A, code.get(12).byteValue()); // LDAX B
        assertEquals(0x0B, code.get(13).byteValue()); // DCX B
        assertEquals(0x0C, code.get(14).byteValue()); // INR C
        assertEquals(0x0D, code.get(15).byteValue()); // DCR C
        assertEquals(0x0E, code.get(16).byteValue()); // MVI C
        assertEquals(8, code.get(17).byteValue());
        assertEquals(0x0F, code.get(18).byteValue()); // RRC
        assertEquals(0x11, code.get(19).byteValue()); // LXI D
        assertEquals(0x45, code.get(20).byteValue());
        assertEquals(0x23, code.get(21).byteValue());
        assertEquals(0x12, code.get(22).byteValue()); // STAX D
        assertEquals(0x13, code.get(23).byteValue()); // INX D
        assertEquals(0x14, code.get(24).byteValue()); // INR D
        assertEquals(0x15, code.get(25).byteValue()); // DCR D
        assertEquals(0x16, code.get(26).byteValue()); // MVI D
        assertEquals(9, code.get(27).byteValue());
        assertEquals(0x17, code.get(28).byteValue()); // RAL
        assertEquals(0x19, code.get(29).byteValue()); // DAD D
        assertEquals(0x1A, code.get(30).byteValue()); // LDAX D
        assertEquals(0x1B, code.get(31).byteValue()); // DCX D
        assertEquals(0x1C, code.get(32).byteValue()); // INR E
        assertEquals(0x1D, code.get(33).byteValue()); // DCR E
        assertEquals(0x1E, code.get(34).byteValue()); // MVI E
        assertEquals(10, code.get(35).byteValue());
        assertEquals(0x1F, code.get(36).byteValue()); // RAR
        assertEquals(0x21, code.get(37).byteValue()); // LXI H
        assertEquals(0x45, code.get(38).byteValue());
        assertEquals(0x23, code.get(39).byteValue());
        assertEquals(0x22, code.get(40).byteValue()); // SHLD
        assertEquals(0x45, code.get(41).byteValue());
        assertEquals(0x23, code.get(42).byteValue());
        assertEquals(0x23, code.get(43).byteValue()); // INX H
        assertEquals(0x24, code.get(44).byteValue()); // INR H
        assertEquals(0x25, code.get(45).byteValue()); // DCR H
        assertEquals(0x26, code.get(46).byteValue()); // MVI H
        assertEquals(0x28, code.get(47).byteValue());
        assertEquals(0x27, code.get(48).byteValue()); // DAA
        assertEquals(0x29, code.get(49).byteValue()); // DAD H
        assertEquals(0x2A, code.get(50).byteValue()); // LHLD
        assertEquals(0x45, code.get(51).byteValue());
        assertEquals(0x23, code.get(52).byteValue());
        assertEquals(0x2B, code.get(53).byteValue()); // DCX H
        assertEquals(0x2C, code.get(54).byteValue()); // INR L
        assertEquals(0x2D, code.get(55).byteValue()); // DCR L
        assertEquals(0x2E, code.get(56).byteValue()); // MVI L
        assertEquals(10, code.get(57).byteValue());
        assertEquals(0x2F, code.get(58).byteValue()); // CMA
        assertEquals(0x31, code.get(59).byteValue()); // LXI SP
        assertEquals(0x45, code.get(60).byteValue());
        assertEquals(0x23, code.get(61).byteValue());
        assertEquals(0x32, code.get(62).byteValue()); // STA
        assertEquals(0x45, code.get(63).byteValue());
        assertEquals(0x23, code.get(64).byteValue());
        assertEquals(0x33, code.get(65).byteValue()); // INX SP
        assertEquals(0x34, code.get(66).byteValue()); // INR M
        assertEquals(0x35, code.get(67).byteValue()); // DCR M
        assertEquals(0x36, code.get(68).byteValue()); // MVI M
        assertEquals(7, code.get(69).byteValue());
        assertEquals(0x37, code.get(70).byteValue()); // STC
        assertEquals(0x39, code.get(71).byteValue()); // DAD SP
        assertEquals(0x3A, code.get(72).byteValue()); // LDA
        assertEquals(0x45, code.get(73).byteValue());
        assertEquals(0x23, code.get(74).byteValue());
        assertEquals(0x3B, code.get(75).byteValue()); // DCX SP
        assertEquals(0x3C, code.get(76).byteValue()); // INR A
        assertEquals(0x3D, code.get(77).byteValue()); // DCR A
        assertEquals(0x3E, code.get(78).byteValue()); // MVI A
        assertEquals(-128, code.get(79).byteValue());
        assertEquals(0x3F, code.get(80).byteValue()); // CMC
        assertEquals(0x40, code.get(81).byteValue()); // MOV B,B
        assertEquals(0x41, code.get(82).byteValue()); // MOV B,C
        assertEquals(0x42, code.get(83).byteValue()); // MOV B,D
        assertEquals(0x43, code.get(84).byteValue()); // MOV B,E
        assertEquals(0x44, code.get(85).byteValue()); // MOV B,H
        assertEquals(0x45, code.get(86).byteValue()); // MOV B,L
        assertEquals(0x46, code.get(87).byteValue()); // MOV B,M
        assertEquals(0x47, code.get(88).byteValue()); // MOV B,A
        assertEquals(0x48, code.get(89).byteValue()); // MOV C,B
        assertEquals(0x49, code.get(90).byteValue()); // MOV C,C
        assertEquals(0x4A, code.get(91).byteValue()); // MOV C,D
        assertEquals(0x4B, code.get(92).byteValue()); // MOV C,E
        assertEquals(0x4C, code.get(93).byteValue()); // MOV C,H
        assertEquals(0x4D, code.get(94).byteValue()); // MOV C,L
        assertEquals(0x4E, code.get(95).byteValue()); // MOV C,M
        assertEquals(0x4F, code.get(96).byteValue()); // MOV C,A
        assertEquals(0x50, code.get(97).byteValue()); // MOV D,B
        assertEquals(0x51, code.get(98).byteValue()); // MOV D,C
        assertEquals(0x52, code.get(99).byteValue()); // MOV D,D
        assertEquals(0x53, code.get(100).byteValue()); // MOV D,E
        assertEquals(0x54, code.get(101).byteValue()); // MOV D,H
        assertEquals(0x55, code.get(102).byteValue()); // MOV D,L
        assertEquals(0x56, code.get(103).byteValue()); // MOV D,M
        assertEquals(0x57, code.get(104).byteValue()); // MOV D,A
        assertEquals(0x58, code.get(105).byteValue()); // MOV E,B
        assertEquals(0x59, code.get(106).byteValue()); // MOV E,C
        assertEquals(0x5A, code.get(107).byteValue()); // MOV E,D
        assertEquals(0x5B, code.get(108).byteValue()); // MOV E,E
        assertEquals(0x5C, code.get(109).byteValue()); // MOV E,H
        assertEquals(0x5D, code.get(110).byteValue()); // MOV E,L
        assertEquals(0x5E, code.get(111).byteValue()); // MOV E,M
        assertEquals(0x5F, code.get(112).byteValue()); // MOV E,A
        assertEquals(0x60, code.get(113).byteValue()); // MOV H,B
        assertEquals(0x61, code.get(114).byteValue()); // MOV H,C
        assertEquals(0x62, code.get(115).byteValue()); // MOV H,D
        assertEquals(0x63, code.get(116).byteValue()); // MOV H,E
        assertEquals(0x64, code.get(117).byteValue()); // MOV H,H
        assertEquals(0x65, code.get(118).byteValue()); // MOV H,L
        assertEquals(0x66, code.get(119).byteValue()); // MOV H,M
        assertEquals(0x67, code.get(120).byteValue()); // MOV H,A
        assertEquals(0x68, code.get(121).byteValue()); // MOV L,B
        assertEquals(0x69, code.get(122).byteValue()); // MOV L,C
        assertEquals(0x6A, code.get(123).byteValue()); // MOV L,D
        assertEquals(0x6B, code.get(124).byteValue()); // MOV L,E
        assertEquals(0x6C, code.get(125).byteValue()); // MOV L,H
        assertEquals(0x6D, code.get(126).byteValue()); // MOV L,L
        assertEquals(0x6E, code.get(127).byteValue()); // MOV L,M
        assertEquals(0x6F, code.get(128).byteValue()); // MOV L,A
        assertEquals(0x70, code.get(129).byteValue()); // MOV M,B
        assertEquals(0x71, code.get(130).byteValue()); // MOV M,C
        assertEquals(0x72, code.get(131).byteValue()); // MOV M,D
        assertEquals(0x73, code.get(132).byteValue()); // MOV M,E
        assertEquals(0x74, code.get(133).byteValue()); // MOV M,H
        assertEquals(0x75, code.get(134).byteValue()); // MOV M,L
        assertEquals(0x76, code.get(135).byteValue()); // MOV M,M / HLT
        assertEquals(0x77, code.get(136).byteValue()); // MOV M,A
        assertEquals(0x78, code.get(137).byteValue()); // MOV A,B
        assertEquals(0x79, code.get(138).byteValue()); // MOV A,C
        assertEquals(0x7A, code.get(139).byteValue()); // MOV A,D
        assertEquals(0x7B, code.get(140).byteValue()); // MOV A,E
        assertEquals(0x7C, code.get(141).byteValue()); // MOV A,H
        assertEquals(0x7D, code.get(142).byteValue()); // MOV A,L
        assertEquals(0x7E, code.get(143).byteValue()); // MOV A,M
        assertEquals(0x7F, code.get(144).byteValue()); // MOV A,A
        assertEquals(0x80, code.get(145) & 0xFF); // ADD B
        assertEquals(0x81, code.get(146) & 0xFF); // ADD C
        assertEquals(0x82, code.get(147) & 0xFF); // ADD D
        assertEquals(0x83, code.get(148) & 0xFF); // ADD E
        assertEquals(0x84, code.get(149) & 0xFF); // ADD H
        assertEquals(0x85, code.get(150) & 0xFF); // ADD L
        assertEquals(0x86, code.get(151) & 0xFF); // ADD M
        assertEquals(0x87, code.get(152) & 0xFF); // ADD A
        assertEquals(0x88, code.get(153) & 0xFF); // ADC B
        assertEquals(0x89, code.get(154) & 0xFF); // ADC C
        assertEquals(0x8A, code.get(155) & 0xFF); // ADC D
        assertEquals(0x8B, code.get(156) & 0xFF); // ADC E
        assertEquals(0x8C, code.get(157) & 0xFF); // ADC H
        assertEquals(0x8D, code.get(158) & 0xFF); // ADC L
        assertEquals(0x8E, code.get(159) & 0xFF); // ADC M
        assertEquals(0x8F, code.get(160) & 0xFF); // ADC A
        assertEquals(0x90, code.get(161) & 0xFF); // SUB B
        assertEquals(0x91, code.get(162) & 0xFF); // SUB C
        assertEquals(0x92, code.get(163) & 0xFF); // SUB D
        assertEquals(0x93, code.get(164) & 0xFF); // SUB E
        assertEquals(0x94, code.get(165) & 0xFF); // SUB H
        assertEquals(0x95, code.get(166) & 0xFF); // SUB L
        assertEquals(0x96, code.get(167) & 0xFF); // SUB M
        assertEquals(0x97, code.get(168) & 0xFF); // SUB A
        assertEquals(0x98, code.get(169) & 0xFF); // SBB B
        assertEquals(0x99, code.get(170) & 0xFF); // SBB C
        assertEquals(0x9A, code.get(171) & 0xFF); // SBB D
        assertEquals(0x9B, code.get(172) & 0xFF); // SBB E
        assertEquals(0x9C, code.get(173) & 0xFF); // SBB H
        assertEquals(0x9D, code.get(174) & 0xFF); // SBB L
        assertEquals(0x9E, code.get(175) & 0xFF); // SBB M
        assertEquals(0x9F, code.get(176) & 0xFF); // SBB A
        assertEquals(0xA0, code.get(177) & 0xFF); // ANA B
        assertEquals(0xA1, code.get(178) & 0xFF); // ANA C
        assertEquals(0xA2, code.get(179) & 0xFF); // ANA D
        assertEquals(0xA3, code.get(180) & 0xFF); // ANA E
        assertEquals(0xA4, code.get(181) & 0xFF); // ANA H
        assertEquals(0xA5, code.get(182) & 0xFF); // ANA L
        assertEquals(0xA6, code.get(183) & 0xFF); // ANA M
        assertEquals(0xA7, code.get(184) & 0xFF); // ANA A
        assertEquals(0xA8, code.get(185) & 0xFF); // XRA B
        assertEquals(0xA9, code.get(186) & 0xFF); // XRA C
        assertEquals(0xAA, code.get(187) & 0xFF); // XRA D
        assertEquals(0xAB, code.get(188) & 0xFF); // XRA E
        assertEquals(0xAC, code.get(189) & 0xFF); // XRA H
        assertEquals(0xAD, code.get(190) & 0xFF); // XRA L
        assertEquals(0xAE, code.get(191) & 0xFF); // XRA M
        assertEquals(0xAF, code.get(192) & 0xFF); // XRA A
        assertEquals(0xB0, code.get(193) & 0xFF); // ORA B
        assertEquals(0xB1, code.get(194) & 0xFF); // ORA C
        assertEquals(0xB2, code.get(195) & 0xFF); // ORA D
        assertEquals(0xB3, code.get(196) & 0xFF); // ORA E
        assertEquals(0xB4, code.get(197) & 0xFF); // ORA H
        assertEquals(0xB5, code.get(198) & 0xFF); // ORA L
        assertEquals(0xB6, code.get(199) & 0xFF); // ORA M
        assertEquals(0xB7, code.get(200) & 0xFF); // ORA A
        assertEquals(0xB8, code.get(201) & 0xFF); // CMP B
        assertEquals(0xB9, code.get(202) & 0xFF); // CMP C
        assertEquals(0xBA, code.get(203) & 0xFF); // CMP D
        assertEquals(0xBB, code.get(204) & 0xFF); // CMP E
        assertEquals(0xBC, code.get(205) & 0xFF); // CMP H
        assertEquals(0xBD, code.get(206) & 0xFF); // CMP L
        assertEquals(0xBE, code.get(207) & 0xFF); // CMP M
        assertEquals(0xBF, code.get(208) & 0xFF); // CMP A
        assertEquals(0xC0, code.get(209) & 0xFF); // RNZ
        assertEquals(0xC1, code.get(210) & 0xFF); // POP B
        assertEquals(0xC2, code.get(211) & 0xFF); // JNZ
        assertEquals(2, code.get(212).byteValue());
        assertEquals(0, code.get(213).byteValue());
        assertEquals(0xC3, code.get(214) & 0xFF); // JMP
        assertEquals(0, code.get(215).byteValue());
        assertEquals(2, code.get(216).byteValue());
        assertEquals(0xC4, code.get(217) & 0xFF); // CNZ
        assertEquals(0xA0, code.get(218) & 0xFF);
        assertEquals(0xF0, code.get(219) & 0xFF);
        assertEquals(0xC5, code.get(220) & 0xFF); // PUSH B
        assertEquals(0xC6, code.get(221) & 0xFF); // ADI
        assertEquals(0xF0, code.get(222) & 0xFF);
        assertEquals(0xC7, code.get(223) & 0xFF); // RST 0
        assertEquals(0xC8, code.get(224) & 0xFF); // RZ
        assertEquals(0xC9, code.get(225) & 0xFF); // RET
        assertEquals(0xCA, code.get(226) & 0xFF); // JZ
        assertEquals(0x34, code.get(227) & 0xFF);
        assertEquals(0x12, code.get(228) & 0xFF);
        assertEquals(0xCC, code.get(229) & 0xFF); // CZ
        assertEquals(0x34, code.get(230) & 0xFF);
        assertEquals(0x12, code.get(231) & 0xFF);
        assertEquals(0xCD, code.get(232) & 0xFF); // CALL
        assertEquals(0x34, code.get(233) & 0xFF);
        assertEquals(0x12, code.get(234) & 0xFF);
        assertEquals(0xCE, code.get(235) & 0xFF); // ACI
        assertEquals(0xF0, code.get(236) & 0xFF);
        assertEquals(0xCF, code.get(237) & 0xFF); // RST 1
        assertEquals(0xD0, code.get(238) & 0xFF); // RNC
        assertEquals(0xD1, code.get(239) & 0xFF); // POP D
        assertEquals(0xD2, code.get(240) & 0xFF); // JNC
        assertEquals(0x34, code.get(241) & 0xFF);
        assertEquals(0x12, code.get(242) & 0xFF);
        assertEquals(0xD3, code.get(243) & 0xFF); // OUT
        assertEquals(0xF0, code.get(244) & 0xFF);
        assertEquals(0xD4, code.get(245) & 0xFF); // CNC
        assertEquals(0x34, code.get(246) & 0xFF);
        assertEquals(0x12, code.get(247) & 0xFF);
        assertEquals(0xD5, code.get(248) & 0xFF); // PUSH D
        assertEquals(0xD6, code.get(249) & 0xFF); // SUI
        assertEquals(0xF0, code.get(250) & 0xFF);
        assertEquals(0xD7, code.get(251) & 0xFF); // RST 2
        assertEquals(0xD8, code.get(252) & 0xFF); // RC
        assertEquals(0xDA, code.get(253) & 0xFF); // JC
        assertEquals(0x34, code.get(254) & 0xFF);
        assertEquals(0x12, code.get(255) & 0xFF);
        assertEquals(0xDB, code.get(256) & 0xFF); // IN
        assertEquals(0xF0, code.get(257) & 0xFF);
        assertEquals(0xDC, code.get(258) & 0xFF); // CC
        assertEquals(0x34, code.get(259) & 0xFF);
        assertEquals(0x12, code.get(260) & 0xFF);
        assertEquals(0xDE, code.get(261) & 0xFF); // SBI
        assertEquals(0xF0, code.get(262) & 0xFF);
        assertEquals(0xDF, code.get(263) & 0xFF); // RST 3
        assertEquals(0xE0, code.get(264) & 0xFF); // RPO
        assertEquals(0xE1, code.get(265) & 0xFF); // POP H
        assertEquals(0xE2, code.get(266) & 0xFF); // JPO
        assertEquals(0x34, code.get(267) & 0xFF);
        assertEquals(0x12, code.get(268) & 0xFF);
        assertEquals(0xE3, code.get(269) & 0xFF); // XTHL
        assertEquals(0xE4, code.get(270) & 0xFF); // CPO
        assertEquals(0x34, code.get(271) & 0xFF);
        assertEquals(0x12, code.get(272) & 0xFF);
        assertEquals(0xE5, code.get(273) & 0xFF); // PUSH H
        assertEquals(0xE6, code.get(274) & 0xFF); // ANI
        assertEquals(0xF0, code.get(275) & 0xFF);
        assertEquals(0xE7, code.get(276) & 0xFF); // RST
        assertEquals(0xE8, code.get(277) & 0xFF); // RPE
        assertEquals(0xE9, code.get(278) & 0xFF); // PCHL
        assertEquals(0xEA, code.get(279) & 0xFF); // JPE
        assertEquals(0x34, code.get(280) & 0xFF);
        assertEquals(0x12, code.get(281) & 0xFF);
        assertEquals(0xEB, code.get(282) & 0xFF); // XCHG
        assertEquals(0xEC, code.get(283) & 0xFF); // CPE
        assertEquals(0x34, code.get(284) & 0xFF);
        assertEquals(0x12, code.get(285) & 0xFF);
        assertEquals(0xEE, code.get(286) & 0xFF); // XRI
        assertEquals(0xF0, code.get(287) & 0xFF);
        assertEquals(0xEF, code.get(288) & 0xFF); // RST
        assertEquals(0xF0, code.get(289) & 0xFF); // RP
        assertEquals(0xF1, code.get(290) & 0xFF); // POP PSW
        assertEquals(0xF2, code.get(291) & 0xFF); // JP
        assertEquals(0x00, code.get(292) & 0xFF);
        assertEquals(0x02, code.get(293) & 0xFF);
        assertEquals(0xF3, code.get(294) & 0xFF); // DI
        assertEquals(0xF4, code.get(295) & 0xFF); // CP
        assertEquals(0x00, code.get(296) & 0xFF);
        assertEquals(0x02, code.get(297) & 0xFF);
        assertEquals(0xF5, code.get(298) & 0xFF); // PUSH PSW
        assertEquals(0xF6, code.get(299) & 0xFF); // ORI
        assertEquals(0xF0, code.get(300) & 0xFF);
        assertEquals(0xF7, code.get(301) & 0xFF); // RST 6
        assertEquals(0xF8, code.get(302) & 0xFF); // RM
        assertEquals(0xF9, code.get(303) & 0xFF); // SPHL
        assertEquals(0xFA, code.get(304) & 0xFF); // JM
        assertEquals(0x00, code.get(305) & 0xFF);
        assertEquals(0x02, code.get(306) & 0xFF);
        assertEquals(0xFB, code.get(307) & 0xFF); // EI
        assertEquals(0xFC, code.get(308) & 0xFF); // CM
        assertEquals(0x00, code.get(309) & 0xFF);
        assertEquals(0x02, code.get(310) & 0xFF);
        assertEquals(0xFE, code.get(311) & 0xFF); // CPI
        assertEquals(0xF0, code.get(312) & 0xFF);
        assertEquals(0xFF, code.get(313) & 0xFF); // RST 7
    }
}
