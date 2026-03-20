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
        playback.onHeaderStart();
        playback.onBlockFlag(0);
        playback.onProgram(eq("TestProg  "), eq(100), eq(10), eq(50));
        expectLastCall();
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    @Test
    public void testLoadNumberArrayHeader() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);
        int variable = ('D' << 8) | 0x00;
        writeTzxHeaderBlock(out, 0x10, 500, 1, "NumArr", 200, variable, 0);
        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onHeaderStart();
        playback.onBlockFlag(0);
        playback.onNumberArray(eq("NumArr    "), eq(200), eq('D'));
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }

    @Test
    public void testLoadStringArrayHeader() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTzxFileHeader(out);
        int variable = ('M' << 8) | 0x00;
        writeTzxHeaderBlock(out, 0x10, 500, 2, "StrArr", 300, variable, 0);
        File tzxFile = writeTzxFile(out.toByteArray());

        Loader.TapePlayback playback = createStrictMock(Loader.TapePlayback.class);
        playback.onHeaderStart();
        playback.onBlockFlag(0);
        playback.onStringArray(eq("StrArr    "), eq(300), eq('M'));
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
        playback.onHeaderStart();
        playback.onBlockFlag(0);
        playback.onMemoryBlock(eq("MemBlock  "), eq(512), eq(32768));
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
        playback.onDataStart();
        playback.onBlockFlag(255);
        playback.onBlockData(aryEq(data));
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
        // Header block
        playback.onHeaderStart();
        playback.onBlockFlag(0);
        playback.onProgram(eq("Program   "), eq(100), eq(10), eq(50));
        // Data block
        playback.onDataStart();
        playback.onBlockFlag(255);
        playback.onBlockData(aryEq(data));
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
        // no block callbacks expected
        replay(playback);

        TzxLoader loader = new TzxLoader(tzxFile.toPath());
        loader.load(playback);

        verify(playback);
    }
}

