package net.emustudio.plugins.memory.bytemem.loaders;

import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public interface Loader {

    Map<String, Loader> IMAGE_LOADERS = Map.of(
            "hex", new HexLoader(),
            "tap", new TapLoader(),
            "tzx", new TzxLoader(),
            "bin", new BinaryLoader(),
            "com", new BinaryLoader(),
            "out", new BinaryLoader()
    );

    /**
     * Determines if this loader/format is aware of memory addresses (so user is or is not allowed to choose memory
     * address when loading the file).
     *
     * @return true if the loader is aware of memory addresses; false otherwise
     */
    boolean isMemoryAddressAware();

    /**
     * Loads an image file to memory
     *
     * @param path   image file path
     * @param memory memory context
     * @param bank   memory bank + address
     */
    void load(Path path, ByteMemoryContext memory, MemoryBank bank) throws Exception;

    class MemoryBank {
        final int bank;
        final int address;

        public MemoryBank(int bank, int address) {
            this.bank = bank;
            this.address = address;
        }

        public static MemoryBank of(int bank, int address) {
            return new MemoryBank(bank, address);
        }
    }


    static Loader createLoader(Path path) {
        int index = path.toString().lastIndexOf(".");
        String extension = (index == -1) ?
                "" : path.toString().substring(index + 1).toLowerCase(Locale.ENGLISH);

        return IMAGE_LOADERS
                .entrySet()
                .stream()
                .filter(l -> l.getKey().equals(extension))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(new BinaryLoader()); // unknown/no extension
    }
}
