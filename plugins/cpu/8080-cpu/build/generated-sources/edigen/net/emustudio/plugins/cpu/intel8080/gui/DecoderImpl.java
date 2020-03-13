/* Auto-generated file. Do not modify. */
package net.emustudio.plugins.cpu.intel8080.gui;

import net.emustudio.emulib.plugins.cpu.DecodedInstruction;
import net.emustudio.emulib.plugins.cpu.Decoder;
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;
import net.emustudio.emulib.plugins.memory.MemoryContext;

import java.util.Arrays;
import java.util.Objects;

/**
 * The instruction decoder.
 */
public class DecoderImpl implements Decoder {
    private MemoryContext<?> memory;
    private int memoryPosition;
    private int unit;
    private byte[] instructionBytes = new byte[1024];
    private int bytesRead;
    private DecodedInstruction instruction;
    
    public static final int ADD = 1;
    public static final int ADC = 2;
    public static final int SUB = 3;
    public static final int SBB = 4;
    public static final int ANA = 5;
    public static final int XRA = 6;
    public static final int ORA = 7;
    public static final int CMP = 8;
    public static final int MOV = 9;
    public static final int NOP = 10;
    public static final int XCHG = 11;
    public static final int SPHL = 12;
    public static final int XTHL = 13;
    public static final int DAA = 14;
    public static final int CMA = 15;
    public static final int RLC = 16;
    public static final int RRC = 17;
    public static final int RAL = 18;
    public static final int RAR = 19;
    public static final int STC = 20;
    public static final int CMC = 21;
    public static final int PCHL = 22;
    public static final int RET = 23;
    public static final int RNZ = 24;
    public static final int RZ = 25;
    public static final int RNC = 26;
    public static final int RC = 27;
    public static final int RPO = 28;
    public static final int RPE = 29;
    public static final int RP = 30;
    public static final int RM = 31;
    public static final int EI = 32;
    public static final int DI = 33;
    public static final int HLT = 34;
    public static final int LDA = 35;
    public static final int STA = 36;
    public static final int LHLD = 37;
    public static final int SHLD = 38;
    public static final int JMP = 39;
    public static final int JNZ = 40;
    public static final int JZ = 41;
    public static final int JNC = 42;
    public static final int JC = 43;
    public static final int JPO = 44;
    public static final int JPE = 45;
    public static final int JP = 46;
    public static final int JM = 47;
    public static final int CALL = 48;
    public static final int CNZ = 49;
    public static final int CZ = 50;
    public static final int CNC = 51;
    public static final int CC = 52;
    public static final int CPO = 53;
    public static final int CPE = 54;
    public static final int CP = 55;
    public static final int CM = 56;
    public static final int IN = 57;
    public static final int OUT = 58;
    public static final int ADI = 59;
    public static final int ACI = 60;
    public static final int SUI = 61;
    public static final int SBI = 62;
    public static final int CPI = 63;
    public static final int ANI = 64;
    public static final int ORI = 65;
    public static final int XRI = 66;
    public static final int LDAX = 67;
    public static final int STAX = 68;
    public static final int POP = 69;
    public static final int PUSH = 70;
    public static final int DAD = 71;
    public static final int INX = 72;
    public static final int DCX = 73;
    public static final int LXI = 74;
    public static final int MVI = 75;
    public static final int INR = 76;
    public static final int DCR = 77;
    public static final int RST = 78;
    public static final int INSTRUCTION = 79;
    public static final int BC = 80;
    public static final int DE = 81;
    public static final int BCDE = 82;
    public static final int HL = 83;
    public static final int SP = 84;
    public static final int HLSP = 85;
    public static final int PSW = 86;
    public static final int HLPSW = 87;
    public static final int B = 88;
    public static final int C = 89;
    public static final int D = 90;
    public static final int E = 91;
    public static final int H = 92;
    public static final int L = 93;
    public static final int M = 94;
    public static final int A = 95;
    public static final int REG = 96;
    public static final int REG_BCDE = 97;
    public static final int REG_HL = 98;
    public static final int REG_A = 99;
    public static final int NUMBER = 100;
    public static final int IMM8 = 101;
    public static final int IMM16 = 102;

    
    /**
     * The constructor.
     * @param memory the memory context which will be used to read instructions
     */
    public DecoderImpl(MemoryContext<?> memory) {
        this.memory = Objects.requireNonNull(memory);
    }
    
    /**
     * Decodes an instruction.
     * @param memoryPosition the address of the start of the instruction
     * @return the decoded instruction object
     * @throws InvalidInstructionException when decoding is not successful
     */
    @Override
    public DecodedInstruction decode(int memoryPosition) throws InvalidInstructionException {
        this.memoryPosition = memoryPosition;
        bytesRead = 0;

        instruction = new DecodedInstruction();
        instruction(0);
        instruction.setImage(Arrays.copyOfRange(instructionBytes, 0, bytesRead));
        return instruction;
    }

    /**
     * Reads an arbitrary number of bits of the current instruction into a byte array.
     * @param start the number of bits from the start of the current instruction
     * @param length the number of bits to read
     * @return the bytes read
     */
    private byte[] readBytes(int start, int length) {
        int startByte = start / 8;
        int endByte = (start + length - 1) / 8;
        int clear = start % 8;
        int shift = (8 - ((start + length) % 8)) % 8;

        while (bytesRead <= endByte) {
            instructionBytes[bytesRead++] = ((Number) memory.read(memoryPosition++)).byteValue();
        }

        byte[] result = Arrays.copyOfRange(instructionBytes, startByte, endByte + 1);
        result[0] &= 0xFF >> clear;

        // right shift all bits
        for (int i = result.length - 1; i >= 0; i--) {
            result[i] = (byte) ((result[i] & 0xFF) >>> shift);
            if (i > 0)
                result[i] |= (result[i - 1] & (0xFF >>> (8 - shift))) << (8 - shift);
        }

        // if the leftmost byte is now unused
        if (result.length > 8 * length)
            result = Arrays.copyOfRange(result, 1, result.length);

        return result;
    }

    /**
     * Reads at most one unit (int) of the current instruction.
     * @param start the number of bits from the start of the current instruction
     * @param length the number of bits to read
     * @return the bits read
     */
    private int read(int start, int length) {
        int number = 0;
        byte[] bytes = readBytes(start, length);

        for (int i = 0; i < bytes.length; i++)
            number |= (bytes[i] & 0xFF) << (8 * (bytes.length - i - 1));

        return number;
    }
    
    private void instruction(int start) throws InvalidInstructionException {
        unit = read(start + 0, 5);
        
        switch (unit & 0x1f) {
        case 0x10:
            instruction.add(INSTRUCTION, "add", ADD);
            REG(start + 5);
            break;
        case 0x11:
            instruction.add(INSTRUCTION, "adc", ADC);
            REG(start + 5);
            break;
        case 0x12:
            instruction.add(INSTRUCTION, "sub", SUB);
            REG(start + 5);
            break;
        case 0x13:
            instruction.add(INSTRUCTION, "sbb", SBB);
            REG(start + 5);
            break;
        case 0x14:
            instruction.add(INSTRUCTION, "ana", ANA);
            REG(start + 5);
            break;
        case 0x15:
            instruction.add(INSTRUCTION, "xra", XRA);
            REG(start + 5);
            break;
        case 0x16:
            instruction.add(INSTRUCTION, "ora", ORA);
            REG(start + 5);
            break;
        case 0x17:
            instruction.add(INSTRUCTION, "cmp", CMP);
            REG(start + 5);
            break;
        case 0x0f:
            instruction.add(INSTRUCTION, "mov", MOV);
            REG_A(start + 2);
            REG(start + 5);
            break;
        default:
            unit = read(start + 0, 5);
            
            switch (unit & 0x1c) {
            case 0x08:
                instruction.add(INSTRUCTION, "mov", MOV);
                REG_BCDE(start + 3);
                REG(start + 5);
                break;
            default:
                unit = read(start + 0, 5);
                
                switch (unit & 0x1e) {
                case 0x0c:
                    instruction.add(INSTRUCTION, "mov", MOV);
                    REG_HL(start + 4);
                    REG(start + 5);
                    break;
                default:
                    unit = read(start + 0, 6);
                    
                    switch (unit & 0x3f) {
                    case 0x1c:
                        instruction.add(INSTRUCTION, "mov", MOV);
                        REG_BCDE(start + 6);
                        break;
                    default:
                        unit = read(start + 0, 7);
                        
                        switch (unit & 0x7f) {
                        case 0x3a:
                            instruction.add(INSTRUCTION, "mov", MOV);
                            REG_HL(start + 7);
                            break;
                        default:
                            unit = read(start + 0, 8);
                            
                            switch (unit & 0xff) {
                            case 0x00:
                                instruction.add(INSTRUCTION, "nop", NOP);
                                break;
                            case 0xeb:
                                instruction.add(INSTRUCTION, "xchg", XCHG);
                                break;
                            case 0xf9:
                                instruction.add(INSTRUCTION, "sphl", SPHL);
                                break;
                            case 0xe3:
                                instruction.add(INSTRUCTION, "xthl", XTHL);
                                break;
                            case 0x27:
                                instruction.add(INSTRUCTION, "daa", DAA);
                                break;
                            case 0x2f:
                                instruction.add(INSTRUCTION, "cma", CMA);
                                break;
                            case 0x07:
                                instruction.add(INSTRUCTION, "rlc", RLC);
                                break;
                            case 0x0f:
                                instruction.add(INSTRUCTION, "rrc", RRC);
                                break;
                            case 0x17:
                                instruction.add(INSTRUCTION, "ral", RAL);
                                break;
                            case 0x1f:
                                instruction.add(INSTRUCTION, "rar", RAR);
                                break;
                            case 0x37:
                                instruction.add(INSTRUCTION, "stc", STC);
                                break;
                            case 0x3f:
                                instruction.add(INSTRUCTION, "cmc", CMC);
                                break;
                            case 0xe9:
                                instruction.add(INSTRUCTION, "pchl", PCHL);
                                break;
                            case 0xc9:
                                instruction.add(INSTRUCTION, "ret", RET);
                                break;
                            case 0xc0:
                                instruction.add(INSTRUCTION, "rnz", RNZ);
                                break;
                            case 0xc8:
                                instruction.add(INSTRUCTION, "rz", RZ);
                                break;
                            case 0xd0:
                                instruction.add(INSTRUCTION, "rnc", RNC);
                                break;
                            case 0xd8:
                                instruction.add(INSTRUCTION, "rc", RC);
                                break;
                            case 0xe0:
                                instruction.add(INSTRUCTION, "rpo", RPO);
                                break;
                            case 0xe8:
                                instruction.add(INSTRUCTION, "rpe", RPE);
                                break;
                            case 0xf0:
                                instruction.add(INSTRUCTION, "rp", RP);
                                break;
                            case 0xf8:
                                instruction.add(INSTRUCTION, "rm", RM);
                                break;
                            case 0xfb:
                                instruction.add(INSTRUCTION, "ei", EI);
                                break;
                            case 0xf3:
                                instruction.add(INSTRUCTION, "di", DI);
                                break;
                            case 0x76:
                                instruction.add(INSTRUCTION, "hlt", HLT);
                                break;
                            case 0x3a:
                                instruction.add(INSTRUCTION, "lda", LDA);
                                imm16(start + 8);
                                break;
                            case 0x32:
                                instruction.add(INSTRUCTION, "sta", STA);
                                imm16(start + 8);
                                break;
                            case 0x2a:
                                instruction.add(INSTRUCTION, "lhld", LHLD);
                                imm16(start + 8);
                                break;
                            case 0x22:
                                instruction.add(INSTRUCTION, "shld", SHLD);
                                imm16(start + 8);
                                break;
                            case 0xc3:
                                instruction.add(INSTRUCTION, "jmp", JMP);
                                imm16(start + 8);
                                break;
                            case 0xc2:
                                instruction.add(INSTRUCTION, "jnz", JNZ);
                                imm16(start + 8);
                                break;
                            case 0xca:
                                instruction.add(INSTRUCTION, "jz", JZ);
                                imm16(start + 8);
                                break;
                            case 0xd2:
                                instruction.add(INSTRUCTION, "jnc", JNC);
                                imm16(start + 8);
                                break;
                            case 0xda:
                                instruction.add(INSTRUCTION, "jc", JC);
                                imm16(start + 8);
                                break;
                            case 0xe2:
                                instruction.add(INSTRUCTION, "jpo", JPO);
                                imm16(start + 8);
                                break;
                            case 0xea:
                                instruction.add(INSTRUCTION, "jpe", JPE);
                                imm16(start + 8);
                                break;
                            case 0xf2:
                                instruction.add(INSTRUCTION, "jp", JP);
                                imm16(start + 8);
                                break;
                            case 0xfa:
                                instruction.add(INSTRUCTION, "jm", JM);
                                imm16(start + 8);
                                break;
                            case 0xcd:
                                instruction.add(INSTRUCTION, "call", CALL);
                                imm16(start + 8);
                                break;
                            case 0xc4:
                                instruction.add(INSTRUCTION, "cnz", CNZ);
                                imm16(start + 8);
                                break;
                            case 0xcc:
                                instruction.add(INSTRUCTION, "cz", CZ);
                                imm16(start + 8);
                                break;
                            case 0xd4:
                                instruction.add(INSTRUCTION, "cnc", CNC);
                                imm16(start + 8);
                                break;
                            case 0xdc:
                                instruction.add(INSTRUCTION, "cc", CC);
                                imm16(start + 8);
                                break;
                            case 0xe4:
                                instruction.add(INSTRUCTION, "cpo", CPO);
                                imm16(start + 8);
                                break;
                            case 0xec:
                                instruction.add(INSTRUCTION, "cpe", CPE);
                                imm16(start + 8);
                                break;
                            case 0xf4:
                                instruction.add(INSTRUCTION, "cp", CP);
                                imm16(start + 8);
                                break;
                            case 0xfc:
                                instruction.add(INSTRUCTION, "cm", CM);
                                imm16(start + 8);
                                break;
                            case 0xdb:
                                instruction.add(INSTRUCTION, "in", IN);
                                imm8(start + 8);
                                break;
                            case 0xd3:
                                instruction.add(INSTRUCTION, "out", OUT);
                                imm8(start + 8);
                                break;
                            case 0xc6:
                                instruction.add(INSTRUCTION, "adi", ADI);
                                imm8(start + 8);
                                break;
                            case 0xce:
                                instruction.add(INSTRUCTION, "aci", ACI);
                                imm8(start + 8);
                                break;
                            case 0xd6:
                                instruction.add(INSTRUCTION, "sui", SUI);
                                imm8(start + 8);
                                break;
                            case 0xde:
                                instruction.add(INSTRUCTION, "sbi", SBI);
                                imm8(start + 8);
                                break;
                            case 0xfe:
                                instruction.add(INSTRUCTION, "cpi", CPI);
                                imm8(start + 8);
                                break;
                            case 0xe6:
                                instruction.add(INSTRUCTION, "ani", ANI);
                                imm8(start + 8);
                                break;
                            case 0xf6:
                                instruction.add(INSTRUCTION, "ori", ORI);
                                imm8(start + 8);
                                break;
                            case 0xee:
                                instruction.add(INSTRUCTION, "xri", XRI);
                                imm8(start + 8);
                                break;
                            case 0x77:
                                instruction.add(INSTRUCTION, "mov", MOV);
                                REG_A(start + 5);
                                break;
                            default:
                                unit = read(start + 0, 8);
                                
                                switch (unit & 0xef) {
                                case 0x0a:
                                    instruction.add(INSTRUCTION, "ldax", LDAX);
                                    BCDE(start + 3);
                                    break;
                                case 0x02:
                                    instruction.add(INSTRUCTION, "stax", STAX);
                                    BCDE(start + 3);
                                    break;
                                case 0xc1:
                                    instruction.add(INSTRUCTION, "pop", POP);
                                    BCDE(start + 3);
                                    break;
                                case 0xc5:
                                    instruction.add(INSTRUCTION, "push", PUSH);
                                    BCDE(start + 3);
                                    break;
                                case 0x09:
                                    instruction.add(INSTRUCTION, "dad", DAD);
                                    BCDE(start + 3);
                                    break;
                                case 0x03:
                                    instruction.add(INSTRUCTION, "inx", INX);
                                    BCDE(start + 3);
                                    break;
                                case 0x0b:
                                    instruction.add(INSTRUCTION, "dcx", DCX);
                                    BCDE(start + 3);
                                    break;
                                case 0x29:
                                    instruction.add(INSTRUCTION, "dad", DAD);
                                    HLSP(start + 3);
                                    break;
                                case 0x23:
                                    instruction.add(INSTRUCTION, "inx", INX);
                                    HLSP(start + 3);
                                    break;
                                case 0x2b:
                                    instruction.add(INSTRUCTION, "dcx", DCX);
                                    HLSP(start + 3);
                                    break;
                                case 0xe1:
                                    instruction.add(INSTRUCTION, "pop", POP);
                                    HLPSW(start + 3);
                                    break;
                                case 0xe5:
                                    instruction.add(INSTRUCTION, "push", PUSH);
                                    HLPSW(start + 3);
                                    break;
                                case 0x01:
                                    instruction.add(INSTRUCTION, "lxi", LXI);
                                    BCDE(start + 3);
                                    imm16(start + 8);
                                    break;
                                case 0x21:
                                    instruction.add(INSTRUCTION, "lxi", LXI);
                                    HLSP(start + 3);
                                    imm16(start + 8);
                                    break;
                                default:
                                    unit = read(start + 0, 8);
                                    
                                    switch (unit & 0xc7) {
                                    case 0x06:
                                        instruction.add(INSTRUCTION, "mvi", MVI);
                                        REG(start + 2);
                                        imm8(start + 8);
                                        break;
                                    case 0x04:
                                        instruction.add(INSTRUCTION, "inr", INR);
                                        REG(start + 2);
                                        break;
                                    case 0x05:
                                        instruction.add(INSTRUCTION, "dcr", DCR);
                                        REG(start + 2);
                                        break;
                                    case 0xc7:
                                        instruction.add(INSTRUCTION, "rst", RST);
                                        NUMBER(start + 2);
                                        break;
                                    default:
                                        throw new InvalidInstructionException();
                                    }
                                    break;
                                }
                                break;
                            }
                            break;
                        }
                        break;
                    }
                    break;
                }
                break;
            }
            break;
        }
    }
    
    private void BCDE(int start) throws InvalidInstructionException {
        unit = read(start + 0, 1);
        
        switch (unit & 0x1) {
        case 0x0:
            instruction.add(BCDE, "BC", BC);
            break;
        case 0x1:
            instruction.add(BCDE, "DE", DE);
            break;
        default:
            throw new InvalidInstructionException();
        }
    }
    
    private void HLSP(int start) throws InvalidInstructionException {
        unit = read(start + 0, 1);
        
        switch (unit & 0x1) {
        case 0x0:
            instruction.add(HLSP, "HL", HL);
            break;
        case 0x1:
            instruction.add(HLSP, "SP", SP);
            break;
        default:
            throw new InvalidInstructionException();
        }
    }
    
    private void HLPSW(int start) throws InvalidInstructionException {
        unit = read(start + 0, 1);
        
        switch (unit & 0x1) {
        case 0x0:
            instruction.add(HLPSW, "HL", HL);
            break;
        case 0x1:
            instruction.add(HLPSW, "PSW", PSW);
            break;
        default:
            throw new InvalidInstructionException();
        }
    }
    
    private void REG(int start) throws InvalidInstructionException {
        unit = read(start + 0, 3);
        
        switch (unit & 0x7) {
        case 0x0:
            instruction.add(REG, "B", B);
            break;
        case 0x1:
            instruction.add(REG, "C", C);
            break;
        case 0x2:
            instruction.add(REG, "D", D);
            break;
        case 0x3:
            instruction.add(REG, "E", E);
            break;
        case 0x4:
            instruction.add(REG, "H", H);
            break;
        case 0x5:
            instruction.add(REG, "L", L);
            break;
        case 0x6:
            instruction.add(REG, "M", M);
            break;
        case 0x7:
            instruction.add(REG, "A", A);
            break;
        default:
            throw new InvalidInstructionException();
        }
    }
    
    private void REG_BCDE(int start) throws InvalidInstructionException {
        unit = read(start + 0, 2);
        
        switch (unit & 0x3) {
        case 0x0:
            instruction.add(REG_BCDE, "B", B);
            break;
        case 0x1:
            instruction.add(REG_BCDE, "C", C);
            break;
        case 0x2:
            instruction.add(REG_BCDE, "D", D);
            break;
        case 0x3:
            instruction.add(REG_BCDE, "E", E);
            break;
        default:
            throw new InvalidInstructionException();
        }
    }
    
    private void REG_HL(int start) throws InvalidInstructionException {
        unit = read(start + 0, 1);
        
        switch (unit & 0x1) {
        case 0x0:
            instruction.add(REG_HL, "H", H);
            break;
        case 0x1:
            instruction.add(REG_HL, "L", L);
            break;
        default:
            throw new InvalidInstructionException();
        }
    }
    
    private void REG_A(int start) throws InvalidInstructionException {
        unit = read(start + 0, 3);
        
        switch (unit & 0x7) {
        case 0x7:
            instruction.add(REG_A, "A", A);
            break;
        default:
            throw new InvalidInstructionException();
        }
    }
    
    private void NUMBER(int start) throws InvalidInstructionException {
        unit = read(start + 0, 3);
        
        instruction.add(NUMBER, readBytes(start + 0, 3));
    }
    
    private void imm8(int start) throws InvalidInstructionException {
        unit = read(start + 0, 8);
        
        instruction.add(IMM8, readBytes(start + 0, 8));
    }
    
    private void imm16(int start) throws InvalidInstructionException {
        unit = read(start + 0, 16);
        
        instruction.add(IMM16, readBytes(start + 0, 16));
    }
    

}
