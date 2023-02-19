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

import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Optional;

public class TapLoader implements Loader {

    @Override
    public boolean isMemoryAddressAware() {
        // Tape is usually loaded by BASIC; interprets program start line to address; it depends on emulated machine..
        return false;
    }

    @Override
    public void load(Path path, ByteMemoryContext memory, MemoryBank bank) throws Exception {
        int oldBank = memory.getSelectedBank();
        try (FileInputStream stream = new FileInputStream(path.toFile())) {
            memory.selectBank(bank.bank);
            parse(stream.readAllBytes(), bank.address, memory);
        } catch (Exception e) {
            memory.selectBank(oldBank);
            throw e;
        }
    }

    private void parse(byte[] content, int address, ByteMemoryContext memory) {
        Optional<Integer> startAddress = Optional.empty();

        ByteBuffer buffer = ByteBuffer.wrap(content);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.position() < buffer.limit()) {
            int blockLength = buffer.getShort() & 0xFFFF;
            int flagByte = buffer.get() & 0xFF;

            if (flagByte == 0) {
                final Optional<Integer> javaFail = startAddress;
                startAddress = parseHeader(buffer).or(() -> javaFail);
            } else {
                byte[] data = new byte[blockLength - 2];
                buffer.get(data);

                if (startAddress.isPresent()) {
                    memory.write(startAddress.get(), NumberUtils.nativeBytesToBytes(data));
                } else {
                    memory.write(0, NumberUtils.nativeBytesToBytes(data));
                    startAddress = Optional.of(data.length);
                }
            }
            buffer.get(); // checksum
        }

        memory.write(address, NumberUtils.nativeBytesToBytes(content));
    }

    private Optional<Integer> parseHeader(ByteBuffer buffer) {
        int headerFlag = buffer.get() & 0xFF;
        byte[] maybeFileName = new byte[10];
        buffer.get(maybeFileName); // filename
        buffer.getShort(); // length
        int maybeAddress = buffer.getShort() & 0xFFFF;
        buffer.getShort();

        if (headerFlag == 3) {
            return Optional.of(maybeAddress);
        }
        return Optional.empty();
    }
}
