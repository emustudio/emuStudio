/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.loaders;

import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

final class SpectrumTapeData {
    private final static Logger LOGGER = LoggerFactory.getLogger(SpectrumTapeData.class);

    private Integer startAddress;
    private int expectedLength;

    void accept(byte[] block, ByteMemoryContext memory) throws IOException {
        if (block.length < 2) {
            throw new IOException("TAP block must contain a flag and checksum");
        }
        validateChecksum(block);

        int flag = block[0] & 0xFF;
        if (flag == 0) {
            parseHeader(block);
        } else if (flag == 0xFF) {
            loadData(block, memory);
        }
    }

    private void parseHeader(byte[] block) throws IOException {
        startAddress = null;
        expectedLength = 0;
        if (block.length != 19) {
            throw new IOException("TAP header block must be 19 bytes");
        }

        int headerType = block[1] & 0xFF;
        if (headerType == 3) {
            expectedLength = littleEndianWord(block, 12);
            startAddress = littleEndianWord(block, 14);
        }
    }

    private void loadData(byte[] block, ByteMemoryContext memory) throws IOException {
        byte[] data = Arrays.copyOfRange(block, 1, block.length - 1);
        if (startAddress == null) {
            LOGGER.warn("Ignoring non-memory block data (program or variables)");
            return;
        }
        if (data.length != expectedLength) {
            throw new IOException("TAP data length does not match preceding header");
        }

        memory.write(startAddress, NumberUtils.nativeBytesToBytes(data));
        startAddress = null;
        expectedLength = 0;
    }

    private static void validateChecksum(byte[] block) throws IOException {
        int checksum = 0;
        for (byte value : block) {
            checksum ^= value & 0xFF;
        }
        if (checksum != 0) {
            throw new IOException("Invalid TAP block checksum");
        }
    }

    private static int littleEndianWord(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }
}
