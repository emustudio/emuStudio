/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.virtualcomputer;

import net.emustudio.emulib.plugins.Context;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ContextNotFoundException;
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
    private OtherContext compilerContextMock;
    private OtherContext compilerContextMockAnother;
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
        compilerContextMock = mock(OtherContext.class);
        compilerContextMockAnother = mock(OtherContext.class);
        shortDeviceContextMock = mock(DeviceContext.class);
        shortDeviceContextMockAnother = mock(DeviceContext.class);

        contextPool = new ContextPoolImpl(emuStudioId);
        contextPool.setComputer(connectedComputer(true));
    }

    @Test
    public void testRegisterGetUnregisterCPU() throws Exception {
        contextPool.register(0, cpuContextMock, CPUContext.class);
        assertEquals(cpuContextMock, contextPool.get(1, CPUContext.class));
        assertTrue(contextPool.unregister(0, CPUContext.class));
        assertThrows(ContextNotFoundException.class, () -> contextPool.get(1, CPUContext.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegisterGetUnregisterMemory() throws Exception {
        contextPool.register(0, shortMemoryContextMock, MemoryContext.class);
        assertEquals(shortMemoryContextMock, contextPool.get(1, MemoryContext.class));
        assertTrue(contextPool.unregister(0, MemoryContext.class));
        assertThrows(ContextNotFoundException.class, () -> contextPool.get(1, MemoryContext.class));
    }

    @Test
    public void testRegisterGetUnregisterCompiler() throws Exception {
        contextPool.register(0, compilerContextMock, OtherContext.class);
        assertEquals(compilerContextMock, contextPool.get(1, OtherContext.class));
        assertTrue(contextPool.unregister(0, OtherContext.class));
        assertThrows(ContextNotFoundException.class, () -> contextPool.get(1, OtherContext.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegisterGetUnregisterDevice() throws Exception {
        contextPool.register(0, shortDeviceContextMock, DeviceContext.class);
        assertEquals(shortDeviceContextMock, contextPool.get(1, DeviceContext.class));
        assertTrue(contextPool.unregister(0, DeviceContext.class));
        assertThrows(ContextNotFoundException.class, () -> contextPool.get(1, DeviceContext.class));
    }


    //

    @Test
    public void testRegisterTwiceDifferentCPU() throws Exception {
        contextPool.register(0, cpuContextMock, CPUContext.class);
        contextPool.register(0, cpuContextMockAnother, CPUContext.class);

        assertEquals(cpuContextMock, contextPool.get(1, CPUContext.class, 0));
        assertEquals(cpuContextMockAnother, contextPool.get(1, CPUContext.class, 1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegisterTwiceDifferentMemory() throws Exception {
        contextPool.register(0, shortMemoryContextMock, MemoryContext.class);
        contextPool.register(0, shortMemoryContextMockAnother, MemoryContext.class);

        assertEquals(shortMemoryContextMock, contextPool.get(1, MemoryContext.class, 0));
        assertEquals(shortMemoryContextMockAnother, contextPool.get(1, MemoryContext.class, 1));
    }

    @Test
    public void testRegisterTwiceDifferentCompiler() throws Exception {
        contextPool.register(0, compilerContextMock, OtherContext.class);
        contextPool.register(0, compilerContextMockAnother, OtherContext.class);

        assertEquals(compilerContextMock, contextPool.get(1, OtherContext.class, 0));
        assertEquals(compilerContextMockAnother, contextPool.get(1, OtherContext.class, 1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRegisterTwiceDifferentDevice() throws Exception {
        contextPool.register(0, shortDeviceContextMock, DeviceContext.class);
        contextPool.register(0, shortDeviceContextMockAnother, DeviceContext.class);

        assertEquals(shortDeviceContextMock, contextPool.get(1, DeviceContext.class, 0));
        assertEquals(shortDeviceContextMockAnother, contextPool.get(1, DeviceContext.class, 1));
    }

    //

    @Test(expected = IllegalStateException.class)
    public void testRegisterTwiceSameOwnerCPU() throws Exception {
        contextPool.register(0, cpuContextMock, CPUContext.class);
        contextPool.register(0, cpuContextMock, CPUContext.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testRegisterTwiceDifferentOwnerCPU() throws Exception {
        contextPool.register(0, cpuContextMock, CPUContext.class);
        contextPool.register(1, cpuContextMock, CPUContext.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testRegisterTwiceSameOwnerMemory() throws Exception {
        contextPool.register(0, shortMemoryContextMock, MemoryContext.class);
        contextPool.register(0, shortMemoryContextMock, MemoryContext.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testRegisterTwiceDifferentOwnerMemory() throws Exception {
        contextPool.register(0, shortMemoryContextMock, MemoryContext.class);
        contextPool.register(1, shortMemoryContextMock, MemoryContext.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testRegisterTwiceSameOwnerCompiler() throws Exception {
        contextPool.register(0, compilerContextMock, OtherContext.class);
        contextPool.register(0, compilerContextMock, OtherContext.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testRegisterTwiceDifferentOwnerCompiler() throws Exception {
        contextPool.register(0, compilerContextMock, OtherContext.class);
        contextPool.register(1, compilerContextMock, OtherContext.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testRegisterTwiceSameOwnerDevice() throws Exception {
        contextPool.register(0, shortDeviceContextMock, DeviceContext.class);
        contextPool.register(0, shortDeviceContextMock, DeviceContext.class);
    }

    @Test(expected = IllegalStateException.class)
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
        contextPool.register(0, compilerContextMock, OtherContext.class);
        assertFalse(contextPool.unregister(1, OtherContext.class));
    }

    @Test
    public void testUnregisterNotOwnerDevice() throws Exception {
        contextPool.register(0, shortDeviceContextMock, DeviceContext.class);
        assertFalse(contextPool.unregister(1, DeviceContext.class));
    }

    //

    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetUnconnectedCompiler() throws Exception {
        contextPool.register(1, compilerContextMock, OtherContext.class);
        contextPool.get(1, OtherContext.class);
    }

    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetUnconnectedCPU() throws Exception {
        contextPool.register(1, cpuContextMock, CPUContext.class);
        contextPool.get(1, CPUContext.class);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetUnconnectedMemory() throws Exception {
        contextPool.register(1, shortMemoryContextMock, MemoryContext.class);
        contextPool.get(1, MemoryContext.class);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetUnconnectedDevice() throws Exception {
        contextPool.register(1, shortDeviceContextMock, DeviceContext.class);
        contextPool.get(1, DeviceContext.class);
    }

    //

    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetUnregisteredCPU() throws ContextNotFoundException {
        contextPool.get(0, CPUContext.class);
    }

    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetUnregisteredCompiler() throws ContextNotFoundException {
        contextPool.get(0, OtherContext.class);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetUnregisteredMemory() throws ContextNotFoundException {
        contextPool.get(0, MemoryContext.class);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetUnregisteredDevice() throws ContextNotFoundException {
        contextPool.get(0, DeviceContext.class);
    }

    //

    @Test(expected = ContextNotFoundException.class)
    public void testGetWrongIndexCPU() throws Exception {
        contextPool.register(1, cpuContextMock, CPUContext.class);
        contextPool.get(0, CPUContext.class, 1);
    }

    @Test(expected = ContextNotFoundException.class)
    public void testGetWrongIndexCompiler() throws Exception {
        contextPool.register(1, compilerContextMock, OtherContext.class);
        contextPool.get(0, OtherContext.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ContextNotFoundException.class)
    public void testGetWrongIndexMemory() throws Exception {
        contextPool.register(1, shortMemoryContextMock, MemoryContext.class);
        contextPool.get(0, MemoryContext.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ContextNotFoundException.class)
    public void testGetWrongIndexDevice() throws Exception {
        contextPool.register(1, shortDeviceContextMock, DeviceContext.class);
        contextPool.get(0, DeviceContext.class, 1);
    }

    //

    @Test
    public void testRejectsUnregisteredSubinterface() throws Exception {
        TestOtherContext compilerContextMock = niceMock(TestOtherContext.class);

        contextPool.register(0, compilerContextMock, TestOtherContext.class);

        assertEquals(compilerContextMock, contextPool.get(1, TestOtherContext.class));
        assertThrows(ContextNotFoundException.class,
                () -> contextPool.get(1, DifferentTestOtherContext.class));
    }

    @Test
    public void sameShapeInterfacesHaveIndependentRegistrationAndRemoval() throws Exception {
        FirstContext first = () -> 1;
        SecondContext second = () -> 2;
        contextPool.register(0, first, FirstContext.class);
        assertThrows(ContextNotFoundException.class,
                () -> contextPool.get(emuStudioId, SecondContext.class, -1));
        contextPool.register(0, second, SecondContext.class);
        assertEquals(1, contextPool.get(emuStudioId, FirstContext.class, -1).value());
        assertEquals(2, contextPool.get(emuStudioId, SecondContext.class, -1).value());
        assertTrue(contextPool.unregister(0, FirstContext.class));
        assertEquals(2, contextPool.get(emuStudioId, SecondContext.class, -1).value());
    }

    interface FirstContext extends Context { int value(); }

    interface SecondContext extends Context { int value(); }

    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetGeneralInterfaceWhenNotRegisteredCPU() throws Exception {
        TestCPUContext cpuContextMock = niceMock(TestCPUContext.class);
        contextPool.register(0, cpuContextMock, TestCPUContext.class);
        contextPool.get(1, CPUContext.class);
    }

    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetGeneralInterfaceWhenNotRegisteredCompiler() throws Exception {
        TestOtherContext compilerContextMock = niceMock(TestOtherContext.class);
        contextPool.register(0, compilerContextMock, TestOtherContext.class);
        contextPool.get(1, OtherContext.class);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetGeneralInterfaceWhenNotRegisteredMemory() throws Exception {
        TestMemoryContext shortMemoryContextMock = niceMock(TestMemoryContext.class);
        contextPool.register(0, shortMemoryContextMock, TestMemoryContext.class);
        contextPool.get(1, MemoryContext.class);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetGeneralInterfaceWhenNotRegisteredDevice() throws Exception {
        TestDeviceContext shortDeviceContextMock = niceMock(TestDeviceContext.class);
        contextPool.register(0, shortDeviceContextMock, TestDeviceContext.class);
        contextPool.get(1, DeviceContext.class);
    }

    //

    @Test
    public void testRejectsUnregisteredMemorySpecialization() throws Exception {
        contextPool.register(0, shortMemoryContextMock, MemoryContext.class);
        assertThrows(ContextNotFoundException.class,
                () -> contextPool.get(1, GenericByteMemoryContext.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterWrongInterfaceCPU() throws Exception {
        contextPool.register(1, cpuContextMock, MemoryContext.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterWrongInterfaceMemory() throws Exception {
        contextPool.register(1, shortMemoryContextMock, CPUContext.class);
    }

    //

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterWrongInterfaceCompiler() throws Exception {
        contextPool.register(1, compilerContextMock, CPUContext.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterWrongInterfaceDevice() throws Exception {
        contextPool.register(1, shortDeviceContextMock, CPUContext.class);
    }

    @Test
    public void testUnannotatedContextInterface() throws Exception {
        UnannotatedContext unannotatedContext = niceMock(UnannotatedContext.class);
        contextPool.register(0, unannotatedContext, UnannotatedContext.class);
        assertSame(unannotatedContext, contextPool.get(1, UnannotatedContext.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterNonInterface() {
        contextPool.register(0, new ContextClass(), ContextClass.class);
    }

    @Test(expected = NullPointerException.class)
    public void testGetNullCPU() throws Exception {
        contextPool.get(0, null);
    }

    //

    @Test(expected = NullPointerException.class)
    public void testGetNullCompiler() throws Exception {
        contextPool.get(1, null);
    }

    //

    @Test(expected = NullPointerException.class)
    public void testGetNullMemory() throws Exception {
        contextPool.get(2, null);
    }

    @Test(expected = NullPointerException.class)
    public void testGetNullDevice() throws Exception {
        contextPool.get(3, null);
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
        contextPool.register(0, compilerContextMock, OtherContext.class);
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
        assertEquals(cpuContextMock, contextPool.get(emuStudioId, CPUContext.class));
    }


    private static <T> T niceMock(Class<T> type) {
        return mock(type);
    }

    private static PluginConnections connectedComputer(boolean connected) {
        return (pluginID, toPluginID) -> pluginID != toPluginID && connected;
    }

    interface TestOtherContext extends OtherContext {
        void testCompilerMethod();
    }

    interface DifferentTestOtherContext extends TestOtherContext {
    }

    interface TestCPUContext extends CPUContext {
        void testMethod();
    }

    interface TestMemoryContext extends MemoryContext<Short> {
        void testMemoryMethod();
    }

    interface TestDeviceContext extends DeviceContext<Short> {
        void testDeviceMethod();
    }

    interface GenericByteMemoryContext extends MemoryContext<Byte> {
    }

    interface OtherContext extends Context {
    }

    interface UnannotatedContext extends Context {
    }

    static class ContextClass implements Context {
    }
}
