/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp;

import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class RaspCpuContextImplTest {
    private ContextPool contextPool;
    private AbstractTapeContext inputTape;
    private AbstractTapeContext outputTape;

    @Before
    public void setup() {
        contextPool = createNiceMock(ContextPool.class);
        inputTape = createNiceMock(AbstractTapeContext.class);
        outputTape = createNiceMock(AbstractTapeContext.class);
    }

    private RaspCpuContextImpl createAndInit() throws Exception {
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(0))).andReturn(inputTape).anyTimes();
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(1))).andReturn(outputTape).anyTimes();
        replay(contextPool, inputTape, outputTape);

        RaspCpuContextImpl context = new RaspCpuContextImpl();
        context.init(0L, contextPool);
        return context;
    }

    @Test
    public void testInitSetsTapes() throws Exception {
        RaspCpuContextImpl context = createAndInit();
        assertNotNull(context.getInputTape());
        assertNotNull(context.getOutputTape());
    }

    @Test
    public void testGetInputTapeReturnsCorrectTape() throws Exception {
        RaspCpuContextImpl context = createAndInit();
        assertSame(inputTape, context.getInputTape());
    }

    @Test
    public void testGetOutputTapeReturnsCorrectTape() throws Exception {
        RaspCpuContextImpl context = createAndInit();
        assertSame(outputTape, context.getOutputTape());
    }

    @Test
    public void testInitConfiguresInputTape() throws Exception {
        reset(inputTape);
        inputTape.setLeftBounded(true);
        expectLastCall().once();
        inputTape.setEditable(true);
        expectLastCall().once();
        inputTape.setHighlightHeadPosition(true);
        expectLastCall().once();
        inputTape.setClearAtReset(false);
        expectLastCall().once();
        inputTape.setTitle("Input tape");
        expectLastCall().once();
        inputTape.setAcceptTypes(TapeSymbol.Type.NUMBER);
        expectLastCall().once();
        replay(inputTape);

        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(0))).andReturn(inputTape).anyTimes();
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(1))).andReturn(outputTape).anyTimes();
        replay(contextPool, outputTape);

        RaspCpuContextImpl context = new RaspCpuContextImpl();
        context.init(0L, contextPool);
        verify(inputTape);
    }

    @Test
    public void testInitConfiguresOutputTape() throws Exception {
        reset(outputTape);
        outputTape.setLeftBounded(true);
        expectLastCall().once();
        outputTape.setEditable(false);
        expectLastCall().once();
        outputTape.setHighlightHeadPosition(true);
        expectLastCall().once();
        outputTape.setClearAtReset(true);
        expectLastCall().once();
        outputTape.setTitle("Output tape");
        expectLastCall().once();
        outputTape.setAcceptTypes(TapeSymbol.Type.NUMBER);
        expectLastCall().once();
        replay(outputTape);

        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(0))).andReturn(inputTape).anyTimes();
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(1))).andReturn(outputTape).anyTimes();
        replay(contextPool, inputTape);

        RaspCpuContextImpl context = new RaspCpuContextImpl();
        context.init(0L, contextPool);
        verify(outputTape);
    }

    @Test
    public void testDestroyClears() throws Exception {
        reset(inputTape, outputTape);
        // init expectations
        inputTape.setLeftBounded(true);
        inputTape.setEditable(true);
        inputTape.setHighlightHeadPosition(true);
        inputTape.setClearAtReset(false);
        inputTape.setTitle("Input tape");
        inputTape.setAcceptTypes(TapeSymbol.Type.NUMBER);

        outputTape.setLeftBounded(true);
        outputTape.setEditable(false);
        outputTape.setHighlightHeadPosition(true);
        outputTape.setClearAtReset(true);
        outputTape.setTitle("Output tape");
        outputTape.setAcceptTypes(TapeSymbol.Type.NUMBER);

        // destroy expectations
        inputTape.clear();
        expectLastCall().once();
        outputTape.clear();
        expectLastCall().once();
        replay(inputTape, outputTape);

        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(0))).andReturn(inputTape).anyTimes();
        expect(contextPool.getDeviceContext(eq(0L), eq(AbstractTapeContext.class), eq(1))).andReturn(outputTape).anyTimes();
        replay(contextPool);

        RaspCpuContextImpl context = new RaspCpuContextImpl();
        context.init(0L, contextPool);
        context.destroy();
        verify(inputTape, outputTape);
    }

    @Test
    public void testDestroyBeforeInitDoesNotThrow() {
        RaspCpuContextImpl context = new RaspCpuContextImpl();
        context.destroy(); // should not throw
    }

    @Test
    public void testPassedCyclesNotSupported() {
        RaspCpuContextImpl context = new RaspCpuContextImpl();
        assertFalse(context.isPassedCyclesSupported());
    }

    @Test
    public void testAddPassedCyclesListenerDoesNotThrow() {
        RaspCpuContextImpl context = new RaspCpuContextImpl();
        context.addPassedCyclesListener(cycles -> {});
    }

    @Test
    public void testRemovePassedCyclesListenerDoesNotThrow() {
        RaspCpuContextImpl context = new RaspCpuContextImpl();
        context.removePassedCyclesListener(cycles -> {});
    }
}

