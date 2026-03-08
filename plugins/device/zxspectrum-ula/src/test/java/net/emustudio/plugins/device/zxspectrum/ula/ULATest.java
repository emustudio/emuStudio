/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula;

import net.emustudio.plugins.device.zxspectrum.bus.api.ZxSpectrumBus;
import net.emustudio.plugins.device.zxspectrum.ula.audio.AudioSink;
import net.emustudio.plugins.device.zxspectrum.ula.audio.Beeper;
import org.junit.Test;

import java.awt.Canvas;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import static net.emustudio.plugins.device.zxspectrum.ula.ULA.VIDEO_FLASH_FRAME;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ULATest {

    @Test
    public void testPortFeSpeakerLevelUsesEarAndMicBits() {
        MockBus bus = new MockBus();
        RecordingBeeper beeper = new RecordingBeeper();
        ULA ula = new ULA(bus.bus(), beeper);

        ula.write(0xFE, (byte) 0x10);
        ula.passedCycles(10);

        ula.write(0xFE, (byte) 0x00);
        ula.passedCycles(10);

        ula.write(0xFE, (byte) 0x08);
        ula.passedCycles(10);
        ula.close();

        assertEquals(List.of(Boolean.TRUE, Boolean.TRUE, Boolean.FALSE), beeper.levels);
        assertEquals(30, beeper.cycles);
    }

    @Test
    public void testReadIncludesSelectedKeyboardLineAndEarInputBit() {
        MockBus bus = new MockBus();
        bus.setLineIn(1);
        ULA ula = new ULA(bus.bus());

        ula.onKeyEvent(keyPressed(KeyEvent.VK_A, 0));
        assertEquals(0xFE, ula.read(0xFDFE) & 0xFF);

        ula.onKeyEvent(keyReleased(KeyEvent.VK_A, 0));
        assertEquals(0xFF, ula.read(0xFDFE) & 0xFF);
    }

    @Test
    public void testReadAndsAllSelectedKeyboardLines() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        ula.onKeyEvent(keyPressed(KeyEvent.VK_A, 0));
        ula.onKeyEvent(keyPressed(KeyEvent.VK_W, 0));

        assertEquals(0xBC, ula.read(0xF9FE) & 0xFF);
    }

    @Test
    public void testShiftModifierPressesShiftMatrixBitAlongsideKey() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        ula.onKeyEvent(keyPressed(KeyEvent.VK_Z, KeyEvent.SHIFT_DOWN_MASK));
        assertEquals(0xBC, ula.read(0xFEFE) & 0xFF);
    }

    @Test
    public void testResetRestoresBorderColorAndKeyboardMatrix() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        ula.write(0xFE, (byte) 0x02);
        ula.onKeyEvent(keyPressed(KeyEvent.VK_A, 0));

        ula.reset();

        assertEquals(7, ula.getBorderColor());
        assertEquals(0xBF, ula.read(0xFDFE) & 0xFF);
    }

    @Test
    public void testReadScreenUsesSpectrumMemoryLayout() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        bus.setMemory(0x4920, 0x11);
        bus.setMemory(0x492A, 0x22);
        bus.setMemory(0x493F, 0x33);
        bus.setMemory(0x5920, 0x44);
        bus.setMemory(0x592A, 0x55);
        bus.setMemory(0x593F, 0x66);

        ula.readScreen();

        assertEquals(0x11, ula.videoMemory[0][73] & 0xFF);
        assertEquals(0x22, ula.videoMemory[10][73] & 0xFF);
        assertEquals(0x33, ula.videoMemory[31][73] & 0xFF);
        assertEquals(0x44, ula.attributeMemory[0][9] & 0xFF);
        assertEquals(0x55, ula.attributeMemory[10][9] & 0xFF);
        assertEquals(0x66, ula.attributeMemory[31][9] & 0xFF);
    }

    @Test
    public void testFrameInterruptAndFlashToggleAreDrivenByNextFrame() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        for (int i = 0; i < VIDEO_FLASH_FRAME; i++) {
            ula.onNextFrame();
        }

        assertFalse(ula.videoFlash);
        assertEquals(VIDEO_FLASH_FRAME, bus.interruptSignals);

        ula.onNextFrame();

        assertTrue(ula.videoFlash);
        assertEquals(VIDEO_FLASH_FRAME + 1, bus.interruptSignals);

        for (int i = 0; i < VIDEO_FLASH_FRAME + 1; i++) {
            ula.onNextFrame();
        }

        assertFalse(ula.videoFlash);
        assertEquals((VIDEO_FLASH_FRAME + 1) * 2, bus.interruptSignals);
    }

    private static KeyEvent keyPressed(int keyCode, int modifiersEx) {
        return new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, 0, modifiersEx, keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    private static KeyEvent keyReleased(int keyCode, int modifiersEx) {
        return new KeyEvent(new Canvas(), KeyEvent.KEY_RELEASED, 0, modifiersEx, keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    private static final class RecordingBeeper extends Beeper {
        private final List<Boolean> levels = new ArrayList<>();
        private long cycles;

        private RecordingBeeper() {
            super(AudioSink.NULL, 1200, 120, 16);
        }

        @Override
        public void setLevel(boolean levelHigh) {
            levels.add(levelHigh);
        }

        @Override
        public void passedCycles(long cycles) {
            this.cycles += cycles;
        }
    }

    private static final class MockBus {
        private final byte[] memory = new byte[0x10000];
        private final ZxSpectrumBus bus = createNiceMock(ZxSpectrumBus.class);
        private byte lineIn;
        private int interruptSignals;

        private MockBus() {
            expect(bus.readData()).andStubAnswer(() -> lineIn);
            expect(bus.readMemoryNotContended(anyInt())).andStubAnswer(() -> {
                int address = (int) getCurrentArguments()[0];
                return memory[address & 0xFFFF];
            });
            bus.signalInterrupt(anyObject(byte[].class));
            expectLastCall().andAnswer(() -> {
                interruptSignals++;
                return null;
            }).anyTimes();
            replay(bus);
        }

        private ZxSpectrumBus bus() {
            return bus;
        }

        private void setLineIn(int lineIn) {
            this.lineIn = (byte) lineIn;
        }

        private void setMemory(int address, int value) {
            memory[address & 0xFFFF] = (byte) value;
        }
    }
}
