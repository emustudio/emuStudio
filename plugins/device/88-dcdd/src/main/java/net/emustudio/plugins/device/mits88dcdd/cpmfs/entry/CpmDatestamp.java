package net.emustudio.plugins.device.mits88dcdd.cpmfs.entry;

import java.time.LocalDateTime;

public interface CpmDatestamp {

    LocalDateTime getCreateDateTime();
    LocalDateTime getModifyDateTime();
    LocalDateTime getAccessDateTime();
}
