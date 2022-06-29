package net.emustudio.plugins.device.mits88dcdd.cpmfs.entry;

import net.jcip.annotations.Immutable;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static net.emustudio.emulib.runtime.helpers.NumberUtils.bcd2bin;
import static net.emustudio.emulib.runtime.helpers.NumberUtils.bin2bcd;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmFile.ENTRY_SIZE;


// http://gaby.de/ftp/pub/cpm/znode51/articles/tcj/tcj35bmm.ws
// Z80DOS, the finely-tuned final product is fully compatible with BackGrounder ii, NZ-COM and ZCPR34.
// ZSDOS is, foremost, an up-to-date DOS.  It fully supports the established DateStamper standard

// BackGrounder ii, as many readers of Jay Sage's column know, is a task-switching operating system
// system extension of CP/M 2.2, ZSDOS, and ZRDOS.

/**
 * CP/M Native datestamps entry
 *
 * It holds date/time stamps for previous 3 files. It is placed at each 4th place in a sector.
 * Used in P2DOS and CP/M Plus, maybe ZSDOS.
 *
 * Sources:
 * - <a href="https://www.cpm8680.com/cpmtools/cpm.htm">https://www.cpm8680.com/cpmtools/cpm.htm</a>
 *
 * Structure:
 * 21 C1 C1 C1 C1 M1 M1 M1 M1 00 00 C2 C2 C2 C2 M2
 * M2 M2 M2 00 00 C3 C3 C3 C3 M3 M3 M3 M3 00 00 00
 *
 * C1 = File 1 Create date/time
 * M1 = File 1 Modify date/time
 * C2 = File 2 Create date/time
 * M2 = File 2 Modify date/time
 * C3 = File 3 Create date/time
 * M3 = File 3 Modify date/time
 *
 * The format of a date/time entry is:
 *
 *         DW      day     ;Julian day number, stored low byte first.
 *                         ;Day 1 = 1 Jan 1978.
 *         DB      hour    ;BCD hour, eg 13h => 13:xx
 *         DB      min     ;BCD minute
 */
@Immutable
public class CpmNativeDate2 {
    public final static int STATUS_DATESTAMP = 0x21;

    private final static LocalDate FIRST_DAY = LocalDate.of(1978, 1, 1);
    private final static int DATES_PER_ENTRY = 3;

    public final byte status; // must be 0x21
    public final FileNativeDate2[] dates;

    public CpmNativeDate2(byte status, FileNativeDate2[] fileNativeDates) {
        if (fileNativeDates.length != DATES_PER_ENTRY) {
            throw new IllegalArgumentException("Expected " + DATES_PER_ENTRY + " file dates");
        }
        this.status = status;
        this.dates = fileNativeDates;
    }

    public static CpmNativeDate2 fromEntry(ByteBuffer entry) {
        byte status = entry.get();
        if (status != STATUS_DATESTAMP) {
            throw new IllegalArgumentException("Invalid entry status (expected " + STATUS_DATESTAMP + ")");
        }

        // * 21 C1 C1 C1 C1 M1 M1 M1 M1 00 00 C2 C2 C2 C2 M2
        // * M2 M2 M2 00 00 C3 C3 C3 C3 M3 M3 M3 M3 00 00 00

        FileNativeDate2[] dates = new FileNativeDate2[DATES_PER_ENTRY];
        for (int i = 0; i < DATES_PER_ENTRY; i++) {
            //;Julian day number, stored low byte first. Day 1 = 1 Jan 1978.
            int createDate = (entry.get() | (entry.get() << 8)) & 0xFF; // TODO: not bcd here ??
            int createHour = bcd2bin(entry.get() & 0xFF);
            int createMinute = bcd2bin(entry.get() & 0xFF);
            int modifyDate = (entry.get() | (entry.get() << 8)) & 0xFF;
            int modifyHour = bcd2bin(entry.get() & 0xFF);
            int modifyMinute = bcd2bin(entry.get() & 0xFF);
            entry.get();
            entry.get();

            dates[i] = new FileNativeDate2(
                createDate, createHour, createMinute, modifyDate, modifyHour, modifyMinute
            );
        }
        return new CpmNativeDate2(status, dates);
    }

    public ByteBuffer toEntry() {
        ByteBuffer entry = ByteBuffer.allocate(ENTRY_SIZE);
        entry.put(status);
        for (int i = 0; i < DATES_PER_ENTRY; i++) {
            FileNativeDate2 date = dates[i];

            entry.put((byte) (date.createDate & 0xFF));
            entry.put((byte) (date.createDate >>> 8));
            entry.put((byte) bin2bcd(date.createHour & 0xFF));
            entry.put((byte) bin2bcd(date.createMinute & 0xFF));
            entry.put((byte) (date.modifyDate & 0xFF));
            entry.put((byte) (date.modifyDate >>> 8));
            entry.put((byte) bin2bcd(date.modifyHour & 0xFF));
            entry.put((byte) bin2bcd(date.modifyMinute & 0xFF));
            entry.put((byte)0);
            entry.put((byte)0);
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

    public static class FileNativeDate2 implements CpmDatestamp {
        public final int createDate; // Julian day; day1 = 1 Jan 1978
        public final int createHour;
        public final int createMinute;
        public final int modifyDate; // Julian day; day1 = 1 Jan 1978
        public final int modifyHour; // not in BCD (the rest as well)
        public final int modifyMinute;

        public final LocalDateTime createDateTime;
        public final LocalDateTime modifyDateTime;
        public final LocalDateTime accessDateTime = FIRST_DAY.atTime(0, 0);

        public FileNativeDate2(int createDate, int createHour, int createMinute,
                               int modifyDate, int modifyHour, int modifyMinute) {
            this.createDate = createDate;
            this.createHour = createHour;
            this.createMinute = createMinute;
            this.modifyDate = modifyDate;
            this.modifyHour = modifyHour;
            this.modifyMinute = modifyMinute;

            this.createDateTime = FIRST_DAY.plusDays(createDate).atTime(0, 0);
            this.modifyDateTime = FIRST_DAY.plusDays(modifyDate).atTime(modifyHour, modifyMinute);
        }

        public String toString() {
            return "create=" + createDateTime + ", modify=" + modifyDateTime;
        }

        @Override
        public LocalDateTime getCreateDateTime() {
            return createDateTime;
        }

        @Override
        public LocalDateTime getModifyDateTime() {
            return modifyDateTime;
        }

        @Override
        public LocalDateTime getAccessDateTime() {
            return accessDateTime;
        }
    }
}
