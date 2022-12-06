package net.emustudio.plugins.device.mits88dcdd;

import net.emustudio.emulib.runtime.settings.BasicSettings;
import net.jcip.annotations.Immutable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class DiskSettings {
    public final static int DEFAULT_CPU_PORT1 = 0x8;
    public final static int DEFAULT_CPU_PORT2 = 0x9;
    public final static int DEFAULT_CPU_PORT3 = 0xA;
    public final static int DEFAULT_INTERRUPT_VECTOR = 7;
    public final static boolean DEFAULT_INTERRUPTS_SUPPORTED = true;
    public final static int DEFAULT_SECTORS_PER_TRACK = 32;
    public final static int DEFAULT_SECTOR_SIZE = 137;

    private static final String KEY_PORT1_CPU = "port1CPU";
    private static final String KEY_PORT2_CPU = "port2CPU";
    private static final String KEY_PORT3_CPU = "port3CPU";
    private static final String KEY_SECTORS_PER_TRACK = "sectorsPerTrack";
    private static final String KEY_SECTOR_SIZE = "sectorSize";
    private static final String KEY_IMAGE = "image";
    private static final String KEY_IMAGE_MOUNTED = "imageMounted";
    private static final String KEY_INTERRUPT_VECTOR = "interruptVector";
    private static final String KEY_INTERRUPTS_SUPPORTED = "interruptsSupported";

    private final BasicSettings settings;
    private final List<SettingsObserver> observers = new CopyOnWriteArrayList<>();

    private volatile int port1CPU;
    private volatile int port2CPU;
    private volatile int port3CPU;
    private volatile int interruptVector;
    private volatile boolean interruptsSupported;
    private final DriveSettings[] driveSettings = new DriveSettings[16];

    @FunctionalInterface
    public interface SettingsObserver {

        void settingsChanged();
    }

    @Immutable
    public static class DriveSettings {
        public final static DriveSettings DEFAULT = new DriveSettings(
            DEFAULT_SECTOR_SIZE, DEFAULT_SECTORS_PER_TRACK, null, false);

        public final int sectorSize;
        public final int sectorsPerTrack;
        public final String imagePath;
        public final boolean mounted;

        public DriveSettings(int sectorSize, int sectorsPerTrack, String imagePath, boolean mounted) {
            this.sectorSize = sectorSize;
            this.sectorsPerTrack = sectorsPerTrack;
            this.imagePath = imagePath;
            this.mounted = mounted;
        }
    }

    public DiskSettings(BasicSettings settings) {
        this.settings = Objects.requireNonNull(settings);

        this.port1CPU = settings.getInt(KEY_PORT1_CPU, DEFAULT_CPU_PORT1);
        this.port2CPU = settings.getInt(KEY_PORT2_CPU, DEFAULT_CPU_PORT2);
        this.port3CPU = settings.getInt(KEY_PORT3_CPU, DEFAULT_CPU_PORT3);
        this.interruptVector = settings.getInt(KEY_INTERRUPT_VECTOR, DEFAULT_INTERRUPT_VECTOR);
        this.interruptsSupported = settings.getBoolean(KEY_INTERRUPTS_SUPPORTED, DEFAULT_INTERRUPTS_SUPPORTED);

        for (int i = 0; i < driveSettings.length; i++) {
            driveSettings[i] = new DriveSettings(
                settings.getInt(KEY_SECTOR_SIZE + i, DEFAULT_SECTOR_SIZE),
                settings.getInt(KEY_SECTORS_PER_TRACK + i, DEFAULT_SECTORS_PER_TRACK),
                settings.getString(KEY_IMAGE + i, null),
                settings.getBoolean(KEY_IMAGE_MOUNTED + i, false));
        }
    }

    public void addObserver(SettingsObserver observer) {
        observers.add(observer);
    }

    public void clearObservers() {
        observers.clear();
    }

    public DriveSettings getDriveSettings(int drive) {
        return driveSettings[drive];
    }

    public void setDriveSettings(int drive, DriveSettings driveSettings) {
        if (drive < 0 || drive > 15) {
            throw new IllegalArgumentException("88-DCDD: Only drive from 0-15 is allowed");
        }

        this.driveSettings[drive] = driveSettings;
        settings.setInt(KEY_SECTOR_SIZE + drive, driveSettings.sectorSize);
        settings.setInt(KEY_SECTORS_PER_TRACK + drive, driveSettings.sectorsPerTrack);
        if (driveSettings.imagePath == null) {
            settings.remove(KEY_IMAGE + drive);
        } else {
            settings.setString(KEY_IMAGE + drive, driveSettings.imagePath);
        }
        settings.setBoolean(KEY_IMAGE_MOUNTED + drive, driveSettings.mounted);
        notifySettingsChanged();
    }

    public int getPort1CPU() {
        return port1CPU;
    }

    public void setPort1CPU(int port1CPU) {
        if (port1CPU < 0 || port1CPU > 0xFF) {
            throw new IllegalArgumentException("88-DCDD: CPU port 1 allowed range: 0-255");
        }
        this.port1CPU = port1CPU;
        settings.setInt(KEY_PORT1_CPU, port1CPU);
        notifySettingsChanged();
    }

    public int getPort2CPU() {
        return port2CPU;
    }

    public void setPort2CPU(int port2CPU) {
        if (port2CPU < 0 || port2CPU > 0xFF) {
            throw new IllegalArgumentException("88-DCDD: CPU port 2 allowed range: 0-255");
        }
        this.port2CPU = port2CPU;
        settings.setInt(KEY_PORT2_CPU, port2CPU);
        notifySettingsChanged();
    }

    public int getPort3CPU() {
        return port3CPU;
    }

    public void setPort3CPU(int port3CPU) {
        if (port3CPU < 0 || port3CPU > 0xFF) {
            throw new IllegalArgumentException("88-DCDD: CPU port 3 allowed range: 0-255");
        }
        this.port3CPU = port3CPU;
        settings.setInt(KEY_PORT3_CPU, port3CPU);
        notifySettingsChanged();
    }

    public int getInterruptVector() {
        return interruptVector;
    }

    public void setInterruptVector(int interruptVector) {
        if (interruptVector < 0 || interruptVector > 7) {
            throw new IllegalArgumentException("88-DCDD: Interrupt vector must be in range 0-7");
        }
        this.interruptVector = interruptVector;
        settings.setInt(KEY_INTERRUPT_VECTOR, interruptVector);
        notifySettingsChanged();
    }

    public boolean getInterruptsSupported() {
        return interruptsSupported;
    }

    public void setInterruptsSupported(boolean value) {
        this.interruptsSupported = value;
        settings.setBoolean(KEY_INTERRUPTS_SUPPORTED, value);
        notifySettingsChanged();
    }

    private void notifySettingsChanged() {
        observers.forEach(SettingsObserver::settingsChanged);
    }
}
