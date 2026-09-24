/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

final class ZxSpectrumSnapshotLoader {
    private static final int SNA_HEADER_SIZE = 27;
    private static final int Z80_HEADER_SIZE = 30;
    private static final int PAGE_SIZE = 0x4000;

    private ZxSpectrumSnapshotLoader() {
    }

    static ZxSpectrumSnapshot load(Path path) throws IOException {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ENGLISH);
        byte[] data = Files.readAllBytes(path);
        if (fileName.endsWith(".sna")) {
            return loadSna(data);
        }
        if (fileName.endsWith(".z80")) {
            return loadZ80(data);
        }
        throw new IOException("Unsupported snapshot format: " + path.getFileName());
    }

    static ZxSpectrumSnapshot loadSna(byte[] data) throws IOException {
        if (data.length != SNA_HEADER_SIZE + ZxSpectrumSnapshot.RAM_SIZE) {
            throw new IOException("Only 48K SNA snapshots are supported");
        }

        ZxSpectrumSnapshot snapshot = new ZxSpectrumSnapshot();
        snapshot.interrupt = unsigned(data[0]);
        setPair(snapshot.alternateRegisters, EmulatorEngine.REG_H, EmulatorEngine.REG_L, word(data, 1));
        setPair(snapshot.alternateRegisters, EmulatorEngine.REG_D, EmulatorEngine.REG_E, word(data, 3));
        setPair(snapshot.alternateRegisters, EmulatorEngine.REG_B, EmulatorEngine.REG_C, word(data, 5));
        snapshot.alternateFlags = unsigned(data[7]);
        snapshot.alternateRegisters[EmulatorEngine.REG_A] = unsigned(data[8]);
        setPair(snapshot.registers, EmulatorEngine.REG_H, EmulatorEngine.REG_L, word(data, 9));
        setPair(snapshot.registers, EmulatorEngine.REG_D, EmulatorEngine.REG_E, word(data, 11));
        setPair(snapshot.registers, EmulatorEngine.REG_B, EmulatorEngine.REG_C, word(data, 13));
        snapshot.indexY = word(data, 15);
        snapshot.indexX = word(data, 17);
        snapshot.iff1 = snapshot.iff2 = (unsigned(data[19]) & 0x04) != 0;
        snapshot.refresh = unsigned(data[20]);
        snapshot.flags = unsigned(data[21]);
        snapshot.registers[EmulatorEngine.REG_A] = unsigned(data[22]);
        snapshot.stackPointer = word(data, 23);
        snapshot.interruptMode = unsigned(data[25]);
        snapshot.border = unsigned(data[26]) & 0x07;
        if (snapshot.interruptMode > 2) {
            throw new IOException("Invalid SNA interrupt mode: " + snapshot.interruptMode);
        }

        copyRam(data, SNA_HEADER_SIZE, snapshot.ram, 0, ZxSpectrumSnapshot.RAM_SIZE);
        if (snapshot.stackPointer < ZxSpectrumSnapshot.RAM_START || snapshot.stackPointer == 0xFFFF) {
            throw new IOException("SNA program counter is outside the 48K RAM stack");
        }
        int stackOffset = snapshot.stackPointer - ZxSpectrumSnapshot.RAM_START;
        snapshot.programCounter = unsigned(snapshot.ram[stackOffset])
                | (unsigned(snapshot.ram[stackOffset + 1]) << 8);
        snapshot.stackPointer = (snapshot.stackPointer + 2) & 0xFFFF;
        return snapshot;
    }

    static ZxSpectrumSnapshot loadZ80(byte[] data) throws IOException {
        if (data.length < Z80_HEADER_SIZE) {
            throw new IOException("Truncated Z80 snapshot header");
        }

        ZxSpectrumSnapshot snapshot = parseZ80Header(data);
        int headerProgramCounter = word(data, 6);
        if (headerProgramCounter != 0) {
            snapshot.programCounter = headerProgramCounter;
            boolean compressed = (unsigned(data[12]) & 0x20) != 0;
            byte[] ram = compressed
                    ? decompress(data, Z80_HEADER_SIZE, data.length - Z80_HEADER_SIZE, ZxSpectrumSnapshot.RAM_SIZE)
                    : exactSlice(data, Z80_HEADER_SIZE, ZxSpectrumSnapshot.RAM_SIZE, "Z80 v1 RAM");
            copyRam(ram, 0, snapshot.ram, 0, ram.length);
            return snapshot;
        }

        if (data.length < 35) {
            throw new IOException("Truncated extended Z80 snapshot header");
        }
        int extendedHeaderSize = word(data, 30);
        int blocksOffset = 32 + extendedHeaderSize;
        if (extendedHeaderSize < 23 || blocksOffset > data.length) {
            throw new IOException("Invalid extended Z80 snapshot header");
        }
        int hardwareMode = unsigned(data[34]);
        boolean versionThree = extendedHeaderSize >= 54;
        if (!(hardwareMode == 0 || hardwareMode == 1 || (versionThree && hardwareMode == 3))) {
            throw new IOException("Only 48K Z80 snapshots are supported (hardware mode " + hardwareMode + ")");
        }

        snapshot.programCounter = word(data, 32);
        boolean[] loadedPages = new boolean[3];
        int offset = blocksOffset;
        while (offset < data.length) {
            if (data.length - offset < 3) {
                throw new IOException("Truncated Z80 memory block header");
            }
            int encodedSize = word(data, offset);
            int page = unsigned(data[offset + 2]);
            offset += 3;
            int sourceSize = encodedSize == 0xFFFF ? PAGE_SIZE : encodedSize;
            if (sourceSize > data.length - offset) {
                throw new IOException("Truncated Z80 memory block");
            }

            int ramOffset = pageOffset(page);
            if (ramOffset >= 0) {
                byte[] pageData = encodedSize == 0xFFFF
                        ? exactSlice(data, offset, PAGE_SIZE, "Z80 memory page")
                        : decompress(data, offset, encodedSize, PAGE_SIZE);
                copyRam(pageData, 0, snapshot.ram, ramOffset, PAGE_SIZE);
                loadedPages[ramOffset / PAGE_SIZE] = true;
            }
            offset += sourceSize;
        }
        if (!loadedPages[0] || !loadedPages[1] || !loadedPages[2]) {
            throw new IOException("Z80 snapshot does not contain all 48K RAM pages");
        }
        return snapshot;
    }

    private static ZxSpectrumSnapshot parseZ80Header(byte[] data) {
        ZxSpectrumSnapshot snapshot = new ZxSpectrumSnapshot();
        snapshot.registers[EmulatorEngine.REG_A] = unsigned(data[0]);
        snapshot.flags = unsigned(data[1]);
        snapshot.registers[EmulatorEngine.REG_C] = unsigned(data[2]);
        snapshot.registers[EmulatorEngine.REG_B] = unsigned(data[3]);
        snapshot.registers[EmulatorEngine.REG_L] = unsigned(data[4]);
        snapshot.registers[EmulatorEngine.REG_H] = unsigned(data[5]);
        snapshot.stackPointer = word(data, 8);
        snapshot.interrupt = unsigned(data[10]);
        snapshot.refresh = (unsigned(data[11]) & 0x7F) | ((unsigned(data[12]) & 0x01) << 7);
        snapshot.border = (unsigned(data[12]) >>> 1) & 0x07;
        snapshot.registers[EmulatorEngine.REG_E] = unsigned(data[13]);
        snapshot.registers[EmulatorEngine.REG_D] = unsigned(data[14]);
        snapshot.alternateRegisters[EmulatorEngine.REG_C] = unsigned(data[15]);
        snapshot.alternateRegisters[EmulatorEngine.REG_B] = unsigned(data[16]);
        snapshot.alternateRegisters[EmulatorEngine.REG_E] = unsigned(data[17]);
        snapshot.alternateRegisters[EmulatorEngine.REG_D] = unsigned(data[18]);
        snapshot.alternateRegisters[EmulatorEngine.REG_L] = unsigned(data[19]);
        snapshot.alternateRegisters[EmulatorEngine.REG_H] = unsigned(data[20]);
        snapshot.alternateRegisters[EmulatorEngine.REG_A] = unsigned(data[21]);
        snapshot.alternateFlags = unsigned(data[22]);
        snapshot.indexY = word(data, 23);
        snapshot.indexX = word(data, 25);
        snapshot.iff1 = data[27] != 0;
        snapshot.iff2 = data[28] != 0;
        snapshot.interruptMode = unsigned(data[29]) & 0x03;
        if (snapshot.interruptMode == 3) {
            snapshot.interruptMode = 2;
        }
        return snapshot;
    }

    private static byte[] decompress(byte[] data, int offset, int length, int expectedSize) throws IOException {
        int end = offset + length;
        ByteArrayOutputStream result = new ByteArrayOutputStream(expectedSize);
        while (offset < end && result.size() < expectedSize) {
            int value = unsigned(data[offset++]);
            if (value == 0xED && offset + 2 < end && unsigned(data[offset]) == 0xED) {
                offset++;
                int count = unsigned(data[offset++]);
                int repeated = unsigned(data[offset++]);
                if (count == 0 || result.size() + count > expectedSize) {
                    throw new IOException("Invalid Z80 compressed run");
                }
                for (int i = 0; i < count; i++) {
                    result.write(repeated);
                }
            } else {
                result.write(value);
            }
        }
        if (result.size() != expectedSize) {
            throw new IOException("Z80 compressed block has " + result.size() + " bytes; expected " + expectedSize);
        }
        return result.toByteArray();
    }

    private static byte[] exactSlice(byte[] data, int offset, int size, String description) throws IOException {
        if (offset < 0 || size < 0 || data.length - offset < size) {
            throw new IOException("Truncated " + description);
        }
        if (offset + size != data.length && "Z80 v1 RAM".equals(description)) {
            throw new IOException("Invalid uncompressed Z80 v1 snapshot size");
        }
        byte[] result = new byte[size];
        System.arraycopy(data, offset, result, 0, size);
        return result;
    }

    private static void copyRam(byte[] source, int sourceOffset, Byte[] target, int targetOffset, int count) {
        for (int i = 0; i < count; i++) {
            target[targetOffset + i] = source[sourceOffset + i];
        }
    }

    private static int pageOffset(int page) {
        if (page == 8) return 0;
        if (page == 4) return PAGE_SIZE;
        if (page == 5) return 2 * PAGE_SIZE;
        return -1;
    }

    private static void setPair(int[] registers, int high, int low, int value) {
        registers[high] = (value >>> 8) & 0xFF;
        registers[low] = value & 0xFF;
    }

    private static int word(byte[] data, int offset) {
        return unsigned(data[offset]) | (unsigned(data[offset + 1]) << 8);
    }

    private static int unsigned(byte value) {
        return value & 0xFF;
    }

    private static int unsigned(Byte value) {
        return value & 0xFF;
    }
}
