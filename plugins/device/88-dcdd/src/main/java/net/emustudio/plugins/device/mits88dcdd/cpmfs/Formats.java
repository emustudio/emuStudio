package net.emustudio.plugins.device.mits88dcdd.cpmfs;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFormat.RECORD_SIZE;

public class Formats {
    public static final Map<String, CpmFormat> FORMATS = new HashMap<>();

    // Altair 8" floppy
    // Some Info: https://altairclone.com/downloads/roms/M%20Eberhard%20Improved%20ROMs/CDBL%20Manual.pdf
    // However... what matters only is the track number
    public final static CpmFormat ALTAIR_CPM_2_2 = new CpmFormat(
        new DiskParameterBlock(32, 3, 7, 254, 255, 0xFF, 6),
        77, 137, 17, false, CpmFormat.DateStampFormat.NONE,
        new CpmFormat.SectorOps() {

            @Override
            public ByteBuffer toSector(ByteBuffer record, Position position) {
                ByteBuffer sector = ByteBuffer.allocate(137);
                sector.put((byte) (position.track | 0x80)); // Track Number, with MSB set (the sync bit)
                sector.put((byte) ((position.sector * 17) % 32)); // sector number; or used/unused size...?
                sector.put((byte) 0);
                sector.put(record);

                record.position(0);
                int checksum = 0;
                for (int i = 0; i < 128; i++) {
                    checksum = (checksum + record.get()) & 0xFF;
                }
                sector.put((byte) 0xFF); // stop byte
                sector.put((byte) checksum);

                record.position(0);
                sector.position(0);
                sector.limit(sector.capacity());
                return sector;
            }

            @Override
            public ByteBuffer toRecord(ByteBuffer sector) {
                sector.position(3);
                byte[] record = new byte[RECORD_SIZE];
                sector.get(record);
                sector.position(0);
                return ByteBuffer.wrap(record);
            }
        });

    static {
        FORMATS.put("altair_floppy", ALTAIR_CPM_2_2);
    }
}
