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

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public interface Loader {

    Map<String, Loader> IMAGE_LOADERS = Map.of(
            "hex", new HexLoader(),
            "tap", new TapLoader(),
            "tzx", new TzxLoader(),
            "bin", new BinaryLoader(),
            "com", new BinaryLoader(),
            "out", new BinaryLoader()
    );

    /**
     * Determines if this loader/format is aware of memory addresses (so user is or is not allowed to choose memory
     * address when loading the file).
     *
     * @return true if the loader is aware of memory addresses; false otherwise
     */
    boolean isMemoryAddressAware();

    /**
     * Loads an image file to memory
     *
     * @param path   image file path
     * @param memory memory context
     * @param bank   memory bank + address
     */
    void load(Path path, ByteMemoryContext memory, MemoryBank bank) throws Exception;

    class MemoryBank {
        final int bank;
        final int address;

        public MemoryBank(int bank, int address) {
            this.bank = bank;
            this.address = address;
        }

        public static MemoryBank of(int bank, int address) {
            return new MemoryBank(bank, address);
        }
    }


    static Loader createLoader(Path path) {
        int index = path.toString().lastIndexOf(".");
        String extension = (index == -1) ?
                "" : path.toString().substring(index + 1).toLowerCase(Locale.ENGLISH);

        return IMAGE_LOADERS
                .entrySet()
                .stream()
                .filter(l -> l.getKey().equals(extension))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(new BinaryLoader()); // unknown/no extension
    }
}
