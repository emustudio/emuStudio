/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.loaders;

import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.emulib.runtime.io.FileLoader;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/** Bank selection, byte placement and strict Spectrum CODE policy; emuLib decodes files. */
public final class MemoryImageLoader {
    private MemoryImageLoader() { }

    public static FileLoader createLoader(Path path) {
        return FileLoader.forPath(path).orElseGet(() -> FileLoader.forFormat(FileLoader.Format.BIN));
    }

    public static boolean requiresLoadAddress(FileLoader loader) {
        return loader.getFormat() == FileLoader.Format.BIN;
    }

    public static void load(Path path, ByteMemoryContext memory, MemoryBank bank) throws IOException {
        int oldBank = memory.getSelectedBank();
        try {
            memory.selectBank(bank.bank);
            if (bank.address < 0) throw new IllegalArgumentException("Negative load address");
            createLoader(path).load(path, new FileLoader.Listener() {
                private FileLoader.SpectrumHeader header;

                @Override
                public void onBlock(FileLoader.Block block) throws IOException {
                    if (!block.getFormat().isTape()) {
                        write(block.getAddress().orElse(bank.address), block.getData());
                        return;
                    }
                    if (block.getId() != 0x10 && block.getId() != 0x11) {
                        throw new IOException(String.format("Unsupported TZX block ID: 0x%02X", block.getId()));
                    }
                    if (block.getUsedBitsInLastByte() != 8) {
                        throw new IOException("Memory loading requires complete data bytes");
                    }
                    byte[] packet = block.getData();
                    if (packet.length < 2) throw new IOException("Tape block must contain a flag and checksum");
                    if (!block.hasValidChecksum()) throw new IOException("Invalid tape block checksum");
                    int flag = packet[0] & 0xFF;
                    if (flag == 0) {
                        header = block.getSpectrumHeader()
                                .orElseThrow(() -> new IOException("Tape header block must be 19 bytes"));
                        if (header.getType() != 3) header = null;
                    } else if (flag == 0xFF && header != null) {
                        byte[] data = Arrays.copyOfRange(packet, 1, packet.length - 1);
                        if (data.length != header.getDataLength()) {
                            throw new IOException("Tape data length does not match preceding header");
                        }
                        write(header.getParameter1(), data);
                        header = null;
                    }
                }

                private void write(int address, byte[] bytes) {
                    memory.write(address, NumberUtils.nativeBytesToBytes(bytes));
                }
            }, FileLoader.Options.fileOrder());
        } catch (IOException e) {
            memory.selectBank(oldBank);
            throw e;
        }
    }

    public static final class MemoryBank {
        final int bank;
        final int address;

        private MemoryBank(int bank, int address) {
            this.bank = bank;
            this.address = address;
        }

        public static MemoryBank of(int bank, int address) { return new MemoryBank(bank, address); }
    }
}
