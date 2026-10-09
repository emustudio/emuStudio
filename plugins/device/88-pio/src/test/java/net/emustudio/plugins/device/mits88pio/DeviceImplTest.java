/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88pio;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88pio.api.PioContext;
import org.easymock.Capture;
import org.junit.Test;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

public class DeviceImplTest {
    @Test public void originalBoardUsesConnectedContextAnnotationAsDeviceId() throws Exception {
        assertOriginalIdentity(new AnnotatedByteDevice(), "Annotated byte peripheral");
    }

    @Test public void originalBoardFallsBackToConnectedContextToString() throws Exception {
        assertOriginalIdentity(new ByteDevice(), "Byte peripheral");
    }

    @Test public void originalBoardWithoutAConnectedContextShowsUnknown() throws Exception {
        assertOriginalIdentity(null, "unknown");
    }

    private void assertOriginalIdentity(DeviceContext<Byte> connected, String name) throws Exception {
        PluginSettings settings = createNiceMock(PluginSettings.class);
        expect(settings.getString("boardType", "88-4PIO")).andReturn("88-PIO");
        expect(settings.getInt("basePort", 4)).andReturn(4);
        expect(settings.getInt("interruptVector", 7)).andReturn(7);
        Context8080 cpu = createNiceMock(Context8080.class);
        expect(cpu.attachDevice(anyInt(), anyObject())).andReturn(true).times(2);
        ContextPool pool = createNiceMock(ContextPool.class);
        expect(pool.getCPUContext(0, Context8080.class)).andReturn(cpu);
        if (connected == null) {
            expect(pool.getDeviceContext(0, DeviceContext.class)).andThrow(new ContextNotFoundException("No peripheral"));
        } else {
            expect(pool.getDeviceContext(0, DeviceContext.class)).andReturn(connected);
        }
        ApplicationApi api = createNiceMock(ApplicationApi.class);
        expect(api.getContextPool()).andReturn(pool).anyTimes();
        replay(settings, cpu, pool, api);
        DeviceImpl device = new DeviceImpl(0, api, settings);
        device.initialize();
        assertEquals(name, device.getAttachedDeviceId());
        device.destroy();
        assertEquals("unknown", device.getAttachedDeviceId());
        verify(settings, cpu, pool);
    }

    @Test public void fourPioIdentityFollowsActualAttachmentAndDetachment() throws Exception {
        ContextPool pool = createMock(ContextPool.class);
        Capture<PioContext> registered = newCapture();
        pool.register(eq(0L), capture(registered), eq(PioContext.class));
        ApplicationApi api = createNiceMock(ApplicationApi.class);
        expect(api.getContextPool()).andReturn(pool).anyTimes();
        replay(pool, api);
        DeviceImpl device = new DeviceImpl(0, api, PluginSettings.UNAVAILABLE);
        assertEquals("unknown", device.getAttachedDeviceId());
        Peripheral peripheral = new Peripheral();
        PioContext board = registered.getValue();
        board.attachPeripheral(peripheral);
        assertEquals("Pin peripheral", device.getAttachedDeviceId());
        board.detachPeripheral(new Peripheral());
        assertEquals("Pin peripheral", device.getAttachedDeviceId());
        board.detachPeripheral(peripheral);
        assertEquals("unknown", device.getAttachedDeviceId());
        peripheral = new AnnotatedPeripheral();
        board.attachPeripheral(peripheral);
        assertEquals("Annotated pin peripheral", device.getAttachedDeviceId());
        board.detachPeripheral(peripheral);
        device.destroy();
        verify(pool);
    }

    private static class ByteDevice implements DeviceContext<Byte> {
        @Override public Byte readData() { return 0; }
        @Override public void writeData(Byte data) { }
        @Override public Class<Byte> getDataType() { return Byte.class; }
        @Override public String toString() { return "Byte peripheral"; }
    }

    @PluginContext(id = "Annotated byte peripheral")
    private static class AnnotatedByteDevice extends ByteDevice { }

    private static class Peripheral implements PioContext.Peripheral {
        @Override public void outputChanged(int channel, int data, int mask) { }
        @Override public void controlOutputChanged(int channel, boolean high) { }
        @Override public void reset() { }
        @Override public String toString() { return "Pin peripheral"; }
    }

    @PluginContext(id = "Annotated pin peripheral")
    private static class AnnotatedPeripheral extends Peripheral { }

    @Test public void statusAndSettingsAreSupportedTogether() {
        ApplicationApi api = createNiceMock(ApplicationApi.class);
        ContextPool pool = createNiceMock(ContextPool.class);
        expect(api.getContextPool()).andReturn(pool).anyTimes();
        replay(api, pool);
        DeviceImpl device = new DeviceImpl(0, api, PluginSettings.UNAVAILABLE);
        assertTrue(device.isGuiSupported());
        assertTrue(device.isShowSettingsSupported());
        device.destroy();
    }

    @Test public void noGuiModeNeverRequestsUiServices() {
        PluginSettings settings = createNiceMock(PluginSettings.class);
        expect(settings.getString("boardType", "88-4PIO")).andReturn("88-4PIO");
        expect(settings.getInt("basePort", 0xA0)).andReturn(0xA0);
        expect(settings.getInt("piaCount", 2)).andReturn(2);
        expect(settings.getInt("interruptVector", 7)).andReturn(7);
        expect(settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false)).andReturn(true);
        ApplicationApi api = createStrictMock(ApplicationApi.class);
        ContextPool pool = createNiceMock(ContextPool.class);
        expect(api.getContextPool()).andReturn(pool);
        replay(api, pool, settings);
        DeviceImpl device = new DeviceImpl(0, api, settings);
        assertFalse(device.isGuiSupported());
        assertFalse(device.isShowSettingsSupported());
        device.showGUI(null);
        device.showSettings(null);
        device.destroy();
        verify(api, settings);
    }

    @Test public void originalBoardUsesTwoPortsAndConfiguredRestartVector() throws Exception {
        PluginSettings settings = createNiceMock(PluginSettings.class);
        expect(settings.getString("boardType", "88-4PIO")).andReturn("88-PIO");
        expect(settings.getInt("basePort", 4)).andReturn(4);
        expect(settings.getInt("interruptVector", 7)).andReturn(3);
        Context8080 cpu = createStrictMock(Context8080.class);
        Capture<Context8080.CpuPortDevice> ports = newCapture();
        expect(cpu.attachDevice(eq(4), capture(ports))).andReturn(true);
        expect(cpu.attachDevice(eq(5), anyObject())).andReturn(true);
        expect(cpu.isInterruptSupported()).andReturn(true);
        cpu.signalInterrupt(aryEq(new byte[]{(byte) 0xDF}));
        cpu.detachDevice(5); cpu.detachDevice(4);
        ContextPool pool = createNiceMock(ContextPool.class);
        expect(pool.getCPUContext(0, Context8080.class)).andReturn(cpu);
        ApplicationApi api = createNiceMock(ApplicationApi.class);
        expect(api.getContextPool()).andReturn(pool).anyTimes();
        replay(settings, cpu, pool, api);
        DeviceImpl device = new DeviceImpl(0, api, settings);
        device.initialize();
        ports.getValue().write(4, (byte) 2);
        ((PioUnit) ports.getValue()).writeData((byte) 42);
        device.destroy();
        verify(settings, cpu);
    }

    @Test public void attachFailureNeverDetachesConflictingPortOrUnownedPorts() throws Exception {
        for (int conflict = 0; conflict < 8; conflict++) {
            Context8080 cpu = createStrictMock(Context8080.class);
            for (int port = 0; port < conflict; port++) {
                expect(cpu.attachDevice(eq(0xA0 + port), anyObject())).andReturn(true);
            }
            expect(cpu.attachDevice(eq(0xA0 + conflict), anyObject())).andReturn(false);
            for (int port = conflict - 1; port >= 0; port--) { cpu.detachDevice(0xA0 + port); }
            ContextPool pool = createNiceMock(ContextPool.class);
            expect(pool.getCPUContext(0, Context8080.class)).andReturn(cpu);
            ApplicationApi api = createNiceMock(ApplicationApi.class);
            expect(api.getContextPool()).andReturn(pool).anyTimes();
            replay(cpu, pool, api);
            DeviceImpl device = new DeviceImpl(0, api, PluginSettings.UNAVAILABLE);
            try { device.initialize(); fail("Conflict must fail initialization"); }
            catch (PluginInitializationException expected) { }
            device.destroy();
            verify(cpu);
        }
    }

    @Test public void destroyDetachesEachOwnedPortOnce() throws Exception {
        Context8080 cpu = createStrictMock(Context8080.class);
        for (int port = 0xA0; port <= 0xA7; port++) {
            expect(cpu.attachDevice(eq(port), anyObject())).andReturn(true);
        }
        for (int port = 0xA7; port >= 0xA0; port--) { cpu.detachDevice(port); }
        ContextPool pool = createNiceMock(ContextPool.class);
        expect(pool.getCPUContext(0, Context8080.class)).andReturn(cpu);
        ApplicationApi api = createNiceMock(ApplicationApi.class);
        expect(api.getContextPool()).andReturn(pool).anyTimes();
        replay(cpu, pool, api);
        DeviceImpl device = new DeviceImpl(0, api, PluginSettings.UNAVAILABLE);
        device.initialize();
        device.destroy();
        device.destroy();
        verify(cpu);
    }
}
