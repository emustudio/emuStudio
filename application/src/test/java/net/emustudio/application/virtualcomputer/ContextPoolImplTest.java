/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.virtualcomputer;

import net.emustudio.emulib.plugins.Context;
import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.compiler.CompilerContext;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.InvalidContextException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class ContextPoolImplTest {
    private static final int emuStudioId = 555;

    private CPUContext cpuContextMock;
    private CPUContext cpuContextMockAnother;
    private MemoryContext<Short> shortMemoryContextMock;
    private MemoryContext<Short> shortMemoryContextMockAnother;
    private CompilerContext compilerContextMock;
    private CompilerContext compilerContextMockAnother;
    private DeviceContext<Short> shortDeviceContextMock;
    private DeviceContext<Short> shortDeviceContextMockAnother;
    private ContextPoolImpl contextPool;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        cpuContextMock = mock(CPUContext.class);
        cpuContextMockAnother = mock(CPUContext.class);
        shortMemoryContextMock = mock(MemoryContext.class);
        shortMemoryContextMockAnother = mock(MemoryContext.class);
        compilerContextMock = mock(CompilerContext.class);
        compilerContextMockAnother = mock(CompilerContext.class);
        shortDeviceContextMock = mock(DeviceContext.class);
        shortDeviceContextMockAnother = mock(DeviceContext.class);

        contextPool = new ContextPoolImpl(emuStudioId);
        contextPool.setComputer(connectedComputer(true));
    }

    @Test
    public void testRegisterGetUnregisterCPU() throws Exception {
        contextPool.register(0, cpuContextMock, CPUContext.class);
        assertEquals(cpuContextMock, contextPool.getCPUContext(1));
        assertTrue(contextPool.unregister(0, CPUContext.class));
        assertThrows(ContextNotFoundException.class, () -> contextPool.getCPUContext(1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegisterGetUnregisterMemory() throws Exception {
        contextPool.register(0, shortMemoryContextMock, MemoryContext.class);
        assertEquals(shortMemoryContextMock, contextPool.getMemoryContext(1, MemoryContext.class));
        assertTrue(contextPool.unregister(0, MemoryContext.class));
        assertThrows(ContextNotFoundException.class, () -> contextPool.getMemoryContext(1, MemoryContext.class));
    }

    @Test
    public void testRegisterGetUnregisterCompiler() throws Exception {
        contextPool.register(0, compilerContextMock, CompilerContext.class);
        assertEquals(compilerContextMock, contextPool.getCompilerContext(1));
        assertTrue(contextPool.unregister(0, CompilerContext.class));
        assertThrows(ContextNotFoundException.class, () -> contextPool.getCompilerContext(1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegisterGetUnregisterDevice() throws Exception {
        contextPool.register(0, shortDeviceContextMock, DeviceContext.class);
        assertEquals(shortDeviceContextMock, contextPool.getDeviceContext(1, DeviceContext.class));
        assertTrue(contextPool.unregister(0, DeviceContext.class));
        assertThrows(ContextNotFoundException.class, () -> contextPool.getDeviceContext(1, DeviceContext.class));
    }


    //

    @Test
    public void testRegisterTwiceDifferentCPU() throws Exception {
        contextPool.register(0, cpuContextMock, CPUContext.class);
        contextPool.register(0, cpuContextMockAnother, CPUContext.class);

        assertEquals(cpuContextMock, contextPool.getCPUContext(1, CPUContext.class, 0));
        assertEquals(cpuContextMockAnother, contextPool.getCPUContext(1, CPUContext.class, 1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegisterTwiceDifferentMemory() throws Exception {
        contextPool.register(0, shortMemoryContextMock, MemoryContext.class);
        contextPool.register(0, shortMemoryContextMockAnother, MemoryContext.class);

        assertEquals(shortMemoryContextMock, contextPool.getMemoryContext(1, MemoryContext.class, 0));
        assertEquals(shortMemoryContextMockAnother, contextPool.getMemoryContext(1, MemoryContext.class, 1));
    }

    @Test
    public void testRegisterTwiceDifferentCompiler() throws Exception {
        contextPool.register(0, compilerContextMock, CompilerContext.class);
        contextPool.register(0, compilerContextMockAnother, CompilerContext.class);

        assertEquals(compilerContextMock, contextPool.getCompilerContext(1, CompilerContext.class, 0));
        assertEquals(compilerContextMockAnother, contextPool.getCompilerContext(1, CompilerContext.class, 1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegisterTwiceDifferentDevice() throws Exception {
        contextPool.register(0, shortDeviceContextMock, DeviceContext.class);
        contextPool.register(0, shortDeviceContextMockAnother, DeviceContext.class);

        assertEquals(shortDeviceContextMock, contextPool.getDeviceContext(1, DeviceContext.class, 0));
        assertEquals(shortDeviceContextMockAnother, contextPool.getDeviceContext(1, DeviceContext.class, 1));
    }

    //

    @Test(expected = ContextAlreadyRegisteredException.class)
    public void testRegisterTwiceSameOwnerCPU() throws Exception {
        contextPool.register(0, cpuContextMock, CPUContext.class);
        contextPool.register(0, cpuContextMock, CPUContext.class);
    }

    @Test(expected = ContextAlreadyRegisteredException.class)
    public void testRegisterTwiceDifferentOwnerCPU() throws Exception {
        contextPool.register(0, cpuContextMock, CPUContext.class);
        contextPool.register(1, cpuContextMock, CPUContext.class);
    }

    @Test(expected = ContextAlreadyRegisteredException.class)
    public void testRegisterTwiceSameOwnerMemory() throws Exception {
        contextPool.register(0, shortMemoryContextMock, MemoryContext.class);
        contextPool.register(0, shortMemoryContextMock, MemoryContext.class);
    }

    @Test(expected = ContextAlreadyRegisteredException.class)
    public void testRegisterTwiceDifferentOwnerMemory() throws Exception {
        contextPool.register(0, shortMemoryContextMock, MemoryContext.class);
        contextPool.register(1, shortMemoryContextMock, MemoryContext.class);
    }

    @Test(expected = ContextAlreadyRegisteredException.class)
    public void testRegisterTwiceSameOwnerCompiler() throws Exception {
        contextPool.register(0, compilerContextMock, CompilerContext.class);
        contextPool.register(0, compilerContextMock, CompilerContext.class);
    }

    @Test(expected = ContextAlreadyRegisteredException.class)
    public void testRegisterTwiceDifferentOwnerCompiler() throws Exception {
        contextPool.register(0, compilerContextMock, CompilerContext.class);
        contextPool.register(1, compilerContextMock, CompilerContext.class);
    }

    @Test(expected = ContextAlreadyRegisteredException.class)
    public void testRegisterTwiceSameOwnerDevice() throws Exception {
        contextPool.register(0, shortDeviceContextMock, DeviceContext.class);
        contextPool.register(0, shortDeviceContextMock, DeviceContext.class);
    }

    @Test(expected = ContextAlreadyRegisteredException.class)
    public void testRegisterTwiceDifferentOwnerDevice() throws Exception {
        contextPool.register(0, shortDeviceContextMock, DeviceContext.class);
        contextPool.register(1, shortDeviceContextMock, DeviceContext.class);
    }

    //

    @Test
    public void testUnregisterNotOwnerCPU() throws Exception {
        contextPool.register(0, cpuContextMock, CPUContext.class);
        assertFalse(contextPool.unregister(1, CPUContext.class));
    }

    @Test
    public void testUnregisterNotOwnerMemory() throws Exception {
        contextPool.register(0, shortMemoryContextMock, MemoryContext.class);
        assertFalse(contextPool.unregister(1, MemoryContext.class));
    }

    @Test
    public void testUnregisterNotOwnerCompiler() throws Exception {
        contextPool.register(0, compilerContextMock, CompilerContext.class);
        assertFalse(contextPool.unregister(1, CompilerContext.class));
    }

    @Test
    public void testUnregisterNotOwnerDevice() throws Exception {
        contextPool.register(0, shortDeviceContextMock, DeviceContext.class);
        assertFalse(contextPool.unregister(1, DeviceContext.class));
    }

    //

    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetUnconnectedCompiler() throws Exception {
        contextPool.register(1, compilerContextMock, CompilerContext.class);
        contextPool.getCompilerContext(1);
    }

    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetUnconnectedCPU() throws Exception {
        contextPool.register(1, cpuContextMock, CPUContext.class);
        contextPool.getCPUContext(1);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetUnconnectedMemory() throws Exception {
        contextPool.register(1, shortMemoryContextMock, MemoryContext.class);
        contextPool.getMemoryContext(1, MemoryContext.class);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetUnconnectedDevice() throws Exception {
        contextPool.register(1, shortDeviceContextMock, DeviceContext.class);
        contextPool.getDeviceContext(1, DeviceContext.class);
    }

    //

    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetUnregisteredCPU() throws ContextNotFoundException, InvalidContextException {
        contextPool.getCPUContext(0);
    }

    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetUnregisteredCompiler() throws ContextNotFoundException, InvalidContextException {
        contextPool.getCompilerContext(0);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetUnregisteredMemory() throws ContextNotFoundException, InvalidContextException {
        contextPool.getMemoryContext(0, MemoryContext.class);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetUnregisteredDevice() throws ContextNotFoundException, InvalidContextException {
        contextPool.getDeviceContext(0, DeviceContext.class);
    }

    //

    @Test(expected = ContextNotFoundException.class)
    public void testGetWrongIndexCPU() throws Exception {
        contextPool.register(1, cpuContextMock, CPUContext.class);
        contextPool.getCPUContext(0, CPUContext.class, 1);
    }

    @Test(expected = ContextNotFoundException.class)
    public void testGetWrongIndexCompiler() throws Exception {
        contextPool.register(1, compilerContextMock, CompilerContext.class);
        contextPool.getCompilerContext(0, CompilerContext.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ContextNotFoundException.class)
    public void testGetWrongIndexMemory() throws Exception {
        contextPool.register(1, shortMemoryContextMock, MemoryContext.class);
        contextPool.getMemoryContext(0, MemoryContext.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ContextNotFoundException.class)
    public void testGetWrongIndexDevice() throws Exception {
        contextPool.register(1, shortDeviceContextMock, DeviceContext.class);
        contextPool.getDeviceContext(0, DeviceContext.class, 1);
    }

    //

    @Test
    public void testRegisterWithDifferentInterfaceThanGetCompiler() throws Exception {
        TestCompilerContext compilerContextMock = niceMock(TestCompilerContext.class);

        contextPool.register(0, compilerContextMock, TestCompilerContext.class);

        assertEquals(compilerContextMock, contextPool.getCompilerContext(1, TestCompilerContext.class));
        assertEquals(compilerContextMock, contextPool.getCompilerContext(1, DifferentTestCompilerContext.class));
    }

    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetGeneralInterfaceWhenNotRegisteredCPU() throws Exception {
        TestCPUContext cpuContextMock = niceMock(TestCPUContext.class);
        contextPool.register(0, cpuContextMock, TestCPUContext.class);
        contextPool.getCPUContext(1);
    }

    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetGeneralInterfaceWhenNotRegisteredCompiler() throws Exception {
        TestCompilerContext compilerContextMock = niceMock(TestCompilerContext.class);
        contextPool.register(0, compilerContextMock, TestCompilerContext.class);
        contextPool.getCompilerContext(1);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetGeneralInterfaceWhenNotRegisteredMemory() throws Exception {
        TestMemoryContext shortMemoryContextMock = niceMock(TestMemoryContext.class);
        contextPool.register(0, shortMemoryContextMock, TestMemoryContext.class);
        contextPool.getMemoryContext(1, MemoryContext.class);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetGeneralInterfaceWhenNotRegisteredDevice() throws Exception {
        TestDeviceContext shortDeviceContextMock = niceMock(TestDeviceContext.class);
        contextPool.register(0, shortDeviceContextMock, TestDeviceContext.class);
        contextPool.getDeviceContext(1, DeviceContext.class);
    }

    //

    @Test
    public void testRegisterWithDifferentDataTypeThanGetMemory() throws Exception {
        contextPool.register(0, shortMemoryContextMock, MemoryContext.class);
        assertEquals(shortMemoryContextMock, contextPool.getMemoryContext(1, GenericByteMemoryContext.class));
    }

    @Test(expected = InvalidContextException.class)
    public void testRegisterWrongInterfaceCPU() throws Exception {
        contextPool.register(1, cpuContextMock, MemoryContext.class);
    }

    @Test(expected = InvalidContextException.class)
    public void testRegisterWrongInterfaceMemory() throws Exception {
        contextPool.register(1, shortMemoryContextMock, CPUContext.class);
    }

    //

    @Test(expected = InvalidContextException.class)
    public void testRegisterWrongInterfaceCompiler() throws Exception {
        contextPool.register(1, compilerContextMock, CPUContext.class);
    }

    @Test(expected = InvalidContextException.class)
    public void testRegisterWrongInterfaceDevice() throws Exception {
        contextPool.register(1, shortDeviceContextMock, CPUContext.class);
    }

    @Test(expected = InvalidContextException.class)
    public void testUnannotatedContextInterface() throws Exception {
        Context unannotatedContext = niceMock(UnannotatedContext.class);
        contextPool.register(0, unannotatedContext, UnannotatedContext.class);
    }

    @Test(expected = NullPointerException.class)
    public void testGetNullCPU() throws Exception {
        contextPool.getCPUContext(0, null);
    }

    //

    @Test(expected = NullPointerException.class)
    public void testGetNullCompiler() throws Exception {
        contextPool.getCompilerContext(1, null);
    }

    //

    @Test(expected = NullPointerException.class)
    public void testGetNullMemory() throws Exception {
        contextPool.getMemoryContext(2, null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetNullDevice() throws Exception {
        contextPool.getDeviceContext(3, null);
    }

    @Test
    public void testUnregisterDifferentContextCPU() throws Exception {
        contextPool.register(0, cpuContextMock, CPUContext.class);
        assertFalse(contextPool.unregister(0, MemoryContext.class));
    }

    @Test
    public void testUnregisterDifferentContextMemory() throws Exception {
        contextPool.register(0, shortMemoryContextMock, MemoryContext.class);
        assertFalse(contextPool.unregister(0, CPUContext.class));
    }

    //

    @Test
    public void testUnregisterDifferentContextCompiler() throws Exception {
        contextPool.register(0, compilerContextMock, CompilerContext.class);
        assertFalse(contextPool.unregister(0, CPUContext.class));
    }

    @Test
    public void testUnregisterDifferentContextDevice() throws Exception {
        contextPool.register(0, shortDeviceContextMock, DeviceContext.class);
        assertFalse(contextPool.unregister(0, CPUContext.class));
    }

    @Test
    public void testGetByEmuStudio() throws Exception {
        contextPool.setComputer(connectedComputer(false));
        contextPool.register(0, cpuContextMock, CPUContext.class);
        assertEquals(cpuContextMock, contextPool.getCPUContext(emuStudioId));
    }


    private static <T> T niceMock(Class<T> type) {
        return mock(type);
    }

    private static PluginConnections connectedComputer(boolean connected) {
        return (pluginID, toPluginID) -> pluginID != toPluginID && connected;
    }

    @PluginContext
    interface TestCompilerContext extends CompilerContext {
        void testCompilerMethod();
    }

    @PluginContext
    interface DifferentTestCompilerContext extends TestCompilerContext {
    }

    @PluginContext
    interface TestCPUContext extends CPUContext {
        void testMethod();
    }

    @PluginContext
    interface TestMemoryContext extends MemoryContext<Short> {
        void testMemoryMethod();
    }

    @PluginContext
    interface TestDeviceContext extends DeviceContext<Short> {
        void testDeviceMethod();
    }

    @PluginContext
    interface GenericByteMemoryContext extends MemoryContext<Byte> {
    }

    interface UnannotatedContext extends Context {
    }
}
