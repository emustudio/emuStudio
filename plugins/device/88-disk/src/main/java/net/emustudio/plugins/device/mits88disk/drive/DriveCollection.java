package net.emustudio.plugins.device.mits88disk.drive;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class DriveCollection implements Iterable<Drive> {
    private final static int DRIVES_COUNT = 16;
    private final List<Drive> drives = new ArrayList<>();

    private volatile int currentDrive;

    public DriveCollection() {
        for (int i = 0; i < DRIVES_COUNT; i++) {
            drives.add(new Drive(i));
        }

        this.currentDrive = 0;  // TODO: what should be here?
    }

    public void destroy() {
        drives.clear();
    }

    public Drive getCurrentDrive() {
        return drives.get(currentDrive);
    }

    public void setCurrentDrive(int index) {
        currentDrive = index;
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
