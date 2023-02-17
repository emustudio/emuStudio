/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.memory.bytemem.loaders;

import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.io.RandomAccessFile;
import java.nio.file.Path;

public class BinaryLoader implements Loader {

    @Override
    public boolean isMemoryAddressAware() {
        return false;
    }

    @Override
    public void load(Path path, ByteMemoryContext memory, MemoryBank bank) throws Exception {
        int oldBank = memory.getSelectedBank();
        try (RandomAccessFile binaryFile = new RandomAccessFile(path.toFile(), "r")) {
            memory.selectBank(bank.bank);

            long position = 0, length = binaryFile.length();
            int address = bank.address;
            while (position < length) {
                memory.write(address++, (byte) (binaryFile.readUnsignedByte() & 0xFF));
                position++;
            }
        } catch (Exception e) {
            memory.selectBank(oldBank);
            throw e;
        }
    }
}
