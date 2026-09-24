/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.loaders;

import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

public class TzxLoader implements Loader {
    private static final byte[] SIGNATURE = {'Z', 'X', 'T', 'a', 'p', 'e', '!', 0x1A};

    @Override
    public boolean isMemoryAddressAware() {
        // Only tapes with memory blocks (with given start address) are loadable
        return true;
    }

    @Override
    public void load(Path path, ByteMemoryContext memory, MemoryBank bank) throws IOException {
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
        readHeader(buffer);

        SpectrumTapeData tapeData = new SpectrumTapeData();
        while (buffer.hasRemaining()) {
            int blockId = buffer.get() & 0xFF;
            int blockLength;
            if (blockId == 0x10) {
                requireRemaining(buffer, 4, "standard-speed block header");
                buffer.getShort(); // pause after block
                blockLength = buffer.getShort() & 0xFFFF;
            } else if (blockId == 0x11) {
                requireRemaining(buffer, 18, "turbo-speed block header");
                buffer.position(buffer.position() + 15);
                blockLength = readUnsigned24(buffer);
            } else {
                throw new IOException(String.format("Unsupported TZX block ID: 0x%02X", blockId));
            }
            requireRemaining(buffer, blockLength, "block data");
            byte[] block = new byte[blockLength];
            buffer.get(block);
            tapeData.accept(block, memory);
        }
    }

    private static void readHeader(ByteBuffer buffer) throws IOException {
        requireRemaining(buffer, SIGNATURE.length + 2, "file header");
        for (byte expected : SIGNATURE) {
            if (buffer.get() != expected) {
                throw new IOException("Invalid TZX signature");
            }
        }
        buffer.get(); // major version
        buffer.get(); // minor version
    }

    private static int readUnsigned24(ByteBuffer buffer) {
        return (buffer.get() & 0xFF) | ((buffer.get() & 0xFF) << 8) | ((buffer.get() & 0xFF) << 16);
    }

    private static void requireRemaining(ByteBuffer buffer, int count, String section) throws IOException {
        if (buffer.remaining() < count) {
            throw new IOException("Incomplete TZX " + section);
        }
    }
}
