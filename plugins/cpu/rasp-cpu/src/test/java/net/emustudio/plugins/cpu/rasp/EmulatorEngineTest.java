/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static net.emustudio.plugins.memory.rasp.gui.Disassembler.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class EmulatorEngineTest {
    private MemoryStub memory;
    private AbstractTapeContext inputTape;
    private AbstractTapeContext outputTape;
    private EmulatorEngine engine;

    @Before
    public void setup() {
        memory = new MemoryStub(new Annotations());
        inputTape = createNiceMock(AbstractTapeContext.class);
        outputTape = createNiceMock(AbstractTapeContext.class);
    }

    private void setupEngine(int... memoryContent) {
        int address = 0;
        for (int item : memoryContent) {
            memory.write(address++, item);
        }
        replay(inputTape, outputTape);
        engine = new EmulatorEngine(memory, inputTape, outputTape);
        engine.reset(0);
    }

    @Test
    public void testJumpInstruction() throws IOException {
        setupEngine(JMP, 4, JMP, 0, HALT);
        engine.step(); // jmp to 4
        CPU.RunState state = engine.step(); // halt
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, state);
    }

    @Test
    public void testHaltInstruction() throws IOException {
        setupEngine(HALT);
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.step());
    }

    @Test
    public void testReadInstruction() throws IOException {
        reset(inputTape, outputTape);
        expect(inputTape.readData()).andReturn(new TapeSymbol(42)).once();
        inputTape.moveRight();
        expectLastCall().once();
        replay(inputTape, outputTape);

        memory.write(0, READ);
        memory.write(1, 5); // register 5
        engine = new EmulatorEngine(memory, inputTape, outputTape);
        engine.reset(0);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(Integer.valueOf(42), memory.read(5));
    }

    @Test
    public void testWriteConstantInstruction() throws IOException {
        reset(inputTape, outputTape);
        outputTape.writeData(eq(new TapeSymbol(99)));
        expectLastCall().once();
        outputTape.moveRight();
        expectLastCall().once();
        replay(inputTape, outputTape);

        memory.write(0, 2); // WRITE_C (constant write)
        memory.write(1, 99);
        engine = new EmulatorEngine(memory, inputTape, outputTape);
        engine.reset(0);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
    }

    @Test
    public void testWriteRegisterInstruction() throws IOException {
        reset(inputTape, outputTape);
        outputTape.writeData(eq(new TapeSymbol(77)));
        expectLastCall().once();
        outputTape.moveRight();
        expectLastCall().once();
        replay(inputTape, outputTape);

        memory.write(0, 3); // WRITE (register)
        memory.write(1, 5); // register 5
        memory.write(5, 77); // value at register 5
        engine = new EmulatorEngine(memory, inputTape, outputTape);
        engine.reset(0);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
    }

    @Test
    public void testLoadConstantInstruction() throws IOException {
        setupEngine(4, 42, HALT); // LOAD_C 42, HALT
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(Integer.valueOf(42), memory.read(0));
    }

    @Test
    public void testLoadRegisterInstruction() throws IOException {
        memory.write(0, 5); // LOAD register
        memory.write(1, 10); // register 10
        memory.write(10, 77); // value at register 10
        memory.write(2, HALT);
        setupEngineFromMemory();

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(Integer.valueOf(77), memory.read(0));
    }

    @Test
    public void testStoreInstruction() throws IOException {
        memory.write(0, 6); // STORE
        memory.write(1, 10); // register 10
        memory.write(2, HALT);
        // accumulator (mem[0]) will be overwritten by STORE reading its own opcode first
        // let's set ACC separately
        setupEngineFromMemory();
        // Set accumulator value before STORE
        memory.write(0, 6); // opcode
        // The accumulator is mem[0], but store reads from mem[0] as acc value.
        // After STORE executes: mem[register] = mem[0]
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
    }

    @Test
    public void testAddConstantInstruction() throws IOException {
        memory.write(0, 100); // ACC = 100 (will be set via load_c first)
        memory.write(0, 4); // LOAD_C
        memory.write(1, 10); // ACC = 10
        memory.write(2, 7); // ADD_C
        memory.write(3, 5);  // ACC = 10 + 5 = 15
        memory.write(4, HALT);
        setupEngineFromMemory();

        engine.step(); // load_c 10
        engine.step(); // add_c 5
        assertEquals(Integer.valueOf(15), memory.read(0));
    }

    @Test
    public void testSubConstantInstruction() throws IOException {
        memory.write(0, 4); // LOAD_C
        memory.write(1, 10);
        memory.write(2, 9); // SUB_C
        memory.write(3, 3);
        memory.write(4, HALT);
        setupEngineFromMemory();

        engine.step(); // load_c 10
        engine.step(); // sub_c 3
        assertEquals(Integer.valueOf(7), memory.read(0));
    }

    @Test
    public void testMulConstantInstruction() throws IOException {
        memory.write(0, 4); // LOAD_C
        memory.write(1, 6);
        memory.write(2, 11); // MUL_C
        memory.write(3, 3);
        memory.write(4, HALT);
        setupEngineFromMemory();

        engine.step(); // load_c 6
        engine.step(); // mul_c 3
        assertEquals(Integer.valueOf(18), memory.read(0));
    }

    @Test
    public void testDivConstantInstruction() throws IOException {
        memory.write(0, 4); // LOAD_C
        memory.write(1, 12);
        memory.write(2, 13); // DIV_C
        memory.write(3, 4);
        memory.write(4, HALT);
        setupEngineFromMemory();

        engine.step(); // load_c 12
        engine.step(); // div_c 4
        assertEquals(Integer.valueOf(3), memory.read(0));
    }

    @Test
    public void testDivByZeroConstantInstruction() throws IOException {
        memory.write(0, 4); // LOAD_C
        memory.write(1, 12);
        memory.write(2, 13); // DIV_C
        memory.write(3, 0);  // divide by zero
        setupEngineFromMemory();

        engine.step(); // load_c 12
        assertEquals(CPU.RunState.STATE_STOPPED_BAD_INSTR, engine.step()); // div_c 0
    }

    @Test
    public void testAddRegisterInstruction() throws IOException {
        memory.write(0, 4); // LOAD_C
        memory.write(1, 10);
        memory.write(2, 8); // ADD register
        memory.write(3, 20); // from register 20
        memory.write(4, HALT);
        memory.write(20, 7); // register 20 = 7
        setupEngineFromMemory();

        engine.step(); // load_c 10
        engine.step(); // add r20
        assertEquals(Integer.valueOf(17), memory.read(0));
    }

    @Test
    public void testSubRegisterInstruction() throws IOException {
        memory.write(0, 4); // LOAD_C
        memory.write(1, 10);
        memory.write(2, 10); // SUB register
        memory.write(3, 20);
        memory.write(4, HALT);
        memory.write(20, 3);
        setupEngineFromMemory();

        engine.step();
        engine.step();
        assertEquals(Integer.valueOf(7), memory.read(0));
    }

    @Test
    public void testMulRegisterInstruction() throws IOException {
        memory.write(0, 4); // LOAD_C
        memory.write(1, 5);
        memory.write(2, 12); // MUL register
        memory.write(3, 20);
        memory.write(4, HALT);
        memory.write(20, 4);
        setupEngineFromMemory();

        engine.step();
        engine.step();
        assertEquals(Integer.valueOf(20), memory.read(0));
    }

    @Test
    public void testDivRegisterInstruction() throws IOException {
        memory.write(0, 4); // LOAD_C
        memory.write(1, 20);
        memory.write(2, 14); // DIV register
        memory.write(3, 30);
        memory.write(4, HALT);
        memory.write(30, 5);
        setupEngineFromMemory();

        engine.step();
        engine.step();
        assertEquals(Integer.valueOf(4), memory.read(0));
    }

    @Test
    public void testDivByZeroRegisterInstruction() throws IOException {
        memory.write(0, 4); // LOAD_C
        memory.write(1, 20);
        memory.write(2, 14); // DIV register
        memory.write(3, 30);
        memory.write(30, 0); // register 30 = 0
        setupEngineFromMemory();

        engine.step();
        assertEquals(CPU.RunState.STATE_STOPPED_BAD_INSTR, engine.step());
    }

    @Test
    public void testJzWhenZero() throws IOException {
        memory.write(0, 4); // LOAD_C
        memory.write(1, 0); // ACC = 0
        memory.write(2, JZ);
        memory.write(3, 10);
        memory.write(4, HALT);
        memory.write(10, HALT);
        setupEngineFromMemory();

        engine.step(); // load_c 0
        engine.step(); // jz 10
        assertEquals(10, engine.IP.get());
    }

    @Test
    public void testJzWhenNotZero() throws IOException {
        memory.write(0, 4); // LOAD_C
        memory.write(1, 5); // ACC = 5
        memory.write(2, JZ);
        memory.write(3, 10);
        memory.write(4, HALT);
        setupEngineFromMemory();

        engine.step(); // load_c 5
        engine.step(); // jz 10 - should NOT jump
        assertEquals(4, engine.IP.get());
    }

    @Test
    public void testJgtzWhenPositive() throws IOException {
        memory.write(0, 4); // LOAD_C
        memory.write(1, 5); // ACC = 5
        memory.write(2, JGTZ);
        memory.write(3, 10);
        memory.write(4, HALT);
        memory.write(10, HALT);
        setupEngineFromMemory();

        engine.step(); // load_c 5
        engine.step(); // jgtz 10
        assertEquals(10, engine.IP.get());
    }

    @Test
    public void testJgtzWhenZero() throws IOException {
        memory.write(0, 4); // LOAD_C
        memory.write(1, 0); // ACC = 0
        memory.write(2, JGTZ);
        memory.write(3, 10);
        memory.write(4, HALT);
        setupEngineFromMemory();

        engine.step(); // load_c 0
        engine.step(); // jgtz 10 - should NOT jump
        assertEquals(4, engine.IP.get());
    }

    @Test
    public void testJgtzWhenNegative() throws IOException {
        memory.write(0, 4); // LOAD_C
        memory.write(1, -3); // ACC = -3
        memory.write(2, JGTZ);
        memory.write(3, 10);
        memory.write(4, HALT);
        setupEngineFromMemory();

        engine.step(); // load_c -3
        engine.step(); // jgtz 10 - should NOT jump
        assertEquals(4, engine.IP.get());
    }

    @Test
    public void testBadInstruction() throws IOException {
        memory.write(0, 99); // invalid opcode
        setupEngineFromMemory();
        assertEquals(CPU.RunState.STATE_STOPPED_BAD_INSTR, engine.step());
    }

    @Test
    public void testSetInstructionLocationValid() {
        setupEngine(HALT);
        assertTrue(engine.setInstructionLocation(5));
        assertEquals(5, engine.IP.get());
    }

    @Test
    public void testSetInstructionLocationNegative() {
        setupEngine(HALT);
        assertFalse(engine.setInstructionLocation(-1));
    }

    @Test
    public void testResetWithInputs() {
        memory.setInputs(List.of(10, 20, 30));
        reset(inputTape, outputTape);
        inputTape.clear();
        expectLastCall().once();
        inputTape.setSymbolAt(eq(0), eq(new TapeSymbol(10)));
        expectLastCall().once();
        inputTape.setSymbolAt(eq(1), eq(new TapeSymbol(20)));
        expectLastCall().once();
        inputTape.setSymbolAt(eq(2), eq(new TapeSymbol(30)));
        expectLastCall().once();
        outputTape.clear();
        expectLastCall().once();
        replay(inputTape, outputTape);

        engine = new EmulatorEngine(memory, inputTape, outputTape);
        engine.reset(0);

        verify(inputTape, outputTape);
    }

    @Test
    public void testSetInstructionLocationZero() {
        setupEngine(HALT);
        assertTrue(engine.setInstructionLocation(0));
        assertEquals(0, engine.IP.get());
    }

    @Test
    public void testStepWithZeroOpcode() throws IOException {
        memory.write(0, 0); // opcode 0 is null in dispatcher
        setupEngineFromMemory();
        assertEquals(CPU.RunState.STATE_STOPPED_BAD_INSTR, engine.step());
    }

    @Test
    public void testStepWithOpcodeAboveHalt() throws IOException {
        memory.write(0, HALT + 1); // opcode above HALT
        setupEngineFromMemory();
        assertEquals(CPU.RunState.STATE_STOPPED_BAD_INSTR, engine.step());
    }

    @Test
    public void testStepWithNegativeOpcode() throws IOException {
        memory.write(0, -1); // negative opcode
        setupEngineFromMemory();
        assertEquals(CPU.RunState.STATE_STOPPED_BAD_INSTR, engine.step());
    }

    @Test
    public void testResetSetsIPToLocation() {
        setupEngine(HALT);
        engine.reset(15);
        assertEquals(15, engine.IP.get());
    }

    @Test
    public void testStoreInstructionVerifyValue() throws IOException {
        // LOAD_C 42, STORE R10, HALT
        memory.write(0, 4); // LOAD_C
        memory.write(1, 42);
        memory.write(2, 6); // STORE
        memory.write(3, 10); // into register 10
        memory.write(4, HALT);
        setupEngineFromMemory();

        engine.step(); // load_c 42
        engine.step(); // store R10
        assertEquals(Integer.valueOf(42), memory.read(10));
    }

    @Test
    public void testMultipleArithmetic() throws IOException {
        // LOAD_C 10, ADD_C 5, SUB_C 3, MUL_C 2, DIV_C 4, HALT
        memory.write(0, 4); // LOAD_C
        memory.write(1, 10); // ACC=10
        memory.write(2, 7); // ADD_C
        memory.write(3, 5);  // ACC=15
        memory.write(4, 9); // SUB_C
        memory.write(5, 3);  // ACC=12
        memory.write(6, 11); // MUL_C
        memory.write(7, 2);  // ACC=24
        memory.write(8, 13); // DIV_C
        memory.write(9, 4);  // ACC=6
        memory.write(10, HALT);
        setupEngineFromMemory();

        engine.step(); // load_c 10
        engine.step(); // add_c 5
        assertEquals(Integer.valueOf(15), memory.read(0));
        engine.step(); // sub_c 3
        assertEquals(Integer.valueOf(12), memory.read(0));
        engine.step(); // mul_c 2
        assertEquals(Integer.valueOf(24), memory.read(0));
        engine.step(); // div_c 4
        assertEquals(Integer.valueOf(6), memory.read(0));
    }

    @Test
    public void testReadMultiple() throws IOException {
        reset(inputTape, outputTape);
        expect(inputTape.readData()).andReturn(new TapeSymbol(10)).once();
        expect(inputTape.readData()).andReturn(new TapeSymbol(20)).once();
        inputTape.moveRight();
        expectLastCall().times(2);
        replay(inputTape, outputTape);

        memory.write(0, READ);
        memory.write(1, 5);
        memory.write(2, READ);
        memory.write(3, 6);
        memory.write(4, HALT);
        engine = new EmulatorEngine(memory, inputTape, outputTape);
        engine.reset(0);

        engine.step(); // read into R5
        engine.step(); // read into R6
        assertEquals(Integer.valueOf(10), memory.read(5));
        assertEquals(Integer.valueOf(20), memory.read(6));
    }

    private void setupEngineFromMemory() {
        replay(inputTape, outputTape);
        engine = new EmulatorEngine(memory, inputTape, outputTape);
        engine.reset(0);
    }
}
