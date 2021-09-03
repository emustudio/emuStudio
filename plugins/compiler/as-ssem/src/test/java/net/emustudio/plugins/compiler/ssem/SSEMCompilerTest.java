/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.compiler.ssem;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.cpu.testsuite.memory.MemoryStub;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;

public class SSEMCompilerTest {
    private SSEMCompiler compiler;
    private MemoryStub<Byte> memoryStub;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        memoryStub = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);

        ContextPool pool = createNiceMock(ContextPool.class);
        expect(pool.getMemoryContext(0, MemoryContext.class)).andReturn(memoryStub).anyTimes();
        replay(pool);
        ApplicationApi applicationApi = createNiceMock(ApplicationApi.class);
        expect(applicationApi.getContextPool()).andReturn(pool).anyTimes();
        replay(applicationApi);

        compiler = new SSEMCompiler(0L, applicationApi, PluginSettings.UNAVAILABLE);
        compiler.initialize();
    }

    private void compile(String content) throws Exception {
        File sourceFile = folder.newFile();
        Files.write(sourceFile.toPath(), content.getBytes(), StandardOpenOption.WRITE);

        File outputFile = folder.newFile();
        if (!compiler.compile(sourceFile.getAbsolutePath(), outputFile.getAbsolutePath())) {
            throw new Exception("Compilation failed");
        }
    }

    private void assertProgram(int... bytes) {
        Byte[] value = memoryStub.read(0, bytes.length);

        assertArrayEquals(
            String.format(
                "Expected=%x, but was=%x",
                NumberUtils.readInt(bytes, NumberUtils.Strategy.BIG_ENDIAN),
                NumberUtils.readInt(value, NumberUtils.Strategy.BIG_ENDIAN)
            ),
            NumberUtils.nativeIntsToNativeBytes(bytes), NumberUtils.numbersToNativeBytes(value)
        );
    }

    @Test
    public void testVersionIsKnown() {
        assertNotEquals("(unknown)", compiler.getVersion());
    }

    @Test
    public void testCopyrightIsKnown() {
        assertNotEquals("(unknown)", compiler.getCopyright());
    }

    @Test
    public void testSTO() throws Exception {
        compile("00 STO 22\n");
        assertProgram(0x68, 3, 0, 0);
    }

    @Test
    public void testLDN() throws Exception {
        compile("00 LDN 29");
        assertProgram(0xB8,2,0,0);
    }

    @Test
    public void testSUB() throws Exception {
        compile("00 SUB 30");
        assertProgram(0x78,4,0,0);
    }

    @Test
    public void testSTP() throws Exception {
        compile("00 STP");
        assertProgram(0,7,0,0);
    }
}
