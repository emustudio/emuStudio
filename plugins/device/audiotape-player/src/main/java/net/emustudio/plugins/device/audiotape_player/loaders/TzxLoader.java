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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

// https://k1.spdns.de/Develop/Projects/zasm/Info/TZX%20format.html#GLUEBLOCK
// https://worldofspectrum.org/faq/reference/formats.htm
// https://documentation.help/BASin/format_tape.html
@ThreadSafe
public class TzxLoader implements Loader {
    private final static Logger LOGGER = LoggerFactory.getLogger(TzxLoader.class);
    private final Path path;

    private final static String[] ARCHIVE_INFO_TYPES = {
            "Title", "Publisher", "Author", "Publication year",
            "Language", "Type", "Price", "Protection",
            "Origin", "Comment"
    };

    public TzxLoader(Path path) {
        this.path = Objects.requireNonNull(path);
    }

    @Override
    public void load(TapePlayback playback) throws IOException {
        try (FileInputStream stream = new FileInputStream(path.toFile())) {
            interpret(stream.readAllBytes(), playback);
        }
    }

    private void interpret(byte[] content, TapePlayback listener) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(content);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        parseFileHeader(buffer);

        // First pass: index block positions
        List<Integer> blockPositions = indexBlockPositions(buffer);

        listener.onFileStart();

        // Second pass: interpret blocks with control flow support
        int blockIndex = 0;
        Deque<Integer> callStack = new ArrayDeque<>();
        int loopStart = -1;
        int loopCount = 0;

        while (blockIndex >= 0 && blockIndex < blockPositions.size() && !Thread.currentThread().isInterrupted()) {
            buffer.position(blockPositions.get(blockIndex));
            int id = buffer.get() & 0xFF;

            switch (id) {
                case 0x10:
                    parseBlock10(buffer, listener);
                    break;
                case 0x11:
                    parseBlock11(buffer, listener);
                    break;
                case 0x12:
                    parseBlock12(buffer, listener);
                    break;
                case 0x13:
                    parseBlock13(buffer, listener);
                    break;
                case 0x14:
                    parseBlock14(buffer, listener);
                    break;
                case 0x15:
                    parseBlock15(buffer, listener);
                    break;
                case 0x18:
                    parseBlock18(buffer, listener);
                    break;
                case 0x19:
                    parseBlock19(buffer, listener);
                    break;
                case 0x20:
                    parseBlock20(buffer, listener);
                    break;
                case 0x21:
                    parseBlock21(buffer, listener);
                    break;
                case 0x22:
                    listener.onGroupEnd();
                    break;
                case 0x23: { // Jump to block
                    short jump = buffer.getShort();
                    blockIndex += jump;
                    continue; // don't increment blockIndex
                }
                case 0x24: { // Loop start
                    loopCount = buffer.getShort() & 0xFFFF;
                    loopStart = blockIndex;
                    break;
                }
                case 0x25: { // Loop end
                    if (loopCount > 1) {
                        loopCount--;
                        blockIndex = loopStart + 1;
                        continue;
                    }
                    break;
                }
                case 0x26: { // Call sequence
                    int numCalls = buffer.getShort() & 0xFFFF;
                    int[] callTargets = new int[numCalls];
                    for (int i = 0; i < numCalls; i++) {
                        callTargets[i] = blockIndex + buffer.getShort();
                    }
                    // Push return address (after this block), then remaining call targets in reverse
                    callStack.push(blockIndex + 1);
                    for (int i = numCalls - 1; i >= 1; i--) {
                        callStack.push(callTargets[i]);
                    }
                    if (numCalls > 0) {
                        blockIndex = callTargets[0];
                        continue;
                    }
                    break;
                }
                case 0x27: { // Return from sequence
                    if (!callStack.isEmpty()) {
                        blockIndex = callStack.pop();
                        continue;
                    }
                    break;
                }
                case 0x28:
                    parseBlock28(buffer);
                    break;
                case 0x2A:
                    parseBlock2A(buffer, listener);
                    break;
                case 0x2B:
                    parseBlock2B(buffer, listener);
                    break;
                case 0x30:
                    parseBlock30(buffer, listener);
                    break;
                case 0x31:
                    parseBlock31(buffer, listener);
                    break;
                case 0x32:
                    parseBlock32(buffer, listener);
                    break;
                case 0x33:
                    parseBlock33(buffer, listener);
                    break;
                case 0x35:
                    parseBlock35(buffer, listener);
                    break;
                case 0x5A:
                    parseBlock5A(buffer, listener);
                    break;
                default:
                    LOGGER.warn("TZX: Unknown block ID: 0x{}", String.format("%02X", id));
                    break;
            }
            blockIndex++;
        }

        listener.onFileEnd();
    }

    private void parseFileHeader(ByteBuffer buffer) throws IOException {
        byte[] signature = new byte[7];
        buffer.get(signature);
        if (!new String(signature).equals("ZXTape!")) {
            throw new IOException("Invalid file content! (signature mismatch)");
        }
        int endOfTextMarker = buffer.get() & 0xFF;
        if (endOfTextMarker != 0x1A) {
            throw new IOException("Invalid file content! (end of text marker mismatch)");
        }
        buffer.get(); // major version
        buffer.get(); // minor version
    }

    /**
     * First pass: scan through the file to index block start positions.
     * This is needed for control flow blocks (jump, loop, call/return).
     */
    private List<Integer> indexBlockPositions(ByteBuffer buffer) throws IOException {
        List<Integer> positions = new ArrayList<>();
        int startPos = buffer.position();

        while (buffer.position() < buffer.limit()) {
            positions.add(buffer.position());
            int id = buffer.get() & 0xFF;
            skipBlockData(buffer, id);
        }

        buffer.position(startPos);
        return positions;
    }

    /**
     * Skip block data in the buffer (first pass).
     * Each block type has its own length encoding.
     */
    private void skipBlockData(ByteBuffer buffer, int id) throws IOException {
        switch (id) {
            case 0x10: { // Standard Speed Data
                buffer.getShort(); // pause
                int len = buffer.getShort() & 0xFFFF;
                buffer.position(buffer.position() + len);
                break;
            }
            case 0x11: { // Turbo Speed Data
                buffer.position(buffer.position() + 15); // fixed fields
                int len = readUint24(buffer);
                buffer.position(buffer.position() + len);
                break;
            }
            case 0x12: // Pure Tone
                buffer.position(buffer.position() + 4);
                break;
            case 0x13: { // Pulse Sequence
                int count = buffer.get() & 0xFF;
                buffer.position(buffer.position() + count * 2);
                break;
            }
            case 0x14: { // Pure Data
                buffer.position(buffer.position() + 7); // fixed fields
                int len = readUint24(buffer);
                buffer.position(buffer.position() + len);
                break;
            }
            case 0x15: { // Direct Recording
                buffer.position(buffer.position() + 5); // fixed fields
                int len = readUint24(buffer);
                buffer.position(buffer.position() + len);
                break;
            }
            case 0x18: // CSW Recording
            case 0x19: { // Generalized Data
                int len = buffer.getInt();
                buffer.position(buffer.position() + len);
                break;
            }
            case 0x20: // Pause
                buffer.getShort();
                break;
            case 0x21: { // Group Start
                int len = buffer.get() & 0xFF;
                buffer.position(buffer.position() + len);
                break;
            }
            case 0x22: // Group End
            case 0x25: // Loop End
            case 0x27: // Return from Sequence
                // no data
                break;
            case 0x23: // Jump to Block
            case 0x24: // Loop Start
                buffer.getShort();
                break;
            case 0x26: { // Call Sequence
                int count = buffer.getShort() & 0xFFFF;
                buffer.position(buffer.position() + count * 2);
                break;
            }
            case 0x28: { // Select Block
                int len = buffer.getShort() & 0xFFFF;
                buffer.position(buffer.position() + len);
                break;
            }
            case 0x2A: // Stop if in 48K mode
                buffer.getInt(); // always 0
                break;
            case 0x2B: // Set Signal Level
                buffer.getInt(); // block length = 1
                buffer.get();    // signal level
                break;
            case 0x30: { // Text Description
                int len = buffer.get() & 0xFF;
                buffer.position(buffer.position() + len);
                break;
            }
            case 0x31: { // Message
                buffer.get(); // display time
                int len = buffer.get() & 0xFF;
                buffer.position(buffer.position() + len);
                break;
            }
            case 0x32: { // Archive Info
                int len = buffer.getShort() & 0xFFFF;
                buffer.position(buffer.position() + len);
                break;
            }
            case 0x33: { // Hardware Type
                int count = buffer.get() & 0xFF;
                buffer.position(buffer.position() + count * 3);
                break;
            }
            case 0x35: { // Custom Info
                buffer.position(buffer.position() + 16); // ID string
                int len = buffer.getInt();
                buffer.position(buffer.position() + len);
                break;
            }
            case 0x5A: // Glue
                buffer.position(buffer.position() + 9);
                break;
            default:
                throw new IOException(String.format("TZX: Unknown block ID 0x%02X at position %d - cannot skip",
                        id, buffer.position() - 1));
        }
    }

    /**
     * Block 0x10 - Standard Speed Data Block.
     */
    private void parseBlock10(ByteBuffer buffer, TapePlayback listener) {
        int pause = buffer.getShort() & 0xFFFF;
        int blockLength = buffer.getShort() & 0xFFFF;
        int flagByte = buffer.get() & 0xFF;

        if (flagByte < 0x80) {
            // Header: 17 bytes between flag and checksum. Parse for metadata, then emit as
            // pulses so the ROM loader actually receives the header payload (was previously
            // dropped, breaking LOAD "" for any TZX with a real BASIC header block).
            byte[] headerData = new byte[17];
            buffer.get(headerData);
            TapTzxHeader header = TapTzxHeader.parse(ByteBuffer.wrap(headerData).order(ByteOrder.LITTLE_ENDIAN));
            byte checksum = buffer.get();

            listener.onHeaderStart();
            listener.onBlockFlag(flagByte);
            switch (header.id) {
                case 0:
                    listener.onProgram(header.fileName, header.dataLength, header.parameter1, header.parameter2);
                    break;
                case 1:
                    listener.onNumberArray(header.fileName, header.dataLength, header.getVariable());
                    break;
                case 2:
                    listener.onStringArray(header.fileName, header.dataLength, header.getVariable());
                    break;
                case 3:
                    listener.onMemoryBlock(header.fileName, header.dataLength, header.parameter1);
                    break;
                default:
                    LOGGER.warn("TZX: Unknown header ID: {}", header.id);
            }
            listener.onBlockData(headerData);
            listener.onBlockChecksum(checksum);
        } else {
            byte[] data = new byte[blockLength - 2]; // subtract flag and checksum
            buffer.get(data);
            byte checksum = buffer.get();

            listener.onDataStart();
            listener.onBlockFlag(flagByte);
            listener.onBlockData(data);
            listener.onBlockChecksum(checksum);
        }
        if (pause > 0) {
            listener.onPause(pause);
        }
    }

    /**
     * Block 0x11 - Turbo Speed Data Block.
     */
    private void parseBlock11(ByteBuffer buffer, TapePlayback listener) {
        int pilotPulseLen = buffer.getShort() & 0xFFFF;
        int sync1PulseLen = buffer.getShort() & 0xFFFF;
        int sync2PulseLen = buffer.getShort() & 0xFFFF;
        int zeroBitPulseLen = buffer.getShort() & 0xFFFF;
        int oneBitPulseLen = buffer.getShort() & 0xFFFF;
        int pilotToneCount = buffer.getShort() & 0xFFFF;
        int usedBitsInLastByte = buffer.get() & 0xFF;
        int pauseAfterMs = buffer.getShort() & 0xFFFF;
        int dataLength = readUint24(buffer);

        byte[] data = new byte[dataLength];
        buffer.get(data);

        listener.onTurboSpeedData(pilotPulseLen, sync1PulseLen, sync2PulseLen,
                zeroBitPulseLen, oneBitPulseLen, pilotToneCount,
                usedBitsInLastByte, pauseAfterMs, data);
    }

    /**
     * Block 0x12 - Pure Tone.
     */
    private void parseBlock12(ByteBuffer buffer, TapePlayback listener) {
        int pulseLength = buffer.getShort() & 0xFFFF;
        int pulseCount = buffer.getShort() & 0xFFFF;
        listener.onPureTone(pulseLength, pulseCount);
    }

    /**
     * Block 0x13 - Pulse Sequence.
     */
    private void parseBlock13(ByteBuffer buffer, TapePlayback listener) {
        int count = buffer.get() & 0xFF;
        int[] pulseLengths = new int[count];
        for (int i = 0; i < count; i++) {
            pulseLengths[i] = buffer.getShort() & 0xFFFF;
        }
        listener.onPulseSequence(pulseLengths);
    }

    /**
     * Block 0x14 - Pure Data Block.
     */
    private void parseBlock14(ByteBuffer buffer, TapePlayback listener) {
        int zeroBitPulseLen = buffer.getShort() & 0xFFFF;
        int oneBitPulseLen = buffer.getShort() & 0xFFFF;
        int usedBitsInLastByte = buffer.get() & 0xFF;
        int pauseAfterMs = buffer.getShort() & 0xFFFF;
        int dataLength = readUint24(buffer);

        byte[] data = new byte[dataLength];
        buffer.get(data);

        listener.onPureData(zeroBitPulseLen, oneBitPulseLen, usedBitsInLastByte, pauseAfterMs, data);
    }

    /**
     * Block 0x15 - Direct Recording.
     */
    private void parseBlock15(ByteBuffer buffer, TapePlayback listener) {
        int tstatesPerSample = buffer.getShort() & 0xFFFF;
        int pauseAfterMs = buffer.getShort() & 0xFFFF;
        int usedBitsInLastByte = buffer.get() & 0xFF;
        int dataLength = readUint24(buffer);

        byte[] samples = new byte[dataLength];
        buffer.get(samples);

        listener.onDirectRecording(tstatesPerSample, pauseAfterMs, usedBitsInLastByte, samples);
    }

    /**
     * Block 0x18 - CSW Recording.
     */
    private void parseBlock18(ByteBuffer buffer, TapePlayback listener) {
        int blockLength = buffer.getInt();
        int pauseAfterMs = buffer.getShort() & 0xFFFF;
        int sampleRate = readUint24(buffer);
        int compressionType = buffer.get() & 0xFF;
        long storedPulseCount = buffer.getInt() & 0xFFFFFFFFL;
        int dataLength = blockLength - 10;

        byte[] data = new byte[dataLength];
        buffer.get(data);

        listener.onCswRecording(pauseAfterMs, sampleRate, compressionType, storedPulseCount, data);
    }

    /**
     * Block 0x19 - Generalized Data Block.
     */
    private void parseBlock19(ByteBuffer buffer, TapePlayback listener) {
        int blockLength = buffer.getInt();
        int pauseAfterMs = buffer.getShort() & 0xFFFF;
        int remainingLength = blockLength - 2;

        byte[] data = new byte[remainingLength];
        buffer.get(data);

        listener.onGeneralizedData(pauseAfterMs, data);
    }

    /**
     * Block 0x20 - Pause (Silence) or Stop the Tape.
     */
    private void parseBlock20(ByteBuffer buffer, TapePlayback listener) {
        int durationMs = buffer.getShort() & 0xFFFF;
        listener.onPause(durationMs);
    }

    /**
     * Block 0x21 - Group Start.
     */
    private void parseBlock21(ByteBuffer buffer, TapePlayback listener) {
        int len = buffer.get() & 0xFF;
        byte[] name = new byte[len];
        buffer.get(name);
        listener.onGroupStart(new String(name, StandardCharsets.US_ASCII));
    }

    /**
     * Block 0x28 - Select Block (interactive - skipped with warning).
     */
    private void parseBlock28(ByteBuffer buffer) {
        int len = buffer.getShort() & 0xFFFF;
        buffer.position(buffer.position() + len);
        LOGGER.warn("TZX: Select Block (0x28) is not supported, skipping");
    }

    /**
     * Block 0x2A - Stop the tape if in 48K mode.
     */
    private void parseBlock2A(ByteBuffer buffer, TapePlayback listener) {
        buffer.getInt(); // always 0
        listener.onStopIfIn48KMode();
    }

    /**
     * Block 0x2B - Set Signal Level.
     */
    private void parseBlock2B(ByteBuffer buffer, TapePlayback listener) {
        buffer.getInt(); // block length = 1
        int level = buffer.get() & 0xFF;
        listener.onSetSignalLevel(level);
    }

    /**
     * Block 0x30 - Text Description.
     */
    private void parseBlock30(ByteBuffer buffer, TapePlayback listener) {
        int len = buffer.get() & 0xFF;
        byte[] text = new byte[len];
        buffer.get(text);
        listener.onTextDescription(new String(text, StandardCharsets.US_ASCII));
    }

    /**
     * Block 0x31 - Message Block.
     */
    private void parseBlock31(ByteBuffer buffer, TapePlayback listener) {
        int displayTime = buffer.get() & 0xFF;
        int len = buffer.get() & 0xFF;
        byte[] msg = new byte[len];
        buffer.get(msg);
        listener.onMessage(new String(msg, StandardCharsets.US_ASCII), displayTime);
    }

    /**
     * Block 0x32 - Archive Info.
     */
    private void parseBlock32(ByteBuffer buffer, TapePlayback listener) {
        buffer.getShort(); // block length
        int numStrings = buffer.get() & 0xFF;
        List<String[]> entries = new ArrayList<>();
        for (int i = 0; i < numStrings; i++) {
            int type = buffer.get() & 0xFF;
            int len = buffer.get() & 0xFF;
            byte[] text = new byte[len];
            buffer.get(text);
            String typeName = type < ARCHIVE_INFO_TYPES.length ? ARCHIVE_INFO_TYPES[type] : "Unknown (" + type + ")";
            entries.add(new String[]{typeName, new String(text, StandardCharsets.US_ASCII)});
        }
        listener.onArchiveInfo(entries);
    }

    /**
     * Block 0x33 - Hardware Type.
     */
    private void parseBlock33(ByteBuffer buffer, TapePlayback listener) {
        int count = buffer.get() & 0xFF;
        List<int[]> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int hwType = buffer.get() & 0xFF;
            int hwId = buffer.get() & 0xFF;
            int hwInfo = buffer.get() & 0xFF;
            entries.add(new int[]{hwType, hwId, hwInfo});
        }
        listener.onHardwareInfo(entries);
    }

    /**
     * Block 0x35 - Custom Info Block.
     */
    private void parseBlock35(ByteBuffer buffer, TapePlayback listener) {
        byte[] id = new byte[16];
        buffer.get(id);
        int len = buffer.getInt();
        byte[] data = new byte[len];
        buffer.get(data);
        listener.onCustomInfo(new String(id, StandardCharsets.US_ASCII).trim(), data);
    }

    /**
     * Block 0x5A - Glue Block.
     */
    private void parseBlock5A(ByteBuffer buffer, TapePlayback listener) {
        buffer.position(buffer.position() + 9); // skip 9 bytes
        listener.onGlueBlock();
    }

    /**
     * Read 3-byte unsigned integer (little-endian).
     */
    private int readUint24(ByteBuffer buffer) {
        int b0 = buffer.get() & 0xFF;
        int b1 = buffer.get() & 0xFF;
        int b2 = buffer.get() & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16);
    }
}
