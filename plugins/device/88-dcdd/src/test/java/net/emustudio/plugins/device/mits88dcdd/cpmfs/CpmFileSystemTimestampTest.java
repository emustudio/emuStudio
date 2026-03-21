/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.cpmfs;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.sectorops.SectorOps;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.Assert.*;

/**
 * Integration tests for CpmFileSystem with NATIVE2 timestamp format.
 * Verifies that timestamps are written and cleared during file operations.
 */
public class CpmFileSystemTimestampTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private CpmFormat cpmFormat;
    private Path imageFile;

    /**
     * Create a CP/M format with NATIVE2 timestamps.
     * With timestamps, every 4th directory entry is an SFCB, so only 3 out of 4 entries per record
     * are usable for files.
     */
    @Before
    public void setup() throws IOException {
        DiskParameterBlock dpb = DiskParameterBlock.fromBSH(16, 16, 3, 59, 31, 0x80, 0, 2);
        cpmFormat = new CpmFormat("test-ts", dpb, 128, Optional.of(1), Optional.empty(),
                SectorOps.DUMMY, false, DateFormat.NATIVE2);

        imageFile = folder.newFile("test-ts.dsk").toPath();
        java.nio.file.Files.delete(imageFile);
        DriveIO.format(imageFile, cpmFormat);
    }

    private CpmFileSystem openFs() throws IOException {
        return new CpmFileSystem(new DriveIO(imageFile, cpmFormat, READ, WRITE));
    }

    @Test
    public void testWriteFileWithTimestamps() throws IOException {
        CpmFileSystem fs = openFs();
        fs.writeFile("A.TXT", "hello");
        assertTrue(fs.exists("A.TXT"));
    }

    @Test
    public void testListNativeDatesAfterWrite() throws IOException {
        CpmFileSystem fs = openFs();
        fs.writeFile("TS.TXT", "data");

        List<String> dates = fs.listNativeDates();
        assertFalse("Native dates should be present after writing a file", dates.isEmpty());
        assertTrue(dates.get(0).contains("TS.TXT"));
    }

    @Test
    public void testTimestampsAreNonZeroAfterWrite() throws IOException {
        CpmFileSystem fs = openFs();
        fs.writeFile("NEW.TXT", "content");

        List<String> dates = fs.listNativeDates();
        assertFalse(dates.isEmpty());
        String dateStr = dates.get(0);
        // Format: "    NEW.TXT : create | modify | access"
        // For NATIVE2, access is always empty (epoch). Check that the modify field (2nd) has current year.
        String[] parts = dateStr.split("\\|");
        assertTrue("Should have 3 date parts", parts.length >= 2);
        // Modify timestamp (parts[1]) should not contain 1978 epoch
        String modifyPart = parts[1].trim();
        assertFalse("Modify timestamp should not be from epoch", modifyPart.contains("-78 "));
    }

    @Test
    public void testRemoveFileClearsTimestamp() throws IOException {
        CpmFileSystem fs = openFs();
        fs.writeFile("DEL.TXT", "to-delete");

        List<String> datesBefore = fs.listNativeDates();
        assertFalse(datesBefore.isEmpty());

        fs.removeFile("DEL.TXT");

        List<String> datesAfter = fs.listNativeDates();
        // After removal, no valid file dates should reference DEL.TXT
        for (String d : datesAfter) {
            assertFalse("Removed file should not have dates", d.contains("DEL.TXT"));
        }
    }

    @Test
    public void testMultipleFilesWithTimestamps() throws IOException {
        CpmFileSystem fs = openFs();
        fs.writeFile("F1.TXT", "one");
        fs.writeFile("F2.TXT", "two");
        fs.writeFile("F3.TXT", "three");

        assertEquals(3, fs.listExistingFiles().count());
        List<String> dates = fs.listNativeDates();
        // Should have date entries for all 3 files
        assertEquals(3, dates.size());
    }

    @Test
    public void testWriteAndReadWithTimestamps() throws IOException {
        CpmFileSystem fs = openFs();
        String content = "timestamp test content";
        fs.writeFile("TST.TXT", content);

        String read = fs.readFile("TST.TXT");
        assertTrue(read.startsWith(content));
    }

    @Test
    public void testRemoveOneFilePreservesOtherTimestamps() throws IOException {
        CpmFileSystem fs = openFs();
        fs.writeFile("KEEP.TXT", "keep");
        fs.writeFile("GONE.TXT", "gone");

        fs.removeFile("GONE.TXT");

        assertTrue(fs.exists("KEEP.TXT"));
        assertFalse(fs.exists("GONE.TXT"));

        List<String> dates = fs.listNativeDates();
        boolean keepFound = dates.stream().anyMatch(d -> d.contains("KEEP.TXT"));
        assertTrue("KEEP.TXT timestamp should still be present", keepFound);
    }
}

