/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.loaders;

import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

public class TapLoader implements Loader {
    @Override
    public boolean isMemoryAddressAware() {
        // Only tapes with memory blocks (with given start address) are loadable
        return true;
    }

    @Override
    public void load(Path path, ByteMemoryContext memory, MemoryBank bank) throws IOException{
        int oldBank = memory.getSelectedBank();
        try (FileInputStream stream = new FileInputStream(path.toFile())) {
            memory.selectBank(bank.bank);
            parse(stream.readAllBytes(), memory);
        } catch (IOException e) {
            memory.selectBank(oldBank);
            throw e;
        }
    }

    private void parse(byte[] content, ByteMemoryContext memory) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(content);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        SpectrumTapeData tapeData = new SpectrumTapeData();
        while (buffer.hasRemaining()) {
            if (buffer.remaining() < Short.BYTES) {
                throw new IOException("Incomplete TAP block length");
            }
            int blockLength = buffer.getShort() & 0xFFFF;
            if (blockLength < 2 || blockLength > buffer.remaining()) {
                throw new IOException("Invalid TAP block length: " + blockLength);
            }

            byte[] block = new byte[blockLength];
            buffer.get(block);
            tapeData.accept(block, memory);
        }
    }
}
