/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88sio;

import net.emustudio.emulib.runtime.settings.BasicSettings;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class SioUnitSettingsTest {

    private BasicSettings basicSettings;

    @Before
    public void setup() {
        basicSettings = createNiceMock(BasicSettings.class);
        expect(basicSettings.getBoolean("clearInputBit8", false)).andReturn(false).anyTimes();
        expect(basicSettings.getBoolean("clearOutputBit8", false)).andReturn(false).anyTimes();
        expect(basicSettings.getBoolean("inputToUpperCase", false)).andReturn(false).anyTimes();
        expect(basicSettings.getString("mapDeleteChar")).andReturn(Optional.empty()).anyTimes();
        expect(basicSettings.getString("mapBackspaceChar")).andReturn(Optional.empty()).anyTimes();
        expect(basicSettings.getBoolean("interruptsSupported")).andReturn(Optional.of(true)).anyTimes();
        expect(basicSettings.getInt("inputInterruptVector")).andReturn(Optional.of(7)).anyTimes();
        expect(basicSettings.getInt("outputInterruptVector")).andReturn(Optional.of(7)).anyTimes();
        expect(basicSettings.getString("statusPorts", "")).andReturn("").anyTimes();
        expect(basicSettings.getString("dataPorts", "")).andReturn("").anyTimes();
    }

    private SioUnitSettings createSettings() {
        replay(basicSettings);
        return new SioUnitSettings(basicSettings);
    }

    @Test
    public void testDefaultClearInputBit8IsFalse() {
        assertFalse(createSettings().isClearInputBit8());
    }

    @Test
    public void testDefaultClearOutputBit8IsFalse() {
        assertFalse(createSettings().isClearOutputBit8());
    }

    @Test
    public void testDefaultInputToUpperCaseIsFalse() {
        assertFalse(createSettings().isInputToUpperCase());
    }

    @Test
    public void testDefaultMapDeleteCharIsUnchanged() {
        assertEquals(SioUnitSettings.MAP_CHAR.UNCHANGED, createSettings().getMapDeleteChar());
    }

    @Test
    public void testDefaultMapBackspaceCharIsUnchanged() {
        assertEquals(SioUnitSettings.MAP_CHAR.UNCHANGED, createSettings().getMapBackspaceChar());
    }

    @Test
    public void testDefaultInterruptsSupportedIsTrue() {
        assertTrue(createSettings().getInterruptsSupported());
    }

    @Test
    public void testDefaultInputInterruptVectorIs7() {
        assertEquals(7, createSettings().getInputInterruptVector());
    }

    @Test
    public void testDefaultOutputInterruptVectorIs7() {
        assertEquals(7, createSettings().getOutputInterruptVector());
    }

    @Test
    public void testDefaultStatusPortsEmpty() {
        assertTrue(createSettings().getStatusPorts().isEmpty());
    }

    @Test
    public void testDefaultDataPortsEmpty() {
        assertTrue(createSettings().getDataPorts().isEmpty());
    }

    @Test
    public void testSetClearInputBit8() {
        SioUnitSettings settings = createSettings();
        settings.setClearInputBit8(true);
        assertTrue(settings.isClearInputBit8());
    }

    @Test
    public void testSetClearOutputBit8() {
        SioUnitSettings settings = createSettings();
        settings.setClearOutputBit8(true);
        assertTrue(settings.isClearOutputBit8());
    }

    @Test
    public void testSetInputToUpperCase() {
        SioUnitSettings settings = createSettings();
        settings.setInputToUpperCase(true);
        assertTrue(settings.isInputToUpperCase());
    }

    @Test
    public void testSetMapDeleteChar() {
        SioUnitSettings settings = createSettings();
        settings.setMapDeleteChar(SioUnitSettings.MAP_CHAR.BACKSPACE);
        assertEquals(SioUnitSettings.MAP_CHAR.BACKSPACE, settings.getMapDeleteChar());
    }

    @Test
    public void testSetMapBackspaceChar() {
        SioUnitSettings settings = createSettings();
        settings.setMapBackspaceChar(SioUnitSettings.MAP_CHAR.DELETE);
        assertEquals(SioUnitSettings.MAP_CHAR.DELETE, settings.getMapBackspaceChar());
    }

    @Test
    public void testSetStatusPorts() {
        SioUnitSettings settings = createSettings();
        settings.setStatusPorts(List.of(0x10, 0x14));
        assertEquals(List.of(0x10, 0x14), settings.getStatusPorts());
    }

    @Test
    public void testSetDataPorts() {
        SioUnitSettings settings = createSettings();
        settings.setDataPorts(List.of(0x11, 0x15));
        assertEquals(List.of(0x11, 0x15), settings.getDataPorts());
    }

    @Test
    public void testSetInterruptsSupported() {
        SioUnitSettings settings = createSettings();
        settings.setInterruptsSupported(false);
        assertFalse(settings.getInterruptsSupported());
    }

    @Test
    public void testSetInputInterruptVector() {
        SioUnitSettings settings = createSettings();
        settings.setInputInterruptVector(3);
        assertEquals(3, settings.getInputInterruptVector());
    }

    @Test
    public void testSetOutputInterruptVector() {
        SioUnitSettings settings = createSettings();
        settings.setOutputInterruptVector(5);
        assertEquals(5, settings.getOutputInterruptVector());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetInputInterruptVectorTooLowThrows() {
        SioUnitSettings settings = createSettings();
        settings.setInputInterruptVector(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetInputInterruptVectorTooHighThrows() {
        SioUnitSettings settings = createSettings();
        settings.setInputInterruptVector(8);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetOutputInterruptVectorTooLowThrows() {
        SioUnitSettings settings = createSettings();
        settings.setOutputInterruptVector(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetOutputInterruptVectorTooHighThrows() {
        SioUnitSettings settings = createSettings();
        settings.setOutputInterruptVector(8);
    }

    @Test
    public void testSetInputInterruptVectorBoundary0() {
        SioUnitSettings settings = createSettings();
        settings.setInputInterruptVector(0);
        assertEquals(0, settings.getInputInterruptVector());
    }

    @Test
    public void testSetInputInterruptVectorBoundary7() {
        SioUnitSettings settings = createSettings();
        settings.setInputInterruptVector(7);
        assertEquals(7, settings.getInputInterruptVector());
    }

    @Test
    public void testSetOutputInterruptVectorBoundary0() {
        SioUnitSettings settings = createSettings();
        settings.setOutputInterruptVector(0);
        assertEquals(0, settings.getOutputInterruptVector());
    }

    @Test
    public void testSetOutputInterruptVectorBoundary7() {
        SioUnitSettings settings = createSettings();
        settings.setOutputInterruptVector(7);
        assertEquals(7, settings.getOutputInterruptVector());
    }

    @Test
    public void testObserverIsNotifiedOnSettingsChange() {
        SioUnitSettings settings = createSettings();
        AtomicBoolean called = new AtomicBoolean(false);
        settings.addObserver(() -> called.set(true));

        settings.setClearInputBit8(true);
        assertTrue(called.get());
    }

    @Test
    public void testObserverIsNotifiedOnSetClearOutputBit8() {
        SioUnitSettings settings = createSettings();
        AtomicBoolean called = new AtomicBoolean(false);
        settings.addObserver(() -> called.set(true));

        settings.setClearOutputBit8(true);
        assertTrue(called.get());
    }

    @Test
    public void testObserverIsNotifiedOnSetInputToUpperCase() {
        SioUnitSettings settings = createSettings();
        AtomicBoolean called = new AtomicBoolean(false);
        settings.addObserver(() -> called.set(true));

        settings.setInputToUpperCase(true);
        assertTrue(called.get());
    }

    @Test
    public void testObserverIsNotifiedOnSetMapDeleteChar() {
        SioUnitSettings settings = createSettings();
        AtomicBoolean called = new AtomicBoolean(false);
        settings.addObserver(() -> called.set(true));

        settings.setMapDeleteChar(SioUnitSettings.MAP_CHAR.BACKSPACE);
        assertTrue(called.get());
    }

    @Test
    public void testObserverIsNotifiedOnSetMapBackspaceChar() {
        SioUnitSettings settings = createSettings();
        AtomicBoolean called = new AtomicBoolean(false);
        settings.addObserver(() -> called.set(true));

        settings.setMapBackspaceChar(SioUnitSettings.MAP_CHAR.DELETE);
        assertTrue(called.get());
    }

    @Test
    public void testObserverIsNotifiedOnSetStatusPorts() {
        SioUnitSettings settings = createSettings();
        AtomicBoolean called = new AtomicBoolean(false);
        settings.addObserver(() -> called.set(true));

        settings.setStatusPorts(List.of(0x10));
        assertTrue(called.get());
    }

    @Test
    public void testObserverIsNotifiedOnSetDataPorts() {
        SioUnitSettings settings = createSettings();
        AtomicBoolean called = new AtomicBoolean(false);
        settings.addObserver(() -> called.set(true));

        settings.setDataPorts(List.of(0x11));
        assertTrue(called.get());
    }

    @Test
    public void testObserverIsNotifiedOnSetInterruptsSupported() {
        SioUnitSettings settings = createSettings();
        AtomicBoolean called = new AtomicBoolean(false);
        settings.addObserver(() -> called.set(true));

        settings.setInterruptsSupported(false);
        assertTrue(called.get());
    }

    @Test
    public void testObserverIsNotifiedOnSetInputInterruptVector() {
        SioUnitSettings settings = createSettings();
        AtomicBoolean called = new AtomicBoolean(false);
        settings.addObserver(() -> called.set(true));

        settings.setInputInterruptVector(3);
        assertTrue(called.get());
    }

    @Test
    public void testObserverIsNotifiedOnSetOutputInterruptVector() {
        SioUnitSettings settings = createSettings();
        AtomicBoolean called = new AtomicBoolean(false);
        settings.addObserver(() -> called.set(true));

        settings.setOutputInterruptVector(3);
        assertTrue(called.get());
    }

    @Test
    public void testClearObservers() {
        SioUnitSettings settings = createSettings();
        AtomicBoolean called = new AtomicBoolean(false);
        settings.addObserver(() -> called.set(true));
        settings.clearObservers();

        settings.setClearInputBit8(true);
        assertFalse(called.get());
    }

    @Test
    public void testDefaultStatusPortsListValues() {
        SioUnitSettings settings = createSettings();
        assertEquals(List.of(0x10, 0x14, 0x16, 0x18), settings.getDefaultStatusPorts());
    }

    @Test
    public void testDefaultDataPortsListValues() {
        SioUnitSettings settings = createSettings();
        assertEquals(List.of(0x11, 0x15, 0x17, 0x19), settings.getDefaultDataPorts());
    }

    @Test
    public void testPortsAreParsedFromSettings() {
        BasicSettings bs = createNiceMock(BasicSettings.class);
        expect(bs.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        expect(bs.getString(eq("mapDeleteChar"))).andReturn(Optional.empty()).anyTimes();
        expect(bs.getString(eq("mapBackspaceChar"))).andReturn(Optional.empty()).anyTimes();
        expect(bs.getBoolean("interruptsSupported")).andReturn(Optional.of(true)).anyTimes();
        expect(bs.getInt("inputInterruptVector")).andReturn(Optional.of(7)).anyTimes();
        expect(bs.getInt("outputInterruptVector")).andReturn(Optional.of(7)).anyTimes();
        expect(bs.getString("statusPorts", "")).andReturn("0x10, 0x14").anyTimes();
        expect(bs.getString("dataPorts", "")).andReturn("0x11, 0x15").anyTimes();
        replay(bs);

        SioUnitSettings settings = new SioUnitSettings(bs);
        assertEquals(List.of(0x10, 0x14), settings.getStatusPorts());
        assertEquals(List.of(0x11, 0x15), settings.getDataPorts());
    }

    @Test
    public void testMapCharValues() {
        SioUnitSettings settings = createSettings();

        settings.setMapDeleteChar(SioUnitSettings.MAP_CHAR.DELETE);
        assertEquals(SioUnitSettings.MAP_CHAR.DELETE, settings.getMapDeleteChar());

        settings.setMapDeleteChar(SioUnitSettings.MAP_CHAR.UNDERSCORE);
        assertEquals(SioUnitSettings.MAP_CHAR.UNDERSCORE, settings.getMapDeleteChar());

        settings.setMapDeleteChar(SioUnitSettings.MAP_CHAR.UNCHANGED);
        assertEquals(SioUnitSettings.MAP_CHAR.UNCHANGED, settings.getMapDeleteChar());
    }

    @Test
    public void testSettingsFromConfigWithMapChars() {
        BasicSettings bs = createNiceMock(BasicSettings.class);
        expect(bs.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        expect(bs.getString("mapDeleteChar")).andReturn(Optional.of("BACKSPACE")).anyTimes();
        expect(bs.getString("mapBackspaceChar")).andReturn(Optional.of("DELETE")).anyTimes();
        expect(bs.getBoolean("interruptsSupported")).andReturn(Optional.of(true)).anyTimes();
        expect(bs.getInt("inputInterruptVector")).andReturn(Optional.of(3)).anyTimes();
        expect(bs.getInt("outputInterruptVector")).andReturn(Optional.of(5)).anyTimes();
        expect(bs.getString("statusPorts", "")).andReturn("").anyTimes();
        expect(bs.getString("dataPorts", "")).andReturn("").anyTimes();
        replay(bs);

        SioUnitSettings settings = new SioUnitSettings(bs);
        assertEquals(SioUnitSettings.MAP_CHAR.BACKSPACE, settings.getMapDeleteChar());
        assertEquals(SioUnitSettings.MAP_CHAR.DELETE, settings.getMapBackspaceChar());
        assertEquals(3, settings.getInputInterruptVector());
        assertEquals(5, settings.getOutputInterruptVector());
    }

    @Test
    public void testInvalidInputInterruptVectorInConstructorDefaultsTo7() {
        BasicSettings bs = createNiceMock(BasicSettings.class);
        expect(bs.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        expect(bs.getString(eq("mapDeleteChar"))).andReturn(Optional.empty()).anyTimes();
        expect(bs.getString(eq("mapBackspaceChar"))).andReturn(Optional.empty()).anyTimes();
        expect(bs.getBoolean("interruptsSupported")).andReturn(Optional.of(true)).anyTimes();
        expect(bs.getInt("inputInterruptVector")).andReturn(Optional.of(99)).anyTimes();
        expect(bs.getInt("outputInterruptVector")).andReturn(Optional.of(7)).anyTimes();
        expect(bs.getString("statusPorts", "")).andReturn("").anyTimes();
        expect(bs.getString("dataPorts", "")).andReturn("").anyTimes();
        replay(bs);

        SioUnitSettings settings = new SioUnitSettings(bs);
        assertEquals(7, settings.getInputInterruptVector());
    }

    @Test
    public void testInvalidOutputInterruptVectorInConstructorDefaultsTo7() {
        BasicSettings bs = createNiceMock(BasicSettings.class);
        expect(bs.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        expect(bs.getString(eq("mapDeleteChar"))).andReturn(Optional.empty()).anyTimes();
        expect(bs.getString(eq("mapBackspaceChar"))).andReturn(Optional.empty()).anyTimes();
        expect(bs.getBoolean("interruptsSupported")).andReturn(Optional.of(true)).anyTimes();
        expect(bs.getInt("inputInterruptVector")).andReturn(Optional.of(7)).anyTimes();
        expect(bs.getInt("outputInterruptVector")).andReturn(Optional.of(-1)).anyTimes();
        expect(bs.getString("statusPorts", "")).andReturn("").anyTimes();
        expect(bs.getString("dataPorts", "")).andReturn("").anyTimes();
        replay(bs);

        SioUnitSettings settings = new SioUnitSettings(bs);
        assertEquals(7, settings.getOutputInterruptVector());
    }

    @Test
    public void testNegativeInputInterruptVectorInConstructorDefaultsTo7() {
        BasicSettings bs = createNiceMock(BasicSettings.class);
        expect(bs.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        expect(bs.getString(eq("mapDeleteChar"))).andReturn(Optional.empty()).anyTimes();
        expect(bs.getString(eq("mapBackspaceChar"))).andReturn(Optional.empty()).anyTimes();
        expect(bs.getBoolean("interruptsSupported")).andReturn(Optional.of(true)).anyTimes();
        expect(bs.getInt("inputInterruptVector")).andReturn(Optional.of(-5)).anyTimes();
        expect(bs.getInt("outputInterruptVector")).andReturn(Optional.of(7)).anyTimes();
        expect(bs.getString("statusPorts", "")).andReturn("").anyTimes();
        expect(bs.getString("dataPorts", "")).andReturn("").anyTimes();
        replay(bs);

        SioUnitSettings settings = new SioUnitSettings(bs);
        assertEquals(7, settings.getInputInterruptVector());
    }

    @Test
    public void testSetEmptyStatusPorts() {
        SioUnitSettings settings = createSettings();
        settings.setStatusPorts(List.of());
        assertTrue(settings.getStatusPorts().isEmpty());
    }

    @Test
    public void testSetEmptyDataPorts() {
        SioUnitSettings settings = createSettings();
        settings.setDataPorts(List.of());
        assertTrue(settings.getDataPorts().isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testNullBasicSettingsThrows() {
        new SioUnitSettings(null);
    }

    @Test
    public void testMultipleObserversAreAllNotified() {
        SioUnitSettings settings = createSettings();
        AtomicBoolean called1 = new AtomicBoolean(false);
        AtomicBoolean called2 = new AtomicBoolean(false);
        settings.addObserver(() -> called1.set(true));
        settings.addObserver(() -> called2.set(true));

        settings.setClearInputBit8(true);
        assertTrue(called1.get());
        assertTrue(called2.get());
    }

    @Test
    public void testConstructorWithBooleanSettings() {
        BasicSettings bs = createNiceMock(BasicSettings.class);
        expect(bs.getBoolean("clearInputBit8", false)).andReturn(true).anyTimes();
        expect(bs.getBoolean("clearOutputBit8", false)).andReturn(true).anyTimes();
        expect(bs.getBoolean("inputToUpperCase", false)).andReturn(true).anyTimes();
        expect(bs.getString(eq("mapDeleteChar"))).andReturn(Optional.empty()).anyTimes();
        expect(bs.getString(eq("mapBackspaceChar"))).andReturn(Optional.empty()).anyTimes();
        expect(bs.getBoolean("interruptsSupported")).andReturn(Optional.of(false)).anyTimes();
        expect(bs.getInt("inputInterruptVector")).andReturn(Optional.of(3)).anyTimes();
        expect(bs.getInt("outputInterruptVector")).andReturn(Optional.of(5)).anyTimes();
        expect(bs.getString("statusPorts", "")).andReturn("").anyTimes();
        expect(bs.getString("dataPorts", "")).andReturn("").anyTimes();
        replay(bs);

        SioUnitSettings settings = new SioUnitSettings(bs);
        assertTrue(settings.isClearInputBit8());
        assertTrue(settings.isClearOutputBit8());
        assertTrue(settings.isInputToUpperCase());
        assertFalse(settings.getInterruptsSupported());
    }
}

