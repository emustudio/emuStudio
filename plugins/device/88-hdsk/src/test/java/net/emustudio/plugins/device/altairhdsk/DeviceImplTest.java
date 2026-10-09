/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.altairhdsk;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88pio.PioBoard;
import net.emustudio.plugins.device.mits88pio.api.PioContext;
import org.easymock.Capture;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DeviceImplTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    private PluginSettings mutableSettings(Map<String, Object> values) {
        PluginSettings settings = createNiceMock(PluginSettings.class);
        expect(settings.getString(anyString(), anyString())).andAnswer(() ->
                (String) values.getOrDefault(getCurrentArgument(0), getCurrentArgument(1))).anyTimes();
        expect(settings.getString(anyString())).andAnswer(() ->
                Optional.ofNullable((String) values.get(getCurrentArgument(0)))).anyTimes();
        expect(settings.getInt(anyString(), anyInt())).andAnswer(() ->
                (int) values.getOrDefault(getCurrentArgument(0), getCurrentArgument(1))).anyTimes();
        expect(settings.getBoolean(anyString(), anyBoolean())).andAnswer(() ->
                (boolean) values.getOrDefault(getCurrentArgument(0), getCurrentArgument(1))).anyTimes();
        replay(settings);
        return settings;
    }

    private PluginSettings settings(String mode) {
        PluginSettings settings = createNiceMock(PluginSettings.class);
        expect(settings.getString("controllerType", "SIMH")).andReturn(mode);
        expect(settings.getInt(anyString(), anyInt())).andAnswer(() -> (int) getCurrentArguments()[1]).anyTimes();
        expect(settings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        replay(settings);
        return settings;
    }

    private ApplicationApi application(ContextPool pool) {
        ApplicationApi api = createStrictMock(ApplicationApi.class);
        expect(api.getContextPool()).andReturn(pool).anyTimes();
        replay(api);
        return api;
    }

    @SuppressWarnings("unchecked")
    private MemoryContext<Byte> memory() {
        MemoryContext<Byte> memory = createStrictMock(MemoryContext.class);
        expect(memory.getCellTypeClass()).andReturn(Byte.class);
        replay(memory);
        return memory;
    }

    @Test public void defaultModePreservesSimhFdPortAndDmaMemoryConnection() throws Exception {
        ContextPool pool = createStrictMock(ContextPool.class);
        Context8080 cpu = createStrictMock(Context8080.class);
        MemoryContext<Byte> memory = memory();
        expect(pool.getCPUContext(0, Context8080.class)).andReturn(cpu);
        expect(pool.getMemoryContext(0, MemoryContext.class)).andReturn(memory);
        Capture<Context8080.CpuPortDevice> controller = newCapture();
        expect(cpu.attachDevice(eq(0xFD), capture(controller))).andReturn(true);
        cpu.detachDevice(0xFD);
        replay(pool, cpu);
        DeviceImpl device = new DeviceImpl(0, application(pool), PluginSettings.UNAVAILABLE);
        device.initialize();
        assertTrue(controller.getValue() instanceof HdskController);
        assertTrue(device.getDescription().contains("FDh"));
        device.reset();
        device.destroy();
        device.destroy();
        verify(pool, cpu, memory);
    }

    @Test public void explicitSimhModeUsesExistingController() throws Exception {
        ContextPool pool = createStrictMock(ContextPool.class);
        Context8080 cpu = createStrictMock(Context8080.class);
        MemoryContext<Byte> memory = memory();
        expect(pool.getCPUContext(0, Context8080.class)).andReturn(cpu);
        expect(pool.getMemoryContext(0, MemoryContext.class)).andReturn(memory);
        expect(cpu.attachDevice(eq(0xFD), isA(HdskController.class))).andReturn(true);
        cpu.detachDevice(0xFD);
        replay(pool, cpu);
        DeviceImpl device = new DeviceImpl(0, application(pool), settings("SIMH"));
        device.initialize();
        device.destroy();
        verify(pool, cpu, memory);
    }

    @Test public void mitsModeUsesPioOnlyAndDetachesItsOwnPeripheral() throws Exception {
        ContextPool pool = createStrictMock(ContextPool.class);
        PioBoard pio = new PioBoard(0xA0, 2);
        expect(pool.getContext(0, PioContext.class, 0)).andReturn(pio);
        replay(pool);
        DeviceImpl device = new DeviceImpl(0, application(pool), settings("MITS"));
        device.initialize();
        assertEquals(255, pio.getInputPins(0));
        assertTrue(device.getDescription().contains("88-4PIO"));
        pio.setInputPins(0, 0);
        device.reset();
        assertEquals(255, pio.getInputPins(0));
        device.destroy();
        device.destroy();
        MhdskController replacement = new MhdskController(pio);
        pio.attachPeripheral(replacement);
        replacement.close();
        verify(pool); // Any CPU/memory context request would fail the strict mock.
    }

    @Test public void simhPortConflictDoesNotDetachSomeoneElsesPort() throws Exception {
        ContextPool pool = createStrictMock(ContextPool.class);
        Context8080 cpu = createStrictMock(Context8080.class);
        MemoryContext<Byte> memory = memory();
        expect(pool.getCPUContext(0, Context8080.class)).andReturn(cpu);
        expect(pool.getMemoryContext(0, MemoryContext.class)).andReturn(memory);
        expect(cpu.attachDevice(eq(0xFD), anyObject())).andReturn(false);
        replay(pool, cpu);
        DeviceImpl device = new DeviceImpl(0, application(pool), settings("SIMH"));
        try { device.initialize(); fail("Port conflict must fail initialization"); }
        catch (PluginInitializationException expected) { }
        device.destroy();
        verify(pool, cpu);
    }

    @Test public void mitsConnectorConflictPreservesExistingPeripheral() throws Exception {
        ContextPool pool = createStrictMock(ContextPool.class);
        PioBoard pio = new PioBoard(0xA0, 2);
        MhdskController existing = new MhdskController(pio);
        pio.attachPeripheral(existing);
        expect(pool.getContext(0, PioContext.class, 0)).andReturn(pio);
        replay(pool);
        DeviceImpl device = new DeviceImpl(0, application(pool), settings("MITS"));
        try { device.initialize(); fail("Connector conflict must fail initialization"); }
        catch (PluginInitializationException expected) { }
        device.destroy();
        MhdskController other = new MhdskController(pio);
        try { pio.attachPeripheral(other); fail("Existing peripheral must remain connected"); }
        catch (IllegalStateException expected) { }
        finally { existing.close(); other.close(); }
        verify(pool);
    }

    @Test public void mitsRequiresTwoPias() throws Exception {
        ContextPool pool = createStrictMock(ContextPool.class);
        expect(pool.getContext(0, PioContext.class, 0)).andReturn(new PioBoard(0xA0, 1));
        replay(pool);
        DeviceImpl device = new DeviceImpl(0, application(pool), settings("MITS"));
        try { device.initialize(); fail("One PIA cannot connect MHDSK"); }
        catch (PluginInitializationException expected) { }
        device.destroy();
        verify(pool);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownControllerTypeFailsInsteadOfSilentlySelectingAnotherProtocol() {
        new DeviceImpl(0, createNiceMock(ApplicationApi.class), settings("typo"));
    }

    @Test public void savedSimhSettingsApplyGeometryImagesAndUnmountsWithoutReattachingPorts() throws Exception {
        Map<String, Object> values = new HashMap<>();
        ContextPool pool = createStrictMock(ContextPool.class);
        Context8080 cpu = createStrictMock(Context8080.class);
        MemoryContext<Byte> memory = memory();
        expect(pool.getCPUContext(0, Context8080.class)).andReturn(cpu);
        expect(pool.getMemoryContext(0, MemoryContext.class)).andReturn(memory);
        Capture<Context8080.CpuPortDevice> captured = newCapture();
        expect(cpu.attachDevice(eq(0xFD), capture(captured))).andReturn(true);
        cpu.detachDevice(0xFD);
        replay(pool, cpu);
        DeviceImpl device = new DeviceImpl(0, application(pool), mutableSettings(values));
        device.initialize();
        try {
            HdskController controller = (HdskController) captured.getValue();
            Path path = temporary.newFile("simh.dsk").toPath();
            values.put("image15", path.toString());
            values.put("sectorSize15", 512);
            values.put("sectorsPerTrack15", 64);
            device.applySettings();
            assertEquals(path, controller.imagePath(15).orElseThrow());
            assertEquals(512, controller.sectorSize(15));
            assertEquals(64, controller.sectorsPerTrack(15));
            values.remove("image15");
            device.applySettings();
            assertTrue(controller.imagePath(15).isEmpty());
        } finally { device.destroy(); }
        verify(pool, cpu);
    }

    @Test public void savedMitsSettingsApplyImagesWriteProtectionAndUnmounts() throws Exception {
        Map<String, Object> values = new HashMap<>();
        values.put("controllerType", "MITS");
        ContextPool pool = createStrictMock(ContextPool.class);
        PioContext pio = createNiceMock(PioContext.class);
        expect(pio.getChannelCount()).andReturn(4).anyTimes();
        Capture<PioContext.Peripheral> captured = newCapture();
        pio.attachPeripheral(capture(captured));
        expect(pool.getContext(0, PioContext.class, 0)).andReturn(pio);
        replay(pool, pio);
        DeviceImpl device = new DeviceImpl(0, application(pool), mutableSettings(values));
        device.initialize();
        try {
            MhdskController controller = (MhdskController) captured.getValue();
            Path path = temporary.getRoot().toPath().resolve("mits.dsk");
            MhdskController.createImage(path);
            values.put("image7", path.toString());
            values.put("readOnly7", true);
            device.applySettings();
            assertEquals(path, controller.getImage(7));
            assertTrue(controller.isReadOnly(7));
            values.put("readOnly7", false);
            device.applySettings();
            assertFalse(controller.isReadOnly(7));
            values.remove("image7");
            device.applySettings();
            assertNull(controller.getImage(7));
        } finally { device.destroy(); }
        verify(pool, pio);
    }

    @Test public void changingControllerIsDeferredWithoutChangingTheActiveController() throws Exception {
        Map<String, Object> values = new HashMap<>();
        ContextPool pool = createStrictMock(ContextPool.class);
        Context8080 cpu = createStrictMock(Context8080.class);
        expect(pool.getCPUContext(0, Context8080.class)).andReturn(cpu);
        expect(pool.getMemoryContext(0, MemoryContext.class)).andReturn(memory());
        Capture<Context8080.CpuPortDevice> captured = newCapture();
        expect(cpu.attachDevice(eq(0xFD), capture(captured))).andReturn(true);
        cpu.detachDevice(0xFD);
        replay(pool, cpu);
        DeviceImpl device = new DeviceImpl(0, application(pool), mutableSettings(values));
        device.initialize();
        try {
            values.put("controllerType", "MITS");
            values.put("sectorSize0", 512);
            device.applySettings();
            assertEquals(128, ((HdskController) captured.getValue()).sectorSize(0));
            assertTrue(device.getDescription().contains("FDh"));
        } finally { device.destroy(); }
        verify(pool, cpu);
    }

    @Test public void settingsAreAvailableOnlyWhenGuiIsEnabled() {
        ApplicationApi api = createStrictMock(ApplicationApi.class);
        replay(api);
        assertTrue(new DeviceImpl(0, api, PluginSettings.UNAVAILABLE).isShowSettingsSupported());
        DeviceImpl headless = new DeviceImpl(0, api, mutableSettings(Map.of(PluginSettings.EMUSTUDIO_NO_GUI, true)));
        assertFalse(headless.isShowSettingsSupported());
        headless.showSettings(null);
        verify(api);
    }
}
