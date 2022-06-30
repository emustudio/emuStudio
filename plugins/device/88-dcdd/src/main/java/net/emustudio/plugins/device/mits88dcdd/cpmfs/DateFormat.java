package net.emustudio.plugins.device.mits88dcdd.cpmfs;

// https://www.seasip.info/Cpm/dosses.html
public enum DateFormat {
    NOT_USED,

    /**
     * 21 00 C1 C1 M1 M1 M1 M1 A1 A1 A1 A1 C2 C2 M2 M2
     * M2 M2 A2 A2 A2 A2 C3 C3 M3 M3 M3 M3 A3 A3 A3 A3
     */
    NATIVE, // P2DOS  and CP/M Plus; every 4th entry

    /**
     * 21 C1 C1 C1 C1 M1 M1 M1 M1 00 00 C2 C2 C2 C2 M2
     * M2 M2 M2 00 00 C3 C3 C3 C3 M3 M3 M3 M3 00 00 00
     */
    NATIVE2, // ZSDOS and CP/M Plus; every 4th entry

    /**
     * CP/M 3, sometimes ZSDOS
     */
    DATE_STAMPER // !!!TIME&.DAT; in 1st entry
}
