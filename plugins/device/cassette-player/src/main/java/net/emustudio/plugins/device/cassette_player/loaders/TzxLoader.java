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
package net.emustudio.plugins.device.cassette_player.loaders;

import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Objects;

@ThreadSafe
public class TzxLoader implements Loader {
    private final static Logger LOGGER = LoggerFactory.getLogger(TzxLoader.class);
    private final Path path;

    public TzxLoader(Path path) {
        this.path = Objects.requireNonNull(path);
    }

    @Override
    public void load(CassetteListener listener) throws IOException {
        try (FileInputStream stream = new FileInputStream(path.toFile())) {
            interpret(stream.readAllBytes(), listener);
        }
    }

    private void interpret(byte[] content, CassetteListener listener) {
        ByteBuffer buffer = ByteBuffer.wrap(content);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.position() < buffer.limit()) {
            int id = buffer.get() & 0xFF; // 16 for ROM-saved block
            int pause = buffer.getShort() & 0xFFFF; // pause after this block
            int blockLength = buffer.getShort() & 0xFFFF;
            int flagByte = buffer.get() & 0xFF;

            if (flagByte == 0) {
                TapTzxHeader header = TapTzxHeader.parse(buffer);
                switch (header.id) {
                    case 0: // program
                        listener.onProgram(header.fileName, header.dataLength, header.parameter1, header.parameter2);
                        break;
                    case 1: // number array
                        listener.onNumberArray(header.fileName, header.dataLength, header.getVariable());
                        break;
                    case 2: // String array
                        listener.onStringArray(header.fileName, header.dataLength, header.getVariable());
                        break;
                    case 3: // Memory block
                        listener.onMemoryBlock(header.fileName, header.dataLength, header.parameter1);
                        break;
                    default:
                        LOGGER.warn("TZX: Unknown header ID: " + header.id);
                }
            } else {
                byte[] data = new byte[blockLength - 2];
                buffer.get(data);

                if (flagByte == 255) {
                    listener.onData(data);
                } else {
                    LOGGER.warn("TZX: Unknown flag: " + flagByte);
                }
            }
            buffer.get(); // checksum
            listener.onPause(pause);
        }
    }
}
