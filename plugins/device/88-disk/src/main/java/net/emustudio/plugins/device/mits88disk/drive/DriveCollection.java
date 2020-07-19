package net.emustudio.plugins.device.mits88disk.drive;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class DriveCollection implements Iterable<Drive> {
    private final static int DRIVES_COUNT = 16;
    private final List<Drive> drives = new ArrayList<>();

    private volatile int currentDrive;

    public DriveCollection() {
        for (int i = 0; i < DRIVES_COUNT; i++) {
            drives.add(new Drive(i));
        }

        this.currentDrive = DRIVES_COUNT;
    }

    public void destroy() {
        drives.clear();
    }

    public Optional<Drive> getCurrentDrive() {
        return (currentDrive >= DRIVES_COUNT || currentDrive < 0) ?
            Optional.empty() : Optional.of(drives.get(currentDrive));
    }

    public void setCurrentDrive(int index) {
        if (index < 0 || index >= DRIVES_COUNT) {
            throw new IllegalArgumentException("Index of drive must be between 0 and " + DRIVES_COUNT);
        }
        currentDrive = index;
    }

    public void unsetCurrentDrive() {
        currentDrive = DRIVES_COUNT;
    }

    public Iterator<Drive> iterator() {
        return drives.iterator();
    }

    public Drive get(int index) {
        return drives.get(index);
    }

    public void foreach(BiFunction<Integer, Drive, Void> function) {
        int i = 0;
        for (Drive drive : drives) {
            function.apply(i, drive);
            i++;
        }
    }
}
