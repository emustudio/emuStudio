package net.emustudio.plugins.device.mits88dcdd.cpmfs.entry;

import net.jcip.annotations.Immutable;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static net.emustudio.emulib.runtime.helpers.NumberUtils.bcd2bin;
import static net.emustudio.emulib.runtime.helpers.NumberUtils.bin2bcd;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmFile.ENTRY_SIZE;


// Another source (https://www.cpm8680.com/cpmtools/cpm.htm) says:
// P2DOS and CP/M Plus support time stamps, which are stored in each fourth directory entry.
// This entry contains the time stamps for the extents using the previous three directory entries.
// Note that you really have time stamps for each extent, no matter if it is the first extent of a file
// or not. The structure of time stamp entries is:
// 1 byte status 0x21
// 8 bytes time stamp for third-last directory entry
// 2 bytes unused
// 8 bytes time stamp for second-last directory entry
// 2 bytes unused
// 8 bytes time stamp for last directory entry
//

// 1+10+10+8 = 29

// A time stamp consists of two dates: Creation and modification date (the latter being recorded when the
// file is closed). CP/M Plus further allows optionally to record the access instead of creation date as
// first time stamp.

// 2 bytes (little-endian) days starting with 1 at 01-01-1978
// 1 byte hour in BCD format
// 1 byte minute in BCD format

// http://gaby.de/ftp/pub/cpm/znode51/articles/tcj/tcj35bmm.ws
// Z80DOS, the finely-tuned final product is fully compatible with BackGrounder ii, NZ-COM and ZCPR34.
// ZSDOS is, foremost, an up-to-date DOS.  It fully supports the established DateStamper standard

// BackGrounder ii, as many readers of Jay Sage's column know, is a task-switching operating system
// system extension of CP/M 2.2, ZSDOS, and ZRDOS.

/**
 * CP/M Native datestamps entry
 *
 * It holds date/time stamps for previous 3 files. It is placed at each 4th place in a sector.
 * Used in CP/M Plus or ZDOS.
 *
 * Sources:
 * - <a href="https://manpages.debian.org/testing/cpmtools/cpm.5.en.html">https://manpages.debian.org/testing/cpmtools/cpm.5.en.html</a>
 * - <a href="https://www.seasip.info/Cpm/format22.html">https://www.seasip.info/Cpm/format22.html</a>
 *
 * Structure:
 * 21 00 C1 C1 M1 M1 M1 M1 A1 A1 A1 A1 C2 C2 M2 M2    !...............
 * M2 M2 A2 A2 A2 A2 C3 C3 M3 M3 M3 M3 A3 A3 A3 A3    ................
 *
 * C1 = File 1 Create date
 * M1 = File 1 Modify date/time
 * A1 = File 1 Access date/time
 * C2 = File 2 Create date
 * M2 = File 2 Modify date/time
 * A2 = File 2 Access date/time
 * C3 = File 3 Create date
 * M3 = File 3 Modify date/time
 * A3 = File 3 Access date/time
 *
 * The format of a date/time entry is:
 *
 *         DW      day     ;Julian day number, stored low byte first.
 *                         ;Day 1 = 1 Jan 1978.
 *         DB      hour    ;BCD hour, eg 13h => 13:xx
 *         DB      min     ;BCD minute
 */
@Immutable
public class CpmNativeDate {
    public final static int STATUS_DATESTAMP = 0x21;

    private final static LocalDate FIRST_DAY = LocalDate.of(1978, 1, 1);
    private final static int DATES_PER_ENTRY = 3;

    public final byte status; // must be 0x21
    public final FileNativeDate[] dates;

    public CpmNativeDate(byte status, FileNativeDate[] fileNativeDates) {
        if (fileNativeDates.length != DATES_PER_ENTRY) {
            throw new IllegalArgumentException("Expected " + DATES_PER_ENTRY + " file dates");
        }
        this.status = status;
        this.dates = fileNativeDates;
    }

    public static CpmNativeDate fromEntry(ByteBuffer entry) {
        byte status = entry.get();
        if (status != STATUS_DATESTAMP) {
            throw new IllegalArgumentException("Invalid entry status (expected " + STATUS_DATESTAMP + ")");
        }
        entry.position(2);

        FileNativeDate[] dates = new FileNativeDate[DATES_PER_ENTRY];
        for (int i = 0; i < DATES_PER_ENTRY; i++) {
            //;Julian day number, stored low byte first. Day 1 = 1 Jan 1978.
            int createDate = (entry.get() | (entry.get() << 8)) & 0xFF; // TODO: not bcd here ??
            int modifyDate = (entry.get() | (entry.get() << 8)) & 0xFF;
            int modifyHour = bcd2bin(entry.get() & 0xFF);
            int modifyMinute = bcd2bin(entry.get() & 0xFF);
            int accessDate = (entry.get() | (entry.get() << 8)) & 0xFF;
            int accesssHour = bcd2bin(entry.get() & 0xFF);
            int accessMinute = bcd2bin(entry.get() & 0xFF);

            dates[i] = new FileNativeDate(
                createDate, modifyDate, modifyHour, modifyMinute, accessDate, accesssHour, accessMinute
            );
        }
        return new CpmNativeDate(status, dates);
    }

    public ByteBuffer toEntry() {
        ByteBuffer entry = ByteBuffer.allocate(ENTRY_SIZE);
        entry.put((byte) status);
        entry.put((byte) 0);
        for (int i = 0; i < DATES_PER_ENTRY; i++) {
            FileNativeDate date = dates[i];

            entry.put((byte) (date.createDate & 0xFF));
            entry.put((byte) (date.createDate >>> 8));
            entry.put((byte) (date.modifyDate & 0xFF));
            entry.put((byte) (date.modifyDate >>> 8));
            entry.put((byte) bin2bcd(date.modifyHour & 0xFF));
            entry.put((byte) bin2bcd(date.modifyMinute & 0xFF));
            entry.put((byte) (date.accessDate & 0xFF));
            entry.put((byte) (date.accessDate >>> 8));
            entry.put((byte) bin2bcd(date.accessHour & 0xFF));
            entry.put((byte) bin2bcd(date.accessMinute & 0xFF));
        }
        entry.position(0);
        entry.limit(ENTRY_SIZE);
        return entry;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < DATES_PER_ENTRY; i++) {
            builder.append(i + 1).append(". ").append(dates[i].toString()).append("\n");
        }
        return builder.toString();
    }

    public static class FileNativeDate {
        public final int createDate; // Julian day; day1 = 1 Jan 1978
        public final int modifyDate; // Julian day; day1 = 1 Jan 1978
        public final int modifyHour; // not in BCD (the rest as well)
        public final int modifyMinute;
        public final int accessDate; // Julian day; day1 = 1 Jan 1978
        public final int accessHour;
        public final int accessMinute;

        public final LocalDateTime modifyDateTime;
        public final LocalDateTime accessDateTime;

        public FileNativeDate(int createDate, int modifyDate, int modifyHour, int modifyMinute,
                              int accessDate, int accessHour, int accessMinute) {
            this.createDate = createDate;
            this.modifyDate = modifyDate;
            this.modifyHour = modifyHour;
            this.modifyMinute = modifyMinute;
            this.accessDate = accessDate;
            this.accessHour = accessHour;
            this.accessMinute = accessMinute;
            this.modifyDateTime = FIRST_DAY.atTime(modifyHour, modifyMinute);
            this.accessDateTime = FIRST_DAY.atTime(accessHour, accessMinute);
        }

        public String toString() {
            return "create=" + FIRST_DAY.plusDays(createDate) + ", modify=" + modifyDateTime + ", access=" + accessDateTime;
        }
    }
}
