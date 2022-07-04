package net.emustudio.plugins.device.mits88dcdd.cpmfs.entry;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.DateFormat;
import net.jcip.annotations.Immutable;

import java.nio.ByteBuffer;
import java.util.Objects;

import static net.emustudio.emulib.runtime.helpers.NumberUtils.bcd2bin;
import static net.emustudio.emulib.runtime.helpers.NumberUtils.bin2bcd;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.DateFormat.NATIVE;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.DateFormat.NATIVE2;
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
 * <p>
 * It holds date/time stamps for previous 3 files. It is placed at each 4th place in a sector.
 * Used in CP/M Plus or ZDOS.
 * <p>
 * Sources:
 * - <a href="https://manpages.debian.org/testing/cpmtools/cpm.5.en.html">https://manpages.debian.org/testing/cpmtools/cpm.5.en.html</a>
 * - <a href="https://www.seasip.info/Cpm/format22.html">https://www.seasip.info/Cpm/format22.html</a>
 * <p>
 * Structure:
 * 21 00 C1 C1 M1 M1 M1 M1 A1 A1 A1 A1 C2 C2 M2 M2    !...............
 * M2 M2 A2 A2 A2 A2 C3 C3 M3 M3 M3 M3 A3 A3 A3 A3    ................
 * <p>
 * C1 = File 1 Create date
 * M1 = File 1 Modify date/time
 * A1 = File 1 Access date/time
 * C2 = File 2 Create date
 * M2 = File 2 Modify date/time
 * A2 = File 2 Access date/time
 * C3 = File 3 Create date
 * M3 = File 3 Modify date/time
 * A3 = File 3 Access date/time
 * <p>
 * The format of a date/time entry is:
 * <p>
 * DW      day     ;Julian day number, stored low byte first.
 * ;Day 1 = 1 Jan 1978.
 * DB      hour    ;BCD hour, eg 13h => 13:xx
 * DB      min     ;BCD minute
 */
@Immutable
public class CpmNativeDate implements CpmEntry {
    public final static int STATUS_DATESTAMP = 0x21;

    public final static int CREATE = 0;
    public final static int MODIFY = 1;
    public final static int ACCESS = 2;

    public final DateStamp[][] datestamps;
    public final DateFormat format;

    public CpmNativeDate(DateStamp[] file1, DateStamp[] file2, DateStamp[] file3, DateFormat format) {
        if ((format != NATIVE) && (format != NATIVE2)) {
            throw new IllegalArgumentException("Expected NATIVE or NATIVE2 date format!");
        }

        this.format = Objects.requireNonNull(format);
        this.datestamps = new DateStamp[][]{
            Objects.requireNonNull(file1),
            Objects.requireNonNull(file2),
            Objects.requireNonNull(file3)
        };
    }

    public ByteBuffer toEntry() {
        switch (format) {
            case NATIVE:
                return toEntryNative();
            case NATIVE2:
                return toEntryNative2();
            default:
                throw new IllegalStateException("This entry can be just NATIVE or NATIVE2");
        }
    }

    public static CpmNativeDate fromEntry(ByteBuffer entry, DateFormat format) {
        switch (format) {
            case NATIVE:
                return fromEntryNative(entry);
            case NATIVE2:
                return fromEntryNative2(entry);
            default:
                throw new IllegalArgumentException("Expected NATIVE or NATIVE2 date format!");
        }
    }

    private static CpmNativeDate fromEntryNative(ByteBuffer entry) {
        byte status = entry.get();
        if (status != STATUS_DATESTAMP) {
            throw new IllegalArgumentException("Invalid entry status (expected " + STATUS_DATESTAMP + ")");
        }
        entry.position(2);
        DateStamp[] file1 = parseNativeFile(entry);
        DateStamp[] file2 = parseNativeFile(entry);
        DateStamp[] file3 = parseNativeFile(entry);

        return new CpmNativeDate(file1, file2, file3, NATIVE);
    }

    private static CpmNativeDate fromEntryNative2(ByteBuffer entry) {
        byte status = entry.get();
        if (status != STATUS_DATESTAMP) {
            throw new IllegalArgumentException("Invalid entry status (expected " + STATUS_DATESTAMP + ")");
        }

        DateStamp[] file1 = parseNative2File(entry);
        DateStamp[] file2 = parseNative2File(entry);
        DateStamp[] file3 = parseNative2File(entry);

        return new CpmNativeDate(file1, file2, file3, NATIVE2);
    }

    private static DateStamp[] parseNativeFile(ByteBuffer entry) {
        DateStamp create = new DateStamp(
            (entry.get() | (entry.get() << 8)) & 0xFF,
            0, 0
        );
        DateStamp modify = parseDateStamp(entry);
        DateStamp access = parseDateStamp(entry);
        return new DateStamp[]{create, modify, access};
    }

    private static DateStamp[] parseNative2File(ByteBuffer entry) {
        DateStamp create = parseDateStamp(entry);
        DateStamp modify = parseDateStamp(entry);
        entry.get();
        entry.get();
        return new DateStamp[]{create, modify, DateStamp.EMPTY};
    }

    private static DateStamp parseDateStamp(ByteBuffer entry) {
        return new DateStamp(
            (entry.get() | (entry.get() << 8)) & 0xFF,
            bcd2bin(entry.get() & 0xFF),
            bcd2bin(entry.get() & 0xFF)
        );
    }

    private ByteBuffer toEntryNative() {
        ByteBuffer entry = ByteBuffer.allocate(ENTRY_SIZE);
        entry.put((byte) STATUS_DATESTAMP);
        entry.put((byte) 0);
        for (DateStamp[] ds : datestamps) {
            entry.put((byte) (ds[CREATE].days & 0xFF));
            entry.put((byte) (ds[CREATE].days >>> 8));
            entry.put((byte) (ds[MODIFY].days & 0xFF));
            entry.put((byte) (ds[MODIFY].days >>> 8));
            entry.put((byte) bin2bcd(ds[MODIFY].hour & 0xFF));
            entry.put((byte) bin2bcd(ds[MODIFY].minute & 0xFF));
            entry.put((byte) (ds[ACCESS].days & 0xFF));
            entry.put((byte) (ds[ACCESS].days >>> 8));
            entry.put((byte) bin2bcd(ds[ACCESS].hour & 0xFF));
            entry.put((byte) bin2bcd(ds[ACCESS].minute & 0xFF));
        }
        entry.position(0);
        entry.limit(ENTRY_SIZE);
        return entry;
    }

    private ByteBuffer toEntryNative2() {
        ByteBuffer entry = ByteBuffer.allocate(ENTRY_SIZE);
        entry.put((byte) STATUS_DATESTAMP);
        for (DateStamp[] ds : datestamps) {
            entry.put((byte) (ds[CREATE].days & 0xFF));
            entry.put((byte) (ds[CREATE].days >>> 8));
            entry.put((byte) bin2bcd(ds[CREATE].hour & 0xFF));
            entry.put((byte) bin2bcd(ds[CREATE].minute & 0xFF));
            entry.put((byte) (ds[MODIFY].days & 0xFF));
            entry.put((byte) (ds[MODIFY].days >>> 8));
            entry.put((byte) bin2bcd(ds[MODIFY].hour & 0xFF));
            entry.put((byte) bin2bcd(ds[MODIFY].minute & 0xFF));
            entry.put((byte) 0);
            entry.put((byte) 0);
        }
        entry.position(0);
        entry.limit(ENTRY_SIZE);
        return entry;
    }
}
