/*
 * Run-time library for emuStudio and plugins.
 *
 *     Copyright (C) 2006-2023  Peter Jakubčo
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.virtualcomputer;

import net.emustudio.application.virtualcomputer.ContextStubs.DifferentCPUContextStubWithEqualHash;
import net.emustudio.application.virtualcomputer.ContextStubs.DifferentCompilerContextStubWithEqualHash;
import net.emustudio.application.virtualcomputer.ContextStubs.DifferentDeviceContextStubWithEqualHash;
import net.emustudio.application.virtualcomputer.ContextStubs.DifferentShortMemoryContextStubWithEqualHash;
import net.emustudio.application.virtualcomputer.stubs.*;
import net.emustudio.emulib.plugins.Context;
import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.compiler.CompilerContext;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.InvalidContextException;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

public class ContextPoolImplTest {
    private static final int emuStudioId = 555;

    private CPUContextStub cpuContextMock;
    private CPUContextStub cpuContextMockAnother;
    private ShortMemoryContextStub shortMemoryContextMock;
    private ShortMemoryContextStub shortMemoryContextMockAnother;
    private CompilerContextStub compilerContextMock;
    private CompilerContextStub compilerContextMockAnother;
    private DeviceContextStub shortDeviceContextMock;
    private DeviceContextStub shortDeviceContextMockAnother;
    private ContextPoolImpl contextPool;

    @Before
    public void setUp() {
        cpuContextMock = EasyMock.createNiceMock(CPUContextStub.class);
        cpuContextMockAnother = EasyMock.createNiceMock(CPUContextStub.class);
        shortMemoryContextMock = EasyMock.createNiceMock(ShortMemoryContextStub.class);
        shortMemoryContextMockAnother = EasyMock.createNiceMock(ShortMemoryContextStub.class);
        compilerContextMock = EasyMock.createNiceMock(CompilerContextStub.class);
        compilerContextMockAnother = EasyMock.createNiceMock(CompilerContextStub.class);
        shortDeviceContextMock = EasyMock.createNiceMock(DeviceContextStub.class);
        shortDeviceContextMockAnother = EasyMock.createNiceMock(DeviceContextStub.class);
        replay(
                cpuContextMock, cpuContextMockAnother,
                shortMemoryContextMock, shortMemoryContextMockAnother,
                compilerContextMock, compilerContextMockAnother,
                shortDeviceContextMock, shortDeviceContextMockAnother
        );

        contextPool = new ContextPoolImpl(emuStudioId);
        contextPool.setComputer(new ComputerStub(true));
    }

    @After
    public void tearDown() {
        verify(cpuContextMock, shortMemoryContextMock, compilerContextMock, shortDeviceContextMock);
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
        contextPool.register(0, compilerContextMock, CompilerContextStub.class);
        assertEquals(compilerContextMock, contextPool.getCompilerContext(1, CompilerContextStub.class));
        assertEquals(compilerContextMock, contextPool.getCompilerContext(1, DifferentCompilerContextStubWithEqualHash.class));
    }

    @Test
    public void testRegisterWithDifferentInterfaceThanGetCPU() throws Exception {
        contextPool.register(0, cpuContextMock, CPUContextStub.class);
        assertEquals(cpuContextMock, contextPool.getCPUContext(1, CPUContextStub.class));
        assertEquals(cpuContextMock, contextPool.getCPUContext(1, DifferentCPUContextStubWithEqualHash.class));
    }

    @Test
    public void testRegisterWithDifferentInterfaceThanGetMemory() throws Exception {
        contextPool.register(0, shortMemoryContextMock, ShortMemoryContextStub.class);
        assertEquals(shortMemoryContextMock, contextPool.getMemoryContext(1, ShortMemoryContextStub.class));
        assertEquals(shortMemoryContextMock, contextPool.getMemoryContext(1, DifferentShortMemoryContextStubWithEqualHash.class));
    }

    @Test
    public void testRegisterWithDifferentInterfaceThanGetDevice() throws Exception {
        contextPool.register(0, shortDeviceContextMock, DeviceContextStub.class);
        assertEquals(shortDeviceContextMock, contextPool.getDeviceContext(1, DeviceContextStub.class));
        assertEquals(shortDeviceContextMock, contextPool.getDeviceContext(1, DifferentDeviceContextStubWithEqualHash.class));
    }

    //

    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetGeneralInterfaceWhenNotRegisteredCPU() throws Exception {
        contextPool.register(0, cpuContextMock, CPUContextStub.class);
        contextPool.getCPUContext(1);
    }

    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetGeneralInterfaceWhenNotRegisteredCompiler() throws Exception {
        contextPool.register(0, compilerContextMock, CompilerContextStub.class);
        contextPool.getCompilerContext(1);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetGeneralInterfaceWhenNotRegisteredMemory() throws Exception {
        contextPool.register(0, shortMemoryContextMock, ShortMemoryContextStub.class);
        contextPool.getMemoryContext(1, MemoryContext.class);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = ContextNotFoundException.class)
    public void testCannotGetGeneralInterfaceWhenNotRegisteredDevice() throws Exception {
        contextPool.register(0, shortDeviceContextMock, DeviceContextStub.class);
        contextPool.getDeviceContext(1, DeviceContext.class);
    }

    //

    @Test
    public void testRegisterWithDifferentDataTypeThanGetMemory() throws Exception {
        contextPool.register(0, shortMemoryContextMock, MemoryContext.class);
        assertEquals(shortMemoryContextMock, contextPool.getMemoryContext(1, ByteMemoryContext.class));
    }

    @Test
    public void testRegisterWithDifferentDataTypeThanGetDevice() throws Exception {
        contextPool.register(0, shortDeviceContextMock, DeviceContext.class);
        assertEquals(shortDeviceContextMock, contextPool.getDeviceContext(1, ByteDeviceContext.class));
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
        Context unannotatedContext = EasyMock.createNiceMock(UnannotatedContextStub.class);
        contextPool.register(0, unannotatedContext, UnannotatedContextStub.class);
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
        contextPool.setComputer(new ComputerStub(false));
        contextPool.register(0, cpuContextMock, CPUContext.class);
        assertEquals(cpuContextMock, contextPool.getCPUContext(emuStudioId));
    }

    @PluginContext
    interface ByteMemoryContext extends MemoryContext<Byte> {
    }

    //

    @PluginContext
    interface ByteDeviceContext extends DeviceContext<Byte> {
    }

    private static class ComputerStub implements PluginConnections {
        private final boolean connected;

        ComputerStub(boolean connected) {
            this.connected = connected;
        }

        @Override
        public boolean isConnected(long pluginID, long toPluginID) {
            return pluginID != toPluginID && connected;
        }
    }
}
