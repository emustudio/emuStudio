/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.cpmfs;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmFile;
import net.emustudio.plugins.device.mits88dcdd.cpmfs.sectorops.SectorOps;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFileSystem.STATUS_UNUSED;
import static org.junit.Assert.*;

/**
 * Integration tests for CpmFileSystem using a formatted disk image.
 * Uses DUMMY sector ops (sector = record, no prefix/suffix) for simplicity.
 */
public class CpmFileSystemTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private CpmFormat cpmFormat;
    private Path imageFile;

    /**
     * Create a small CP/M 2.2-like format with DUMMY sector ops.
     * bsh=3 (1K blocks), dsm=59, drm=31, spt=16, ofs=2, sectorSize=128 (DUMMY)
     * al0=0x80 => 1 directory block (block 0)
     */
    @Before
    public void setup() throws IOException {
        DiskParameterBlock dpb = DiskParameterBlock.fromBSH(16, 16, 3, 59, 31, 0x80, 0, 2);
        cpmFormat = new CpmFormat("test", dpb, 128, Optional.of(1), Optional.empty(),
                SectorOps.DUMMY, false, DateFormat.NOT_USED);

        imageFile = folder.newFile("test.dsk").toPath();
        java.nio.file.Files.delete(imageFile); // DriveIO.format expects non-existing file
        DriveIO.format(imageFile, cpmFormat);
    }

    private CpmFileSystem openFs() throws IOException {
        return new CpmFileSystem(new DriveIO(imageFile, cpmFormat, READ, WRITE));
    }

    @Test
    public void testFormatCreatesImage() {
        assertTrue(java.nio.file.Files.exists(imageFile));
    }

    @Test
    public void testFormattedDiskHasNoFiles() throws IOException {
        CpmFileSystem fs = openFs();
        assertEquals(0, fs.listExistingFiles().count());
    }

    @Test
    public void testWriteAndReadTextFile() throws IOException {
        CpmFileSystem fs = openFs();
        String content = "Hello, CP/M world!";
        fs.writeFile("HELLO.TXT", content);

        String read = fs.readFile("HELLO.TXT");
        assertTrue(read.startsWith(content));
    }

    @Test
    public void testWriteAndReadBinaryFile() throws IOException {
        CpmFileSystem fs = openFs();
        byte[] data = new byte[256];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i & 0xFF);
        }
        fs.writeFileBytes("DATA.BIN", data);

        byte[] read = fs.readFileBytes("DATA.BIN");
        // read may have trailing padding, so compare only the first data.length bytes
        assertTrue(read.length >= data.length);
        for (int i = 0; i < data.length; i++) {
            assertEquals("byte at index " + i, data[i], read[i]);
        }
    }

    @Test
    public void testFileExistsAfterWrite() throws IOException {
        CpmFileSystem fs = openFs();
        assertFalse(fs.exists("TEST.COM"));
        fs.writeFile("TEST.COM", "data");
        assertTrue(fs.exists("TEST.COM"));
    }

    @Test
    public void testExistsCaseInsensitive() throws IOException {
        CpmFileSystem fs = openFs();
        fs.writeFile("MY.TXT", "hello");
        assertTrue(fs.exists("my.txt"));
        assertTrue(fs.exists("MY.TXT"));
    }

    @Test(expected = IOException.class)
    public void testWriteDuplicateFileThrows() throws IOException {
        CpmFileSystem fs = openFs();
        fs.writeFile("X.Y", "a");
        fs.writeFile("X.Y", "b"); // should throw
    }

    @Test
    public void testListExistingFiles() throws IOException {
        CpmFileSystem fs = openFs();
        fs.writeFile("A.TXT", "aa");
        fs.writeFile("B.COM", "bb");

        List<String> names = fs.listExistingFiles()
                .map(CpmFile::getFileName)
                .collect(Collectors.toList());
        assertEquals(2, names.size());
        assertTrue(names.contains("A.TXT"));
        assertTrue(names.contains("B.COM"));
    }

    @Test
    public void testRemoveFile() throws IOException {
        CpmFileSystem fs = openFs();
        fs.writeFile("DEL.ME", "content");
        assertTrue(fs.exists("DEL.ME"));

        fs.removeFile("DEL.ME");
        assertFalse(fs.exists("DEL.ME"));
    }

    @Test
    public void testRemoveNonExistentFileDoesNothing() throws IOException {
        CpmFileSystem fs = openFs();
        fs.writeFile("KEEP.TXT", "data");
        fs.removeFile("NOPE.TXT"); // should not throw
        assertTrue(fs.exists("KEEP.TXT"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadNonExistentFileThrows() throws IOException {
        CpmFileSystem fs = openFs();
        fs.readFile("NOFILE.TXT");
    }

    @Test
    public void testListValidFilesIncludesAllExtents() throws IOException {
        CpmFileSystem fs = openFs();
        fs.writeFile("A.TXT", "small");

        long total = fs.listValidFiles().count();
        long existing = fs.listExistingFiles().count();
        // For a small file, total == existing (1 extent)
        assertTrue(total >= existing);
    }

    @Test
    public void testGetLabelReturnsEmptyOnFormattedDisk() throws IOException {
        CpmFileSystem fs = openFs();
        assertEquals("", fs.getLabel());
    }

    @Test
    public void testListPasswordsReturnsEmptyOnFormattedDisk() throws IOException {
        CpmFileSystem fs = openFs();
        assertTrue(fs.listPasswords().isEmpty());
    }

    @Test
    public void testListNativeDatesReturnsEmptyWhenNotUsed() throws IOException {
        CpmFileSystem fs = openFs();
        assertTrue(fs.listNativeDates().isEmpty());
    }

    @Test
    public void testWriteMultipleFiles() throws IOException {
        CpmFileSystem fs = openFs();
        for (int i = 0; i < 5; i++) {
            fs.writeFile("F" + i + ".TXT", "content" + i);
        }
        assertEquals(5, fs.listExistingFiles().count());
    }

    @Test
    public void testRemoveOneOfMultipleFiles() throws IOException {
        CpmFileSystem fs = openFs();
        fs.writeFile("A.TXT", "a");
        fs.writeFile("B.TXT", "b");
        fs.writeFile("C.TXT", "c");

        fs.removeFile("B.TXT");

        assertTrue(fs.exists("A.TXT"));
        assertFalse(fs.exists("B.TXT"));
        assertTrue(fs.exists("C.TXT"));
        assertEquals(2, fs.listExistingFiles().count());
    }

    @Test
    public void testWriteAfterRemoveReusesSpace() throws IOException {
        CpmFileSystem fs = openFs();
        fs.writeFile("OLD.TXT", "old data");
        fs.removeFile("OLD.TXT");
        fs.writeFile("NEW.TXT", "new data");

        assertTrue(fs.exists("NEW.TXT"));
        String read = fs.readFile("NEW.TXT");
        assertTrue(read.startsWith("new data"));
    }
}

