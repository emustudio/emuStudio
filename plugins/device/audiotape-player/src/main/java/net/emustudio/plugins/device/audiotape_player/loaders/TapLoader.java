/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player.loaders;

import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Objects;

// https://sinclair.wiki.zxnet.co.uk/wiki/TAP_format
// https://documentation.help/BASin/format_tape.html

/**
 * TAP file loader.
 * <p>
 * Loads full file content and schedules it for playback.
 */
@ThreadSafe
public class TapLoader implements Loader {
    private final static Logger LOGGER = LoggerFactory.getLogger(TapLoader.class);
    private final Path path;

    public TapLoader(Path path) {
        this.path = Objects.requireNonNull(path);
    }

    @Override
    public void load(TapePlayback playback) throws IOException {
        try (FileInputStream stream = new FileInputStream(path.toFile())) {
            interpret(stream.readAllBytes(), playback);
        }
    }

    private void interpret(byte[] content, TapePlayback playback) {
        ByteBuffer buffer = ByteBuffer.wrap(content);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        playback.onFileStart();

        while (buffer.position() < buffer.limit() && !Thread.currentThread().isInterrupted()) {
            int blockLength = buffer.getShort() & 0xFFFF; // - flag - checksum
            int flagByte = buffer.get() & 0xFF;

            byte[] data = new byte[blockLength - 2];
            buffer.get(data);

            byte checksum = buffer.get(); // checksum
            int controlChecksum = flagByte;
            for (byte d : data) {
                controlChecksum ^= (d & 0xFF);
            }
            if ((checksum & 0xFF) != (controlChecksum & 0xFF)) {
                LOGGER.error(String.format("Tape checksum is wrong: expected=%02X != %02X", checksum & 0xFF, controlChecksum & 0xFF));
            }

            if (flagByte < 0x80) {
                playback.onHeaderStart();
                TapTzxHeader header = TapTzxHeader.parse(ByteBuffer.wrap(data));
                switch (header.id) {
                    case 0: // program
                        playback.onProgram(header.fileName, header.dataLength, header.parameter1, header.parameter2);
                        break;
                    case 1: // number array
                        playback.onNumberArray(header.fileName, header.dataLength, header.getVariable());
                        break;
                    case 2: // String array
                        playback.onStringArray(header.fileName, header.dataLength, header.getVariable());
                        break;
                    case 3: // Memory block
                        playback.onMemoryBlock(header.fileName, header.dataLength, header.parameter1);
                        break;
                    default:
                        LOGGER.warn("TAP: Unknown header ID: " + header.id);
                }
            } else {
                playback.onDataStart();
            }
            playback.onBlockFlag(flagByte);
            playback.onBlockData(data);
            playback.onBlockChecksum(checksum);
        }
        playback.onFileEnd();
    }
}
