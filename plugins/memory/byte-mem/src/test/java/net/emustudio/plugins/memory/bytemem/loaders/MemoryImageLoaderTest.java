/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.loaders;

import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertThrows;

public class MemoryImageLoaderTest {
    @Test
    public void everyFormatRestoresBankOnFileFailure() {
        for (String extension : new String[]{"bin", "hex", "tap", "tzx", "unknown"}) {
            ByteMemoryContext memory = createStrictMock(ByteMemoryContext.class);
            expect(memory.getSelectedBank()).andReturn(3);
            memory.selectBank(1);
            memory.selectBank(3);
            replay(memory);
            assertThrows(IOException.class, () -> MemoryImageLoader.load(
                    Path.of("/nonexistent/image." + extension), memory, MemoryImageLoader.MemoryBank.of(1, 0)));
            verify(memory);
        }
    }
}
