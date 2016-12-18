/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.brainduck.cpu.impl;

import emulib.emustudio.SettingsManager;
import emulib.plugins.cpu.CPU;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import net.sf.emustudio.brainduck.cpu.BrainCPUContext;
import net.sf.emustudio.brainduck.memory.RawMemoryContext;
import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Objects;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CpuImplTest {
    private CpuImpl emulator;
    private MemoryStub memory;
    private DeviceStub ioDevice;

    @Before
    public void setUp() throws Exception {
        memory = new MemoryStub();
        ioDevice = new DeviceStub();

        Capture<BrainCPUContextImpl> cpuContextCapture = new Capture<>();

        ContextPool contextPool = createNiceMock(ContextPool.class);
        expect(contextPool.getMemoryContext(0, RawMemoryContext.class)).andReturn(memory).anyTimes();
        expect(contextPool.getMemoryContext(0, MemoryContext.class)).andReturn(memory).anyTimes();
        contextPool.register(eq(0L), capture(cpuContextCapture), same(BrainCPUContext.class));
        expectLastCall().once();
        replay(contextPool);

        emulator = new CpuImpl(0L, contextPool);

        SettingsManager settingsManager = createNiceMock(SettingsManager.class);
        emulator.initialize(settingsManager);

        verify(contextPool);

        BrainCPUContext cpuContext = cpuContextCapture.getValue();
        cpuContext.attachDevice(ioDevice);
    }

    @After
    public void tearDown() {
        emulator.destroy();
    }

    private void setupEmulator(byte[] program, byte[] data, byte[] input) {
        memory.setProgram(Objects.requireNonNull(program));
        emulator.reset();
        if (data != null) {
            memory.setData(data);
        }
        if (input != null) {
            ioDevice.setInput(input);
        }
    }

    private void emulateWithBreakpoint(byte[] program, byte[] data, byte[] input, int breakpoint) {
        setupEmulator(program, data, input);
        emulator.setBreakpoint(breakpoint);
        try {
            assertEquals(CPU.RunState.STATE_STOPPED_BREAK, emulator.call());
        } finally {
            emulator.unsetBreakpoint(breakpoint);
        }
    }

    private void emulate(byte[] program, byte[] data, byte[] input) {
        setupEmulator(program, data, input);
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, emulator.call());
    }

    @Test
    public void testResetSetsCorrectlyP() {
        memory.setProgram(new byte[]{7, 4, 8});

        emulator.reset();
        assertEquals(4, emulator.getEngine().P);
    }

    @Test(timeout = 3000)
    public void testClearCell() {
        // [-]
        memory.setProgram(new byte[]{7, 4, 8});

        emulator.reset();

        memory.setData(new byte[]{2});

        emulator.step(); // [
        emulator.step(); // -

        assertEquals(1, memory.read(memory.getDataStart()).byteValue());

        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, emulator.call());

        assertEquals(0, memory.read(memory.getDataStart()).byteValue());
        assertEquals(4, emulator.getInstructionPosition());
    }

    @Test(timeout = 3000)
    public void testSimpleOutput() {
        // +[,.--]
        byte[] program = new byte[] { 3, 7, 6, 5, 4, 4, 8 };
        byte[] input = new byte[] { 4, 3, 2 };

        emulate(program, null, input);

        assertTrue(ioDevice.wasInputRead());

        List<Short> output = ioDevice.getOutput();

        assertEquals(3, output.size());
        assertEquals(4, output.get(0).byteValue());
        assertEquals(3, output.get(1).byteValue());
        assertEquals(2, output.get(2).byteValue());
    }

    @Test(timeout = 3000)
    public void testMoveCell() {
        // [->+<]
        byte[] program = new byte[] { 7, 4, 1, 3, 2, 8 };
        byte[] data = new byte[] { 4 };

        emulate(program, data, null);

        assertEquals(memory.getDataStart(), emulator.getEngine().P);
        assertEquals(0, memory.read(memory.getDataStart()).byteValue());
        assertEquals(4, memory.read(memory.getDataStart() + 1).byteValue());
    }

    @Test(timeout = 3000)
    public void testCopyCell() {
        // [->+>+<<]
        byte[] program = new byte[] { 7, 4, 1, 3, 1, 3, 2, 2, 8 };
        byte[] data = new byte[] { 4 };

        emulate(program, data, null);

        assertEquals(memory.getDataStart(), emulator.getEngine().P);
        assertEquals(0, memory.read(memory.getDataStart()).byteValue());
        assertEquals(4, memory.read(memory.getDataStart() + 1).byteValue());
        assertEquals(4, memory.read(memory.getDataStart() + 2).byteValue());
    }

    @Test(timeout = 3000)
    public void testCopyCell2() {
        // [->+>+<<]>>[-<<+>>]
        byte[] program = new byte[] {
                7, 4, 1, 3, 1, 3, 2, 2, 8, 1, 1, 7, 4, 2, 2, 3,
                1, 1, 8
        };
        byte[] data = new byte[] { 4 };

        emulate(program, data, null);

        assertEquals(memory.getDataStart() + 2, emulator.getEngine().P);
        assertEquals(4, memory.read(memory.getDataStart()).byteValue());
        assertEquals(4, memory.read(memory.getDataStart() + 1).byteValue());
        assertEquals(0, memory.read(memory.getDataStart() + 2).byteValue());
    }

    @Test(timeout = 3000)
    public void testAddition() {
        // ,>++++++[<-------->-],[<+>-],<.>.
        byte[] program = new byte[] {
                6, 1, 3, 3, 3, 3, 3, 3, 7, 2, 4, 4, 4, 4, 4, 4,
                4, 4, 1, 4, 8, 6, 7, 2, 3, 1, 4, 8, 6, 2, 5, 1,
                5
        };
        byte[] input = new byte[] { '4', '4', '\n' };

        emulate(program, null, input);

        assertEquals(memory.getDataStart() + 1, emulator.getEngine().P);
        assertEquals('8', memory.read(memory.getDataStart()).byteValue());
        assertEquals('\n', memory.read(memory.getDataStart() + 1).byteValue());

        assertTrue(ioDevice.wasInputRead());

        List<Short> output = ioDevice.getOutput();
        assertEquals(2, output.size());
        assertEquals('8', output.get(0).byteValue());
        assertEquals('\n', output.get(1).byteValue());
    }

    @Test(timeout = 3000)
    public void testMoreAddition() {
        // ,>++++++[<-------->-],[<+>-],<.>.
        byte[] program = new byte[] {
                6, 1, 3, 3, 3, 3, 3, 3, 7, 2, 4, 4, 4, 4, 4, 4,
                4, 4, 1, 4, 8, 6, 7, 2, 3, 1, 4, 8, 6, 2, 5, 1,
                5 };
        byte[] input = new byte[] { '8', '8', 'a' };

        emulate(program, null, input);

        assertEquals(memory.getDataStart() + 1, emulator.getEngine().P);
        assertEquals(64, memory.read(memory.getDataStart()).byteValue()); // 64 is ASCII for '0' + 16
        assertEquals('a', memory.read(memory.getDataStart() + 1).byteValue());

        assertTrue(ioDevice.wasInputRead());

        List<Short> output = ioDevice.getOutput();
        assertEquals(2, output.size());
        assertEquals(64, output.get(0).byteValue());
        assertEquals('a', output.get(1).byteValue());
    }

    @Test(timeout = 3000)
    public void testMultiplyThreeTimesFive() {
        // +++[>+++++<-]
        byte[] program = new byte[] { 3, 3, 3, 7, 1, 3, 3, 3, 3, 3, 2, 4, 8 };

        emulate(program, null, null);

        assertEquals(memory.getDataStart(), emulator.getEngine().P);
        assertEquals(0, memory.read(memory.getDataStart()).byteValue());
        assertEquals(3 * 5, memory.read(memory.getDataStart() + 1).byteValue());
    }

    @Test(timeout = 3000)
    public void testMultiply() {
        // [>>>+>+<<<<-]>>>>[<<<<+>>>>-]<[<<[>>>+>+<<<<-]>>>>[<<<<+>>>>-]<[<<+>>-]<-]

        byte[] program = new byte[] {
                7, 1, 1, 1, 3, 1, 3, 2, 2, 2, 2, 4, 8, 1, 1, 1,
                1, 7, 2, 2, 2, 2, 3, 1, 1, 1, 1, 4, 8, 2, 7, 2,
                2, 7, 1, 1, 1, 3, 1, 3, 2, 2, 2, 2, 4, 8, 1, 1,
                1, 1, 7, 2, 2, 2, 2, 3, 1, 1, 1, 1, 4, 8, 2, 7,
                2, 2, 3, 1, 1, 4, 8, 2, 4, 8
        };
        byte[] data = new byte[] { 20, 5 };

        emulate(program, data, null);

        assertEquals(memory.getDataStart() + 3, emulator.getEngine().P);
        assertEquals(20, memory.read(memory.getDataStart()).byteValue());
        assertEquals(5, memory.read(memory.getDataStart() + 1).byteValue());
        assertEquals(20 * 5, memory.read(memory.getDataStart() + 2).byteValue());
    }

    @Test(timeout = 3000)
    public void testDecrementZeroGives255() {
        // -
        byte[] program = new byte[] { 4 };

        emulate(program, null, null);

        assertEquals(memory.getDataStart(), emulator.getEngine().P);
        assertEquals(255, memory.read(memory.getDataStart()).shortValue());
    }

    @Test(timeout = 3000)
    public void testTwoLoopsInLoop() {
        // +[[-]+[-]]
        byte[] program = new byte[] { 3, 7, 7, 4, 8, 3, 7, 4, 8, 8 };

        emulate(program, null, null);
        assertEquals(11, emulator.getInstructionPosition());
    }

    @Test(timeout = 3000)
    public void testMinusPlusGivesZero() {
        // -+
        byte[] program = new byte[] { 4, 3 };

        emulate(program, null, null);
        assertEquals(0, memory.read(emulator.getEngine().P).byteValue());
    }

    @Test(timeout = 3000)
    public void testSelfPrint() {
        // +++++[>+++++++++<-],[[>--.++>+<<-]>+.->[<.>-]<<,]
        byte[] program = new byte[] {
                3, 3, 3, 3, 3, 7, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3,
                2, 4, 8, 6, 7, 7, 1, 4, 4, 5, 3, 3, 1, 3, 2, 2,
                4, 8, 1, 3, 5, 4, 1, 7, 2, 5, 1, 4, 8, 2, 2, 6,
                8
        };
        byte[] input = new byte[] { 1, 0 };

        emulateWithBreakpoint(program, null, input, 19);

        assertEquals(19, emulator.getInstructionPosition());
        assertEquals(45, memory.read(emulator.getEngine().P + 1).shortValue());

        emulator.step(); // ,
        assertEquals(1, memory.read(emulator.getEngine().P).shortValue());

        emulator.step(); // [
        emulator.step(); // [
        emulator.step(); // >
        assertEquals(memory.getDataStart() + 1, emulator.getEngine().P);
        assertEquals(45, memory.read(emulator.getEngine().P).shortValue());

        emulator.step(); // -
        emulator.step(); // -
        assertEquals(43, memory.read(emulator.getEngine().P).shortValue());

        emulator.step(); // .

        emulator.step(); // +
        emulator.step(); // +
        assertEquals(45, memory.read(emulator.getEngine().P).shortValue());

        emulator.step(); // >
        assertEquals(memory.getDataStart() + 2, emulator.getEngine().P);
        assertEquals(0, memory.read(emulator.getEngine().P).shortValue());
        assertEquals(29, emulator.getInstructionPosition());
        assertEquals(3, memory.read(emulator.getInstructionPosition()).shortValue());


        emulator.step(); // +
        assertEquals(1, memory.read(emulator.getEngine().P).shortValue());

        emulator.call();

        assertTrue(ioDevice.wasInputRead());

        List<Short> output = ioDevice.getOutput();
        assertEquals(3, output.size());
    }

}
