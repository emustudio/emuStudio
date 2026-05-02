/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class RamCpuContextImplTest {
    private ContextPool contextPool;
    private AbstractTapeContext storageTape;
    private AbstractTapeContext inputTape;
    private AbstractTapeContext outputTape;

    @Before
    public void setup() {
        contextPool = createNiceMock(ContextPool.class);
        storageTape = createNiceMock(AbstractTapeContext.class);
        inputTape = createNiceMock(AbstractTapeContext.class);
        outputTape = createNiceMock(AbstractTapeContext.class);
    }

    private RamCpuContextImpl createAndInit() throws Exception {
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(0))).andReturn(storageTape).anyTimes();
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(1))).andReturn(inputTape).anyTimes();
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(2))).andReturn(outputTape).anyTimes();
        replay(contextPool, storageTape, inputTape, outputTape);

        RamCpuContextImpl context = new RamCpuContextImpl(contextPool);
        context.init(0L);
        return context;
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullContextPoolThrows() {
        new RamCpuContextImpl(null);
    }

    @Test
    public void testInitSetsTapes() throws Exception {
        RamCpuContextImpl context = createAndInit();
        assertNotNull(context.getStorageTape());
        assertNotNull(context.getInputTape());
        assertNotNull(context.getOutputTape());
    }

    @Test
    public void testGetStorageTapeReturnsCorrectTape() throws Exception {
        RamCpuContextImpl context = createAndInit();
        assertSame(storageTape, context.getStorageTape());
    }

    @Test
    public void testGetInputTapeReturnsCorrectTape() throws Exception {
        RamCpuContextImpl context = createAndInit();
        assertSame(inputTape, context.getInputTape());
    }

    @Test
    public void testGetOutputTapeReturnsCorrectTape() throws Exception {
        RamCpuContextImpl context = createAndInit();
        assertSame(outputTape, context.getOutputTape());
    }

    @Test(expected = PluginInitializationException.class)
    public void testInitThrowsWhenStorageTapeIsNull() throws Exception {
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(0))).andReturn(null).anyTimes();
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(1))).andReturn(inputTape).anyTimes();
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(2))).andReturn(outputTape).anyTimes();
        replay(contextPool, storageTape, inputTape, outputTape);

        RamCpuContextImpl context = new RamCpuContextImpl(contextPool);
        context.init(0L);
    }

    @Test(expected = PluginInitializationException.class)
    public void testInitThrowsWhenInputTapeIsNull() throws Exception {
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(0))).andReturn(storageTape).anyTimes();
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(1))).andReturn(null).anyTimes();
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(2))).andReturn(outputTape).anyTimes();
        replay(contextPool, storageTape, inputTape, outputTape);

        RamCpuContextImpl context = new RamCpuContextImpl(contextPool);
        context.init(0L);
    }

    @Test(expected = PluginInitializationException.class)
    public void testInitThrowsWhenOutputTapeIsNull() throws Exception {
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(0))).andReturn(storageTape).anyTimes();
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(1))).andReturn(inputTape).anyTimes();
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(2))).andReturn(null).anyTimes();
        replay(contextPool, storageTape, inputTape, outputTape);

        RamCpuContextImpl context = new RamCpuContextImpl(contextPool);
        context.init(0L);
    }

    @Test
    public void testInitConfiguresStorageTape() throws Exception {
        reset(storageTape);
        storageTape.setLeftBounded(true);
        expectLastCall().once();
        storageTape.setEditable(true);
        expectLastCall().once();
        storageTape.setHighlightHeadPosition(false);
        expectLastCall().once();
        storageTape.setClearAtReset(true);
        expectLastCall().once();
        storageTape.setTitle("Storage");
        expectLastCall().once();
        storageTape.setShowPositions(true);
        expectLastCall().once();
        replay(storageTape);

        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(0))).andReturn(storageTape).anyTimes();
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(1))).andReturn(inputTape).anyTimes();
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(2))).andReturn(outputTape).anyTimes();
        replay(contextPool, inputTape, outputTape);

        RamCpuContextImpl context = new RamCpuContextImpl(contextPool);
        context.init(0L);
        verify(storageTape);
    }

    @Test
    public void testDestroyClears() throws Exception {
        reset(storageTape, inputTape, outputTape);
        // record init expectations
        storageTape.setLeftBounded(true);
        storageTape.setEditable(true);
        storageTape.setHighlightHeadPosition(false);
        storageTape.setClearAtReset(true);
        storageTape.setTitle("Storage");
        storageTape.setShowPositions(true);

        inputTape.setLeftBounded(true);
        inputTape.setEditable(true);
        inputTape.setHighlightHeadPosition(true);
        inputTape.setClearAtReset(false);
        inputTape.setTitle("Input tape");
        inputTape.setShowPositions(true);

        outputTape.setLeftBounded(true);
        outputTape.setEditable(false);
        outputTape.setHighlightHeadPosition(true);
        outputTape.setClearAtReset(true);
        outputTape.setTitle("Output tape");
        outputTape.setShowPositions(true);

        // destroy clears all tapes
        inputTape.clear();
        expectLastCall().once();
        storageTape.clear();
        expectLastCall().once();
        outputTape.clear();
        expectLastCall().once();
        replay(storageTape, inputTape, outputTape);

        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(0))).andReturn(storageTape).anyTimes();
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(1))).andReturn(inputTape).anyTimes();
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(2))).andReturn(outputTape).anyTimes();
        replay(contextPool);

        RamCpuContextImpl context = new RamCpuContextImpl(contextPool);
        context.init(0L);
        context.destroy();
        verify(storageTape, inputTape, outputTape);
    }

    @Test
    public void testDestroyBeforeInitDoesNotThrow() {
        replay(contextPool);
        RamCpuContextImpl context = new RamCpuContextImpl(contextPool);
        context.destroy(); // should not throw
    }

    @Test
    public void testPassedCyclesNotSupported() {
        replay(contextPool);
        RamCpuContextImpl context = new RamCpuContextImpl(contextPool);
        assertFalse(context.isPassedCyclesSupported());
    }

    @Test
    public void testAddPassedCyclesListenerDoesNotThrow() {
        replay(contextPool);
        RamCpuContextImpl context = new RamCpuContextImpl(contextPool);
        context.addPassedCyclesListener(cycles -> {});
    }

    @Test
    public void testRemovePassedCyclesListenerDoesNotThrow() {
        replay(contextPool);
        RamCpuContextImpl context = new RamCpuContextImpl(contextPool);
        context.removePassedCyclesListener(cycles -> {});
    }
}

