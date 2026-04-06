/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player.loaders;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static org.easymock.EasyMock.*;

public class TzxLoaderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * Writes the TZX 10-byte file header.
     */
    private void writeTzxFileHeader(ByteArrayOutputStream out) {
        out.writeBytes("ZXTape!".getBytes()); // 7 bytes signature
        out.write(0x1A); // end of text marker
        out.write(1); // major version
        out.write(20); // minor version
    }

    /**
     * Creates a TZX header block (flag=0).
     * Block structure: id (1) + pause (2) + blockLength (2) + flag (1) + TapTzxHeader (17) + checksum (1)
     */
    private void writeTzxHeaderBlock(ByteArrayOutputStream out, int blockId, int pause,
                                     int headerId, String fileName, int dataLength, int param1, int param2) {
        ByteBuffer buf = ByteBuffer.allocate(24);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) blockId);       // block type ID
        buf.putShort((short) pause);    // pause
        buf.putShort((short) 19);       // blockLength (flag + 17 header bytes + checksum)
        buf.put((byte) 0);             // flag = 0 => header

        // TapTzxHeader: 17 bytes
        buf.put((byte) headerId);
        byte[] nameBytes = new byte[10];
        byte[] src = fileName.getBytes();
        System.arraycopy(src, 0, nameBytes, 0, Math.min(src.length, 10));
        for (int i = src.length; i < 10; i++) {
            nameBytes[i] = ' ';
        }
        buf.put(nameBytes);
        buf.putShort((short) dataLength);
        buf.putShort((short) param1);
        buf.putShort((short) param2);

        buf.put((byte) 0); // checksum
        out.writeBytes(buf.array());
    }

    /**
     * Creates a TZX data block (flag=0xFF).
     * Block structure: id (1) + pause (2) + blockLength (2) + flag (1) + data (N) + checksum (1)
     */
    private void writeTzxDataBlock(ByteArrayOutputStream out, int blockId, int pause, byte[] data) {
        int blockLength = data.length + 2; // flag + data + ... actually blockLength - 2 = data.length
        ByteBuffer buf = ByteBuffer.allocate(6 + data.length + 1);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) blockId);
        buf.putShort((short) pause);
        buf.putShort((short) blockLength);
        buf.put((byte) 0xFF); // flag = data
        buf.put(data);
        buf.put((byte) 0); // checksum
        out.writeBytes(buf.array());
    }

    private File writeTzxFile(byte[] content) throws IOException {
        File file = tempFolder.newFile("test.tzx");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content);
        }
        return file;
    }

    @Test
    public void testLoadProgramHeader() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);
        writeTzxHeaderBlock(out, 0x10, 1000, 0, "TestProg", 100, 10, 50);
        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onHeaderStart();
        playback.onBlockFlag(0);
        playback.onProgram(eq("TestProg  "), eq(100), eq(10), eq(50));
        playback.onBlockChecksum(anyByte());
        playback.onPause(1000);
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    @Test
    public void testLoadNumberArrayHeader() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);
        int variable = ('D' << 8);
        writeTzxHeaderBlock(out, 0x10, 500, 1, "NumArr", 200, variable, 0);
        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onHeaderStart();
        playback.onBlockFlag(0);
        playback.onNumberArray(eq("NumArr    "), eq(200), eq('D'));
        playback.onBlockChecksum(anyByte());
        playback.onPause(500);
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    @Test
    public void testLoadStringArrayHeader() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);
        int variable = ('M' << 8);
        writeTzxHeaderBlock(out, 0x10, 500, 2, "StrArr", 300, variable, 0);
        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onHeaderStart();
        playback.onBlockFlag(0);
        playback.onStringArray(eq("StrArr    "), eq(300), eq('M'));
        playback.onBlockChecksum(anyByte());
        playback.onPause(500);
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    @Test
    public void testLoadMemoryBlockHeader() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);
        writeTzxHeaderBlock(out, 0x10, 500, 3, "MemBlock", 512, 32768, 0);
        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onHeaderStart();
        playback.onBlockFlag(0);
        playback.onMemoryBlock(eq("MemBlock  "), eq(512), eq(32768));
        playback.onBlockChecksum(anyByte());
        playback.onPause(500);
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    @Test
    public void testLoadDataBlock() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);
        byte[] data = new byte[]{0x01, 0x02, 0x03, 0x04};
        writeTzxDataBlock(out, 0x10, 1000, data);
        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onDataStart();
        playback.onBlockFlag(255);
        playback.onBlockData(aryEq(data));
        playback.onBlockChecksum(anyByte());
        playback.onPause(1000);
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    @Test(expected = IOException.class)
    public void testInvalidSignatureThrows() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("INVALID".getBytes());
        out.write(0x1A);
        out.write(1);
        out.write(20);
        File tzxFile = writeTzxFile(out.toByteArray());

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        Loader.TapePlayback playback = niceMock(Loader.TapePlayback.class);
        replay(playback);
        loader.load(playback);
    }

    @Test(expected = IOException.class)
    public void testInvalidEndOfTextMarkerThrows() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("ZXTape!".getBytes());
        out.write(0x00); // wrong marker
        out.write(1);
        out.write(20);
        File tzxFile = writeTzxFile(out.toByteArray());

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        Loader.TapePlayback playback = niceMock(Loader.TapePlayback.class);
        replay(playback);
        loader.load(playback);
    }

    @Test(expected = NullPointerException.class)
    public void testNullPathThrows() {
        new TzxLoader(null);
    }

    @Test(expected = IOException.class)
    public void testLoadNonExistentFileThrows() throws IOException {
        TzxLoader loader = new TzxLoader(tempFolder.getRoot().toPath().resolve("nonexistent.tzx"));
        Loader.TapePlayback playback = niceMock(Loader.TapePlayback.class);
        replay(playback);
        loader.load(playback);
    }

    @Test
    public void testLoadHeaderAndDataBlock() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);
        writeTzxHeaderBlock(out, 0x10, 500, 0, "Program", 100, 10, 50);
        byte[] data = new byte[]{(byte) 0xAA, (byte) 0xBB};
        writeTzxDataBlock(out, 0x10, 1000, data);
        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        // Header block
        playback.onHeaderStart();
        playback.onBlockFlag(0);
        playback.onProgram(eq("Program   "), eq(100), eq(10), eq(50));
        playback.onBlockChecksum(anyByte());
        playback.onPause(500);
        // Data block
        playback.onDataStart();
        playback.onBlockFlag(255);
        playback.onBlockData(aryEq(data));
        playback.onBlockChecksum(anyByte());
        playback.onPause(1000);
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    @Test
    public void testEmptyFileWithHeaderOnly() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);
        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x11 - Turbo Speed Data ====================

    @Test
    public void testBlock11TurboSpeedData() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        byte[] data = new byte[]{0x01, 0x02, 0x03};
        ByteBuffer buf = ByteBuffer.allocate(1 + 15 + 3 + data.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x11);
        buf.putShort((short) 2168);  // pilot pulse
        buf.putShort((short) 667);   // sync1
        buf.putShort((short) 735);   // sync2
        buf.putShort((short) 855);   // zero bit
        buf.putShort((short) 1710);  // one bit
        buf.putShort((short) 3223);  // pilot count
        buf.put((byte) 8);           // used bits in last byte
        buf.putShort((short) 1000);  // pause after
        // 3-byte length (little-endian)
        buf.put((byte) (data.length & 0xFF));
        buf.put((byte) ((data.length >> 8) & 0xFF));
        buf.put((byte) ((data.length >> 16) & 0xFF));
        buf.put(data);
        out.write(buf.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onTurboSpeedData(eq(2168), eq(667), eq(735), eq(855), eq(1710), eq(3223), eq(8), eq(1000), aryEq(data));
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x12 - Pure Tone ====================

    @Test
    public void testBlock12PureTone() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        ByteBuffer buf = ByteBuffer.allocate(5);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x12);
        buf.putShort((short) 2168);  // pulse length
        buf.putShort((short) 5000);  // pulse count
        out.write(buf.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onPureTone(2168, 5000);
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x13 - Pulse Sequence ====================

    @Test
    public void testBlock13PulseSequence() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        ByteBuffer buf = ByteBuffer.allocate(1 + 1 + 3 * 2);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x13);
        buf.put((byte) 3);          // 3 pulses
        buf.putShort((short) 667);   // pulse 1
        buf.putShort((short) 735);   // pulse 2
        buf.putShort((short) 954);   // pulse 3
        out.write(buf.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onPulseSequence(aryEq(new int[]{667, 735, 954}));
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x14 - Pure Data ====================

    @Test
    public void testBlock14PureData() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        byte[] data = new byte[]{(byte) 0xAA, (byte) 0x55};
        ByteBuffer buf = ByteBuffer.allocate(1 + 10 + data.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x14);
        buf.putShort((short) 855);   // zero bit
        buf.putShort((short) 1710);  // one bit
        buf.put((byte) 8);           // used bits in last byte
        buf.putShort((short) 500);   // pause after
        buf.put((byte) (data.length & 0xFF));
        buf.put((byte) ((data.length >> 8) & 0xFF));
        buf.put((byte) ((data.length >> 16) & 0xFF));
        buf.put(data);
        out.write(buf.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onPureData(eq(855), eq(1710), eq(8), eq(500), aryEq(data));
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x15 - Direct Recording ====================

    @Test
    public void testBlock15DirectRecording() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        byte[] samples = new byte[]{(byte) 0xFF, (byte) 0x00};
        ByteBuffer buf = ByteBuffer.allocate(1 + 8 + samples.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x15);
        buf.putShort((short) 79);    // T-states per sample
        buf.putShort((short) 1000);  // pause after
        buf.put((byte) 8);           // used bits in last byte
        buf.put((byte) (samples.length & 0xFF));
        buf.put((byte) ((samples.length >> 8) & 0xFF));
        buf.put((byte) ((samples.length >> 16) & 0xFF));
        buf.put(samples);
        out.write(buf.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onDirectRecording(eq(79), eq(1000), eq(8), aryEq(samples));
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x20 - Pause ====================

    @Test
    public void testBlock20Pause() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        ByteBuffer buf = ByteBuffer.allocate(3);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x20);
        buf.putShort((short) 2000);
        out.write(buf.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onPause(2000);
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    @Test
    public void testBlock20PauseZeroStopsTape() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        ByteBuffer buf = ByteBuffer.allocate(3);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x20);
        buf.putShort((short) 0);
        out.write(buf.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onPause(0);
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x21/0x22 - Group Start/End ====================

    @Test
    public void testBlock21And22Group() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        // Group start
        String groupName = "TestGroup";
        ByteBuffer buf = ByteBuffer.allocate(2 + groupName.length());
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x21);
        buf.put((byte) groupName.length());
        buf.put(groupName.getBytes());
        out.write(buf.array());

        // Group end
        out.write(0x22);

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onGroupStart("TestGroup");
        playback.onGroupEnd();
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x24/0x25 - Loop ====================

    @Test
    public void testBlock24And25Loop() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        // Loop start (3 iterations)
        ByteBuffer loopStart = ByteBuffer.allocate(3);
        loopStart.order(ByteOrder.LITTLE_ENDIAN);
        loopStart.put((byte) 0x24);
        loopStart.putShort((short) 3);
        out.write(loopStart.array());

        // A pure tone block inside the loop
        ByteBuffer tone = ByteBuffer.allocate(5);
        tone.order(ByteOrder.LITTLE_ENDIAN);
        tone.put((byte) 0x12);
        tone.putShort((short) 1000);
        tone.putShort((short) 10);
        out.write(tone.array());

        // Loop end
        out.write(0x25);

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        // Block should be executed 3 times
        playback.onPureTone(1000, 10);
        playback.onPureTone(1000, 10);
        playback.onPureTone(1000, 10);
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x2A - Stop if 48K ====================

    @Test
    public void testBlock2AStopIf48K() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        ByteBuffer buf = ByteBuffer.allocate(5);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x2A);
        buf.putInt(0); // block length always 0
        out.write(buf.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onStopIfIn48KMode();
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x2B - Set Signal Level ====================

    @Test
    public void testBlock2BSetSignalLevel() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        ByteBuffer buf = ByteBuffer.allocate(6);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x2B);
        buf.putInt(1); // block length = 1
        buf.put((byte) 1); // signal level = high
        out.write(buf.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onSetSignalLevel(1);
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x30 - Text Description ====================

    @Test
    public void testBlock30TextDescription() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        String text = "Hello World";
        ByteBuffer buf = ByteBuffer.allocate(2 + text.length());
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x30);
        buf.put((byte) text.length());
        buf.put(text.getBytes());
        out.write(buf.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onTextDescription("Hello World");
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x31 - Message ====================

    @Test
    public void testBlock31Message() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        String msg = "Press Play";
        ByteBuffer buf = ByteBuffer.allocate(3 + msg.length());
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x31);
        buf.put((byte) 5);            // display time
        buf.put((byte) msg.length());
        buf.put(msg.getBytes());
        out.write(buf.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onMessage("Press Play", 5);
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x32 - Archive Info ====================

    @Test
    public void testBlock32ArchiveInfo() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        // Build archive info block
        String title = "Test Game";
        String author = "J. Doe";
        // Entry: type(1) + len(1) + text
        int blockLength = 1 + (1 + 1 + title.length()) + (1 + 1 + author.length());

        ByteBuffer buf = ByteBuffer.allocate(1 + 2 + 1 + (1 + 1 + title.length()) + (1 + 1 + author.length()));
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x32);
        buf.putShort((short) blockLength);
        buf.put((byte) 2); // 2 entries
        // Entry 1: Title (type=0)
        buf.put((byte) 0);
        buf.put((byte) title.length());
        buf.put(title.getBytes());
        // Entry 2: Author (type=2)
        buf.put((byte) 2);
        buf.put((byte) author.length());
        buf.put(author.getBytes());
        out.write(buf.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = niceMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onArchiveInfo(anyObject(List.class));
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x33 - Hardware Type ====================

    @Test
    public void testBlock33HardwareType() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        ByteBuffer buf = ByteBuffer.allocate(1 + 1 + 3);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x33);
        buf.put((byte) 1);  // 1 entry
        buf.put((byte) 0);  // hw type
        buf.put((byte) 0);  // hw id
        buf.put((byte) 0);  // hw info
        out.write(buf.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = niceMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onHardwareInfo(anyObject(List.class));
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x35 - Custom Info ====================

    @Test
    public void testBlock35CustomInfo() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        byte[] customData = new byte[]{0x01, 0x02, 0x03};
        String idStr = "POKEs";
        byte[] idBytes = new byte[16];
        System.arraycopy(idStr.getBytes(), 0, idBytes, 0, idStr.length());

        ByteBuffer buf = ByteBuffer.allocate(1 + 16 + 4 + customData.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x35);
        buf.put(idBytes);
        buf.putInt(customData.length);
        buf.put(customData);
        out.write(buf.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onCustomInfo(eq("POKEs"), aryEq(customData));
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x5A - Glue ====================

    @Test
    public void testBlock5AGlue() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        ByteBuffer buf = ByteBuffer.allocate(10);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x5A);
        buf.put("XTape!".getBytes());
        buf.put((byte) 0x1A);
        buf.put((byte) 1);  // major
        buf.put((byte) 20); // minor
        out.write(buf.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onGlueBlock();
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x18 - CSW Recording ====================

    @Test
    public void testBlock18CswRecording() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        byte[] cswData = new byte[]{0x01, 0x02};
        // block length = 2(pause) + 3(sampleRate) + 1(compression) + 4(pulseCount) + data = 10 + data
        int blockLen = 10 + cswData.length;

        ByteBuffer buf = ByteBuffer.allocate(1 + 4 + blockLen);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x18);
        buf.putInt(blockLen);
        buf.putShort((short) 500);  // pause after
        // sample rate (3 bytes LE)
        buf.put((byte) (44100 & 0xFF));
        buf.put((byte) ((44100 >> 8) & 0xFF));
        buf.put((byte) ((44100 >> 16) & 0xFF));
        buf.put((byte) 1);         // RLE compression
        buf.putInt(100);            // stored pulse count
        buf.put(cswData);
        out.write(buf.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onCswRecording(eq(500), eq(44100), eq(1), eq(100L), aryEq(cswData));
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x19 - Generalized Data ====================

    @Test
    public void testBlock19GeneralizedData() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        byte[] genData = new byte[]{0x01, 0x02, 0x03, 0x04};
        // block length includes the pause (2 bytes) + genData
        int blockLen = 2 + genData.length;

        ByteBuffer buf = ByteBuffer.allocate(1 + 4 + blockLen);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0x19);
        buf.putInt(blockLen);
        buf.putShort((short) 1000); // pause after
        buf.put(genData);
        out.write(buf.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onGeneralizedData(eq(1000), aryEq(genData));
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== TZX Block 0x23 - Jump ====================

    @Test
    public void testBlock23JumpForward() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        // Block 0: Jump forward by 2 (skip block 1, go to block 2)
        ByteBuffer jump = ByteBuffer.allocate(3);
        jump.order(ByteOrder.LITTLE_ENDIAN);
        jump.put((byte) 0x23);
        jump.putShort((short) 2); // relative jump +2
        out.write(jump.array());

        // Block 1: Pure tone (should be skipped)
        ByteBuffer tone1 = ByteBuffer.allocate(5);
        tone1.order(ByteOrder.LITTLE_ENDIAN);
        tone1.put((byte) 0x12);
        tone1.putShort((short) 999);
        tone1.putShort((short) 99);
        out.write(tone1.array());

        // Block 2: Pure tone (should be executed)
        ByteBuffer tone2 = ByteBuffer.allocate(5);
        tone2.order(ByteOrder.LITTLE_ENDIAN);
        tone2.put((byte) 0x12);
        tone2.putShort((short) 2168);
        tone2.putShort((short) 50);
        out.write(tone2.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        // Only the second pure tone should be called
        playback.onPureTone(2168, 50);
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== Mixed blocks ====================

    @Test
    public void testMixedBlocks() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);

        // Text description
        String text = "Test";
        ByteBuffer textBuf = ByteBuffer.allocate(2 + text.length());
        textBuf.order(ByteOrder.LITTLE_ENDIAN);
        textBuf.put((byte) 0x30);
        textBuf.put((byte) text.length());
        textBuf.put(text.getBytes());
        out.write(textBuf.array());

        // Pure tone
        ByteBuffer tone = ByteBuffer.allocate(5);
        tone.order(ByteOrder.LITTLE_ENDIAN);
        tone.put((byte) 0x12);
        tone.putShort((short) 2168);
        tone.putShort((short) 100);
        out.write(tone.array());

        // Pause
        ByteBuffer pause = ByteBuffer.allocate(3);
        pause.order(ByteOrder.LITTLE_ENDIAN);
        pause.put((byte) 0x20);
        pause.putShort((short) 500);
        out.write(pause.array());

        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onTextDescription("Test");
        playback.onPureTone(2168, 100);
        playback.onPause(500);
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    // ==================== Block 0x10 with zero pause ====================

    @Test
    public void testBlock10WithZeroPause() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);
        byte[] data = new byte[]{0x01, 0x02};
        writeTzxDataBlock(out, 0x10, 0, data); // pause = 0
        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onFileStart();
        playback.onDataStart();
        playback.onBlockFlag(255);
        playback.onBlockData(aryEq(data));
        playback.onBlockChecksum(anyByte());
        // No onPause call when pause=0
        playback.onFileEnd();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }
}

