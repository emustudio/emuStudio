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

        ula.write(0xFE, (byte) 0x18);
        ula.passedCycles(10);

        ula.write(0xFE, (byte) 0x10);
        ula.passedCycles(10);

        ula.write(0xFE, (byte) 0x08);
        ula.passedCycles(10);

        ula.write(0xFE, (byte) 0x00);
        ula.passedCycles(10);
        ula.close();

        assertEquals(List.of(2, 3, 0, 1), beeper.levels);
        assertEquals(40, beeper.cycles);
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
    public void testKeysComposeRegardlessOfSource() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        // Press SHIFT via direct API (overlay click) and Z via host keyboard WITH shift held
        ula.pressKey((byte) 0, (byte) 1);
        ula.onKeyEvent(keyPressed(KeyEvent.VK_Z, KeyEvent.SHIFT_DOWN_MASK));
        assertEquals(0xBC, ula.read(0xFEFE) & 0xFF); // SHIFT + Z

        // Release Z (shift still held on host)
        ula.onKeyEvent(keyReleased(KeyEvent.VK_Z, KeyEvent.SHIFT_DOWN_MASK));
        assertEquals(0xBE, ula.read(0xFEFE) & 0xFF); // only SHIFT
        assertTrue(ula.isKeyPressed((byte) 0, (byte) 1));

        // Release SHIFT from direct API (overlay) — releases regardless of source
        ula.releaseKey((byte) 0, (byte) 1);
        assertEquals(0xBF, ula.read(0xFEFE) & 0xFF);
        assertFalse(ula.isKeyPressed((byte) 0, (byte) 1));
    }

    @Test
    public void testResetRestoresBorderColorAndKeyboardMatrix() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        ula.write(0xFE, (byte) 0x02);
        ula.onKeyEvent(keyPressed(KeyEvent.VK_A, 0));
        ula.pressKey((byte) 0, (byte) 1);

        ula.reset();

        assertEquals(7, ula.getBorderColor());
        assertEquals(0xBF, ula.read(0xFDFE) & 0xFF);
        assertFalse(ula.isKeyPressed((byte) 0, (byte) 1));
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

        assertFalse(ula.videoFlash.get());
        assertEquals(VIDEO_FLASH_FRAME, bus.interruptSignals);

        ula.onNextFrame();

        assertTrue(ula.videoFlash.get());
        assertEquals(VIDEO_FLASH_FRAME + 1, bus.interruptSignals);

        for (int i = 0; i < VIDEO_FLASH_FRAME + 1; i++) {
            ula.onNextFrame();
        }

        assertFalse(ula.videoFlash.get());
        assertEquals((VIDEO_FLASH_FRAME + 1) * 2, bus.interruptSignals);
    }

    @Test
    public void testFrameInterruptUsesFloatingBusValueForIm2Vectoring() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        ula.onNextFrame();

        assertEquals(1, bus.interruptSignals);
        assertEquals(1, bus.interruptData.size());
        assertEquals(0xFF, bus.interruptData.get(0)[0] & 0xFF);
    }

    // --- Border color ---

    @Test
    public void testWriteSetsBorderColorFromLowest3Bits() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        ula.write(0xFE, (byte) 0x05); // border = 5
        assertEquals(5, ula.getBorderColor());

        ula.write(0xFE, (byte) 0xFF); // border = 7 (lowest 3 bits)
        assertEquals(7, ula.getBorderColor());

        ula.write(0xFE, (byte) 0xF8); // border = 0
        assertEquals(0, ula.getBorderColor());
    }

    @Test
    public void testDefaultBorderColorIs7AfterConstruction() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());
        // Before any write, reset sets it to 7
        ula.reset();
        assertEquals(7, ula.getBorderColor());
    }

    // --- clearInterrupt ---

    @Test
    public void testClearInterruptDelegatesToBus() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        assertEquals(0, bus.interruptClears);
        ula.clearInterrupt();
        assertEquals(1, bus.interruptClears);
    }

    // --- getName / toString ---

    @Test
    public void testGetNameReturnsULAIdentifier() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        assertEquals("ZX-Spectrum48K ULA", ula.getName());
    }

    @Test
    public void testToStringReturnsULAIdentifier() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        assertEquals("ZX-Spectrum48K ULA", ula.toString());
    }

    // --- Tape input mixing ---

    @Test
    public void testPassedCyclesMixesTapeInputIntoBeeper() {
        MockBus bus = new MockBus();
        RecordingBeeper beeper = new RecordingBeeper();
        ULA ula = new ULA(bus.bus(), beeper);

        bus.setLineIn(1); // tape input high (bit 0 = 1)
        ula.passedCycles(10);

        // Tape input changed from false to true => setLevel called with tapeIn=true
        assertTrue(beeper.tapeInValues.size() > 0);
        assertTrue("tapeIn should be true", beeper.tapeInValues.get(beeper.tapeInValues.size() - 1));
    }

    @Test
    public void testPassedCyclesDoesNotCallSetLevelWhenTapeDoesNotChange() {
        MockBus bus = new MockBus();
        RecordingBeeper beeper = new RecordingBeeper();
        ULA ula = new ULA(bus.bus(), beeper);

        // lineIn = 0 (tape input low, same as initial state)
        bus.setLineIn(0);
        ula.passedCycles(10);
        ula.passedCycles(10);

        // No tape input change => setLevel should not be called by passedCycles
        assertEquals(0, beeper.tapeInValues.size());
    }

    // --- Keyboard: KEY_TYPED ignored ---

    @Test
    public void testKeyTypedEventIsIgnored() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        KeyEvent typed = new KeyEvent(new Canvas(), KeyEvent.KEY_TYPED, 0, 0,
                KeyEvent.VK_UNDEFINED, 'a');
        boolean handled = ula.onKeyEvent(typed);
        assertFalse("KEY_TYPED should be ignored", handled);
    }

    // --- Keyboard: Symbol shift (CTRL modifier) ---

    @Test
    public void testCtrlModifierPressesSymbolShift() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        ula.onKeyEvent(keyPressed(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK));
        // Symbol shift is on line 7, value 2
        assertTrue(ula.isKeyPressed((byte) 7, (byte) 2));
    }

    @Test
    public void testAltModifierPressesSymbolShift() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        ula.onKeyEvent(keyPressed(KeyEvent.VK_Z, KeyEvent.ALT_DOWN_MASK));
        assertTrue(ula.isKeyPressed((byte) 7, (byte) 2));
    }

    // --- Keyboard: Backspace/Delete -> SHIFT + 0 ---

    @Test
    public void testBackspaceMapsToShiftPlusZero() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        ula.onKeyEvent(keyPressed(KeyEvent.VK_BACK_SPACE, 0));
        // Backspace = shift(line=0, val=1) + 0(line=4, val=1)
        assertTrue("SHIFT should be pressed", ula.isKeyPressed((byte) 0, (byte) 1));
        assertTrue("0 should be pressed", ula.isKeyPressed((byte) 4, (byte) 1));
    }

    @Test
    public void testDeleteMapsToShiftPlusZero() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        ula.onKeyEvent(keyPressed(KeyEvent.VK_DELETE, 0));
        assertTrue("SHIFT should be pressed", ula.isKeyPressed((byte) 0, (byte) 1));
        assertTrue("0 should be pressed", ula.isKeyPressed((byte) 4, (byte) 1));
    }

    // --- Audio delegation ---

    @Test
    public void testGetAudioSampleRateDelegatesToBeeper() {
        MockBus bus = new MockBus();
        Beeper beeper = new Beeper(AudioSink.NULL, 22_050);
        ULA ula = new ULA(bus.bus(), beeper);

        assertEquals(22_050, ula.getAudioSampleRate());
    }

    @Test
    public void testGetAudioVolumePercentDelegatesToBeeper() {
        MockBus bus = new MockBus();
        Beeper beeper = new Beeper(AudioSink.NULL, 100);
        ULA ula = new ULA(bus.bus(), beeper);

        assertEquals(100, ula.getAudioVolumePercent());
    }

    @Test
    public void testSetAudioVolumePercentDelegatesToBeeper() {
        MockBus bus = new MockBus();
        Beeper beeper = new Beeper(AudioSink.NULL, 100);
        ULA ula = new ULA(bus.bus(), beeper);

        ula.setAudioVolumePercent(42);
        assertEquals(42, ula.getAudioVolumePercent());
    }

    // --- readLine ---

    @Test
    public void testReadLineSingleLinePopulatesVideoAndAttributeMemory() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        // Line 0 in screen bitmap: address 0x4000 + offset for y=0 = 0x4000
        bus.setMemory(0x4000, 0xAA);
        // Attribute for line 0: address 0x5800
        bus.setMemory(0x5800, 0x38);

        ula.readLine(0);

        assertEquals((byte) 0xAA, ula.videoMemory[0][0]);
        assertEquals((byte) 0x38, ula.attributeMemory[0][0]);
    }

    // --- Port address filtering ---

    @Test
    public void testReadWithOddPortAddressReturnsDefault() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        // Port 0xFF (odd, bit 0 = 1) — not port 0xFE, so no keyboard line should be read
        // Result should still include EAR input but no keyboard bits should be cleared
        int result = ula.read(0xFEFF) & 0xFF;
        // When no lines are selected (none of bits 0-7 are low), result = 0xBF | (lineIn << 6)
        assertEquals(0xBF, result);
    }

    // --- Colon (sym shift + Z) ---

    @Test
    public void testColonKeyMapsToSymShiftPlusZ() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        ula.onKeyEvent(keyPressed(KeyEvent.VK_COLON, 0));
        // Colon = command[2]=0 (release shift), command[3]=1 (press sym shift) + Z(line=0, val=2)
        assertTrue("SYM SHIFT should be pressed", ula.isKeyPressed((byte) 7, (byte) 2));
        assertTrue("Z should be pressed", ula.isKeyPressed((byte) 0, (byte) 2));
    }

    // --- Enter ---

    @Test
    public void testEnterKeyMapsCorrectly() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        ula.onKeyEvent(keyPressed(KeyEvent.VK_ENTER, 0));
        // Enter = line 6, value 1
        assertTrue(ula.isKeyPressed((byte) 6, (byte) 1));
    }

    @Test
    public void testEnterKeyRelease() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        ula.onKeyEvent(keyPressed(KeyEvent.VK_ENTER, 0));
        ula.onKeyEvent(keyReleased(KeyEvent.VK_ENTER, 0));
        assertFalse(ula.isKeyPressed((byte) 6, (byte) 1));
    }

    // --- Space ---

    @Test
    public void testSpaceKeyMapsCorrectly() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        ula.onKeyEvent(keyPressed(KeyEvent.VK_SPACE, 0));
        // Space = line 7, value 1
        assertTrue(ula.isKeyPressed((byte) 7, (byte) 1));
    }

    // --- Unknown key with shift/symshift modifiers ---

    @Test
    public void testUnknownKeyWithShiftHeldPressesShiftInMatrix() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        // F1 is not in CHAR_MAPPING, but shift is held
        ula.onKeyEvent(keyPressed(KeyEvent.VK_F1, KeyEvent.SHIFT_DOWN_MASK));
        assertTrue("SHIFT should be pressed", ula.isKeyPressed((byte) 0, (byte) 1));
    }

    @Test
    public void testUnknownKeyWithoutModifiersReleasesShiftAndSymShift() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        // First press shift
        ula.pressKey((byte) 0, (byte) 1);
        ula.pressKey((byte) 7, (byte) 2);
        assertTrue(ula.isKeyPressed((byte) 0, (byte) 1));
        assertTrue(ula.isKeyPressed((byte) 7, (byte) 2));

        // Now press F1 without any modifier — should release both shift and sym shift
        ula.onKeyEvent(keyPressed(KeyEvent.VK_F1, 0));
        assertFalse(ula.isKeyPressed((byte) 0, (byte) 1));
        assertFalse(ula.isKeyPressed((byte) 7, (byte) 2));
    }

    // --- EAR input bit in port read ---

    @Test
    public void testEarInputBitReflectsBusData() {
        MockBus bus = new MockBus();
        ULA ula = new ULA(bus.bus());

        bus.setLineIn(0);
        assertEquals(0xBF, ula.read(0xFEFE) & 0xFF); // bit 6 = 0

        bus.setLineIn(1);
        assertEquals(0xFF, ula.read(0xFEFE) & 0xFF); // bit 6 = 1
    }

    private static KeyEvent keyPressed(int keyCode, int modifiersEx) {
        return new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED, 0, modifiersEx, keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    private static KeyEvent keyReleased(int keyCode, int modifiersEx) {
        return new KeyEvent(new Canvas(), KeyEvent.KEY_RELEASED, 0, modifiersEx, keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    private static final class RecordingBeeper extends Beeper {
        private final List<Integer> levels = new ArrayList<>();
        private final List<Boolean> tapeInValues = new ArrayList<>();
        private long cycles;

        private RecordingBeeper() {
            super(AudioSink.NULL, 120);
        }

        @Override
        public void setLevel(boolean earOn, boolean micOn, boolean tapeIn) {
            levels.add((earOn ? 2 : 0) | (micOn ? 1 : 0));
            tapeInValues.add(tapeIn);
        }

        @Override
        public void passedCycles(long cycles) {
            this.cycles += cycles;
        }
    }

    private static final class MockBus {
        private final byte[] memory = new byte[0x10000];
        private final ZxSpectrumBus bus = createNiceMock(ZxSpectrumBus.class);
        private final List<byte[]> interruptData = new ArrayList<>();
        private byte lineIn;
        private int interruptSignals;
        private int interruptClears;

        private MockBus() {
            expect(bus.readData()).andStubAnswer(() -> lineIn);
            expect(bus.readMemoryNotContended(anyInt())).andStubAnswer(() -> {
                int address = (int) getCurrentArguments()[0];
                return memory[address & 0xFFFF];
            });
            bus.signalInterrupt(anyObject(byte[].class));
            expectLastCall().andAnswer(() -> {
                interruptSignals++;
                byte[] data = (byte[]) getCurrentArguments()[0];
                interruptData.add(data.clone());
                return null;
            }).anyTimes();
            bus.clearInterrupt();
            expectLastCall().andAnswer(() -> {
                interruptClears++;
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
